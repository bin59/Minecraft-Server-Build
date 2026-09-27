# ============================================================
#  云端 MariaDB -> 本地 MySQL 增量同步脚本
#  核心策略：按主键增量合并，本地只增不删（云端删除旧数据不影响本地历史）
#   - 自增主键表：导出云端 "主键 > 本地最大值" 的新数据，INSERT IGNORE 导入本地
#   - 非自增主键/字典表：整表 INSERT IGNORE 合并（云端为准，本地缺失补齐，本地额外保留）
# ============================================================

. (Join-Path $PSScriptRoot "sync_config.ps1")

$ErrorActionPreference = "Continue"
if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir -Force | Out-Null }
$logStamp = Get-Date -Format "yyyyMMdd_HHmmss"
$logFile  = Join-Path $LogDir ("sync_{0}.log" -f $logStamp)

function Write-Log {
    param([string]$msg)
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $msg
    Write-Output $line
    Add-Content -LiteralPath $logFile -Value $line -Encoding UTF8
}

function Invoke-MysqlCloud {
    param([string]$sql)
    $env:MYSQL_PWD = $CloudPass
    $out = & $MysqlCli --user=$CloudUser --host=$CloudHost --port=$CloudPort --connect-timeout=20 -N -e $sql 2>&1
    Remove-Item Env:MYSQL_PWD
    return $out
}

function Invoke-MysqlLocal {
    param([string]$sql)
    $env:MYSQL_PWD = $LocalPass
    $out = & $MysqlCli --user=$LocalUser --host=$LocalHost --port=$LocalPort --protocol=tcp -N -e $sql 2>&1
    Remove-Item Env:MYSQL_PWD
    return $out
}

Write-Log "===== 同步开始 $logStamp ====="

# 测试两端连接
$cloudTest = Invoke-MysqlCloud "SELECT 1;"
if ($LASTEXITCODE -ne 0 -or "$cloudTest" -notmatch "1") {
    Write-Log "ERROR: 云端连接失败: $cloudTest"
    exit 1
}
Write-Log "云端连接 OK"
$localTest = Invoke-MysqlLocal "SELECT 1;"
if ($LASTEXITCODE -ne 0 -or "$localTest" -notmatch "1") {
    Write-Log "ERROR: 本地连接失败: $localTest"
    exit 1
}
Write-Log "本地连接 OK"

$syncTotalNew = 0

foreach ($db in $Databases) {
    Write-Log "---- 数据库: $db ----"

    # 云端该库下的表清单
    $cloudTables = @(Invoke-MysqlCloud "SHOW TABLES FROM ``$db``;")
    if ($cloudTables.Count -eq 0) { Write-Log "云端库 $db 无表或不可见"; continue }

    foreach ($tbl in $cloudTables) {
        $tbl = "$tbl".Trim()
        if (-not $tbl) { continue }
        $fullName = "``$db``.``$tbl``"

        # 获取云端主键信息（单列 + 是否自增）
        $pkInfo = Invoke-MysqlCloud "SELECT column_name, extra FROM information_schema.columns WHERE table_schema='$db' AND table_name='$tbl' AND column_key='PRI';"
        $pkCol = ""; $pkAuto = $false
        foreach ($row in $pkInfo) {
            $parts = $row -split "`t"
            if ($parts.Count -ge 2 -and $parts[0]) {
                $pkCol = $parts[0].Trim('`')
                if ($parts[1] -match "auto_increment") { $pkAuto = $true }
                break
            }
        }
        if (-not $pkCol) {
            Write-Log "跳过 ${fullName} (无主键)"
            continue
        }

        # 云端主键最大值
        $cMax = Invoke-MysqlCloud "SELECT COALESCE(MAX(``$pkCol``),0) FROM $fullName;"
        if ($LASTEXITCODE -ne 0) { Write-Log "ERROR: 读取云端 ${fullName} 失败"; continue }
        $cMaxVal = [int64]("$cMax".Trim())

        # 本地该表是否存在
        $localExists = Invoke-MysqlLocal "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$db' AND table_name='$tbl';"
        $lMaxVal = [int64]0

        if ("$localExists".Trim() -eq "1") {
            $lMax = Invoke-MysqlLocal "SELECT COALESCE(MAX(``$pkCol``),0) FROM $fullName;"
            if ($LASTEXITCODE -eq 0) { $lMaxVal = [int64]("$lMax".Trim()) }
        } else {
            Write-Log "本地缺失表 ${fullName}，先建结构"
            $env:MYSQL_PWD = $CloudPass
            $struct = & $Mysqldump --user=$CloudUser --host=$CloudHost --port=$CloudPort --no-data --skip-add-drop-table --skip-comments $db $tbl 2>$null
            $structRc = $LASTEXITCODE
            Remove-Item Env:MYSQL_PWD
            if ($structRc -eq 0) {
                $env:MYSQL_PWD = $LocalPass
                $struct -join "`n" | & $MysqlCli --user=$LocalUser --host=$LocalHost --port=$LocalPort --protocol=tcp $db 2>&1
                $importRc = $LASTEXITCODE
                Remove-Item Env:MYSQL_PWD
                Write-Log "已建表结构 ${fullName} (rc=$importRc)"
            } else {
                Write-Log "ERROR: 建表 ${fullName} 失败"; continue
            }
        }

        # 决定同步方式
        $dumpFile = Join-Path $env:TEMP ("sync_import_{0}_{1}.sql" -f $db, $tbl)
        if ($pkAuto) {
            # 自增主键 -> 增量
            $newCount = $cMaxVal - $lMaxVal
            if ($newCount -le 0) {
                Write-Log "${fullName}: 无增量 (本地max=$lMaxVal, 云端max=$cMaxVal)"
                continue
            }
            $where = "``$pkCol`` > $lMaxVal"
            Write-Log "${fullName}: 增量同步 pk>$lMaxVal (约 $newCount 行)..."
            $env:MYSQL_PWD = $CloudPass
            & $Mysqldump --user=$CloudUser --host=$CloudHost --port=$CloudPort --single-transaction --skip-add-drop-table --no-create-info --insert-ignore --result-file=$dumpFile --where="$where" $db $tbl 2>$null
            $dumpRc = $LASTEXITCODE
            Remove-Item Env:MYSQL_PWD
            if ($dumpRc -ne 0) { Write-Log "ERROR: 导出增量 ${fullName} 失败 (rc=$dumpRc)"; Remove-Item $dumpFile -Force -ErrorAction SilentlyContinue; continue }
        } else {
            # 非自增主键（字典/统计表）-> 整表 INSERT IGNORE
            Write-Log "${fullName}: 整表合并 (非自增主键)"
            $env:MYSQL_PWD = $CloudPass
            & $Mysqldump --user=$CloudUser --host=$CloudHost --port=$CloudPort --single-transaction --skip-add-drop-table --no-create-info --insert-ignore --result-file=$dumpFile $db $tbl 2>$null
            $dumpRc = $LASTEXITCODE
            Remove-Item Env:MYSQL_PWD
            if ($dumpRc -ne 0) { Write-Log "ERROR: 导出 ${fullName} 失败 (rc=$dumpRc)"; Remove-Item $dumpFile -Force -ErrorAction SilentlyContinue; continue }
        }

        # 导入本地（用 cmd /c 支持 < 重定向，对大数据文件稳定）
        if ((Test-Path $dumpFile) -and (Get-Item $dumpFile).Length -gt 0) {
            $env:MYSQL_PWD = $LocalPass
            $localCli = "`"$($MysqlCli -replace '\\','/')`""
            $impOut = cmd /c "$localCli --user=$LocalUser --host=$LocalHost --port=$LocalPort --protocol=tcp --default-character-set=utf8mb4 $db < `"$dumpFile`"" 2>&1
            $rc = $LASTEXITCODE
            Remove-Item Env:MYSQL_PWD
            Remove-Item -LiteralPath $dumpFile -Force -ErrorAction SilentlyContinue
            if ($rc -eq 0) {
                # 导入后核验本地主键最大值
                $newLocalMax = Invoke-MysqlLocal "SELECT COALESCE(MAX(``$pkCol``),0) FROM $fullName;"
                $added = 0
                if ($LASTEXITCODE -eq 0) {
                    $added = [int64]("$newLocalMax".Trim()) - $lMaxVal
                    if ($added -lt 0) { $added = 0 }
                }
                Write-Log "${fullName}: 导入完成 (本地max=$newLocalMax, 本次新增约 $added 行)"
                $syncTotalNew += $added
            } else {
                Write-Log "ERROR: 导入 ${fullName} 失败 (rc=$rc): $impOut"
            }
        } else {
            Remove-Item -LiteralPath $dumpFile -Force -ErrorAction SilentlyContinue
            Write-Log "${fullName}: 无数据可导"
        }
    }
}

Write-Log "===== 同步结束，共新增约 $syncTotalNew 行 ====="

# 云端 vs 本地 数据库完整性核验（含 Plan 库）
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$dbs = @('coreprotect', 'postracker', 'host21k5c4', 'Plan')

function Q-Cloud([string]$sql) {
    $env:MYSQL_PWD = 'pId5XpS5CcBi'
    $o = (& $mysql --user=host21k5c4 --host=123.207.3.73 --port=3306 -N -e $sql 2>$null)
    Remove-Item Env:MYSQL_PWD
    return $o
}
function Q-Local([string]$sql) {
    $env:MYSQL_PWD = 'zbmlp01.'
    $o = (& $mysql --user=root --host=127.0.0.1 --port=3306 --protocol=tcp -N -e $sql 2>$null)
    Remove-Item Env:MYSQL_PWD
    return $o
}

Write-Output "=================================================="
Write-Output " 云端 vs 本地 完整性核验 (含Plan库) "
Write-Output "=================================================="

$issues = 0
foreach ($db in $dbs) {
    Write-Output ""
    Write-Output "===== 数据库: $db ====="
    $cloudTables = @(Q-Cloud "SHOW TABLES FROM ``$db``;")
    $localTables = @(Q-Local "SHOW TABLES FROM ``$db``;")
    $cloudSet = @{}; $localSet = @{}
    foreach ($t in $cloudTables) { if ($t.Trim()) { $cloudSet[$t.Trim()] = $true } }
    foreach ($t in $localTables) { if ($t.Trim()) { $localSet[$t.Trim()] = $true } }
    Write-Output ("云端表数: {0}, 本地表数: {1}" -f $cloudSet.Count, $localSet.Count)

    $missInLocal = @($cloudSet.Keys | Where-Object { -not $localSet.ContainsKey($_) })
    if ($missInLocal.Count -gt 0) {
        Write-Output "  [!] 云端有但本地缺失: $($missInLocal -join ', ')"
        $issues++
    } else { Write-Output "  [OK] 云端表全部存在于本地" }

    foreach ($t in $cloudSet.Keys) {
        if (-not $localSet.ContainsKey($t)) { continue }
        $pk = Q-Cloud "SELECT c.column_name FROM information_schema.statistics s JOIN information_schema.columns c ON c.table_schema=s.table_schema AND c.table_name=s.table_name AND c.column_name=s.column_name WHERE s.table_schema='$db' AND s.table_name='$t' AND s.index_name='PRIMARY' AND s.seq_in_index=1 LIMIT 1;"
        $pk = "$pk".Trim()
        if (-not $pk) { Write-Output ("  [-] {0}: 无法确定主键" -f $t); continue }
        $qmax = "SELECT COALESCE(MAX($pk),0) FROM $db.$t"
        $qcnt = "SELECT COUNT(*) FROM $db.$t"
        $cm = ("$(Q-Cloud $qmax)").Trim(); $lm = ("$(Q-Local $qmax)").Trim()
        $cc = ("$(Q-Cloud $qcnt)").Trim(); $lc = ("$(Q-Local $qcnt)").Trim()
        $cmv=[int64]0;$lmv=[int64]0;$ccv=[int64]0;$lcv=[int64]0
        [int64]::TryParse($cm,[ref]$cmv)|Out-Null;[int64]::TryParse($lm,[ref]$lmv)|Out-Null
        [int64]::TryParse($cc,[ref]$ccv)|Out-Null;[int64]::TryParse($lc,[ref]$lcv)|Out-Null
        if ($lcv -ge $ccv) { $st="OK(本地>=云端)" }
        elseif ($cmv - $lmv -le 500) { $st="OK(云端新增未同步)" }
        else { $st="!! 可能遗漏"; $issues++ }
        Write-Output ("  {0,-22} max:云端={1,-10} 本地={2,-10} 行数:云端={3,-10} 本地={4,-10} {5}" -f $t,$cm,$lm,$cc,$lc,$st)
    }
}
Write-Output ""
Write-Output "=================================================="
if ($issues -eq 0) { Write-Output "结论: 未发现遗漏。" } else { Write-Output "结论: 发现 $issues 处可能遗漏。" }
Write-Output "=================================================="

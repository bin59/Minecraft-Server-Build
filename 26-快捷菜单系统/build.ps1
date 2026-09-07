<#
.SYNOPSIS
    QuickMenu 插件构建脚本（无需 Maven / Gradle）

.DESCRIPTION
    本脚本完成完整构建流水线：
      0. 自动定位 JDK 21+
      1. 下载编译依赖到 _build\libs（已存在则跳过）
      2. 编译 Java 源码（UTF-8，启用 -Xlint）
      3. 运行静态校验（配置结构 / 材质名 / 槽位冲突）
      4. 打包为可部署的 jar 到 _build\out
      5. 校验 plugin.yml 与主类一致性

    本脚本位置必须在「26-快捷菜单系统」目录下（与 plugin-src、_build 同级），
    因为内部路径均基于 $PSScriptRoot 推算。

    依赖版本已锁定为经实测验证的组合，请勿随意升级：
      - spigot-api 1.21.11-R0.1-SNAPSHOT  对应 Leaf 1.21.11 核心
      - floodgate api 2.2.5-SNAPSHOT
      - cumulus 1.1.2  ← 必须是 1.1.2，不可用 2.0.0-SNAPSHOT
        Floodgate 2.2.5 依赖此版本；2.0.0 缺少 cumulus.util.FormBuilder，
        会导致 sendForm 重载决议失败、编译报错（已实测踩坑）

.USAGE
    powershell -ExecutionPolicy Bypass -File build.ps1
    powershell -ExecutionPolicy Bypass -File build.ps1 -SkipDeps   # 离线时跳过下载
#>

param(
    [switch]$SkipDeps,
    # 清理上次构建的 classes 目录后再编译。
    # 默认不清理：javac 会覆盖同名 class，jar 会覆盖同名产物，
    # 仅在「删除或重命名过源文件」时需要清理，否则会残留旧 class。
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# ---------- 路径定义（均相对本脚本所在目录）----------
$Root      = $PSScriptRoot
$SrcRoot   = Join-Path $Root "plugin-src\src\main\java"
$ResRoot   = Join-Path $Root "plugin-src\src\main\resources"
$BuildRoot = Join-Path $Root "_build"
$LibDir    = Join-Path $BuildRoot "libs"
$ProbeDir  = Join-Path $BuildRoot "probe"
$ClassDir  = Join-Path $BuildRoot "classes"
$DistDir   = Join-Path $BuildRoot "out"
$JarName   = "QuickMenu-1.0.0.jar"
$JarPath   = Join-Path $DistDir $JarName

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  QuickMenu 插件构建" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 前置检查：目录结构是否正确
foreach ($p in @($SrcRoot, $ResRoot)) {
    if (-not (Test-Path $p)) {
        Write-Host "`n[错误] 目录结构不正确，未找到: $p" -ForegroundColor Red
        Write-Host "       本脚本必须放在「26-快捷菜单系统」目录下（与 plugin-src 同级）" -ForegroundColor Red
        exit 1
    }
}

# ---------- 步骤 0：定位 JDK ----------
Write-Host "`n[0/5] 定位 JDK ..." -ForegroundColor Yellow

$JavaHome = $null
$Candidates = @(
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Java\jdk-*",
    "C:\Program Files\Eclipse Adoptium\jdk-*",
    "C:\Program Files\Microsoft\jdk-*"
)
foreach ($c in $Candidates) {
    $resolved = Resolve-Path $c -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved -and (Test-Path (Join-Path $resolved.Path "bin\javac.exe"))) {
        $JavaHome = $resolved.Path
        break
    }
}

# 回退：从 PATH 上的 java.exe 反推 JDK 根目录
if (-not $JavaHome) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $binDir = Split-Path $javaCmd.Source -Parent
        if (Test-Path (Join-Path $binDir "javac.exe")) {
            $JavaHome = Split-Path $binDir -Parent
        }
    }
}

if (-not $JavaHome) {
    Write-Host "  [错误] 未找到 JDK（需要 JDK 21+；JRE 不含 javac 无法编译）" -ForegroundColor Red
    Write-Host "  处理方式：" -ForegroundColor Red
    Write-Host "    1) 安装 JDK 21 后重试" -ForegroundColor Red
    Write-Host "    2) 或修改本脚本顶部 Candidates 列表，加入你的 JDK 安装路径" -ForegroundColor Red
    exit 1
}

$javac  = Join-Path $JavaHome "bin\javac.exe"
$java   = Join-Path $JavaHome "bin\java.exe"
$jarExe = Join-Path $JavaHome "bin\jar.exe"

# javac -version 输出到 stdout，无需合并 stderr。
# 注意：PowerShell 5.1 下对原生命令使用 2>&1 会把 stderr 包装成 ErrorRecord，
# 叠加 $ErrorActionPreference="Stop" 会导致脚本意外终止，因此此处不加 2>&1。
$javacVer = ((& $javac -version) -join "").Trim()
Write-Host "  JDK 路径: $JavaHome"
Write-Host "  编译器  : $javacVer"

if ($javacVer -match "javac (\d+)") {
    if ([int]$Matches[1] -lt 21) {
        Write-Host "  [警告] JDK 版本低于 21，编译 api-version 1.21 的插件可能失败" -ForegroundColor Yellow
    }
}

# ---------- 步骤 1：下载依赖 ----------
New-Item -ItemType Directory -Force -Path $LibDir | Out-Null

$Dependencies = [ordered]@{
    "spigot-api.jar"       = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.21.11-R0.1-SNAPSHOT/spigot-api-1.21.11-R0.1-20251231.154832-9.jar"
    "floodgate-api.jar"    = "https://repo.opencollab.dev/main/org/geysermc/floodgate/api/2.2.5-SNAPSHOT/api-2.2.5-20260809.110940-20.jar"
    "cumulus-1.1.2.jar"    = "https://repo.opencollab.dev/main/org/geysermc/cumulus/cumulus/1.1.2/cumulus-1.1.2.jar"
    "annotations.jar"      = "https://repo1.maven.org/maven2/org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar"
    "guava-33.5.0-jre.jar" = "https://repo1.maven.org/maven2/com/google/guava/guava/33.5.0-jre/guava-33.5.0-jre.jar"
    "snakeyaml-2.2.jar"    = "https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar"
    "gson-2.13.2.jar"      = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar"
}

if ($SkipDeps) {
    Write-Host "`n[1/5] 检查依赖（已指定 -SkipDeps，不联网下载）..." -ForegroundColor Yellow
} else {
    Write-Host "`n[1/5] 准备编译依赖 ..." -ForegroundColor Yellow
}

foreach ($name in $Dependencies.Keys) {
    $dest = Join-Path $LibDir $name
    if (Test-Path $dest) {
        $kb = [math]::Round((Get-Item $dest).Length / 1KB, 1)
        Write-Host "  [跳过] $name 已存在 ($kb KB)"
        continue
    }
    if ($SkipDeps) {
        Write-Host "  [缺失] $name 不存在，但已指定 -SkipDeps，跳过下载" -ForegroundColor Yellow
        continue
    }
    Write-Host "  [下载] $name ..." -NoNewline
    try {
        Invoke-WebRequest -Uri $Dependencies[$name] -OutFile $dest -UseBasicParsing -TimeoutSec 180
        $kb = [math]::Round((Get-Item $dest).Length / 1KB, 1)
        Write-Host " 完成 ($kb KB)" -ForegroundColor Green
    } catch {
        Write-Host " 失败" -ForegroundColor Red
        Write-Host "    原因: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "    请检查网络连接，或手动下载后放到: $dest" -ForegroundColor Red
        exit 1
    }
}

# 必需依赖齐全性检查
$Required = @("spigot-api.jar", "floodgate-api.jar", "cumulus-1.1.2.jar", "annotations.jar")
$Missing = @()
foreach ($r in $Required) {
    if (-not (Test-Path (Join-Path $LibDir $r))) { $Missing += $r }
}
if ($Missing.Count -gt 0) {
    Write-Host "  [错误] 缺少必需依赖: $($Missing -join ', ')" -ForegroundColor Red
    Write-Host "         请去掉 -SkipDeps 重新运行以自动下载" -ForegroundColor Red
    exit 1
}

# classpath 组装
$SpigotApi    = Join-Path $LibDir "spigot-api.jar"
$FloodgateApi = Join-Path $LibDir "floodgate-api.jar"
$Cumulus      = Join-Path $LibDir "cumulus-1.1.2.jar"
$Annotations  = Join-Path $LibDir "annotations.jar"
$Guava        = Join-Path $LibDir "guava-33.5.0-jre.jar"
$SnakeYaml    = Join-Path $LibDir "snakeyaml-2.2.jar"
$Gson         = Join-Path $LibDir "gson-2.13.2.jar"

$CpCompile = "$SpigotApi;$Cumulus;$FloodgateApi;$Annotations"
$CpProbe   = "$SpigotApi;$Annotations;$Guava;$Gson;$SnakeYaml"

# 安全清理：仅删除经解析确认位于 _build 内的构建产物目录。
# 不使用通配符递归删除；目标路径必须是 $BuildRoot 的直接子目录，
# 否则视为路径异常并中止，避免误删源码或项目外文件。
function Remove-BuildDir {
    param([string]$Target)

    if (-not (Test-Path -LiteralPath $Target)) { return }

    # 解析为绝对字面路径（不使用变量拼接后的通配形式）
    $resolved = (Resolve-Path -LiteralPath $Target).Path
    $resolvedFull = [System.IO.Path]::GetFullPath($resolved)
    $buildFull = [System.IO.Path]::GetFullPath($BuildRoot)

    # 校验：必须是 _build 的直接子目录
    $parent = [System.IO.Path]::GetDirectoryName($resolvedFull)
    if ($parent -ne $buildFull) {
        Write-Host "  [中止] 拒绝清理，目标不在构建目录内: $resolvedFull" -ForegroundColor Red
        exit 1
    }

    Remove-Item -LiteralPath $resolvedFull -Recurse -Force
    if ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0) {
        Write-Host "  [中止] 清理失败，停止构建: $resolvedFull" -ForegroundColor Red
        exit 1
    }
}

# ---------- 步骤 2：编译插件 ----------
Write-Host "`n[2/5] 编译插件源码 ..." -ForegroundColor Yellow

if ($Clean) {
    Remove-BuildDir -Target $ClassDir
    Remove-BuildDir -Target $DistDir
    Write-Host "  已清理上次构建产物（-Clean）"
}
New-Item -ItemType Directory -Force -Path $ClassDir | Out-Null

$SourceFiles = @(Get-ChildItem $SrcRoot -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
if ($SourceFiles.Count -eq 0) {
    Write-Host "  [错误] 未找到任何 .java 源文件: $SrcRoot" -ForegroundColor Red
    exit 1
}
Write-Host "  源文件数: $($SourceFiles.Count)"

$CompileLog = Join-Path $BuildRoot "compile.log"

# javac 的警告与错误都走 stderr。PowerShell 5.1 下 2>&1 会把每行 stderr
# 包装成 ErrorRecord，叠加 $ErrorActionPreference="Stop" 会让脚本在
# 「编译成功但有警告」时误判为失败并终止。
# 解决：调用期间临时把错误偏好放宽为 Continue，结束后恢复。
$prevErrorAction = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    & $javac -encoding UTF-8 -Xlint:all,-serial,-deprecation -cp $CpCompile -d $ClassDir $SourceFiles 2>&1 |
        Out-File -FilePath $CompileLog -Encoding utf8
    $javacExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $prevErrorAction
}

if ($javacExit -ne 0) {
    Write-Host "  [错误] 编译失败，前 30 行错误信息：" -ForegroundColor Red
    Get-Content $CompileLog | Select-Object -First 30 | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
    Write-Host "  完整日志: $CompileLog" -ForegroundColor Red
    exit 1
}

$ClassCount = (Get-ChildItem $ClassDir -Recurse -Filter "*.class").Count
Write-Host "  [成功] 生成 $ClassCount 个 class 文件" -ForegroundColor Green

$WarnLines = @(Get-Content $CompileLog -ErrorAction SilentlyContinue | Where-Object { $_ -match "warning|警告" })
if ($WarnLines.Count -gt 0) {
    Write-Host "  [提示] 有 $($WarnLines.Count) 条 lint 警告，详见 _build\compile.log" -ForegroundColor Yellow
}

# ---------- 步骤 3：静态校验 ----------
Write-Host "`n[3/5] 静态校验 ..." -ForegroundColor Yellow

New-Item -ItemType Directory -Force -Path $ProbeDir | Out-Null
$ProbeFiles = @(Get-ChildItem $ProbeDir -Filter "*.java" -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName })

# 逐个独立编译探针：单个探针缺依赖或写错，不会拖垮其余探针，
# 避免「一个探针编译失败 -> 全部校验被静默跳过」的假通过。
$CompiledProbes = @()
if ($ProbeFiles.Count -gt 0) {
    Write-Host "  编译校验探针 ..."
    $prevErrorAction2 = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        foreach ($pf in $ProbeFiles) {
            $leaf = Split-Path $pf -Leaf
            & $javac -encoding UTF-8 -cp $CpProbe -d $ProbeDir $pf 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                $CompiledProbes += $leaf
                Write-Host "    [OK] $leaf" -ForegroundColor Green
            } else {
                Write-Host "    [跳过] $leaf 编译失败（不影响打包，但对应校验将不执行）" -ForegroundColor Yellow
            }
        }
    } finally {
        $ErrorActionPreference = $prevErrorAction2
    }
    Write-Host "  可用探针: $($CompiledProbes.Count)/$($ProbeFiles.Count)"
} else {
    Write-Host "  未发现校验探针，跳过" -ForegroundColor Yellow
}

# 3a. config.yml 结构与材质校验
# 判据用 .class 是否存在（编译成功才会生成），比布尔标记更权威
$ConfigYml = Join-Path $ResRoot "config.yml"
$ConfigOk = $true
if ((Test-Path (Join-Path $ProbeDir "ConfigProbe.class")) -and (Test-Path $ConfigYml)) {
    Write-Host "  校验 config.yml ..." -NoNewline
    $probeOut = & $java -cp "$CpProbe;$ProbeDir" ConfigProbe $ConfigYml
    if ($LASTEXITCODE -eq 0) {
        Write-Host " 通过" -ForegroundColor Green
        # 提取并显示警告行
        $probeOut | Where-Object { $_ -match "^\s+\[!\]" } | ForEach-Object {
            Write-Host "    $_" -ForegroundColor Yellow
        }
        $probeOut | Where-Object { $_ -match "^菜单项总数" } | ForEach-Object { Write-Host "    $_" }
    } else {
        Write-Host " 失败" -ForegroundColor Red
        $probeOut | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
        $ConfigOk = $false
    }
} else {
    Write-Host "  跳过 config.yml 校验（探针或配置文件缺失）" -ForegroundColor Yellow
}

if (-not $ConfigOk) {
    Write-Host "`n[中止] 配置校验未通过，请先修复 config.yml 再构建" -ForegroundColor Red
    exit 1
}

# ---------- 步骤 4：打包 jar ----------
Write-Host "`n[4/5] 打包 jar ..." -ForegroundColor Yellow

# 注意：不在此处删除 $DistDir。jar 命令会覆盖同名产物；
# 若需彻底清理请使用 -Clean 参数（走安全校验路径）。
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

# 资源文件放进 classes 根目录，随 jar 一并打包（plugin.yml 必须在 jar 根）
Copy-Item (Join-Path $ResRoot "plugin.yml") (Join-Path $ClassDir "plugin.yml") -Force
Copy-Item (Join-Path $ResRoot "config.yml") (Join-Path $ClassDir "config.yml") -Force

& $jarExe --create --file $JarPath -C $ClassDir .
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [错误] 打包失败" -ForegroundColor Red
    exit 1
}

$JarKB = [math]::Round((Get-Item $JarPath).Length / 1KB, 1)
Write-Host "  [成功] $JarName ($JarKB KB)" -ForegroundColor Green

# 发布副本：复制到模块根目录的 dist\ 下，方便直接取用部署。
# _build\ 是构建中间目录（含依赖库与 class），dist\ 才是给人用的发布位置。
$ReleaseDir = Join-Path $Root "dist"
New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null
$ReleaseJar = Join-Path $ReleaseDir $JarName
Copy-Item -LiteralPath $JarPath -Destination $ReleaseJar -Force
Write-Host "  [发布] 已复制到 dist\$JarName" -ForegroundColor Green

# ---------- 步骤 5：plugin.yml 与主类一致性 ----------
Write-Host "`n[5/5] 校验 plugin.yml 与主类一致性 ..." -ForegroundColor Yellow

if (Test-Path (Join-Path $ProbeDir "PluginYmlProbe.class")) {
    $ymlOut = & $java -cp "$CpProbe;$ProbeDir" PluginYmlProbe (Join-Path $ClassDir "plugin.yml") $JarPath
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [成功] 主类存在于 jar，commands / permissions 定义合法" -ForegroundColor Green
        $ymlOut | Where-Object { $_ -match "^(name|main|version|api-version)\s+=" } |
            ForEach-Object { Write-Host "    $_" }
    } else {
        Write-Host "  [错误] plugin.yml 校验未通过" -ForegroundColor Red
        $ymlOut | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
        exit 1
    }
} else {
    Write-Host "  跳过（探针不可用）" -ForegroundColor Yellow
}

# ---------- 完成 ----------
Write-Host "`n=============================================" -ForegroundColor Green
Write-Host "  构建完成" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  产物路径: $JarPath"
Write-Host "  部署方式: 复制到服务器 plugins\ 目录，然后重启服务器"
Write-Host "  首次启动会自动生成 plugins\QuickMenu\config.yml"
Write-Host ""

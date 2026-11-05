<#
.SYNOPSIS
    BedrockSkinRecorder 插件构建脚本（无需 Maven / Gradle）

.DESCRIPTION
    0. 自动定位 JDK 21+
    1. 下载编译依赖到 _build\libs（已存在则跳过）
    2. 编译 Java 源码（UTF-8）
    3. 打包为可部署 jar 到 _build\out
    4. 校验 plugin.yml 与主类一致性

.USAGE
    powershell -ExecutionPolicy Bypass -File build.ps1
    powershell -ExecutionPolicy Bypass -File build.ps1 -SkipDeps   # 离线跳过下载
#>

param(
    [switch]$SkipDeps,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# ---------- 路径定义 ----------
$Root      = $PSScriptRoot
$SrcRoot   = Join-Path $Root "plugin-src\src\main\java"
$ResRoot   = Join-Path $Root "plugin-src\src\main\resources"
$BuildRoot = Join-Path $Root "_build"
$LibDir    = Join-Path $BuildRoot "libs"
$ClassDir  = Join-Path $BuildRoot "classes"
$DistDir   = Join-Path $BuildRoot "out"
$JarName   = "BedrockSkinRecorder-1.0.0.jar"
$JarPath   = Join-Path $DistDir $JarName

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  BedrockSkinRecorder 插件构建" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

foreach ($p in @($SrcRoot, $ResRoot)) {
    if (-not (Test-Path $p)) {
        Write-Host "`n[错误] 目录结构不正确，未找到: $p" -ForegroundColor Red
        exit 1
    }
}

# ---------- 步骤 0：定位 JDK ----------
$JavaHome = $null
foreach ($c in @("C:\Program Files\Java\jdk-21", "C:\Program Files\Java\jdk-*",
                 "C:\Program Files\Eclipse Adoptium\jdk-*", "C:\Program Files\Microsoft\jdk-*")) {
    $resolved = Resolve-Path $c -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved -and (Test-Path (Join-Path $resolved.Path "bin\javac.exe"))) {
        $JavaHome = $resolved.Path
        break
    }
}
if (-not $JavaHome) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $binDir = Split-Path $javaCmd.Source -Parent
        if (Test-Path (Join-Path $binDir "javac.exe")) { $JavaHome = Split-Path $binDir -Parent }
    }
}
if (-not $JavaHome) {
    Write-Host "  [错误] 未找到 JDK（需要 JDK 21+）" -ForegroundColor Red
    exit 1
}
$javac  = Join-Path $JavaHome "bin\javac.exe"
$jarExe = Join-Path $JavaHome "bin\jar.exe"
Write-Host "  JDK: $JavaHome"

# ---------- 步骤 1：准备依赖 ----------
New-Item -ItemType Directory -Force -Path $LibDir | Out-Null
$Dependencies = [ordered]@{
    "spigot-api.jar"    = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.21.11-R0.1-SNAPSHOT/spigot-api-1.21.11-R0.1-20251231.154832-9.jar"
    "floodgate-api.jar" = "https://repo.opencollab.dev/main/org/geysermc/floodgate/api/2.2.5-SNAPSHOT/api-2.2.5-20260809.110940-20.jar"
    "annotations.jar"   = "https://repo1.maven.org/maven2/org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar"
    "gson-2.13.2.jar"   = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar"
    "guava-33.5.0-jre.jar" = "https://repo1.maven.org/maven2/com/google/guava/guava/33.5.0-jre/guava-33.5.0-jre.jar"
}
if (-not $SkipDeps) {
    foreach ($name in $Dependencies.Keys) {
        $dest = Join-Path $LibDir $name
        if (Test-Path $dest) { Write-Host "  [跳过] $name"; continue }
        Write-Host "  [下载] $name ..." -NoNewline
        try {
            Invoke-WebRequest -Uri $Dependencies[$name] -OutFile $dest -UseBasicParsing -TimeoutSec 180
            Write-Host " 完成" -ForegroundColor Green
        } catch {
            Write-Host "失败: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
    }
}
# Geyser-Spigot.jar：编译依赖取自运行服务器 plugins（含 geyser api 的 Skin/SkinProvider）
$GeyserSrc = "F:\game\pc\MC\开服\服务器数据备份\leaf-1.21.11\plugins\Geyser-Spigot.jar"
$GeyserLib = Join-Path $LibDir "geyser-spigot.jar"
if (-not (Test-Path $GeyserLib)) {
    if (Test-Path $GeyserSrc) {
        Copy-Item $GeyserSrc $GeyserLib
        Write-Host "  [复制] geyser-spigot.jar（自服务器）"
    } else {
        Write-Host "  [错误] 未找到服务器 Geyser-Spigot.jar: $GeyserSrc" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "  [跳过] geyser-spigot.jar（已存在）"
}
foreach ($r in @($Dependencies.Keys) + @("geyser-spigot.jar")) {
    if (-not (Test-Path (Join-Path $LibDir $r))) {
        Write-Host "  [错误] 缺少依赖: $r（去掉 -SkipDeps 自动下载）" -ForegroundColor Red
        exit 1
    }
}

# ---------- 步骤 2：编译 ----------
if ($Clean) {
    if (Test-Path $ClassDir) { Remove-Item -Recurse -Force $ClassDir }
}
New-Item -ItemType Directory -Force -Path $ClassDir | Out-Null
$cp = @(Get-ChildItem $LibDir -Filter *.jar | ForEach-Object { $_.FullName }) -join ";"
$srcs = Get-ChildItem $SrcRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if ($srcs.Count -eq 0) { Write-Host "  [错误] 无 Java 源文件"; exit 1 }

Write-Host "`n[编译] 源文件 $($srcs.Count) 个 ..."
& $javac -encoding UTF-8 -source 21 -target 21 -nowarn `
    -cp $cp -d $ClassDir $srcs
if ($LASTEXITCODE -ne 0) { Write-Host "  [错误] 编译失败"; exit 1 }
Write-Host "  [OK] 编译通过"

# ---------- 步骤 3：打包 ----------
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
Push-Location $ClassDir
& $jarExe cfe $JarPath "com.nangua.bedrockskinrecorder.BedrockSkinRecorder" .
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Host "  [错误] 打包失败"; exit 1 }
Pop-Location
Push-Location $ResRoot
& $jarExe uf $JarPath plugin.yml
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Host "  [错误] 写入 plugin.yml 失败"; exit 1 }
Pop-Location

# ---------- 步骤 4：校验 ----------
$tmp = Join-Path $env:TEMP ("bsr_check_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
Push-Location $tmp
& $jarExe xf $JarPath plugin.yml
& $jarExe xf $JarPath "com/nangua/bedrockskinrecorder/BedrockSkinRecorder.class"
Pop-Location
if ((Test-Path (Join-Path $tmp "plugin.yml")) -and (Test-Path (Join-Path $tmp "com\nangua\bedrockskinrecorder\BedrockSkinRecorder.class"))) {
    $main = (Get-Content (Join-Path $tmp "plugin.yml") -Encoding UTF8 | Select-String "^main:").Line.Trim()
    Write-Host "  [OK] plugin.yml main: $main"
    Write-Host "  [OK] jar 校验通过"
} else {
    Write-Host "  [错误] jar 校验失败" -ForegroundColor Red
    exit 1
}
Remove-Item -Recurse -Force $tmp

$kb = [math]::Round((Get-Item $JarPath).Length / 1KB, 1)
Write-Host "`n构建完成: $JarPath ($kb KB)" -ForegroundColor Green

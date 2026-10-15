# OfflineReward build script (Windows PowerShell)
# compile + package OfflineReward.jar (embeds sqlite-jdbc)
# usage:  powershell -File build.ps1 -pmm "F:\...\南瓜邮箱-PumpkinMail\PumpkinMail.jar"
param([string]$pmm)
$ErrorActionPreference = "Stop"

# change here if needed
$jdk = "C:\Program Files\Java\jdk-21\bin"

$root      = Split-Path -Parent $MyInvocation.MyCommand.Path
$paper     = Join-Path $root "lib\paper-api-1.21.11-R0.1-SNAPSHOT.jar"
$sqlite    = Join-Path $root "lib\sqlite-jdbc-3.53.4.0.jar"
$classes   = Join-Path $root "out\classes"
$dist      = Join-Path $root "out\dist"
$tmpSqlite = Join-Path $env:TEMP "offr-sqlite"

if (-not (Test-Path "$jdk\javac.exe")) { throw "JDK not found: $jdk - edit \$jdk at top of script" }
foreach ($f in @($paper, $sqlite, $pmm)) { if (-not (Test-Path $f)) { throw "missing dependency: $f" } }

foreach ($d in @($classes, $dist)) {
    if (Test-Path $d) { Get-ChildItem $d -Recurse -Force | Remove-Item -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}
if (Test-Path $tmpSqlite) { Get-ChildItem $tmpSqlite -Recurse -Force | Remove-Item -Recurse -Force }
New-Item -ItemType Directory -Force -Path $tmpSqlite | Out-Null

# 1. compile
$cp  = ((Get-ChildItem (Join-Path $root "lib") -Filter *.jar | ForEach-Object { $_.FullName }) + $pmm) -join ";"
$srcs = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" -encoding UTF-8 -cp $cp -d $classes $srcs
if ($LASTEXITCODE -ne 0) { throw "compile failed (exit=$LASTEXITCODE)" }

# 2. copy resources (plugin.yml / config.yml)
Copy-Item (Join-Path $root "src\main\resources\*") $classes -Recurse -Force

# 3. extract sqlite-jdbc into temp dir
Push-Location $tmpSqlite
& "$jdk\jar.exe" xf $sqlite
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "sqlite extract failed" }
Pop-Location

# 4. package final jar (main class + resources + sqlite)
$outJar = Join-Path $root "OfflineReward.jar"
& "$jdk\jar.exe" cfe $outJar com.nangua.offlinereward.OfflineReward -C $classes . -C $tmpSqlite .
if ($LASTEXITCODE -ne 0) { throw "package failed (exit=$LASTEXITCODE)" }

Write-Host "BUILD OK: $outJar"

$tmp = "F:\game\pc\MC\开服\Minecraft-Server-Build\.workbuddy\scratch\plugyams"
$srv = "C:\mc_serve\1.21.11-test"
$out = "F:\game\pc\MC\开服\Minecraft-Server-Build\.workbuddy\scratch\perms_extracted.txt"
$jars = @(
  "AntiLitematica-7.0.1.jar",
  "BedrockPlayerSupport-2.1.1-all.jar",
  "Chunky-Bukkit-1.4.40.jar",
  "CMILib1.5.9.9.jar",
  "CoreProtect-CE-24.0.jar",
  "customdeathmessages-1.3.jar",
  "DecentHolograms-2.10.1.jar",
  "DeluxeMenus-1.14.1-Release.jar",
  "EasyBot-2.3.1.jar",
  "EssentialsX-2.22.1-dev+23-43cb76a.jar",
  "floodgate-spigot.jar",
  "LuckPerms-Bukkit-5.5.81.jar",
  "opanel-bukkit-1.21.9-build-2.0.1.jar",
  "OpenInv.jar",
  "PlaceholderAPI-2.12.3.jar",
  "Plan-5.8-build-3605.jar",
  "PosTracker-1.0.0.jar",
  "ProtocolLib.jar",
  "QuickMenu-1.0.0.jar",
  "Residence6.0.0.1.jar",
  "SimplePets.jar",
  "SkinsRestorer.jar",
  "SpacePortal-1.0.0.jar",
  "TAB v6.1.2.jar",
  "UuidMigrate-1.0.0.jar",
  "Vault.jar",
  "ViaVersion-5.12.0-SNAPSHOT.jar",
  "voicechat-bukkit-2.6.21.jar",
  "worldedit-bukkit-7.4.2.jar"
)
"" | Out-File -FilePath $out -Encoding utf8
foreach ($jar in $jars) {
  $dest = Join-Path $tmp $jar.Replace('.jar','')
  New-Item -ItemType Directory -Force -Path $dest | Out-Null
  & tar -xf "$srv\plugins\$jar" -C $dest plugin.yml 2>$null
  $pf = Join-Path $dest "plugin.yml"
  "## $jar" | Out-File -Append $out -Encoding utf8
  if (-not (Test-Path $pf)) { "  (no plugin.yml)" | Out-File -Append $out -Encoding utf8; continue }
  $lines = Get-Content -Path $pf
  $permIdx = -1
  for ($i=0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match '^\s*permissions:\s*$') { $permIdx = $i; break }
  }
  if ($permIdx -lt 0) { "  (no permissions: section)" | Out-File -Append $out -Encoding utf8; continue }
  $permIndent = ($lines[$permIdx] -replace '^(.*?)permissions:.*$','$1').Length
  $nodes = @()
  for ($i = $permIdx+1; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line -match '^\s*$') { continue }
    $indent = ($line -replace '^(\s*).*$','$1').Length
    if ($indent -le $permIndent) { break }
    if ($line -match '^\s*([\w.\-*]+):\s*(true|false|)\s*$') {
      $key = $Matches[1]
      if ($key -notin @('description','default','children','info')) { $nodes += $key }
    }
  }
  $nodes = $nodes | Sort-Object -Unique
  "  count=$($nodes.Count)" | Out-File -Append $out -Encoding utf8
  foreach ($n in $nodes) { "  $n" | Out-File -Append $out -Encoding utf8 }
}

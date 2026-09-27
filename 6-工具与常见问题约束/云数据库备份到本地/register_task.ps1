# ============================================================
#  注册 Windows 计划任务：每天定时运行云端->本地增量同步
#  用法：powershell -ExecutionPolicy Bypass -File 本脚本
# ============================================================

$taskName = "DBCloudSyncLocal"
$scriptPath = "F:\game\pc\MC\开服\服务器数据备份\插件数据\数据库备份\sync_mysql.ps1"

# 触发器：每天 04:30 执行（避开服务器游玩高峰）
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`""
$trigger = New-ScheduledTaskTrigger -Daily -At "04:30"

# 设置：失败重试 3 次，间隔 10 分钟；允许错过启动
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 10) -ExecutionTimeLimit (New-TimeSpan -Hours 6)

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Description "每日 04:30 将云端 MariaDB(coreprotect/postracker/host21k5c4) 增量同步到本地 MySQL，本地只增不删。" -Force

Write-Output "计划任务 [$taskName] 注册完成：每天 04:30 执行"
Get-ScheduledTask -TaskName $taskName | Select-Object TaskName, State, @{N='NextRun';E={$_ | Get-ScheduledTaskInfo | Select-Object -Expand NextRunTime}}

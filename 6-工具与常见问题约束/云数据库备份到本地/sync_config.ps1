# ============================================================
#  云端 MariaDB -> 本地 MySQL 增量同步配置
#  策略：按主键增量合并，本地只增不删（云端删旧数据不影响本地历史）
# ============================================================

# 云端（源，MariaDB 10.11）
$CloudHost   = "123.207.3.73"
$CloudPort   = 3306
$CloudUser   = "host21k5c4"
$CloudPass   = "pId5XpS5CcBi"

# 本地（目标，MySQL 8.0）
$LocalHost   = "127.0.0.1"
$LocalPort   = 3306
$LocalUser   = "root"
$LocalPass   = "zbmlp01."

# 需同步的数据库
# 注意：云端库名大小写敏感(Plan大写)，本地lower_case_table_names=1大小写不敏感，故统一写 Plan
$Databases   = @("coreprotect", "postracker", "host21k5c4", "Plan")

# MySQL 客户端路径
$MysqlCli    = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$Mysqldump   = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"

# 日志目录（脚本所在目录下 logs）
$LogDir      = Join-Path $PSScriptRoot "logs"

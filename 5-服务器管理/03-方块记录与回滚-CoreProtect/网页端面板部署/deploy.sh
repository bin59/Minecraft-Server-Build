#!/usr/bin/env bash
#
# CoreProtect Lookup Web (CoLWI — CommunityCraft rework for CoreProtect 23.2) 一键部署脚本
#
# 适用系统 : Ubuntu 20.04 / 22.04 / 24.04，Debian 11 / 12（Nginx + PHP-FPM）
# 前置条件 : 已把程序上传到 WEB_ROOT（目录里应有 index.php / lookup.php / config.php）
#
# 用法:
#   1) 改下面「必填配置区」
#   2) sudo bash deploy.sh          部署（可重复执行，幂等）
#      sudo bash deploy.sh verify   只做健康检查，不改任何东西
#      sudo bash deploy.sh conf     只重新生成 Nginx 站点配置
#
set -Eeuo pipefail

############################## 必填配置区 ##############################
WEB_ROOT="/var/www/CoreProtect-Lookup-Web-23.2"  # 你上传的目录
DOMAIN="_"                    # 有域名填域名；只按 IP 访问就填 "_"
LISTEN_PORT="80"              # 对外端口（建议改成冷门端口，如 18443）

ACCESS_MODE="auth"            # auth=账号密码 / ip=仅白名单 / both=两者都要
AUTH_USER="cpadmin"           # Basic Auth 用户名
AUTH_PASS=""                  # 留空则脚本随机生成并打印（只打印这一次！）
ALLOW_IPS="127.0.0.1"         # ACCESS_MODE 含 ip 时生效，空格分隔，填你的出口公网 IP

DB_TYPE="mysql"               # mysql 或 sqlite
DB_HOST="127.0.0.1:3306"      # mysql 用
DB_NAME="coreprotect"         # CoreProtect 的库名
DB_USER="corelook"            # 建议用只读账号
DB_PASS="改成只读账号密码"     # mysql 用
DB_PREFIX="co_"               # 必须等于 CoreProtect config.yml 里的 table-prefix
SQLITE_PATH=""                # DB_TYPE=sqlite 时填 database.db 绝对路径（需与 MC 同机）

ENABLE_HTTPS="no"             # yes=用 certbot 签证书（需域名已解析到本机且 80 端口可达）
#######################################################################

SERVICE_NAME="colwi"
VHOST="/etc/nginx/sites-available/${SERVICE_NAME}.conf"
VHOST_ENABLED="/etc/nginx/sites-enabled/${SERVICE_NAME}.conf"
HTPASSWD_FILE="/etc/nginx/.${SERVICE_NAME}_htpasswd"
LOG_DIR="/var/log/nginx"

C_RESET=$'\033[0m'; C_RED=$'\033[31m'; C_GRN=$'\033[32m'; C_YEL=$'\033[33m'
log()  { printf '%s[·]%s %s\n' "$C_GRN" "$C_RESET" "$*"; }
warn() { printf '%s[!]%s %s\n' "$C_YEL" "$C_RESET" "$*"; }
err()  { printf '%s[x]%s %s\n' "$C_RED" "$C_RESET" "$*" >&2; }
die()  { err "$*"; exit 1; }

[[ $EUID -eq 0 ]] || die "请用 root 执行：sudo bash $0"
[[ -d "$WEB_ROOT" ]] || die "目录不存在：$WEB_ROOT（先上传程序，或改 WEB_ROOT）"
for f in index.php lookup.php config.php; do
  [[ -f "$WEB_ROOT/$f" ]] || die "$WEB_ROOT 里缺少 $f，上传不完整"
done

########################## 1. 安装依赖 ##########################
install_deps() {
  command -v apt-get >/dev/null 2>&1 || die "本脚本只支持 apt 系（Ubuntu/Debian），其他发行版请手动安装对应包"
  log "安装 Nginx / PHP-FPM / 扩展 ..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq nginx php-cli php-fpm \
    php-mysql php-sqlite3 apache2-utils curl >/dev/null
  log "依赖安装完成"
}

ensure_php() {
  PHP_VER="$(php -r 'echo PHP_MAJOR_VERSION.".".PHP_MINOR_VERSION;')"
  log "PHP 版本：$PHP_VER"
  if [[ "$(php -r 'echo version_compare(PHP_VERSION,"8.1.0",">=") ? 1 : 0;')" != "1" ]]; then
    warn "PHP 低于 8.1，rework 建议 8.4；可能出现语法兼容问题（Ubuntu 20.04 需加 ondrej/php PPA）"
  elif [[ "$(php -r 'echo version_compare(PHP_VERSION,"8.4.0",">=") ? 1 : 0;')" != "1" ]]; then
    warn "PHP 非 8.4（官方开发验证版本），一般可用，但上游主要按 8.4 测试"
  fi
  local driver
  if [[ "$DB_TYPE" == "mysql" ]]; then driver="pdo_mysql"; else driver="pdo_sqlite"; fi
  if php -m | grep -qi "^${driver}$"; then
    log "扩展 $driver 已加载"
  else
    die "扩展 $driver 未加载，检查 php.ini 的 extension 配置后重试"
  fi
  php -m | grep -qi "^PDO$" || die "PDO 扩展未加载"
}

########################## 2. 目录权限 ##########################
fix_perms() {
  log "设置目录权限 ..."
  local u; u="$(ps -o user= -C php-fpm 2>/dev/null | head -n1)"
  u="${u:-www-data}"
  [[ "$u" == "root" ]] && u="www-data"
  chown -R "root:$u" "$WEB_ROOT"
  find "$WEB_ROOT" -type d -exec chmod 750 {} \;
  find "$WEB_ROOT" -type f -exec chmod 640 {} \;
  # 缓存目录需要可写（若存在）
  if [[ -d "$WEB_ROOT/cache" ]]; then
    chmod 770 "$WEB_ROOT/cache"
    log "cache/ 已设为可写"
  fi
  log "属主 root:$u，目录 750 / 文件 640"
}

########################## 3. 写入 config.php ##########################
patch_config() {
  log "写入 config.php 的 server 段 ..."
  local helper="/tmp/colwi_patch_config.php"
  cat > "$helper" <<'PHPEOF'
<?php
$file = $argv[1] ?? '';
$vals = json_decode($argv[2] ?? '{}', true);
if (!is_file($file)) { fwrite(STDERR, "文件不存在: $file\n"); exit(1); }
$src = file_get_contents($file);
if (!preg_match("/('server'\s*=>\s*\[)(.*?)(\s*\]),/s", $src, $m, PREG_OFFSET_CAPTURE)) {
  fwrite(STDERR, "config.php 里未找到 'server' => [ ... ] 块，请手动编辑\n"); exit(2);
}
$lit = function ($v) {
  if (is_bool($v)) return $v ? 'true' : 'false';
  if ($v === null) return 'null';
  if (is_int($v) || is_float($v)) return (string)$v;
  return "'" . str_replace(['\\', "'"], ['\\\\', "\\'"], (string)$v) . "'";
};
$block = $m[2][0]; $new = $block; $changed = []; $missing = [];
foreach ($vals as $k => $v) {
  $kp = "/'" . preg_quote($k, '/') . "'\s*=>\s*(?:'[^']*'|\"[^\"]*\"|true|false|null|-?\d+)/";
  if (preg_match($kp, $new)) {
    $new = preg_replace($kp, "'" . $k . "' => " . $lit($v), $new, 1);
    $changed[] = $k;
  } else { $missing[] = $k; }
}
if ($changed) {
  copy($file, $file . '.bak.' . date('YmdHis'));
  $out = substr($src, 0, $m[2][1]) . $new . substr($src, $m[2][1] + strlen($block));
  file_put_contents($file, $out);
}
echo json_encode(['changed' => $changed, 'missing' => $missing], JSON_UNESCAPED_UNICODE), "\n";
PHPEOF

  local vals
  if [[ "$DB_TYPE" == "mysql" ]]; then
    vals="$(printf '{"type":"mysql","host":"%s","database":"%s","username":"%s","password":"%s","prefix":"%s","preBlockName":true,"mapLink":""}' \
      "$DB_HOST" "$DB_NAME" "$DB_USER" "$DB_PASS" "$DB_PREFIX")"
  else
    [[ -n "$SQLITE_PATH" ]] || die "DB_TYPE=sqlite 时必须填 SQLITE_PATH"
    vals="$(printf '{"type":"sqlite","path":"%s","prefix":"%s","preBlockName":true,"mapLink":""}' \
      "$SQLITE_PATH" "$DB_PREFIX")"
  fi

  local out
  out="$(php "$helper" "$WEB_ROOT/config.php" "$vals")" || die "config.php 改写失败，请手动编辑"
  log "config.php：$out"
  if [[ "$out" == *'"missing":['*'"'* ]]; then
    warn "上面 missing 里的键在原 config.php 中不存在，已被跳过——请手动补进 server 段"
  fi
  php -l "$WEB_ROOT/config.php" >/dev/null || die "config.php 语法错误，已备份原文件为 config.php.bak.*，请检查"
  rm -f "$helper"
}

########################## 4. Nginx 站点 ##########################
gen_vhost() {
  log "生成 Nginx 站点配置 ..."
  local sock
  sock="$(ls /run/php/php*-fpm.sock 2>/dev/null | head -n1 || true)"
  [[ -n "$sock" ]] || sock="/run/php/php${PHP_VER}-fpm.sock"

  local access="" allow_block=""
  if [[ "$ACCESS_MODE" == "auth" || "$ACCESS_MODE" == "both" ]]; then
    access="    auth_basic \"CoreProtect Lookup\";
    auth_basic_user_file ${HTPASSWD_FILE};
"
  fi
  if [[ "$ACCESS_MODE" == "ip" || "$ACCESS_MODE" == "both" ]]; then
    allow_block="    allow 127.0.0.1;
"
    for ip in $ALLOW_IPS; do
      [[ "$ip" == "127.0.0.1" ]] && continue
      allow_block+="    allow ${ip};
"
    done
    allow_block+="    deny all;
"
  fi

  cat > "$VHOST" <<EOF
# CoreProtect Lookup Web — 由 deploy.sh 生成，手动改会被覆盖
server {
    listen ${LISTEN_PORT};
    server_name ${DOMAIN};

    root ${WEB_ROOT};
    index index.php;

    access_log ${LOG_DIR}/${SERVICE_NAME}.access.log;
    error_log  ${LOG_DIR}/${SERVICE_NAME}.error.log;

    client_max_body_size 2m;
    fastcgi_read_timeout 60;

${access}${allow_block}
    location / {
        try_files \$uri \$uri/ /index.php?\$query_string;
    }

    location ~ \.php\$ {
        include snippets/fastcgi-php.conf;
        fastcgi_pass unix:${sock};
    }

    # 关键：绝不让外部拿到数据库凭据与源码目录
    location ~ /(config\.php|\.git|tests|\.env) {
        deny all;
    }
    location ~ /\. {
        deny all;
    }
}
EOF

  ln -sf "$VHOST" "$VHOST_ENABLED"
  nginx -t >/dev/null 2>&1 || { err "nginx 配置校验失败："; nginx -t; exit 1; }
  log "站点配置 OK → $VHOST"
}

setup_auth() {
  [[ "$ACCESS_MODE" == "auth" || "$ACCESS_MODE" == "both" ]] || return 0
  if [[ ! -f "$HTPASSWD_FILE" ]]; then
    if [[ -z "$AUTH_PASS" ]]; then
      AUTH_PASS="$(head -c 18 /dev/urandom | base64 | tr -d '/+=' | head -c 16)"
      GENERATED_PASS="1"
    fi
    htpasswd -bc "$HTPASSWD_FILE" "$AUTH_USER" "$AUTH_PASS" >/dev/null
    chmod 640 "$HTPASSWD_FILE"
    log "已创建 Basic Auth 账号：$AUTH_USER"
    if [[ "${GENERATED_PASS:-0}" == "1" ]]; then
      warn "随机密码（只显示这一次，请立刻保存）：$AUTH_PASS"
    fi
  else
    log "Basic Auth 文件已存在，保留不改：$HTPASSWD_FILE"
  fi
}

open_firewall() {
  command -v ufw >/dev/null 2>&1 || return 0
  if ufw status | grep -qi "Status: active"; then
    ufw allow "${LISTEN_PORT}/tcp" comment "CoLWI" >/dev/null
    log "ufw 已放行 ${LISTEN_PORT}/tcp"
  fi
}

enable_https() {
  [[ "$ENABLE_HTTPS" == "yes" ]] || return 0
  [[ "$DOMAIN" != "_" ]] || { warn "ENABLE_HTTPS=yes 但 DOMAIN=_，跳过证书签发"; return 0; }
  apt-get install -y -qq certbot python3-certbot-nginx >/dev/null
  certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "admin@${DOMAIN}" --redirect || \
    warn "certbot 签发失败（域名解析或 80 端口不可达？），当前仍走 HTTP"
}

########################## 5. 自检 ##########################
verify() {
  log "=== 健康检查 ==="
  php -l "$WEB_ROOT/index.php"  >/dev/null && log "index.php  语法 OK" || err "index.php 语法有误"
  php -l "$WEB_ROOT/lookup.php" >/dev/null && log "lookup.php 语法 OK" || err "lookup.php 语法有误"

  systemctl is-active --quiet nginx  && log "nginx 运行中"  || err "nginx 未运行"
  systemctl is-active --quiet "php${PHP_VER}-fpm" && log "php${PHP_VER}-fpm 运行中" || err "php-fpm 未运行"

  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:${LISTEN_PORT}/index.php" || echo 000)"
  case "$ACCESS_MODE" in
    auth|both)
      [[ "$code" == "401" ]] && log "未带认证访问返回 401 ✓（访问控制生效）" \
        || err "未带认证访问返回 $code，期望 401 —— 访问控制可能没生效！"
      if [[ -n "${AUTH_PASS:-}" ]]; then
        local code2
        code2="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 -u "${AUTH_USER}:${AUTH_PASS}" "http://127.0.0.1:${LISTEN_PORT}/index.php" || echo 000)"
        [[ "$code2" == "200" ]] && log "带认证访问返回 200 ✓" || err "带认证访问返回 $code2，期望 200 —— 查 error_log"
      else
        warn "AUTH_PASS 为空，跳过带认证访问测试（如需测试请填 AUTH_PASS 后跑 verify）"
      fi
      ;;
    *)
      [[ "$code" == "200" ]] && log "访问返回 200 ✓" || err "访问返回 $code，期望 200"
      ;;
  esac

  local code3
  code3="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "http://127.0.0.1:${LISTEN_PORT}/config.php" || echo 000)"
  [[ "$code3" == "403" || "$code3" == "404" ]] && log "config.php 已禁止外部访问 ✓" \
    || err "config.php 返回 $code3 —— 数据库凭据可能泄露，立刻检查配置！"

  log "=== 完成 ==="
  if [[ "$ACCESS_MODE" == "ip" || "$ACCESS_MODE" == "both" ]]; then
    log "仅白名单可访问：127.0.0.1 $ALLOW_IPS"
  fi
  [[ "$ACCESS_MODE" == "auth" || "$ACCESS_MODE" == "both" ]] && log "登录地址：http://服务器IP:${LISTEN_PORT}/  账号：$AUTH_USER"
}

########################## 主流程 ##########################
main() {
  log "Web 根目录：$WEB_ROOT"
  ensure_php
  fix_perms
  patch_config
  gen_vhost
  setup_auth
  open_firewall
  systemctl reload nginx
  systemctl restart "php${PHP_VER}-fpm"
  enable_https
  verify
  warn "别忘了：MySQL 账号只给 SELECT —— GRANT SELECT ON ${DB_NAME}.* TO '${DB_USER}'@'网页服务器IP';"
}

case "${1:-deploy}" in
  deploy) install_deps; ensure_php; main ;;
  conf)   ensure_php; gen_vhost; systemctl reload nginx; log "仅更新站点配置完成" ;;
  verify) ensure_php; verify ;;
  *)      echo "用法: sudo bash $0 [deploy|conf|verify]" ; exit 1 ;;
esac

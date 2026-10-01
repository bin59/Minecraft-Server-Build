#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
网页上传皮肤 → 应用到 Minecraft 服务器  (方案A)

工作流:
  1. 玩家/管理端通过网页 POST 上传皮肤 PNG 图片 + 目标玩家名
  2. 本后端把图片保存到本地静态目录 (或已有公网 URL 则直接用)
  3. 通过 RCON 向服务器发送 SkinsRestorer 命令: /skin url <玩家> <皮肤URL>
  4. 返回执行结果

运行:
  python skin_upload_rcon_backend.py
  (默认监听 127.0.0.1:8090, 纯 Python 标准库, 零第三方依赖, 低资源占用)

依赖说明:
  - RCON 协议用纯 socket 实现(标准库), 不依赖 mcrcon 库
  - HTTP 用 http.server(标准库), 不依赖 Flask/FastAPI

安全提示:
  - 本服务默认仅监听 127.0.0.1(本机), 若需云服务器网页跨机调用,
    请置于内网/加鉴权 Token, 勿直接暴露公网
  - RCON 密码不要使用弱口令(已替换为强随机密码, 请保管好)
"""

import base64
import hashlib
import json
import os
import re
import socket
import struct
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from http import HTTPStatus

# ==================== 配置区(按需修改) ====================
HOST = os.environ.get("SKIN_API_HOST", "127.0.0.1")     # 监听地址
PORT = int(os.environ.get("SKIN_API_PORT", "8090"))      # 监听端口

# Minecraft 服务器 RCON 配置 (与 server.properties 保持一致)
RCON_HOST = os.environ.get("RCON_HOST", "127.0.0.1")     # 服务器地址(本机回环)
RCON_PORT = int(os.environ.get("RCON_PORT", "25595"))    # rcon.port
RCON_PASSWORD = os.environ.get("RCON_PASSWORD", "jUpNJ3sCz0LODRaXPyT9")

# 皮肤图片保存目录 (需能被服务器访问; 若服务器能访问本机文件则用 file://, 否则需配公网)
UPLOAD_DIR = os.environ.get("SKIN_UPLOAD_DIR", "./uploaded_skins")

# 皮肤图片对外可访问的基础 URL。
#   若云服务器网页与 MC 服务器在同一台机器, 可用 file:// 或本机 http;
#   若跨机, 请填该机器上可公网访问的静态目录 URL, 例如 https://yourhost/skins
SKIN_PUBLIC_BASE_URL = os.environ.get("SKIN_PUBLIC_BASE_URL", "http://127.0.0.1:8090/uploaded_skins")

# 可选: 简单鉴权 Token (请求头 X-Api-Token). 留空则关闭鉴权(不建议)
API_TOKEN = os.environ.get("SKIN_API_TOKEN", "")

# SkinsRestorer 命令模板。默认用 /skin url 指定图片 URL。
SKIN_CMD_TEMPLATE = "skinsrestorer:applyskin {player} {url}"
# 备选模板(若命令不同):
# SKIN_CMD_TEMPLATE = "skin url {player} {url}"
# SKIN_CMD_TEMPLATE = "sr applyskin {player} {url}"

# 允许的图片扩展名
ALLOWED_EXT = {".png", ".jpg", ".jpeg", ".webp", ".gif"}
MAX_IMAGE_BYTES = 5 * 1024 * 1024  # 5MB 上限
# ========================================================

os.makedirs(UPLOAD_DIR, exist_ok=True)


# ==================== RCON 客户端(纯标准库) ====================
class RconError(Exception):
    pass


def _pack_rcon(request_id: int, req_type: int, payload: str) -> bytes:
    """构造 RCON 报文: [len][request_id][type][payload][\0][\0]"""
    body = struct.pack("<ii", request_id, req_type) + payload.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(body)) + body


def _unpack_rcon(data: bytes):
    """解析 RCON 响应, 返回 (request_id, type, payload)"""
    if len(data) < 12:
        raise RconError("RCON 响应过短")
    length = struct.unpack("<i", data[:4])[0]
    request_id, resp_type = struct.unpack("<ii", data[4:12])
    payload = data[12:12 + length - 10].decode("utf-8", errors="replace")
    return request_id, resp_type, payload


def rcon_command(command: str, host: str = RCON_HOST, port: int = RCON_PORT,
                 password: str = RCON_PASSWORD, timeout: float = 5.0) -> str:
    """通过 RCON 向 Minecraft 服务器发送一条命令, 返回服务器输出文本。"""
    if not password:
        raise RconError("未配置 RCON 密码")
    sock = socket.create_connection((host, port), timeout=timeout)
    try:
        # 1. 登录
        login_req = _pack_rcon(1, 3, password)  # type 3 = LOGIN
        sock.sendall(login_req)
        resp = _recv_exact(sock)
        req_id, _, _ = _unpack_rcon(resp)
        if req_id == -1:
            raise RconError("RCON 登录失败: 密码错误")
        # 2. 发送命令 (type 2 = COMMAND)
        cmd_req = _pack_rcon(2, 2, command)
        sock.sendall(cmd_req)
        resp = _recv_exact(sock)
        _, _, payload = _unpack_rcon(resp)
        return payload.strip()
    finally:
        try:
            sock.close()
        except Exception:
            pass


def _recv_exact(sock: socket.socket, buf_size: int = 4096) -> bytes:
    """读取一条完整 RCON 报文 (先读4字节长度, 再读剩余)"""
    header = _recv_n(sock, 4)
    length = struct.unpack("<i", header)[0]
    body = _recv_n(sock, length)
    return header + body


def _recv_n(sock: socket.socket, n: int) -> bytes:
    chunks = b""
    while len(chunks) < n:
        chunk = sock.recv(n - len(chunks))
        if not chunk:
            raise RconError("RCON 连接中断")
        chunks += chunk
    return chunks


# ==================== HTTP 服务 ====================
class SkinHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        print(f"[{time.strftime('%H:%M:%S')}] {self.address_string()} - {fmt % args}")

    def _check_token(self) -> bool:
        if not API_TOKEN:
            return True
        token = self.headers.get("X-Api-Token", "")
        return token == API_TOKEN

    def _send_json(self, code: int, obj: dict):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_text(self, code: int, text: str, ctype: str = "text/plain; charset=utf-8"):
        body = text.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    # ---- GET: 健康检查 / 静态皮肤图片 ----
    def do_GET(self):
        path = urllib.parse.urlparse(self.path).path
        if path == "/health":
            self._send_json(200, {"status": "ok", "rcon": RCON_HOST, "port": RCON_PORT})
            return
        # 提供上传的皮肤图片静态访问
        if path.startswith("/uploaded_skins/"):
            fname = os.path.basename(path)
            fpath = os.path.join(UPLOAD_DIR, fname)
            if os.path.isfile(fpath):
                with open(fpath, "rb") as f:
                    data = f.read()
                ext = os.path.splitext(fname)[1].lower()
                ctype = {
                    ".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg",
                    ".webp": "image/webp", ".gif": "image/gif",
                }.get(ext, "application/octet-stream")
                self._send_bytes(data, ctype)
                return
            self._send_json(404, {"error": "not found"})
            return
        self._send_json(404, {"error": "unknown endpoint", "hint": "POST /api/apply-skin"})

    def _send_bytes(self, data: bytes, ctype: str):
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    # ---- POST: 上传皮肤并应用到服务器 ----
    def do_POST(self):
        path = urllib.parse.urlparse(self.path).path
        if path != "/api/apply-skin":
            self._send_json(404, {"error": "unknown endpoint"})
            return
        if not self._check_token():
            self._send_json(401, {"error": "invalid token"})
            return

        # 解析 multipart/form-data 或 application/x-www-form-urlencoded
        content_type = self.headers.get("Content-Type", "")
        length = int(self.headers.get("Content-Length", 0))
        if length > MAX_IMAGE_BYTES * 4:  # 表单本身略大于图片
            self._send_json(413, {"error": "payload too large"})
            return
        body = self.rfile.read(length) if length > 0 else b""

        fields = self._parse_multipart(content_type, body)
        player = fields.get("player", "")
        url = fields.get("url", "").strip()
        image_data = fields.get("_file")  # 上传的图片原始字节

        if not player or not re.fullmatch(r"[A-Za-z0-9_]{1,16}", player):
            self._send_json(400, {"error": "player 名非法或为空(1-16位字母数字下划线)"})
            return

        # 两种输入方式: 直接给 url, 或上传图片文件
        if not url and image_data:
            if len(image_data) > MAX_IMAGE_BYTES:
                self._send_json(413, {"error": "图片超过 5MB"})
                return
            # 用文件内容哈希命名, 避免重复
            digest = hashlib.sha256(image_data).hexdigest()
            fname = digest + ".png"
            with open(os.path.join(UPLOAD_DIR, fname), "wb") as f:
                f.write(image_data)
            url = f"{SKIN_PUBLIC_BASE_URL.rstrip('/')}/{fname}"

        if not url:
            self._send_json(400, {"error": "缺少皮肤: 请传 url 或上传图片文件"})
            return

        # 应用皮肤
        cmd = SKIN_CMD_TEMPLATE.format(player=player, url=url)
        try:
            result = rcon_command(cmd)
        except RconError as e:
            self._send_json(502, {"error": f"RCON 失败: {e}"})
            return

        self._send_json(200, {
            "player": player,
            "url": url,
            "command": cmd,
            "rcon_output": result,
        })

    def _parse_multipart(self, content_type: str, body: bytes) -> dict:
        """极简 multipart 解析(仅提取字段与单个文件), 兼容 urlencoded。"""
        fields: dict = {}
        if content_type.startswith("application/x-www-form-urlencoded"):
            for k, v in urllib.parse.parse_qsl(body.decode("utf-8", errors="replace")):
                fields[k] = v
            return fields
        if "multipart/form-data" not in content_type:
            return fields
        m = re.search(r"boundary=(.+)", content_type)
        if not m:
            return fields
        boundary = m.group(1).strip().strip('"')
        delimiter = b"--" + boundary.encode()
        parts = body.split(delimiter)
        for part in parts:
            part = part.strip(b"\r\n")
            if not part or part == b"--":
                continue
            # 分离头部与内容
            header_end = part.find(b"\r\n\r\n")
            if header_end == -1:
                continue
            header = part[:header_end].decode("utf-8", errors="replace")
            content = part[header_end + 4:]
            name_m = re.search(r'name="([^"]+)"', header)
            if not name_m:
                continue
            name = name_m.group(1)
            filename_m = re.search(r'filename="([^"]*)"', header)
            if filename_m and filename_m.group(1):
                fields["_file"] = content  # 图片文件
            else:
                fields[name] = content.decode("utf-8", errors="replace").strip()
        return fields


def start_server():
    server = ThreadingHTTPServer((HOST, PORT), SkinHandler)
    print(f"[皮肤上传后端] 监听 http://{HOST}:{PORT}")
    print(f"[皮肤上传后端] 上传目录: {UPLOAD_DIR}")
    print(f"[皮肤上传后端] RCON: {RCON_HOST}:{RCON_PORT} -> SkinsRestorer 命令模板: {SKIN_CMD_TEMPLATE}")
    print(f"[皮肤上传后端] 鉴权: {'开启(需 X-Api-Token)' if API_TOKEN else '关闭(危险, 建议设置)'}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[皮肤上传后端] 已停止")
        server.server_close()


# ==================== 自测(可选) ====================
def selftest():
    """本地快速验证 RCON 链路(不带 --selftest 时不会运行)。"""
    print("RCON 自测: 发送 /skin info 查看服务器响应...")
    try:
        out = rcon_command("skin info")
        print("服务器响应:", out or "(空)")
    except RconError as e:
        print("RCON 自测失败:", e)


if __name__ == "__main__":
    import sys
    if "--selftest" in sys.argv:
        selftest()
    else:
        start_server()

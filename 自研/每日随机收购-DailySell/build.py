#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""DailySell 离线编译脚本：JDK 25 javac --release 21 + 26.3 实跑 jar。

用法： python build.py
产物： target/DailySell-1.2.2.jar

依赖（全部本机已有，无网络）：
  - 自研/lib/leaf-api-26.3.build.13-alpha.jar（服务端 API）
  - 自研/lib/floodgate-api.jar（基岩表单 API，运行时服务器提供）
  - 实跑服 plugins/Vault.jar（Vault 经济 API，运行时服务器提供）
  - 实跑服 plugins/Geyser-Spigot.jar（cumulus 表单 API，运行时服务器提供）
  - ~/.m2 的 adventure / annotations / guava / bungeecord-chat
    （仅编译期解析 leaf-api 方法签名用，不打进 jar）
"""
import glob
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "src", "main", "java")
RES = os.path.join(ROOT, "src", "main", "resources")
CLASSES = os.path.join(ROOT, "target", "classes")
JAR_NAME = "DailySell-1.2.3.jar"
TARGET = os.path.join(ROOT, "target", JAR_NAME)

SHARED_LIB = os.path.normpath(os.path.join(ROOT, "..", "lib"))
SERVER_PLUGINS = r"F:\game\pc\MC\开服\服务器数据备份\leaf-26.3\leaf-26.3\plugins"
VAULT_JAR = os.path.join(SERVER_PLUGINS, "Vault.jar")
GEYSER_JAR = os.path.join(SERVER_PLUGINS, "Geyser-Spigot.jar")

JDK_HOME = os.environ.get("PF_JDK_HOME", r"C:\Program Files\Java\jdk-25.0.5.0.3")
JAVAC = os.path.join(JDK_HOME, "bin", "javac.exe")
if not os.path.exists(JAVAC):
    JAVAC = os.path.join(JDK_HOME, "bin", "javac")

M2 = os.path.join(os.path.expanduser("~"), ".m2", "repository")


def extra_jars():
    """补齐 leaf-api 引用的 Adventure / annotations（systemPath 依赖不带传递依赖）。"""
    out = []
    patterns = [
        os.path.join(M2, "net", "kyori", "adventure-api", "5.2.0", "*.jar"),
        os.path.join(M2, "net", "kyori", "adventure-key", "5.2.0", "*.jar"),
        os.path.join(M2, "net", "kyori", "examination-api", "*", "*.jar"),
        os.path.join(M2, "org", "jetbrains", "annotations", "*", "*.jar"),
        os.path.join(M2, "com", "google", "guava", "guava", "33.6.0-jre", "*.jar"),
        os.path.join(M2, "net", "md-5", "bungeecord-chat", "*", "*.jar"),
    ]
    for p in patterns:
        out.extend(sorted(glob.glob(p)))
    return [j for j in out if not j.endswith(("-sources.jar", "-javadoc.jar"))]


def q(path):
    """javac @argfile 会把反斜杠当转义符，统一转正斜杠并加引号。"""
    return '"' + path.replace("\\", "/") + '"'


def main():
    if not os.path.exists(JAVAC):
        print("[FAIL] 找不到 javac：", JAVAC)
        return 1
    jars = [os.path.join(SHARED_LIB, "leaf-api-26.3.build.13-alpha.jar"),
            os.path.join(SHARED_LIB, "floodgate-api.jar"),
            VAULT_JAR,
            GEYSER_JAR] + extra_jars()
    missing = [j for j in jars if not os.path.exists(j)]
    if missing:
        print("[FAIL] 缺依赖 jar：", missing)
        return 1
    cp = os.pathsep.join(jars)
    print("classpath jar 数:", len(jars))

    if os.path.isdir(CLASSES):
        shutil.rmtree(CLASSES)
    os.makedirs(CLASSES, exist_ok=True)

    sources = sorted(glob.glob(os.path.join(SRC, "**", "*.java"), recursive=True))
    print("源文件数:", len(sources))
    argfile = os.path.join(ROOT, "target", "javac.args")
    os.makedirs(os.path.dirname(argfile), exist_ok=True)
    with open(argfile, "w", encoding="utf-8") as f:
        f.write('-encoding UTF-8\n--release 21\n-nowarn\n')
        f.write('-cp ' + q(cp) + '\n')
        f.write('-d ' + q(CLASSES) + '\n')
        for s in sources:
            f.write(q(s) + '\n')

    r = subprocess.run([JAVAC, "@" + argfile], capture_output=True)
    out = (r.stdout or b"").decode("gbk", errors="replace")
    err = (r.stderr or b"").decode("gbk", errors="replace")
    if r.returncode != 0:
        print("[FAIL] javac 编译失败\n" + out + err)
        return 1
    if err.strip():
        print("javac 警告/提示:", err.strip()[:800])

    for f in glob.glob(os.path.join(RES, "*")):
        if os.path.isfile(f):
            shutil.copy2(f, os.path.join(CLASSES, os.path.basename(f)))

    if os.path.exists(TARGET):
        os.remove(TARGET)
    with zipfile.ZipFile(TARGET, "w", zipfile.ZIP_DEFLATED) as z:
        for base, _, files in os.walk(CLASSES):
            for name in files:
                full = os.path.join(base, name)
                z.write(full, os.path.relpath(full, CLASSES).replace(os.sep, "/"))

    print("[OK] 产物:", TARGET, os.path.getsize(TARGET), "bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())

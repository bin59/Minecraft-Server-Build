#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
离线奖励 / 在线奖励 编译脚本（离线 javac + 把 sqlite-jdbc 打进 jar）。

依赖（全部本机已有，无网络）：
  - 自研/lib/leaf-api-26.3.build.13-alpha.jar（标准 Bukkit/Leaf API）
  - 自研/lib/PumpkinMail.jar（编译期解析类型；运行时由服务器 PumpkinMail 插件提供）
  - ~/.m2 补齐 leaf-api 引用的 adventure-api / adventure-key / examination-api / annotations / bungeecord-chat
    （Namespaced 在 adventure-key，BaseComponent 在 bungeecord-chat）
  - ~/.m2 sqlite-jdbc（打进最终 jar，自包含运行）

产物：OfflineReward-1.1.0.jar（含 sqlite-jdbc，自包含）。
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
OUT_JAR = os.path.join(ROOT, "OfflineReward-1.1.0.jar")

SHARED_LIB = os.path.normpath(os.path.join(ROOT, "..", "lib"))
M2 = os.path.join(os.path.expanduser("~"), ".m2", "repository")
JDK_HOME = os.environ.get("PF_JDK_HOME", r"C:\Program Files\Java\jdk-25.0.5.0.3")
JAVAC = os.path.join(JDK_HOME, "bin", "javac.exe")
if not os.path.exists(JAVAC):
    JAVAC = os.path.join(JDK_HOME, "bin", "javac")


def extra_jars():
    """补齐 leaf-api 引用的 Adventure / annotations / bungee-chat（systemPath 依赖不带传递依赖）。"""
    out = []
    patterns = [
        os.path.join(M2, "net", "kyori", "adventure-api", "5.2.0", "*.jar"),
        os.path.join(M2, "net", "kyori", "adventure-key", "5.2.0", "*.jar"),
        os.path.join(M2, "net", "kyori", "examination-api", "*", "*.jar"),
        os.path.join(M2, "org", "jetbrains", "annotations", "*", "*.jar"),
        os.path.join(M2, "net", "md-5", "bungeecord-chat", "*", "*.jar"),
    ]
    for p in patterns:
        out.extend(sorted(glob.glob(p)))
    return [j for j in out if not j.endswith(("-sources.jar", "-javadoc.jar"))]


def q(path):
    return '"' + path.replace("\\", "/") + '"'


def main():
    if not os.path.exists(JAVAC):
        print("[FAIL] 找不到 javac：", JAVAC)
        return 1

    sqlite = os.path.join(M2, "org", "xerial", "sqlite-jdbc", "3.53.0.0", "sqlite-jdbc-3.53.0.0.jar")
    jars = [
        os.path.join(SHARED_LIB, "leaf-api-26.3.build.13-alpha.jar"),
        os.path.join(SHARED_LIB, "PumpkinMail.jar"),
    ] + extra_jars() + [sqlite]

    missing = [j for j in jars if not os.path.exists(j)]
    if missing:
        print("[FAIL] 缺依赖 jar：", missing)
        return 1
    cp = os.pathsep.join(jars)
    print("classpath jar 数:", len(jars))

    if os.path.exists(CLASSES):
        shutil.rmtree(CLASSES)
    os.makedirs(CLASSES, exist_ok=True)

    sources = sorted(glob.glob(os.path.join(SRC, "**", "*.java"), recursive=True))
    print("源文件数:", len(sources))
    argfile = os.path.join(ROOT, "target", "javac.args")
    os.makedirs(os.path.dirname(argfile), exist_ok=True)
    with open(argfile, "w", encoding="utf-8") as f:
        f.write("-encoding UTF-8\n--release 21\n-nowarn\n")
        f.write("-cp " + q(cp) + "\n")
        f.write("-d " + q(CLASSES) + "\n")
        for s in sources:
            f.write(q(s) + "\n")

    r = subprocess.run([JAVAC, "@" + argfile], capture_output=True)
    out = (r.stdout or b"").decode("gbk", errors="replace")
    err = (r.stderr or b"").decode("gbk", errors="replace")
    if r.returncode != 0:
        print("[FAIL] javac 编译失败\n" + out + err)
        return 1
    if err.strip():
        print("javac 警告/提示:", err.strip()[:800])

    # 资源（plugin.yml / config.yml）
    for f in glob.glob(os.path.join(RES, "*")):
        if os.path.isfile(f):
            shutil.copy2(f, os.path.join(CLASSES, os.path.basename(f)))

    # 把 sqlite-jdbc 打进最终 jar（保留 native 与 META-INF/services；跳过其 MANIFEST）
    with zipfile.ZipFile(sqlite) as z:
        for n in z.namelist():
            if n.endswith("/") or n == "META-INF/MANIFEST.MF":
                continue
            z.extract(n, CLASSES)
    print("sqlite-jdbc 已并入 classes")

    # 删除旧 jar 与可能残留的 MANIFEST
    old = os.path.join(ROOT, "OfflineReward-1.0.0.jar")
    if os.path.exists(old):
        os.remove(old)
    mf = os.path.join(CLASSES, "META-INF", "MANIFEST.MF")
    if os.path.exists(mf):
        os.remove(mf)
    if os.path.exists(OUT_JAR):
        os.remove(OUT_JAR)

    with zipfile.ZipFile(OUT_JAR, "w", zipfile.ZIP_DEFLATED) as z:
        for base, _, files in os.walk(CLASSES):
            for name in files:
                full = os.path.join(base, name)
                z.write(full, os.path.relpath(full, CLASSES).replace(os.sep, "/"))

    print("[OK] 产物:", OUT_JAR, os.path.getsize(OUT_JAR), "字节")
    return 0


if __name__ == "__main__":
    sys.exit(main())

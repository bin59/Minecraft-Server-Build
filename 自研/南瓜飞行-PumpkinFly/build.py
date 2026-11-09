#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PumpkinFly 离线编译脚本：JDK 25 javac --release 21 + 26.3 实跑 jar。

用法： python build.py
产物： target/PumpkinFly-1.0.0.jar，并同步到项目根目录
"""
import glob
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
LIB = os.path.join(ROOT, "lib")
SRC = os.path.join(ROOT, "src", "main", "java")
RES = os.path.join(ROOT, "src", "main", "resources")
CLASSES = os.path.join(ROOT, "target", "classes")
TARGET = os.path.join(ROOT, "target", "PumpkinFly-1.0.0.jar")
JAR_NAME = "PumpkinFly-1.0.0.jar"

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
        # leaf-api 的类型批注与 sendMessage 重载解析需要
        os.path.join(M2, "com", "google", "guava", "guava", "33.6.0-jre", "*.jar"),
        os.path.join(M2, "net", "md-5", "bungeecord-chat", "1.21-R0.2-deprecated+build.21", "*.jar"),
        os.path.join(M2, "net", "md-5", "bungeecord-chat", "1.20-R0.1", "*.jar"),
    ]
    for p in patterns:
        out.extend(sorted(glob.glob(p)))
    return [j for j in out if not j.endswith(("-sources.jar", "-javadoc.jar"))]


def q(path):
    """javac @argfile 会把反斜杠当转义符，统一转正斜杠并加引号。"""
    return '"' + path.replace("\\", "/") + '"'


def run_javac(argfile):
    r = subprocess.run([JAVAC, "@" + argfile], capture_output=True)
    out = (r.stdout or b"").decode("gbk", errors="replace")
    err = (r.stderr or b"").decode("gbk", errors="replace")
    return r.returncode, out, err


def main():
    if not os.path.exists(JAVAC):
        print("[FAIL] 找不到 javac：", JAVAC)
        return 1
    jars = sorted(glob.glob(os.path.join(LIB, "*.jar"))) + extra_jars()
    if not jars:
        print("[FAIL] lib/ 中没有依赖 jar")
        return 1
    cp = os.pathsep.join(jars)
    print("classpath jar 数:", len(jars))

    if os.path.isdir(CLASSES):
        shutil.rmtree(CLASSES)
    os.makedirs(CLASSES, exist_ok=True)

    sources = sorted(glob.glob(os.path.join(SRC, "**", "*.java"), recursive=True))
    print("源文件数:", len(sources))
    # 用 argfile 绕开 Windows 命令行长度限制
    argfile = os.path.join(ROOT, "target", "javac.args")
    os.makedirs(os.path.dirname(argfile), exist_ok=True)
    with open(argfile, "w", encoding="utf-8") as f:
        f.write('-encoding UTF-8\n--release 21\n-nowarn\n')
        f.write('-cp ' + q(cp) + '\n')
        f.write('-d ' + q(CLASSES) + '\n')
        for s in sources:
            f.write(q(s) + '\n')

    code, out, err = run_javac(argfile)
    if code != 0:
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

    # 部署包只同步到插件项目根目录
    sync = [os.path.join(ROOT, JAR_NAME)]
    for dst in sync:
        if os.path.isdir(os.path.dirname(dst)):
            shutil.copy2(TARGET, dst)
            print("同步 ->", dst)
    print("[OK] 产物:", TARGET, os.path.getsize(TARGET), "bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())

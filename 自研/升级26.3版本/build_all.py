#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
升级26.3版本 · 一键编译所有自研插件为 jar

依赖：JDK 25（读取 leaf-api 26.3 v69 字节码），产物用 --release 21 锁定 Java 21。
无 Maven 时也可用：本脚本直接用 javac 编译 + 手动 jar 打包。

用法：python3 build_all.py
产物：各模块/<模块>/target/<模块>-<version>.jar
"""
import os
import re
import shutil
import subprocess
import sys
import zipfile

BASE = r"F:\game\pc\MC\开服\Minecraft-Server-Build\自研\升级26.3版本"
LEAF_API = os.path.join(BASE, "lib", "leaf-api-26.3.build.13-alpha.jar")
LIBRARIES = r"F:\game\pc\MC\开服\test\1.21.11升级26版本\leaf-26.3\libraries"
JAVAC = r"C:\Program Files\Java\jdk25.0.5_0\bin\javac.exe"
ANNOT = (r"F:\game\pc\MC\开服\test\1.21.11升级26版本\leaf-26.3\libraries"
         r"\org\jetbrains\annotations\24.1.0\annotations-24.1.0.jar")

# 各模块的“外置软依赖”（运行时由服务器提供，仅编译期需要对应 jar 占位）
DEPS = {
    "QuickMenu": [
        r"F:\game\pc\MC\开服\所需文件\互通服\Geyser互通端\Geyser-Spigot.jar",
        r"F:\game\pc\MC\开服\Minecraft-Server-Build\自研\快捷菜单系统\_build\libs\floodgate-api.jar",
        r"F:\game\pc\MC\开服\所需文件\插件\3-玩法与玩家功能插件\02-领地系统-Residence\Residence6.0.0.1.jar",
    ],
    "BuildRate": [os.path.join(BASE, "lib", "PumpkinMail.jar")],
    "OfflineReward": [os.path.join(BASE, "lib", "PumpkinMail.jar")],
    "DailySell": [r"F:\game\pc\MC\开服\所需文件\插件\3-玩法与玩家功能插件\01-经济系统-Vault\Vault.jar"],
    "MoneyLedger": [r"F:\game\pc\MC\开服\所需文件\插件\3-玩法与玩家功能插件\EssentialsX\EssentialsX-2.22.1-dev+25-cfb6f12.jar"],
    "PlanTop": [r"F:\game\pc\MC\开服\所需文件\插件\4-玩家信息与数据展示\04-PlaceholderAPI占位符\PlaceholderAPI-2.12.3.jar"],
}

# 编译顺序：PumpkinMail 先（其余无强依赖，但 BuildRate/OfflineReward 软依赖它）
MODULES = ["PumpkinMail", "BuildRate", "OfflineReward", "DailySell",
           "EnchantShop", "MoneyLedger", "PlanTop", "QuickMenu", "SpacePortal"]


def prepare_deps():
    depsdir = os.path.join(BASE, "_deps")
    os.makedirs(depsdir, exist_ok=True)
    marker = os.path.join(depsdir, ".ready")
    if os.path.exists(marker):
        return depsdir
    print("[deps] 首次收集依赖到 _deps/ ...")
    shutil.copy(LEAF_API, depsdir)
    copied = 0
    for root, _, files in os.walk(LIBRARIES):
        for f in files:
            if not f.endswith(".jar"):
                continue
            p = os.path.join(root, f).replace("\\", "/")
            if "leaf-api" in p:            # 排除 libraries 里两个 leaf-api（用 lib/ 的 13-alpha）
                continue
            if p.endswith("adventure-api-4.16.0.jar"):  # 保留 5.2.0，排除旧版避免混淆
                continue
            shutil.copy(p, depsdir)
            copied += 1
    if os.path.exists(ANNOT):
        shutil.copy(ANNOT, depsdir)
    for jars in DEPS.values():
        for j in jars:
            if os.path.exists(j):
                shutil.copy(j, depsdir)
    print(f"[deps] 复制 libraries jar {copied} 个 + leaf-api + 软依赖/annotations -> _deps/")
    open(marker, "w").close()
    return depsdir


def read_version(module):
    pom = os.path.join(BASE, module, "pom.xml")
    txt = open(pom, encoding="utf-8").read()
    m = re.search(r"<version>\s*([0-9A-Za-z][^<]*?)\s*</version>", txt)
    return m.group(1) if m else "1.0.0"


def build(module, depsdir):
    src = os.path.join(BASE, module, "src", "main", "java")
    res = os.path.join(BASE, module, "src", "main", "resources")
    out = os.path.join(BASE, module, "target", "classes")
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out, exist_ok=True)

    java_files = []
    for root, _, files in os.walk(src):
        for f in files:
            if f.endswith(".java"):
                java_files.append(os.path.join(root, f))
    if not java_files:
        print(f"[SKIP] {module}: 无 .java 源文件")
        return False

    cp = depsdir.replace("\\", "/") + "/*"
    cmd = [JAVAC, "--release", "21", "-encoding", "UTF-8", "-d", out, "-cp", cp] + java_files
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        print(f"[FAIL] {module} rc={r.returncode}")
        sys.stdout.write(r.stdout[-1500:])
        print(r.stderr[-3500:])
        return False

    if os.path.isdir(res):
        for root, _, files in os.walk(res):
            for f in files:
                sp = os.path.join(root, f)
                rel = os.path.relpath(sp, res)
                dp = os.path.join(out, rel)
                os.makedirs(os.path.dirname(dp), exist_ok=True)
                shutil.copy(sp, dp)

    version = read_version(module)
    jarname = f"{module}-{version}.jar"
    jarpath = os.path.join(BASE, module, "target", jarname)
    with zipfile.ZipFile(jarpath, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0\nCreated-By: build_all.py (JDK25 --release 21)\n")
        for root, _, files in os.walk(out):
            for f in files:
                fp = os.path.join(root, f)
                arc = os.path.relpath(fp, out).replace(os.sep, "/")
                z.write(fp, arc)
    nclass = sum(1 for _, _, fs in os.walk(out) for x in fs if x.endswith(".class"))
    print(f"[OK]   {module:<14} -> target/{jarname}  ({len(java_files)} 源文件, {nclass} class)")
    return True


if __name__ == "__main__":
    depsdir = prepare_deps()
    print(f"[deps] 使用: {depsdir}\n")
    all_ok = True
    for m in MODULES:
        if not build(m, depsdir):
            all_ok = False
    print("\n==== " + ("全部编译打包完成 ✅" if all_ok else "存在失败模块 ❌") + " ====")
    sys.exit(0 if all_ok else 1)

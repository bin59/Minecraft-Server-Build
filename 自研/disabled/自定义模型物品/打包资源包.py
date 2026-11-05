# -*- coding: utf-8 -*-
"""
把「模型物品资源包」目录压成资源包 zip。

- zip 根目录直接是 pack.mcmeta / pack.png / assets/（不要多套一层文件夹）
- 输出：当前目录下的 自定义模型物品包.zip
"""
import os
import zipfile

BASE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(BASE, "模型物品资源包")
OUT = os.path.join(BASE, "自定义模型物品包.zip")

if __name__ == "__main__":
    if not os.path.isdir(SRC):
        raise SystemExit("找不到目录：%s" % SRC)

    n = 0
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        for root, _dirs, files in os.walk(SRC):
            for name in sorted(files):
                full = os.path.join(root, name)
                arc = os.path.relpath(full, SRC).replace("\\", "/")
                z.write(full, arc)
                n += 1

    print("已打包 %d 个文件 -> %s" % (n, OUT))
    print("SHA1（填进 server.properties 的 resource-pack-sha1）：")
    import hashlib
    print(hashlib.sha1(open(OUT, "rb").read()).hexdigest())

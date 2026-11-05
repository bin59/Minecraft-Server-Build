# -*- coding: utf-8 -*-
"""
生成自定义模型物品资源包里用到的占位纹理（PNG）。

用途：本脚本只为「没有画图工具、又想先把资源包跑通」的场景准备占位图。
生成的是 16x16 的纯色/简单图案纹理，可直接替换成真实的 32x32 或 64x64 美术图。

用法：
    python 生成纹理.py      （在脚本所在目录执行）
"""
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "模型物品资源包")


def write_png(path, pixels, size=16):
    """pixels: list[list[(r,g,b,a)]]，左上角为 (0,0)"""
    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type 0
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def solid(color, checker=None):
    """生成一个 16x16 纹理；checker 非空时画成 8x8 棋盘格方便看贴图对齐"""
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            if checker and ((x // 8) + (y // 8)) % 2 == 0:
                row.append(checker)
            else:
                row.append(color)
        px.append(row)
    return px


def gradient(c1, c2):
    """左上 -> 右下的斜向渐变，用来体现立体感"""
    px = []
    for y in range(16):
        row = []
        for x in range(16):
            t = (x + y) / 30.0
            row.append(tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)) + (255,))
        px.append(row)
    return px


def pack_icon():
    """64x64 资源包图标：橙绿配色的南瓜色块"""
    px = []
    for y in range(64):
        row = []
        for x in range(64):
            inside = 12 <= x < 52 and 14 <= y < 56
            stem = 28 <= x < 36 and 4 <= y < 16
            if stem:
                row.append((92, 138, 58, 255))
            elif inside:
                row.append((217, 138, 47, 255))
            else:
                row.append((28, 26, 24, 255))
        px.append(row)
    write_png(os.path.join(ROOT, "pack.png"), px, 64)


if __name__ == "__main__":
    # 南瓜刃：橙黄渐变 + 棋盘格，方便确认 UV 是否对得很正
    write_png(os.path.join(ROOT, "assets", "minecraft", "textures", "item", "pumpkin_blade.png"),
              gradient((214, 133, 40), (247, 205, 96)), 16)
    # 虚空核：深紫到亮紫的渐变，带棋盘格
    write_png(os.path.join(ROOT, "assets", "minecraft", "textures", "item", "nexus_core.png"),
              gradient((78, 44, 128), (162, 108, 232)), 16)
    # 资源包图标
    pack_icon()
    print("纹理生成完毕 ->", ROOT)

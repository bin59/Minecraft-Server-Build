# -*- coding: utf-8 -*-
"""
系统商店商品一键同步
用法：停服后运行  python sync_shop.py
读同目录的「系统商店商品管理.xlsx」，写入 EconomyShop SQLite。

注意：本脚本用 Python 标准库直接解析 xlsx（zipfile + xml.etree），
不依赖 openpyxl，避免某些软件导出的 xlsx 在 openpyxl 解析 styles.xml 时
抛 `Fill() takes no arguments` 之类样式错误导致整脚本崩溃。
"""
import os, sys, sqlite3, time
import zipfile, re
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
XLSX = os.path.join(HERE, "系统商店商品管理.xlsx")
DB = os.path.join(HERE, "data.db")
CATEGORY_NAME = "新手补给"

NS = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
RNS = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"


def _col_to_idx(ref):
    """单元格引用如 'B12' -> 列索引 1（0 基）。"""
    m = re.match(r"([A-Z]+)(\d+)", ref or "")
    if not m:
        return 0
    letters = m.group(1)
    idx = 0
    for ch in letters:
        idx = idx * 26 + (ord(ch) - 64)  # 'A' -> 1
    return idx - 1


def _sheet_map(zf):
    """返回 {工作表名: worksheets/xxx.xml 路径}。"""
    smap = {}
    try:
        wb = ET.fromstring(zf.read("xl/workbook.xml"))
    except KeyError:
        return smap
    rels = ET.fromstring(zf.read("xl/_rels/workbook.xml.rels"))
    rid_to_target = {rel.get("Id"): rel.get("Target") for rel in rels}
    for sheet in wb.iter(f"{NS}sheet"):
        name = sheet.get("name")
        rid = sheet.get(f"{RNS}id")
        target = rid_to_target.get(rid, "")
        if target and not target.startswith("xl/"):
            target = "xl/" + target.lstrip("/")
        if name:
            smap[name] = target
    return smap


def _shared_strings(zf):
    """读取共享字符串表，索引 -> 文本。"""
    try:
        data = zf.read("xl/sharedStrings.xml")
    except KeyError:
        return []
    root = ET.fromstring(data)
    out = []
    for si in root.findall(f"{NS}si"):
        texts = [t.text or "" for t in si.iter(f"{NS}t")]
        out.append("".join(texts))
    return out


def _cell_value(cell, shared):
    """从 <c> 节点取出缓存值（等价于 openpyxl 的 data_only）。"""
    t = cell.get("t")
    v = cell.find(f"{NS}v")
    is_el = cell.find(f"{NS}is")
    if t == "s":  # 共享字符串
        if v is not None and v.text is not None:
            try:
                return shared[int(v.text)]
            except (ValueError, IndexError):
                return ""
        return ""
    if t == "inlineStr":  # 内联字符串
        if is_el is not None:
            return "".join(x.text or "" for x in is_el.iter(f"{NS}t"))
        return ""
    if t == "str":  # 公式字符串结果
        return v.text if v is not None else ""
    if t == "b":  # 布尔
        return (v.text == "1") if v is not None else False
    # 默认（数值/日期序列号）原样返回字符串
    if v is not None:
        return v.text
    return None


def read_sheet_rows(path, sheet_name=None):
    """读取指定工作表所有行，每行是单元格值列表（含 None）。"""
    with zipfile.ZipFile(path) as zf:
        smap = _sheet_map(zf)
        target = None
        if sheet_name and sheet_name in smap:
            target = smap[sheet_name]
        elif smap:
            target = next(iter(smap.values()))
        if not target:
            return []
        shared = _shared_strings(zf)
        root = ET.fromstring(zf.read(target))
        sheet_data = root.find(f"{NS}sheetData")
        if sheet_data is None:
            return []
        rows = []
        for row in sheet_data.findall(f"{NS}row"):
            cells = {}
            maxc = -1
            for c in row.findall(f"{NS}c"):
                ci = _col_to_idx(c.get("r"))
                maxc = max(maxc, ci)
                cells[ci] = _cell_value(c, shared)
            rows.append([cells.get(i) for i in range(maxc + 1)])
        return rows


def main():
    if not os.path.exists(XLSX):
        print("找不到 Excel:", XLSX); sys.exit(1)
    if not os.path.exists(DB):
        print("找不到数据库:", DB); sys.exit(1)

    # 读 Excel（标准库直读，绕过 openpyxl 样式解析）
    try:
        rows_raw = read_sheet_rows(XLSX, "商品清单")
    except Exception as e:
        print("读取 Excel 失败:", e); sys.exit(1)
    if not rows_raw:
        print("Excel 为空或未找到「商品清单」工作表"); sys.exit(1)

    rows = []
    for r in rows_raw[1:]:  # 跳过表头
        r = list(r) + [None] * 9
        r = r[:9]
        if not r or not r[0]:
            continue
        material, cn, cat, buy, sell, smode, qty, enabled, note = r
        rows.append(dict(
            material=str(material).strip().upper(),
            category=str(cat).strip(),
            buy=float(buy or 0),
            sell=float(sell or 0),
            stock_mode=str(smode).strip().lower(),
            stock=int(qty or 0),
            enabled=(str(enabled).strip() == "是"),
        ))
    print(f"读入 {len(rows)} 条商品")

    conn = sqlite3.connect(DB)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    # 找/建分类
    cur.execute("SELECT id FROM es_categories WHERE name=?", (CATEGORY_NAME,))
    row = cur.fetchone()
    if row:
        cat_id = row["id"]
    else:
        cur.execute("INSERT INTO es_categories (name, display_name, slot, sort_order, enabled) VALUES (?,?,?,?,?)",
                    (CATEGORY_NAME, CATEGORY_NAME, 1, 1, 1))
        cat_id = cur.lastrowid
        print(f"新建分类: {CATEGORY_NAME} (id={cat_id})")

    now_ts = int(time.time())

    # 软删旧商品（同分类）
    cur.execute("UPDATE es_items SET deleted_at=? WHERE category_id=? AND deleted_at IS NULL",
                (now_ts, cat_id))
    print(f"软删除旧商品 {cur.rowcount} 条")

    # 插入新商品
    n = 0
    for it in rows:
        if not it["enabled"]:
            continue
        smode = "INFINITE" if it["stock_mode"] != "finite" else "FINITE"
        cur.execute("""INSERT INTO es_items
            (category_id, material, item_data, buy_price, sell_price, stock_mode,
             current_stock, max_stock, restock_amount, restock_interval,
             dynamic_pricing, permission, slot, sort_order, enabled)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (cat_id, it["material"], None, it["buy"], it["sell"], smode,
             it["stock"], it["stock"], 0, 0,
             0, None, 1, n+1, 1))
        n += 1
    conn.commit()

    # 验证
    cur.execute("SELECT material, buy_price, sell_price, stock_mode FROM es_items WHERE category_id=? AND deleted_at IS NULL", (cat_id,))
    result = cur.fetchall()
    print(f"\n同步完成，当前商店有效商品 {len(result)} 条：")
    for r in result:
        print(f"  {r['material']:<16} 买:{r['buy_price']:<6} 卖:{r['sell_price']:<6} {r['stock_mode']}")
    conn.close()
    print("\n下一步：启动服务器，进游戏 /shop admin reload 确认。")


if __name__ == "__main__":
    main()

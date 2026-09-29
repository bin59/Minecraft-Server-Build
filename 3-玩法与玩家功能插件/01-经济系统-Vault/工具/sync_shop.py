# -*- coding: utf-8 -*-
"""
系统商店商品一键同步
用法：停服后运行  python sync_shop.py
读同目录的「系统商店商品管理.xlsx」，写入 EconomyShop SQLite。
"""
import os, sys, sqlite3, datetime
import openpyxl

HERE = os.path.dirname(os.path.abspath(__file__))
XLSX = os.path.join(HERE, "系统商店商品管理.xlsx")
DB = r"C:\mc_serve\1.21.11-test\plugins\EconomyShop\data.db"
CATEGORY_NAME = "新手补给"

def main():
    if not os.path.exists(XLSX):
        print("找不到 Excel:", XLSX); sys.exit(1)
    if not os.path.exists(DB):
        print("找不到数据库:", DB); sys.exit(1)

    # 读 Excel
    wb = openpyxl.load_workbook(XLSX, data_only=True)
    ws = wb["商品清单"]
    rows = []
    for r in ws.iter_rows(min_row=2, values_only=True):
        if not r or not r[0]: continue
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
        cur.execute("INSERT INTO es_categories (name, display_name, slot, sort_order, enabled, created_at) VALUES (?,?,?,?,?,?)",
                    (CATEGORY_NAME, CATEGORY_NAME, 1, 1, 1, datetime.datetime.now().isoformat()))
        cat_id = cur.lastrowid
        print(f"新建分类: {CATEGORY_NAME} (id={cat_id})")

    # 软删旧商品（同分类）
    cur.execute("UPDATE es_items SET deleted_at=? WHERE category_id=? AND deleted_at IS NULL",
                (datetime.datetime.now().isoformat(), cat_id))
    print(f"软删除旧商品 {cur.rowcount} 条")

    # 插入新商品
    n = 0
    for it in rows:
        if not it["enabled"]: continue
        smode = "infinite" if it["stock_mode"] != "finite" else "finite"
        cur.execute("""INSERT INTO es_items
            (category_id, material, item_data, buy_price, sell_price, stock_mode,
             current_stock, max_stock, restock_amount, restock_interval,
             dynamic_pricing, permission, slot, sort_order, enabled, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (cat_id, it["material"], None, it["buy"], it["sell"], smode,
             it["stock"], it["stock"], 0, 0,
             0, None, 1, n+1, 1, datetime.datetime.now().isoformat()))
        n += 1
    conn.commit()

    # 验证
    cur.execute("SELECT material, buy_price, sell_price, stock_mode FROM es_items WHERE category_id=? AND deleted_at IS NULL", (cat_id,))
    print(f"\n同步完成，当前商店有效商品 {cur.rowcount} 条：")
    for r in cur.fetchall():
        print(f"  {r['material']:<16} 买:{r['buy_price']:<6} 卖:{r['sell_price']:<6} {r['stock_mode']}")
    conn.close()
    print("\n下一步：启动服务器，进游戏 /shop admin reload 确认。")

if __name__ == "__main__":
    main()

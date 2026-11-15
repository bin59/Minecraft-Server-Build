# -*- coding: utf-8 -*-
"""Read the whole target doc (paginated) and validate its content."""
import sys, json, re
sys.path.insert(0, r"C:/Users/19594/.workbuddy/plugins/cache/workbuddy-builtin/tencent-docs-plugin/5.9.3-wb.42398134.gd4692421.h480e8d1fee0f/skills/tencent-docs")
import tencentdocs

FILE_ID = "BBWEOPlbmQNb"


def read_all():
    parts = []
    token = ""
    pages = 0
    import time
    while True:
        args = {"file_id": FILE_ID, "size": 20}
        if token:
            args["next_token"] = token
        res, err = tencentdocs.call_tool("tencent-docs", "smartcanvas.read", args)
        # retry on access-limit / errors with backoff
        tries = 0
        while (err or (isinstance(res, dict) and res.get("error"))) and tries < 15:
            code = (res.get("error") or {}).get("code") if isinstance(res, dict) else None
            print("  (rate/access limit %s, retry %d/15)" % (code, tries + 1))
            time.sleep(12)
            res, err = tencentdocs.call_tool("tencent-docs", "smartcanvas.read", args)
            tries += 1
        if err or (isinstance(res, dict) and res.get("error")):
            print("read failed after retries:", res.get("error") if isinstance(res, dict) else err)
            break
        try:
            text = res["result"]["content"][0]["text"]
            obj = json.loads(text)
        except Exception as e:
            print("parse err", e)
            break
        parts.append(obj.get("content", ""))
        token = obj.get("next_token", "")
        pages += 1
        if not token:
            break
        if pages > 200:
            print("safety stop")
            break
    return "\n".join(parts), pages


def main():
    mdx, pages = read_all()
    print("pages read:", pages)
    print("total mdx chars:", len(mdx))

    # frontmatter title
    m = re.search(r"title:\s*(.+)", mdx)
    print("title:", m.group(1).strip() if m else None)

    # chapter titles (H1 text)
    chapters = ["一、核心与网络层", "二、运维监控与面板", "三、基础指令与经济系统",
                "四、玩法与玩家功能", "五、家园/地图/模型/附魔/死亡/皮肤",
                "六、权限/数据/分析核心", "七、服务器管理工具", "八、自研插件"]
    print("--- chapter presence (should each be 1) ---")
    for ch in chapters:
        print("  %s : %d" % (ch, mdx.count(ch)))

    # tables
    print("Table blocks:", mdx.count("<Table"))
    print("TableCell blocks:", mdx.count("<TableCell"))

    # leftover broken anchor links [text]()  (should be 0 after fix)
    anchor_links = re.findall(r"\[[^\]]+\]\(\)", mdx)
    print("empty-anchor links [text]() (should be 0):", len(anchor_links))

    # raw unescaped <玩家> style outside code (rough check)
    raw_angle = len(re.findall(r"<[一-鿿A-Za-z]", mdx))  # < followed by CJK/latin (likely stray)
    print("suspicious '<x' tokens:", raw_angle)

    # plugin section headings count (## plugin names) - just report total Heading count
    print("Heading blocks:", len(re.findall(r"<Heading", mdx)))

    # duplicate detection: count occurrences of a unique marker from chunk 0 start
    print("'说明：本教程仅收录' count (should be 1):", mdx.count("说明：本教程仅收录"))


if __name__ == "__main__":
    main()

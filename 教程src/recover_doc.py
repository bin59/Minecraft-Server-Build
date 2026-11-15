# -*- coding: utf-8 -*-
"""Resilient doc recovery: assess completeness, then reset+repush if needed.

All API calls go through call_retry() which retries on access-limit (400007)
with backoff, so a transient or longer quota cooldown is handled.
"""
import sys, json, re, time, os

HERE = os.path.dirname(os.path.abspath(__file__))
TDOCPATH = r"C:/Users/19594/.workbuddy/plugins/cache/workbuddy-builtin/tencent-docs-plugin/5.9.3-wb.42398134.gd4692421.h480e8d1fee0f/skills/tencent-docs"
sys.path.insert(0, TDOCPATH)
import tencentdocs

FILE_ID = "BBWEOPlbmQNb"
CHUNKS_PATH = os.path.join(HERE, "chunks.json")

RETRY = 40
DELAY = 15


def call_retry(service, tool, args):
    last = None
    for t in range(RETRY):
        res, err = tencentdocs.call_tool(service, tool, args)
        if err:
            print("  call err %s (retry %d/%d)" % (err, t + 1, RETRY))
            time.sleep(DELAY)
            continue
        e = res.get("error") if isinstance(res, dict) else None
        if e:
            print("  api error %s (retry %d/%d)" % (e.get("code"), t + 1, RETRY))
            time.sleep(DELAY)
            last = res
            continue
        return res
    print("  GAVE UP after retries")
    return last


def get_mdx(res):
    try:
        text = res["result"]["content"][0]["text"]
        obj = json.loads(text)
        return obj.get("content", "")
    except Exception:
        return ""


def read_all():
    parts = []
    token = ""
    pages = 0
    while True:
        args = {"file_id": FILE_ID, "size": 20}
        if token:
            args["next_token"] = token
        res = call_retry("tencent-docs", "smartcanvas.read", args)
        if not res or res.get("error"):
            print("read_all failed")
            break
        mdx = get_mdx(res)
        parts.append(mdx)
        obj = json.loads(res["result"]["content"][0]["text"])
        token = obj.get("next_token", "")
        pages += 1
        if not token:
            break
        if pages > 300:
            print("safety stop pages")
            break
    return "\n".join(parts), pages


def reset_doc():
    print("== reset (delete all top-level blocks) ==")
    rounds = 0
    while True:
        res = call_retry("tencent-docs", "smartcanvas.read", {"file_id": FILE_ID, "size": 20})
        if not res or res.get("error"):
            break
        mdx = get_mdx(res)
        ids = re.findall(r'^<(\w+)\b[^>]*\bid="([^"]+)"', mdx, re.MULTILINE)
        ids = [i for _, i in ids]
        if not ids:
            break
        print("round %d: deleting %d top-level blocks" % (rounds, len(ids)))
        for i in ids:
            call_retry("tencent-docs", "smartcanvas.edit",
                       {"file_id": FILE_ID, "action": "DELETE", "id": i})
            time.sleep(0.05)
        rounds += 1
        time.sleep(0.1)
        if rounds > 400:
            print("safety stop rounds")
            break
    print("reset done after %d rounds" % rounds)


def push_all():
    with open(CHUNKS_PATH, encoding="utf-8") as f:
        chunks = json.load(f)
    print("== push %d chunks ==" % len(chunks))
    for i, c in enumerate(chunks):
        res = call_retry("tencent-docs", "smartcanvas.edit",
                         {"file_id": FILE_ID, "action": "INSERT_AFTER", "id": "", "content": c})
        if not res or res.get("error"):
            print("push chunk %d FAILED" % i)
            return False
        time.sleep(0.4)
    print("all chunks pushed")
    return True


def assess(mdx):
    chapters = ["一、核心与网络层", "二、运维监控与面板", "三、基础指令与经济系统",
                "四、玩法与玩家功能", "五、家园/地图/模型/附魔/死亡/皮肤",
                "六、权限/数据/分析核心", "七、服务器管理工具", "八、自研插件"]
    present = [ch for ch in chapters if ch in mdx]
    return present, len(mdx)


def main():
    print("== reading current doc ==")
    mdx, pages = read_all()
    present, length = assess(mdx)
    print("pages:", pages, "chars:", length)
    print("chapters present (%d/8): %s" % (len(present), present))
    print("Table blocks:", mdx.count("<Table"))
    print("说明 marker count (should be 1):", mdx.count("说明：本教程仅收录"))

    if len(present) == 8 and length > 150000:
        print("DOC ALREADY COMPLETE - nothing to do")
    else:
        print("DOC INCOMPLETE - performing reset + repush")
        reset_doc()
        push_all()
        mdx2, pages2 = read_all()
        present2, length2 = assess(mdx2)
        print("AFTER: chapters (%d/8): %s, chars: %d, tables: %d" %
              (len(present2), present2, length2, mdx2.count("<Table")))


if __name__ == "__main__":
    main()

# -*- coding: utf-8 -*-
"""Focused re-push of the 16 chunks into the (currently empty) Tencent smartcanvas doc.

The earlier 'fresh' run cleared the doc but its 16 inserts were all rejected by the
400007 rate limit, so the doc is empty. This script re-pushes with aggressive
backoff per call (no competing reads) and then verifies with a read loop.

Verify also checks each chapter heading appears EXACTLY ONCE (no duplicates).
"""
import os
import sys
import re
import json
import time

HERE = os.path.dirname(os.path.abspath(__file__))
TDOCPATH = r"C:/Users/19594/.workbuddy/plugins/cache/workbuddy-builtin/tencent-docs-plugin/5.9.3-wb.42398134.gd4692421.h480e8d1fee0f/skills/tencent-docs"
sys.path.insert(0, TDOCPATH)
import tencentdocs

FILE_ID = "BBWEOPlbmQNb"
CHUNKS = json.load(open(os.path.join(HERE, "chunks.json"), encoding="utf-8"))

CHAPTERS = [
    "一、核心与网络层", "二、运维监控与面板", "三、基础指令与经济系统",
    "四、玩法与玩家功能", "五、家园/地图/模型/附魔/死亡/皮肤",
    "六、权限/数据/分析核心", "七、服务器管理工具", "八、自研插件",
]

MAX_ATTEMPTS = 60
BACKOFF = [30, 60, 120, 180, 240, 300]


def log(msg):
    print("[%s] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


def call(tool, args, label):
    for attempt in range(MAX_ATTEMPTS):
        res, err = tencentdocs.call_tool("tencent-docs", tool, args)
        if err:
            log("%s err: %s (a%d)" % (label, err, attempt + 1))
            time.sleep(15)
            continue
        e = res.get("error") if isinstance(res, dict) else None
        if e and str(e.get("code")) == "400007":
            wait = BACKOFF[min(attempt, len(BACKOFF) - 1)]
            log("%s 400007 (a%d wait %ds)" % (label, attempt + 1, wait))
            time.sleep(wait)
            continue
        if e:
            log("%s api error: %s" % (label, e))
            return None
        return res
    log("%s gave up" % label)
    return None


def repush():
    ok_count = 0
    for idx, content in enumerate(CHUNKS):
        args = {"file_id": FILE_ID, "action": "INSERT_AFTER", "id": "", "content": content}
        res = call("smartcanvas.edit", args, "ins-%d" % idx)
        if res is None:
            log("FAILED chunk %d" % idx)
            return False
        ok_count += 1
        time.sleep(0.15)
    log("repushed %d/%d chunks" % (ok_count, len(CHUNKS)))
    return True


def read_all():
    parts = []
    token = ""
    pages = 0
    while True:
        args = {"file_id": FILE_ID, "size": 20}
        if token:
            args["next_token"] = token
        res = call("smartcanvas.read", args, "read")
        if res is None:
            return None
        try:
            obj = json.loads(res["result"]["content"][0]["text"])
        except Exception as ex:
            log("read parse err %s" % ex)
            return None
        parts.append(obj.get("content", ""))
        token = obj.get("next_token", "")
        pages += 1
        if not token or pages > 400:
            break
    return "\n".join(parts), pages


def verify(mdx):
    rep = []
    found = [c for c in CHAPTERS if mdx.count(c) >= 1]
    dup = [c for c in CHAPTERS if mdx.count(c) > 1]
    rep.append("章节出现: %d/8" % len(found))
    rep.append("重复章节: %s" % (dup if dup else "无"))
    rep.append("表格<Table: %d, 单元格<TableCell: %d" % (mdx.count("<Table"), mdx.count("<TableCell")))
    rep.append("加粗<Mark bold>: %d" % mdx.count("<Mark bold>"))
    rep.append("损坏锚点[x](): %d" % len(re.findall(r"\[[^\]]+\]\(\s*\)", mdx)))
    rep.append("总块id=: %d" % len(re.findall(r'id="', mdx)))
    ok = (len(found) == 8) and (not dup)
    return ok, rep


def main():
    log("=== repush start (doc known empty) ===")
    if not repush():
        log("repush failed; will retry whole sequence in 120s")
        time.sleep(120)
        return main()
    log("repush returned; verifying via read loop")
    for attempt in range(12):
        out = read_all()
        if out is None:
            log("verify read failed (a%d), wait 120s" % attempt)
            time.sleep(120)
            continue
        mdx, pages = out
        ok, rep = verify(mdx)
        for r in rep:
            log("  " + r)
        if ok:
            log("VERIFY PASS (pages=%d)" % pages)
            return
        else:
            log("verify not clean (a%d); wait 120s then re-read" % attempt)
            time.sleep(120)
    log("verify could not confirm clean after attempts")


if __name__ == "__main__":
    main()

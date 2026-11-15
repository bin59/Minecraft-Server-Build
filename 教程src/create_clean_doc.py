# -*- coding: utf-8 -*-
"""Create a CLEAN Tencent smartcanvas doc from the verified-clean chunks.json.

The original BBWEOPlbmQNb got mass-duplicated by earlier rate-limited pushes and
cannot be cheaply repaired (hundreds of deletes would re-trip the 400007 limit).
So we create a fresh doc: body = chunk0, then INSERT_AFTER append chunks 1..15.

Then verify: 8/8 chapters, single copy (no dupes), tables sane, no broken [x]().
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

TITLE = "南瓜国际服插件使用教程"
CHUNKS = json.load(open(os.path.join(HERE, "chunks.json"), encoding="utf-8"))

CHAPTERS = [
    "一、核心与网络层", "二、运维监控与面板", "三、基础指令与经济系统",
    "四、玩法与玩家功能", "五、家园/地图/模型/附魔/死亡/皮肤",
    "六、权限/数据/分析核心", "七、服务器管理工具", "八、自研插件",
]

MAX_ATTEMPTS = 60
BACKOFF = [20, 40, 80, 120, 180, 240, 300]


def log(msg):
    print("[%s] %s" % (time.strftime("%H:%M:%S"), msg), flush=True)


def call(tool, args, label):
    for attempt in range(MAX_ATTEMPTS):
        res, err = tencentdocs.call_tool("tencent-docs", tool, args)
        if err:
            log("%s err: %s (a%d)" % (label, err, attempt + 1))
            time.sleep(10)
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


def create_doc():
    log("creating SMARTCANVAS doc title=%s" % TITLE)
    res = call("manage.create_file",
               {"title": TITLE, "file_type": "smartcanvas"}, "create")
    if res is None:
        return None
    txt = json.dumps(res, ensure_ascii=False)
    m = re.search(r'"file_id"\s*:\s*"([^"]+)"', txt)
    fid = m.group(1) if m else None
    log("created file_id=%s" % fid)
    log("create resp head: %s" % txt[:400])
    return fid


def append_chunks(fid):
    ok = 0
    for i in range(0, len(CHUNKS)):
        args = {"file_id": fid, "action": "INSERT_AFTER", "id": "", "content": CHUNKS[i]}
        res = call("smartcanvas.edit", args, "ins-%d" % i)
        if res is None:
            log("FAILED append chunk %d" % i)
            return False
        ok += 1
        time.sleep(0.1)
    log("appended %d chunks (0..%d)" % (ok, len(CHUNKS) - 1))
    return True


def read_all(fid):
    parts = []
    token = ""
    pages = 0
    while True:
        args = {"file_id": fid, "size": 20}
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


# unique substring per chapter (heading text varies in read-back normalization)
CHAPTER_KEYS = [
    "核心与网络层", "运维监控与面板", "基础指令与经济系统",
    "玩法与玩家功能", "家园/地图/模型/附魔/死亡/皮肤",
    "权限/数据/分析核心", "服务器管理工具", "自研插件",
]


def verify(mdx):
    rep = []
    counts = {k: mdx.count(k) for k in CHAPTER_KEYS}
    rep.append("各章出现次数: %s" % counts)
    dup = [k for k, n in counts.items() if n > 1]
    missing = [k for k, n in counts.items() if n == 0]
    rep.append("重复章节: %s" % (dup if dup else "无"))
    rep.append("缺失章节: %s" % (missing if missing else "无"))
    rep.append("表格<Table: %d  单元格<TableCell: %d" % (mdx.count("<Table"), mdx.count("<TableCell")))
    # real broken anchors = [label]() where label is NOT a linkified URL
    broken = [m.group(1) for m in re.finditer(r"\[([^\]]+)\]\(\s*\)", mdx)
              if not m.group(1).lower().startswith("http")]
    rep.append("真实损坏锚点([非URL]()): %d" % len(broken))
    rep.append("总块id=: %d" % len(re.findall(r'id="', mdx)))
    ok = (not dup) and (not missing) and (len(broken) == 0)
    return ok, rep


def delete_doc(fid):
    log("deleting stale doc %s" % fid)
    res = call("manage.delete_file", {"file_id": fid}, "del")
    if res is None:
        log("delete returned None (may have failed or ok)")
    else:
        log("delete resp: %s" % json.dumps(res, ensure_ascii=False)[:200])


def main():
    # clean up the previously-created incomplete doc (missing chunk0)
    delete_doc("BapHPvvGWnLP")
    fid = create_doc()
    if not fid:
        log("CREATE FAILED; abort")
        return
    if not append_chunks(fid):
        log("APPEND FAILED; doc %s incomplete" % fid)
        return
    log("waiting 5s then verify")
    time.sleep(5)
    out = read_all(fid)
    if out is None:
        log("verify read failed; doc %s likely OK but unverified" % fid)
        return
    mdx, pages = out
    ok, rep = verify(mdx)
    for r in rep:
        log("  " + r)
    log("VERIFY %s (pages=%d) file_id=%s" % ("PASS" if ok else "CHECK", pages, fid))


if __name__ == "__main__":
    main()

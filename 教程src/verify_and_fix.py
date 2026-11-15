# -*- coding: utf-8 -*-
"""Resilient verify/fix for the Tencent smartcanvas doc.

Strategy to survive the 400007 access-limit:
  - long initial cooldown so the limit window resets
  - exponential backoff on every API call
  - read-only verification first; only reset+repush if genuinely incomplete
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

INITIAL_COOLDOWN = 20 * 60  # 20 min, let the quota window reset
MAX_ATTEMPTS = 15
BACKOFF = [30, 60, 120, 180, 240, 300]  # seconds, cycled


def log(msg):
    ts = time.strftime("%H:%M:%S")
    line = "[%s] %s" % (ts, msg)
    print(line, flush=True)


def call(tool, args, label="call"):
    """Call with exponential backoff on 400007."""
    for attempt in range(MAX_ATTEMPTS):
        res, err = tencentdocs.call_tool("tencent-docs", tool, args)
        if err:
            log("%s err: %s (retry %d)" % (label, err, attempt + 1))
            time.sleep(15)
            continue
        e = res.get("error") if isinstance(res, dict) else None
        if e and str(e.get("code")) == "400007":
            wait = BACKOFF[min(attempt, len(BACKOFF) - 1)]
            log("%s rate-limited 400007 (retry %d, wait %ds)" % (label, attempt + 1, wait))
            time.sleep(wait)
            continue
        if e:
            log("%s api error: %s" % (label, e))
            return None
        return res
    log("%s gave up after %d attempts" % (label, MAX_ATTEMPTS))
    return None


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
            text = res["result"]["content"][0]["text"]
            obj = json.loads(text)
        except Exception as ex:
            log("read parse err: %s" % ex)
            return None
        parts.append(obj.get("content", ""))
        token = obj.get("next_token", "")
        pages += 1
        if not token or pages > 300:
            break
    return "\n".join(parts), pages


def verify(mdx):
    """Return (ok, report_lines)."""
    rep = []
    # chapter headings (our top-level chapters)
    chapters = [
        "一、核心与网络层", "二、运维监控与面板", "三、基础指令与经济系统",
        "四、玩法与玩家功能", "五、家园/地图/模型/附魔/死亡/皮肤",
        "六、权限/数据/分析核心", "七、服务器管理工具", "八、自研插件",
    ]
    found = [c for c in chapters if c in mdx]
    rep.append("章节齐全: %d/8 -> %s" % (len(found), "OK" if len(found) == 8 else "MISSING %s" % (set(chapters) - set(found))))
    rep.append("表格数(Table): %d" % mdx.count("<Table"))
    rep.append("表头单元格(TableCell): %d" % mdx.count("<TableCell"))
    rep.append("加粗标记(Mark bold): %d" % mdx.count("<Mark bold>"))
    # broken anchor links should be gone (no '[x]()')
    broken = re.findall(r"\[[^\]]+\]\(\s*\)", mdx)
    rep.append("损坏锚点链接([x]()): %d" % len(broken))
    # raw stray angle placeholders outside known tags (rough check)
    stray = re.findall(r"<[一-鿿]+>", mdx)
    rep.append("可疑裸占位符(<中文>): %d" % len(stray))
    # total blocks (id=) count
    rep.append("总块数(id=): %d" % len(re.findall(r'id="', mdx)))
    ok = (len(found) == 8) and (len(broken) == 0)
    return ok, rep


def reset_doc():
    log("== resetting doc (top-level cascade deletes) ==")
    rounds = 0
    while True:
        res = call("smartcanvas.read", {"file_id": FILE_ID}, "read-reset")
        if res is None:
            return False
        try:
            obj = json.loads(res["result"]["content"][0]["text"])
            mdx = obj.get("content", "")
        except Exception:
            mdx = ""
        ids = re.findall(r'^<(\w+)\b[^>]*\bid="([^"]+)"', mdx, re.MULTILINE)
        ids = [i for _, i in ids]
        if not ids:
            break
        log("round %d: deleting %d top-level blocks" % (rounds, len(ids)))
        for i in ids:
            call("smartcanvas.edit", {"file_id": FILE_ID, "action": "DELETE", "id": i}, "del")
            time.sleep(0.05)
        rounds += 1
        if rounds > 400:
            log("safety stop after 400 rounds")
            break
    log("reset done (%d rounds)" % rounds)
    return True


def repush():
    for idx, content in enumerate(CHUNKS):
        args = {"file_id": FILE_ID, "action": "INSERT_AFTER", "id": "", "content": content}
        res = call("smartcanvas.edit", args, "insert-%d" % idx)
        if res is None:
            log("FAILED to insert chunk %d" % idx)
            return False
        time.sleep(0.1)
    log("repushed all %d chunks" % len(CHUNKS))
    return True


def main():
    log("start; initial cooldown %ds" % INITIAL_COOLDOWN)
    time.sleep(INITIAL_COOLDOWN)
    log("cooldown done; reading doc")
    out = read_all()
    if out is None:
        log("could not read doc after cooldown")
        return
    mdx, pages = out
    log("read %d pages, mdx chars %d" % (pages, len(mdx)))
    ok, rep = verify(mdx)
    for r in rep:
        log("  " + r)
    if ok:
        log("VERIFY PASS: document is complete and clean.")
    else:
        log("VERIFY FAIL: document incomplete/tainted -> reset + repush")
        if reset_doc():
            repush()
            # re-verify
            out2 = read_all()
            if out2:
                ok2, rep2 = verify(out2[0])
                log("post-fix verify: %s" % ("PASS" if ok2 else "FAIL"))
                for r in rep2:
                    log("  " + r)
    log("DONE")


if __name__ == "__main__":
    main()

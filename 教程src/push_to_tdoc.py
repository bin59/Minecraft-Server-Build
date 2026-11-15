# -*- coding: utf-8 -*-
"""Push converted MDX chunks into the target Tencent smartcanvas doc.

Usage:
  python3 push_to_tdoc.py fresh    -> reset doc (delete all blocks) then insert all chunks
  python3 push_to_tdoc.py test     -> insert only chunk 0, then stop
  python3 push_to_tdoc.py all      -> insert all chunks (append; assumes empty/continuation)
  python3 push_to_tdoc.py range 3 9 -> insert chunks [3,9)
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
CHUNKS_PATH = os.path.join(HERE, "chunks.json")


def push_one(idx, content):
    args = {"file_id": FILE_ID, "action": "INSERT_AFTER", "id": "", "content": content}
    res, err = tencentdocs.call_tool("tencent-docs", "smartcanvas.edit", args)
    if err:
        print("[chunk %d] ERROR: %s" % (idx, err))
        return False
    try:
        txt = json.dumps(res, ensure_ascii=False)
    except Exception:
        txt = str(res)
    print("[chunk %d] ok len=%d -> %s" % (idx, len(content), txt[:120]))
    return True


def get_mdx(res):
    """The read response text is double-encoded JSON: extract the actual MDX string."""
    try:
        text = res["result"]["content"][0]["text"]
        obj = json.loads(text)
        return obj.get("content", "")
    except Exception:
        return ""


def reset_doc():
    print("== resetting doc (deleting all TOP-LEVEL blocks) ==")
    rounds = 0
    while True:
        res, err = tencentdocs.call_tool("tencent-docs", "smartcanvas.read", {"file_id": FILE_ID})
        if err:
            print("read err:", err)
            break
        mdx = get_mdx(res)
        # only top-level blocks (line starts with '<', indent 0) carry cascading deletes
        ids = re.findall(r'^<(\w+)\b[^>]*\bid="([^"]+)"', mdx, re.MULTILINE)
        ids = [i for _, i in ids]
        if not ids:
            break
        print("round %d: deleting %d top-level blocks" % (rounds, len(ids)))
        for i in ids:
            tencentdocs.call_tool(
                "tencent-docs", "smartcanvas.edit", {"file_id": FILE_ID, "action": "DELETE", "id": i}
            )
            time.sleep(0.03)
        rounds += 1
        time.sleep(0.1)
        if rounds > 400:
            print("safety: too many rounds, stopping")
            break
    print("reset done after %d rounds" % rounds)


def main():
    with open(CHUNKS_PATH, encoding="utf-8") as f:
        chunks = json.load(f)
    print("total chunks:", len(chunks))

    mode = sys.argv[1] if len(sys.argv) > 1 else "test"
    if mode == "test":
        rng = range(0, 1)
    elif mode == "all":
        rng = range(0, len(chunks))
    elif mode == "fresh":
        reset_doc()
        time.sleep(0.5)
        rng = range(0, len(chunks))
    elif mode == "range":
        a = int(sys.argv[2]); b = int(sys.argv[3])
        rng = range(a, b)
    else:
        print("unknown mode")
        return

    for i in rng:
        ok = push_one(i, chunks[i])
        if not ok:
            print("STOPPING at chunk", i)
            break
        time.sleep(0.4)


if __name__ == "__main__":
    main()

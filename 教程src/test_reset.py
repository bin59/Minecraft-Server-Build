# -*- coding: utf-8 -*-
import sys, re, time
sys.path.insert(0, r"C:/Users/19594/.workbuddy/plugins/cache/workbuddy-builtin/tencent-docs-plugin/5.9.3-wb.42398134.gd4692421.h480e8d1fee0f/skills/tencent-docs")
import tencentdocs

FILE_ID = "BBWEOPlbmQNb"
res, err = tencentdocs.call_tool("tencent-docs", "smartcanvas.read", {"file_id": FILE_ID})
inner = res["result"]["content"][0]["text"]
print("inner len", len(inner))
# show a snippet around the first id
idx = inner.find("id=")
print("first id= at", idx)
print("snippet:", repr(inner[idx-5:idx+40]))
m = re.search(r'id="([^"]+)"', inner)
print("regex r'id=\"([^\"]+)\"' match:", m.group(1) if m else None)
ids = re.findall(r'id="([^"]+)"', inner)
print("ids found:", len(ids))

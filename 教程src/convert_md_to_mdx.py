# -*- coding: utf-8 -*-
"""Convert the integrated plugin-usage Markdown into Tencent smartcanvas MDX.

Rules applied:
  - Drop the duplicate document H1 (frontmatter already sets the title).
  - Markdown tables -> <Table>/<TableRow>/<TableCell>.
  - **bold** -> <Mark bold>; *italic* -> <Mark italic> (not on list lines).
  - [label](url) -> <Link href="url">label</Link>.
  - Escape &, <, > in raw text so placeholders like <玩家> don't break XML.
  - Everything else kept as clean Markdown (headings #, blockquote >,
    divider ---, bullets -, numbered 1., code fences ```) which the
    smartcanvas parser accepts ("MDX 向下兼容全部 Markdown").
"""
import re
import os
import sys

SRC = r"F:/game/pc/MC/开服/Minecraft-Server-Build/插件纯使用教程.md"
OUT_MDX = r"F:/game/pc/MC/开服/Minecraft-Server-Build/教程src/插件纯使用教程.mdx"
OUT_CHUNKS = r"F:/game/pc/MC/开服/Minecraft-Server-Build/教程src/chunks.json"

TDOC_PATH = r"C:/Users/19594/.workbuddy/plugins/cache/workbuddy-builtin/tencent-docs-plugin/5.9.3-wb.42398134.gd4692421.h480e8d1fee0f/skills/tencent-docs"

MAX_CHARS = 10000


def escape_xml(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def convert_styled(s, allow_italic=True):
    # links [label](http...) -> <Link>
    s = re.sub(r"\[([^\]]+)\]\((https?://[^)\s]+)\)",
               lambda m: '<Link href="%s">%s</Link>' % (m.group(2), m.group(1)), s)
    # anchor links [label](#x) -> plain label (intra-doc anchors unsupported in smartcanvas)
    s = re.sub(r"\[([^\]]+)\]\(\s*#[^)\s]*\s*\)", r"\1", s)
    # bold
    s = re.sub(r"\*\*(.+?)\*\*", r"<Mark bold>\1</Mark>", s)
    if allow_italic:
        s = re.sub(r"(?<!\*)\*(?!\s)([^*\n]+?)(?<!\s)\*(?!\*)", r"<Mark italic>\1</Mark>", s)
    return s


def convert_inline(text, allow_italic=True):
    text = escape_xml(text)
    parts = re.split(r"(`[^`]+`)", text)
    out = []
    for p in parts:
        if len(p) >= 2 and p.startswith("`") and p.endswith("`"):
            out.append(p)  # code span, already escaped
        else:
            out.append(convert_styled(p, allow_italic))
    return "".join(out)


def convert_line(line):
    # headings
    m = re.match(r"^(#{1,6})\s+(.*)$", line)
    if m:
        return "%s %s" % (m.group(1), convert_inline(m.group(2)))
    # blockquote
    if line.lstrip().startswith(">"):
        content = re.sub(r"^\s*>\s?", "", line)
        return "> " + convert_inline(content)
    # list items (protect * as bullet -> no italic conversion)
    m = re.match(r"^(\s*)([-*+]|\d+\.)\s+(.*)$", line)
    if m:
        indent, marker, rest = m.group(1), m.group(2), m.group(3)
        return "%s%s %s" % (indent, marker, convert_inline(rest, allow_italic=False))
    if line.strip() == "---":
        return "---"
    if line.strip() == "":
        return ""
    return convert_inline(line)


def split_row(row):
    s = row.strip()
    if s.startswith("|"):
        s = s[1:]
    if s.endswith("|"):
        s = s[:-1]
    parts = re.split(r"(?<!\\)\|", s)
    return [p.strip().replace("\\|", "|") for p in parts]


def is_table_row(line):
    s = line.strip()
    return s.startswith("|") and s.endswith("|") and len(s) > 1


def is_sep(line):
    s = line.strip()
    s2 = s.strip("|").strip()
    if not s2:
        return False
    return all(c in "-: |" for c in s2) and "-" in s2 and "|" in s2


def build_table(rows):
    header = rows[0]
    data = rows[2:]  # rows[1] is separator
    out = ["<Table>"]

    def emit(cells, is_header):
        out.append("    <TableRow>")
        for c in cells:
            out.append("        <TableCell>")
            txt = convert_inline(c).strip()
            if is_header:
                txt = "<Mark bold>%s</Mark>" % txt
            out.append("            " + txt)
            out.append("        </TableCell>")
        out.append("    </TableRow>")

    emit(split_row(header), True)
    for r in data:
        emit(split_row(r), False)
    out.append("</Table>")
    return "\n".join(out)


def is_table_start(line, lines, i):
    return is_table_row(line) and i + 1 < len(lines) and is_sep(lines[i + 1])


def convert(content):
    lines = content.split("\n")
    # drop duplicate document H1 title line (frontmatter already has title)
    if lines and re.match(r"^#\s", lines[0]):
        lines = lines[1:]
        if lines and lines[0].strip() == "":
            lines = lines[1:]
    out = []
    in_code = False
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        if line.strip().startswith("```"):
            in_code = not in_code
            out.append(line)
            i += 1
            continue
        if in_code:
            out.append(escape_xml(line))
            i += 1
            continue
        if is_table_start(line, lines, i):
            tbl = [line, lines[i + 1]]
            i += 2
            while i < n and is_table_row(lines[i]):
                tbl.append(lines[i])
                i += 1
            out.extend(build_table(tbl).split("\n"))
            continue
        out.append(convert_line(line))
        i += 1
    return out


def chunk_lines(out_lines, max_chars=MAX_CHARS):
    chunks = []
    cur = []
    cur_len = 0
    inside_table = False
    for ln in out_lines:
        if ln.strip().startswith("<Table>"):
            inside_table = True
        if ln.strip() == "</Table>":
            inside_table = False
        add_len = len(ln) + 1
        if cur and cur_len + add_len > max_chars and not inside_table:
            if ln.strip() == "" or re.match(r"^#{1,6}\s", ln) or ln.strip().startswith("---"):
                chunks.append("\n".join(cur))
                cur = []
                cur_len = 0
        cur.append(ln)
        cur_len += add_len
    if cur:
        chunks.append("\n".join(cur))
    return chunks


def main():
    with open(SRC, "r", encoding="utf-8") as f:
        content = f.read()
    out = convert(content)
    mdx = "\n".join(out)
    with open(OUT_MDX, "w", encoding="utf-8") as f:
        f.write(mdx)
    chunks = chunk_lines(out)
    with open(OUT_CHUNKS, "w", encoding="utf-8") as f:
        import json
        json.dump(chunks, f, ensure_ascii=False)
    print("MDX lines:", len(out))
    print("MDX chars:", len(mdx))
    print("Chunks:", len(chunks))
    for idx, c in enumerate(chunks):
        print("  chunk %d: %d chars" % (idx, len(c)))


if __name__ == "__main__":
    main()

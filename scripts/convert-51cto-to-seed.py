"""Convert scraped 51CTO exam JSONs into an OSE-shaped seed JSON.

Output: D:/dev/ose/data/51cto-seed.json

Mapping rules (see schema at src/prisma/schema.prisma):
- 选择题 exams -> Question.type=CHOICE, session=AM
- 案例分析 exams -> Question.type=CASE_ANALYSIS, session=PM
- questionNumber = month*100 + sourceIndex (avoids year-AM/PM collisions across
  May/Nov sessions of the same year, which the existing schema cannot distinguish)
- Case questions are grouped by material_text into CaseScenarios; each group
  becomes one Question (type=CASE_ANALYSIS) whose sub-questions are
  CaseSubQuestions.
- All questions get knowledgePointId="kp-uncategorized" as a placeholder.
- explanation is empty (the scrape API does not return analyses).
"""
import json
import re
import sys
import io
from pathlib import Path
from datetime import datetime, timezone

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

SRC = Path(r"D:/dev/ose/data/51cto-exams")
OUT = Path(r"D:/dev/ose/data/51cto-seed.json")

DROP_IDS = {"19205"}  # 2025下软设 — duplicates 19359 (2025年11月)


def parse_title(title: str):
    """Return (year:int, month:int, kind: 'CHOICE'|'CASE')."""
    m = re.search(r"(\d{4})\s*年\s*(\d{1,2})\s*月", title)
    if m:
        year = int(m.group(1))
        month = int(m.group(2))
    else:
        # Fallbacks like "2025下软设" / "2025上软设"
        m2 = re.search(r"(\d{4})\s*([上下])", title)
        if not m2:
            raise ValueError(f"cannot parse year/month from: {title}")
        year = int(m2.group(1))
        month = 11 if m2.group(2) == "下" else 5
    if "案例" in title:
        kind = "CASE"
    elif "选择" in title or "软设选择" in title:
        kind = "CHOICE"
    else:
        # default
        kind = "CHOICE"
    return year, month, kind


def map_answer_type(show_type_name: str) -> str:
    if show_type_name == "填空题":
        return "FILL_BLANK"
    if show_type_name in ("[材料型]问答题", "问答题", "编程题"):
        return "SHORT_ANSWER"
    return "SHORT_ANSWER"


_ENTITIES = {
    "&nbsp;": " ",
    "&amp;": "&",
    "&lt;": "<",
    "&gt;": ">",
    "&quot;": '"',
    "&#39;": "'",
    "&apos;": "'",
    "&ldquo;": "“",
    "&rdquo;": "”",
    "&lsquo;": "‘",
    "&rsquo;": "’",
    "&hellip;": "…",
    "&mdash;": "—",
    "&ndash;": "–",
}

_TAG_RE = re.compile(r"<[^>]+>")
_IMG_RE = re.compile(r'<img\s[^>]*?src\s*=\s*["\'](?P<url>[^"\']+)["\'][^>]*?>', re.IGNORECASE)
_BR_RE = re.compile(r"<br\s*/?>", re.IGNORECASE)
_BLOCK_RE = re.compile(r"</(?:p|div|h[1-6]|li|tr|table)>", re.IGNORECASE)
_LI_RE = re.compile(r"<li[^>]*>", re.IGNORECASE)

_CODE_HEADER_RE = re.compile(r"【\s*((?:Java|C\+\+|C#|C|Python|伪)?\s*代码)\s*】")
_NEXT_BRACKET_RE = re.compile(r"【[^】]+】")
_LANG_MAP = {"java": "java", "c": "c", "c++": "cpp", "c#": "csharp", "python": "python", "伪": "", "": ""}


def _wrap_code_blocks(text: str) -> str:
    """Wrap content under 【...代码】 markers in a markdown code fence.

    The 51CTO source emits each code line in its own <p>, which becomes a
    newline after stripping. We wrap from each 【代码】 header to the next
    【...】 heading (or end of text) in a fenced block so MarkdownRenderer
    renders it as code rather than prose.
    """
    if "代码】" not in text:
        return text
    parts: list[str] = []
    cursor = 0
    for m in _CODE_HEADER_RE.finditer(text):
        if m.start() < cursor:
            continue
        parts.append(text[cursor:m.start()])
        header = m.group(0)
        lang_key = (m.group(1) or "").strip().replace(" ", "").lower().replace("代码", "")
        lang = _LANG_MAP.get(lang_key, "")
        # Find end of code block: next 【...】 marker after the header.
        rest = text[m.end():]
        nxt = _NEXT_BRACKET_RE.search(rest)
        if nxt:
            code = rest[:nxt.start()]
            new_cursor = m.end() + nxt.start()
        else:
            code = rest
            new_cursor = len(text)
        code = code.strip("\n")
        parts.append(f"{header}\n\n```{lang}\n{code}\n```\n")
        cursor = new_cursor
    parts.append(text[cursor:])
    return "".join(parts)


def html_text(s: str | None) -> str:
    """Convert 51CTO rich-text HTML to plain text suitable for OSE rendering.

    - <p>/<div>/<h_>/<li>/<tr>/<table> closing tags become newlines
    - <br> becomes a newline
    - <img src="URL" .../> becomes "[图: URL]" inline marker
    - Other tags are stripped
    - HTML entities are decoded
    - Repeated whitespace and blank lines collapsed
    """
    if not s:
        return ""
    text = s
    text = _IMG_RE.sub(lambda m: f"![]({m.group('url')})", text)
    text = _BR_RE.sub("\n", text)
    text = _BLOCK_RE.sub("\n", text)
    text = _LI_RE.sub("\n• ", text)
    text = _TAG_RE.sub("", text)
    for entity, ch in _ENTITIES.items():
        text = text.replace(entity, ch)
    text = re.sub(r"&#(\d+);", lambda m: chr(int(m.group(1))), text)
    text = re.sub(r"&#x([0-9a-fA-F]+);", lambda m: chr(int(m.group(1), 16)), text)
    text = _wrap_code_blocks(text)
    # Collapse whitespace (but preserve indentation inside code fences)
    lines = text.split("\n")
    out: list[str] = []
    blank_run = 0
    in_fence = False
    for raw_line in lines:
        if raw_line.strip().startswith("```"):
            in_fence = not in_fence
            out.append(raw_line.rstrip())
            blank_run = 0
            continue
        if in_fence:
            out.append(raw_line)
            blank_run = 0
            continue
        line = raw_line.strip(" \t")
        if not line:
            blank_run += 1
            if blank_run <= 1:
                out.append("")
        else:
            blank_run = 0
            line = re.sub(r"[ \t]{2,}", " ", line)
            out.append(line)
    return "\n".join(out).strip()


_QUESTION_MARKER_RE = re.compile(r"【问题\s*([0-9一二三四五六七八九十]+)\s*】|\[问题\s*([0-9一二三四五六七八九十]+)\]|问题\s*([0-9]+)\s*[:：]")


def split_old_format_case(question_title_html: str, answer_blocks: list[str]) -> tuple[str, list[tuple[str, str]]]:
    """Split a self-contained 试题 (old-format case question) into material and sub-prompts.

    Returns (material_text, [(sub_prompt, sub_answer), ...]).
    Falls back to a single sub if no [问题N] markers are found.
    """
    flat = html_text(question_title_html)
    # Find all 【问题N】 markers
    markers: list[tuple[int, int, str]] = []
    for m in _QUESTION_MARKER_RE.finditer(flat):
        n = m.group(1) or m.group(2) or m.group(3)
        markers.append((m.start(), m.end(), str(n)))
    if not markers:
        joined_answer = "\n".join(html_text(a) for a in (answer_blocks or []))
        # Detect code-fill style: question mentions "应填入(n)处" / "应填入 (n) 处"
        if re.search(r"应填入\s*\(?\s*[nN]\s*\)?\s*处|填入\s*答题纸|代码】", flat):
            prompt = "在背景代码中(1)(2)(3)…等编号处填入合适的内容,逐项作答。"
        else:
            prompt = "请结合上方完整题干作答。"
        return flat, [(prompt, joined_answer)]

    # Material = text before first marker
    material = flat[: markers[0][0]].strip()
    # Sub prompts = text between marker[i].end and marker[i+1].start
    subs: list[tuple[str, str]] = []
    for i, (_, end, n) in enumerate(markers):
        nxt_start = markers[i + 1][0] if i + 1 < len(markers) else len(flat)
        prompt = flat[end:nxt_start].strip()
        prompt_full = f"问题{n}：{prompt}".rstrip("：:")
        subs.append((prompt_full, ""))

    # Answers — try parsing each answer block by 问题N: markers
    parsed_answers: dict[str, str] = {}
    answer_text = "\n".join(html_text(a) for a in (answer_blocks or []))
    answer_markers = list(_QUESTION_MARKER_RE.finditer(answer_text))
    if answer_markers:
        for i, m in enumerate(answer_markers):
            n = m.group(1) or m.group(2) or m.group(3)
            nxt = answer_markers[i + 1].start() if i + 1 < len(answer_markers) else len(answer_text)
            parsed_answers[str(n)] = answer_text[m.end():nxt].strip()
    elif answer_text.strip():
        parsed_answers["1"] = answer_text.strip()

    out_subs: list[tuple[str, str]] = []
    for i, (prompt, _) in enumerate(subs):
        n = markers[i][2]
        out_subs.append((prompt, parsed_answers.get(n, parsed_answers.get(str(i + 1), ""))))

    return material, out_subs


def build_choice_questions(exam_id: str, qs: list, month: int):
    """Return list of seed-shaped CHOICE questions, in source order."""
    out = []
    for q in qs:
        idx = int(q["index"])
        question_number = month * 100 + idx
        # Options: source has option:[html...] and answer:["B"]
        options = []
        opt_arr = q.get("option") or []
        for i, opt_html in enumerate(opt_arr):
            label = chr(ord("A") + i)
            options.append({
                "label": label,
                "content": html_text(opt_html),
                "isCorrect": label in (q.get("answer") or []),
            })
        # Material text appears on 完形类单选题 (cloze): include it before the prompt
        material = html_text(q.get("material_text"))
        prompt = html_text(q.get("question_title"))
        if material and material not in prompt:
            content = f"{material}\n\n{prompt}".strip()
        else:
            content = prompt
        out.append({
            "sourceQuestionId": q["question_id"],
            "questionNumber": question_number,
            "type": "CHOICE",
            "content": content,
            "difficulty": 3,
            "explanation": html_text(q.get("analyze")),
            "knowledgePointId": "kp-uncategorized",
            "showTypeName": q.get("show_type_name"),
            "score": q.get("score") or 1,
            "options": options,
            "rawAnswer": q.get("answer"),
        })
    return out


def extract_title_from_material(material_html: str, fallback_idx: int) -> str:
    """Pull a short '试题N' header from material (or question_title) HTML."""
    m = re.search(r"(试题[\s]*[一二三四五六七八九十0-9]+)", material_html or "")
    if m:
        return m.group(1).strip()
    return f"试题{fallback_idx}"


def build_case_questions(exam_id: str, qs: list, month: int):
    """Build CASE_ANALYSIS scenarios from a 案例分析 exam.

    Two source formats observed:
    1. Newer format (2020+): questions have shared `material_text` across
       sibling [问题N] sub-questions. Group by material into scenarios; each
       sibling becomes a CaseSubQuestion.
    2. Older format (≤2019): `material_text` is empty across the whole exam;
       each question_title is a self-contained 试题 (说明 + 问题1..N inline).
       Treat each question_title as one scenario's background with a single
       sub-question carrying the joined reference answer.
    """
    materials_present = any((q.get("material_text") or "").strip() for q in qs)
    scenarios = []  # list of {"material","items":[(sub_idx_in_source, q)]}
    if materials_present:
        last_material = None
        for q in qs:
            material = (q.get("material_text") or "").strip()
            if material:
                if material != last_material:
                    scenarios.append({"material": material, "items": []})
                    last_material = material
            else:
                if not scenarios:
                    # Should not happen if materials_present, but guard.
                    scenarios.append({"material": "", "items": []})
            scenarios[-1]["items"].append(q)
    else:
        # Old format: 1 scenario per question_title.
        for q in qs:
            scenarios.append({"material": "", "items": [q]})

    out = []
    for scenario_idx, sc in enumerate(scenarios, start=1):
        question_number = month * 100 + scenario_idx
        if sc["material"]:
            # Newer format: shared material across multiple sibling sub-questions.
            background = html_text(sc["material"])
            title = extract_title_from_material(background, scenario_idx)
            sub_qs = []
            total_score = 0
            for sub_idx, q in enumerate(sc["items"], start=1):
                sub_score = int(q.get("score") or 0)
                total_score += sub_score
                sub_qs.append({
                    "sourceQuestionId": q["question_id"],
                    "subNumber": sub_idx,
                    "content": html_text(q.get("question_title")),
                    "answerType": map_answer_type(q.get("show_type_name") or ""),
                    "referenceAnswer": "\n".join(html_text(a) for a in (q.get("answer") or [])),
                    "score": sub_score,
                    "explanation": html_text(q.get("analyze")),
                    "showTypeName": q.get("show_type_name"),
                })
        else:
            # Old format: question_title is the entire 试题 (说明 + 问题1..N).
            # There is exactly one item per scenario in this branch.
            q = sc["items"][0]
            material, sub_pairs = split_old_format_case(
                q.get("question_title") or "",
                q.get("answer") or [],
            )
            background = material
            title = extract_title_from_material(material, scenario_idx) or extract_title_from_material(html_text(q.get("question_title") or ""), scenario_idx)
            scenario_score = int(q.get("score") or 0)
            # Distribute score evenly if more than one sub
            n_subs = max(1, len(sub_pairs))
            sub_score_each = scenario_score // n_subs if scenario_score else 0
            sub_qs = []
            total_score = 0
            for sub_idx, (prompt, ans) in enumerate(sub_pairs, start=1):
                sub_qs.append({
                    "sourceQuestionId": q["question_id"],
                    "subNumber": sub_idx,
                    "content": prompt or f"问题{sub_idx}",
                    "answerType": map_answer_type(q.get("show_type_name") or ""),
                    "referenceAnswer": ans,
                    "score": sub_score_each,
                    "explanation": "",
                    "showTypeName": q.get("show_type_name"),
                })
                total_score += sub_score_each

        out.append({
            "questionNumber": question_number,
            "type": "CASE_ANALYSIS",
            "content": title,
            "difficulty": 3,
            "explanation": "",
            "knowledgePointId": "kp-uncategorized",
            "score": total_score,
            "caseScenario": {
                "background": background,
                "figures": None,
                "subQuestions": sub_qs,
            },
        })
    return out


def main():
    files = sorted(SRC.glob("*.json"), key=lambda p: int(p.stem))
    exams = []
    skipped = []
    seen_year_session_qn = {}  # (year, session, qn) -> exam title
    duplicate_warnings = []

    for p in files:
        if p.stem in DROP_IDS:
            skipped.append((p.stem, "manually dropped (duplicate)"))
            continue
        d = json.loads(p.read_text(encoding="utf-8"))["data"]
        inner = d["data"]
        ex = inner["examine"]
        qs = inner["question"]
        title = ex["title"]
        year, month, kind = parse_title(title)
        session = "AM" if kind == "CHOICE" else "PM"

        if kind == "CHOICE":
            seed_questions = build_choice_questions(p.stem, qs, month)
        else:
            seed_questions = build_case_questions(p.stem, qs, month)

        # Detect collisions on (year, session, questionNumber)
        for sq in seed_questions:
            key = (year, session, sq["questionNumber"])
            if key in seen_year_session_qn:
                duplicate_warnings.append({
                    "key": list(key),
                    "first_exam": seen_year_session_qn[key],
                    "this_exam": title,
                    "this_source_id": p.stem,
                })
            else:
                seen_year_session_qn[key] = title

        exams.append({
            "sourceId": int(p.stem),
            "title": title,
            "type": "REAL",
            "session": session,
            "year": year,
            "month": month,
            "kind": kind,
            "timeLimit": int(ex.get("len_time", {}).get("time") or 0) or (150 if kind == "CHOICE" else 150),
            "totalScore": int(ex.get("total_score") or 0),
            "totalQuestions": int(ex.get("total_question") or len(qs)),
            "questions": seed_questions,
        })

    payload = {
        "metadata": {
            "source": "51cto.com",
            "sourceUrlPattern": "https://t.51cto.com/napi/list/detail?id={examId}&need_answer=1",
            "convertedAt": datetime.now(timezone.utc).isoformat(),
            "examCount": len(exams),
            "questionCount": sum(
                len(e["questions"]) if e["session"] == "AM"
                else sum(len(q["caseScenario"]["subQuestions"]) for q in e["questions"])
                for e in exams
            ),
            "scenarioCount": sum(
                len(e["questions"]) for e in exams if e["session"] == "PM"
            ),
            "scopedQuestionRecordCount": sum(len(e["questions"]) for e in exams),
            "skipped": skipped,
            "duplicateWarnings": duplicate_warnings,
            "questionNumberStrategy": "month*100 + sourceIndex (or scenarioIndex for case)",
            "knowledgePointStrategy": "all questions assigned to placeholder kp-uncategorized; classify later via AI feature",
            "explanationStrategy": "empty (source API does not return analyses)",
        },
        "knowledgePoints": [
            {"id": "kp-uncategorized", "name": "未分类", "sortOrder": 999, "description": "尚未分类的真题(批量导入占位)"}
        ],
        "exams": exams,
    }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    md = payload["metadata"]
    print(f"Wrote {OUT} ({OUT.stat().st_size/1024:.1f} KB)")
    print(f"  exams: {md['examCount']}")
    print(f"  question records (Prisma rows): {md['scopedQuestionRecordCount']}")
    print(f"  total atomic questions (incl. case sub): {md['questionCount']}")
    print(f"  case scenarios: {md['scenarioCount']}")
    print(f"  skipped: {md['skipped']}")
    if md["duplicateWarnings"]:
        print(f"  ⚠ {len(md['duplicateWarnings'])} (year,session,qn) collisions:")
        for w in md["duplicateWarnings"][:10]:
            print(f"     {w['key']} between '{w['first_exam']}' and '{w['this_exam']}'")
    else:
        print("  no (year,session,qn) collisions")


if __name__ == "__main__":
    main()

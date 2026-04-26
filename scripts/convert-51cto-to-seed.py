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


def html_text(s: str | None) -> str:
    return (s or "").strip()


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
        out.append({
            "sourceQuestionId": q["question_id"],
            "questionNumber": question_number,
            "type": "CHOICE",
            "content": html_text(q.get("question_title")),
            "difficulty": 3,
            "explanation": html_text(q.get("analyze")),
            "knowledgePointId": "kp-uncategorized",
            "showTypeName": q.get("show_type_name"),
            "score": q.get("score") or 1,
            "options": options,
            # Keep raw answer for reference (e.g., when no option matches)
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
            background = sc["material"]
            title = extract_title_from_material(background, scenario_idx)
        else:
            # Use the first item's question_title as the case background; this
            # is the 说明+问题 inline text for the older format.
            background = html_text(sc["items"][0].get("question_title"))
            title = extract_title_from_material(background, scenario_idx)

        sub_qs = []
        total_score = 0
        for sub_idx, q in enumerate(sc["items"], start=1):
            sub_score = int(q.get("score") or 0)
            total_score += sub_score
            if sc["material"]:
                content = html_text(q.get("question_title"))
            else:
                # Background already contains the prompts; use a short marker.
                content = "（参考答案见下,完整题干见上方背景）"
            sub_qs.append({
                "sourceQuestionId": q["question_id"],
                "subNumber": sub_idx,
                "content": content,
                "answerType": map_answer_type(q.get("show_type_name") or ""),
                "referenceAnswer": "\n".join(q.get("answer") or []),
                "score": sub_score,
                "explanation": html_text(q.get("analyze")),
                "showTypeName": q.get("show_type_name"),
            })
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

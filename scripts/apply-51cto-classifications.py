"""Merge AI classifications into the OSE seed JSON.

Reads:
  data/51cto-seed.json
  data/51cto-classifications.json

Writes (in place):
  data/51cto-seed.json       # questions get the assigned knowledgePointId
  data/51cto-seed.json#metadata.classification stats appended
"""
import json
import sys
import io
from pathlib import Path
from datetime import datetime, timezone
from collections import Counter

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

SEED_PATH = Path(r"D:/dev/ose/data/51cto-seed.json")
CLASS_PATH = Path(r"D:/dev/ose/data/51cto-classifications.json")

seed = json.loads(SEED_PATH.read_text(encoding="utf-8"))
classes = json.loads(CLASS_PATH.read_text(encoding="utf-8"))["classifications"]

unassigned = []
applied = Counter()
confidence_hist = Counter()
classified_question_count = 0
total_question_records = 0

for exam in seed["exams"]:
    for q in exam["questions"]:
        key = f'{exam["sourceId"]}:{q["questionNumber"]}'
        rec = classes.get(key)
        total_question_records += 1
        if rec:
            q["knowledgePointId"] = rec["knowledgePointId"]
            applied[rec["knowledgePointId"]] += 1
            confidence_hist[rec.get("confidence", "?")] += 1
            classified_question_count += 1
        else:
            unassigned.append(key)

# Annotate seed metadata
seed["metadata"]["classification"] = {
    "appliedAt": datetime.now(timezone.utc).isoformat(),
    "classifiedQuestions": classified_question_count,
    "totalQuestionRecords": total_question_records,
    "unassignedCount": len(unassigned),
    "confidenceHistogram": dict(confidence_hist),
    "topKnowledgePoints": [
        {"id": kp, "count": n} for kp, n in applied.most_common(20)
    ],
}

# Knowledge point list for the seeder: replace the lone placeholder with a marker
# that the loader should upsert the FULL tree (loader reads knowledge-tree.ts).
# Keep the placeholder so unclassified questions still have a valid FK.
if not any(kp.get("id") == "kp-uncategorized" for kp in seed["knowledgePoints"]):
    seed["knowledgePoints"].append({
        "id": "kp-uncategorized",
        "name": "未分类",
        "sortOrder": 999,
        "description": "尚未分类的题目",
    })

SEED_PATH.write_text(json.dumps(seed, ensure_ascii=False, indent=2), encoding="utf-8")

print(f"Applied {classified_question_count}/{total_question_records} classifications")
print(f"Unassigned: {len(unassigned)}")
if unassigned[:10]:
    print(f"  e.g. {unassigned[:10]}")
print(f"Confidence: {dict(confidence_hist)}")
print(f"Top knowledge points:")
for kp, n in applied.most_common(15):
    print(f"  {kp}: {n}")

"""Split 51CTO exam scrape dump into per-exam JSON files."""
import json
import sys
from pathlib import Path

SRC = Path(r"C:/Users/Administrator/.claude/projects/D--dev-ose/0672b60b-340f-42f9-8fea-6f47bbaf0815/tool-results/mcp-chrome-devtools-evaluate_script-1777188488225.txt")
DST = Path(r"D:/dev/ose/data/51cto-exams")
DST.mkdir(parents=True, exist_ok=True)

raw = SRC.read_text(encoding="utf-8")
# MCP wraps result in some envelope; locate the JSON payload.
# Try direct parse first, then fall back to slicing from first '{'.
try:
    payload = json.loads(raw)
except json.JSONDecodeError:
    start = raw.find("{")
    end = raw.rfind("}")
    payload = json.loads(raw[start:end + 1])

# evaluate_script return is wrapped in MCP CallToolResult format.
# Drill down: payload may be {result: {...}} or the dict directly.
if "_raw" not in payload:
    for key in ("result", "data", "value"):
        if key in payload and isinstance(payload[key], dict) and "_raw" in payload[key]:
            payload = payload[key]
            break

raw_map = payload["_raw"]
stats = payload.get("stats", [])

print(f"Found {len(raw_map)} exams")
for stat in stats:
    print(f"  {stat.get('id')}: {stat.get('title')} — {stat.get('questions_returned')}/{stat.get('total')} questions")

for exam_id, val in raw_map.items():
    if not val.get("ok") or val.get("json", {}).get("status") != "0":
        print(f"SKIP {exam_id}: {val}")
        continue
    out = DST / f"{exam_id}.json"
    out.write_text(json.dumps(val["json"], ensure_ascii=False, indent=2), encoding="utf-8")
    size_kb = out.stat().st_size / 1024
    print(f"WROTE {out.name} ({size_kb:.1f} KB)")

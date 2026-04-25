# Question Format

This document describes how to contribute question data.

## Multiple-choice JSON Format

```json
{
  "code": "SD-2024-AM-001",
  "type": "SINGLE_CHOICE",
  "topic": "Software Engineering",
  "stem": "Which model emphasizes iterative risk analysis?",
  "options": [
    { "label": "A", "content": "Waterfall" },
    { "label": "B", "content": "Spiral" },
    { "label": "C", "content": "V-Model" },
    { "label": "D", "content": "Prototype" }
  ],
  "answer": "B",
  "explanation": "The spiral model combines iteration with explicit risk analysis.",
  "difficulty": "MEDIUM",
  "source": "Original contribution"
}
```

Required fields:

- `code`: stable unique identifier.
- `type`: currently `SINGLE_CHOICE`.
- `topic`: knowledge topic title or stable topic key.
- `stem`: question text.
- `options`: answer options.
- `answer`: correct option label.
- `explanation`: reasoning and exam point.
- `difficulty`: `EASY`, `MEDIUM`, or `HARD`.
- `source`: source or contributor note.

## Case Analysis JSON Format

```json
{
  "code": "SD-2024-PM-CASE-001",
  "type": "CASE_ANALYSIS",
  "topic": "Database Design",
  "background": "A company is designing an inventory system...",
  "subQuestions": [
    {
      "prompt": "Identify two entities and their relationship.",
      "maxScore": 6,
      "referenceAnswer": "Product and Warehouse have an inventory relationship.",
      "scoringPoints": [
        { "point": "Product entity", "score": 2 },
        { "point": "Warehouse entity", "score": 2 },
        { "point": "Inventory relationship", "score": 2 }
      ]
    }
  ],
  "difficulty": "MEDIUM",
  "source": "Original contribution"
}
```

## Adding Questions Through Seed Files

Seed logic lives in `src/prisma/seed.ts`. To add data:

1. Add or update topic definitions.
2. Add questions with stable codes.
3. Add options, answers, explanations, and case scoring points.
4. Run `npx prisma db seed`.
5. Verify in the UI.

## Batch Import

For large batches, prepare JSON files with the formats above and write a small importer that:

- Validates required fields.
- Checks duplicate `code` values.
- Maps `topic` to existing knowledge points.
- Creates questions in a transaction.
- Prints a summary of inserted, skipped, and failed records.

## Quality Standards

- Content must be original or legally redistributable.
- Every question must include an explanation.
- Avoid ambiguous wording and multiple plausible answers.
- Keep terminology aligned with the official exam syllabus.
- Prefer small, focused questions over overly broad ones.
- Case questions must include scoring rubrics.
- Use consistent Markdown formatting for code blocks, tables, and diagrams.

# FAQ

## Is OSE free?

Yes. OSE is open-source under AGPL-3.0. You can run it locally or self-host it.

## Do I need an AI API key?

No. Core practice, exams, wrong notes, and analytics work without AI. AI keys unlock explanations, grading, question generation, diagnosis, and study plans.

## Which AI provider should I use?

Claude is strong for case analysis and long explanations. OpenAI is a good general default. Gemini is cost-effective. Custom endpoints are useful for local models and regional providers.

## Can I use PostgreSQL?

Yes. Set `DATABASE_URL` to a PostgreSQL connection string and run `npx prisma migrate deploy`.

## Can I deploy to Vercel?

Yes, but use PostgreSQL or another network database. SQLite is not suitable for serverless production.

## Why AGPL-3.0?

OSE is a SaaS-like education platform. AGPL ensures that hosted modified versions also share source code with users, protecting community improvements.

## How do I contribute questions?

Read [question-format.md](question-format.md), prepare original or legally redistributable content, and submit a PR or issue using the question contribution template.

## Does the desktop app work offline?

The desktop app runs the web application locally and stores data in a local SQLite database. AI features require network access to the configured provider unless you use a local compatible endpoint.

## Where is user data stored?

In the configured database. The desktop app uses the OS app data directory. Docker SQLite deployments use the configured volume.

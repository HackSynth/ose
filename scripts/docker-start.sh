#!/bin/sh
set -eu

DB_PATH=/data/ose.db
SEED_DB_PATH=/app/seed/ose.db
SCHEMA_PATH=src/prisma/schema.prisma

copy_seed_db() {
  echo "Initializing SQLite database from the pre-seeded image..."
  cp "$SEED_DB_PATH" "$DB_PATH"
  touch /data/.seeded
}

if [ ! -s "$DB_PATH" ]; then
  copy_seed_db
fi

./node_modules/.bin/prisma migrate deploy --schema "$SCHEMA_PATH"

if [ ! -f /data/.seeded ]; then
  db_state="$(
    node <<'NODE'
const { PrismaClient } = require('@prisma/client');

const prisma = new PrismaClient();

(async () => {
  const [users, exams, questions, ctoExams] = await Promise.all([
    prisma.user.count(),
    prisma.exam.count(),
    prisma.question.count(),
    prisma.exam.count({ where: { id: { startsWith: 'exam-51cto-' } } }),
  ]);

  if (users === 0 && exams === 0 && questions === 0 && ctoExams === 0) {
    console.log('empty-unseeded');
  } else {
    console.log('keep-existing');
  }
})()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
NODE
  )"

  if [ "$db_state" = "empty-unseeded" ]; then
    copy_seed_db
    ./node_modules/.bin/prisma migrate deploy --schema "$SCHEMA_PATH"
  else
    echo "Existing database detected; leaving it unchanged."
  fi
fi

node server.js

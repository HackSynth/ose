import { Suspense } from "react";
import { PracticeSession } from "@/components/practice-session";

export default function PracticeSessionPage() {
  return (
    <Suspense>
      <PracticeSession />
    </Suspense>
  );
}

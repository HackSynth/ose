"use client";

import { useState } from "react";

export function PlanDayToggle({ planId, dayNumber, initialCompleted }: { planId: string; dayNumber: number; initialCompleted: boolean }) {
  const [completed, setCompleted] = useState(initialCompleted);
  const [pending, setPending] = useState(false);

  async function toggle() {
    if (pending) return;
    const previous = completed;
    const next = !previous;
    setCompleted(next);
    setPending(true);
    try {
      const response = await fetch(`/api/plan/${planId}/day/${dayNumber}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ completed: next }),
      });
      if (!response.ok) setCompleted(previous);
    } catch {
      setCompleted(previous);
    } finally {
      setPending(false);
    }
  }

  return <input aria-label={`标记第${dayNumber}天完成`} type="checkbox" checked={completed} onChange={toggle} disabled={pending} className="h-5 w-5 accent-primary" />;
}

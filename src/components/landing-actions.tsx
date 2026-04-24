"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { isTauri } from "@/lib/tauri";
import { Button } from "@/components/ui/button";

export function LandingActions() {
  const [tauri, setTauri] = useState(false);

  useEffect(() => {
    setTauri(isTauri());
  }, []);

  return (
    <div className="mt-8 flex justify-center gap-4">
      <Button asChild size="lg">
        <Link href="/register">开始备考</Link>
      </Button>
      {!tauri ? (
        <Button asChild variant="secondary" size="lg">
          <a href="#features">了解更多</a>
        </Button>
      ) : null}
    </div>
  );
}

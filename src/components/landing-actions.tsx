'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { ArrowRight, BookOpen } from 'lucide-react';

import { isTauri } from '@/lib/tauri';
import { Button } from '@/components/ui/button';

export function LandingActions() {
  const [tauri, setTauri] = useState(false);

  useEffect(() => {
    setTauri(isTauri());
  }, []);

  return (
    <div className="flex flex-col items-center justify-center gap-3 sm:flex-row">
      <Button asChild size="lg">
        <Link href="/register">
          开始备考
          <ArrowRight className="h-4 w-4" aria-hidden="true" />
        </Link>
      </Button>
      {!tauri ? (
        <Button asChild variant="secondary" size="lg">
          <a href="#features">
            <BookOpen className="h-4 w-4" aria-hidden="true" />
            查看功能
          </a>
        </Button>
      ) : null}
    </div>
  );
}

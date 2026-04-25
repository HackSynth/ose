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
    <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
      <Button asChild size="lg">
        <Link href="/register">
          Start studying
          <ArrowRight className="h-4 w-4" aria-hidden="true" />
        </Link>
      </Button>
      {!tauri ? (
        <Button asChild variant="secondary" size="lg">
          <a href="#features">
            <BookOpen className="h-4 w-4" aria-hidden="true" />
            Explore features
          </a>
        </Button>
      ) : null}
    </div>
  );
}

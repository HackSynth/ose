'use client';

import { useState } from 'react';
import { Shuffle, ListOrdered } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { StartPracticeButton } from '@/components/start-practice-button';

type Filter = 'all' | 'unanswered' | 'wrong-only';

const FILTER_OPTIONS: { value: Filter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'unanswered', label: '没做过' },
  { value: 'wrong-only', label: '做错过' },
];

function FilterToggle({ value, onChange }: { value: Filter; onChange: (v: Filter) => void }) {
  return (
    <div className="mt-4 flex gap-2">
      {FILTER_OPTIONS.map((opt) => (
        <button
          key={opt.value}
          type="button"
          onClick={() => onChange(opt.value)}
          className={`rounded-full px-3 py-1 text-xs font-black transition ${
            value === opt.value
              ? 'bg-primary text-white'
              : 'bg-white/60 text-navy hover:bg-white'
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

export function PracticeFilterCards() {
  const [randomFilter, setRandomFilter] = useState<Filter>('all');
  const [seqFilter, setSeqFilter] = useState<Filter>('all');

  return (
    <>
      <Card className="bg-softBlue p-6 sm:p-7">
        <Shuffle className="h-10 w-10 text-primary" />
        <h2 className="mt-5 text-2xl font-black text-navy">随机练习</h2>
        <p className="mt-2 font-semibold text-muted">从题库随机抽取 20 题，适合快速热身。</p>
        <FilterToggle value={randomFilter} onChange={setRandomFilter} />
        <div className="mt-4">
          <StartPracticeButton payload={{ mode: 'random', limit: 20, filter: randomFilter }}>
            开始随机练习
          </StartPracticeButton>
        </div>
      </Card>
      <Card className="bg-softGreen p-6 sm:p-7">
        <ListOrdered className="h-10 w-10 text-primary" />
        <h2 className="mt-5 text-2xl font-black text-navy">顺序练习</h2>
        <p className="mt-2 font-semibold text-muted">按年份和题号顺序推进，适合系统训练。</p>
        <FilterToggle value={seqFilter} onChange={setSeqFilter} />
        <div className="mt-4">
          <StartPracticeButton payload={{ mode: 'sequential', limit: 20, filter: seqFilter }} variant="secondary">
            开始顺序练习
          </StartPracticeButton>
        </div>
      </Card>
    </>
  );
}

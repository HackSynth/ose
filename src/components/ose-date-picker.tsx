'use client';

import { useMemo, useState } from 'react';
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

const weekDays = ['一', '二', '三', '四', '五', '六', '日'];

function dateKey(date: Date) {
  return date.toISOString().slice(0, 10);
}

function fromDateKey(value: string) {
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(Date.UTC(year, month - 1, day));
}

function formatMonth(date: Date) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  }).format(date);
}

function getMonthDays(monthDate: Date) {
  const year = monthDate.getUTCFullYear();
  const month = monthDate.getUTCMonth();
  const first = new Date(Date.UTC(year, month, 1));
  const firstWeekday = (first.getUTCDay() + 6) % 7;
  const daysInMonth = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  return [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from(
      { length: daysInMonth },
      (_, index) => new Date(Date.UTC(year, month, index + 1))
    ),
  ];
}

export function OSEDatePicker({
  id,
  name,
  value,
  defaultValue,
  required,
  onChange,
}: {
  id?: string;
  name?: string;
  value?: string;
  defaultValue?: string;
  required?: boolean;
  onChange?: (value: string) => void;
}) {
  const controlled = value !== undefined;
  const [internalValue, setInternalValue] = useState(defaultValue ?? '');
  const selectedValue = controlled ? value : internalValue;
  const selectedDate = selectedValue ? fromDateKey(selectedValue) : null;
  const [open, setOpen] = useState(false);
  const [monthDate, setMonthDate] = useState(() => selectedDate ?? new Date());
  const days = useMemo(() => getMonthDays(monthDate), [monthDate]);
  const todayKey = dateKey(new Date());

  function setSelected(nextValue: string) {
    if (!controlled) setInternalValue(nextValue);
    onChange?.(nextValue);
    setOpen(false);
  }

  function moveMonth(offset: number) {
    setMonthDate(
      (current) => new Date(Date.UTC(current.getUTCFullYear(), current.getUTCMonth() + offset, 1))
    );
  }

  return (
    <>
      {name ? <input type="hidden" name={name} value={selectedValue} required={required} /> : null}
      <DropdownMenu open={open} onOpenChange={setOpen}>
        <DropdownMenuTrigger asChild>
          <button id={id} type="button" className="ose-date-field w-full text-left">
            <CalendarDays className="h-5 w-5 shrink-0 text-primary" />
            <span
              className={cn(
                'min-w-0 flex-1 text-sm font-extrabold',
                !selectedValue && 'text-muted'
              )}
            >
              {selectedValue || '选择日期'}
            </span>
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="w-80 p-4">
          <div className="flex items-center justify-between">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => moveMonth(-1)}
              aria-label="上个月"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <p className="font-black text-navy">{formatMonth(monthDate)}</p>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => moveMonth(1)}
              aria-label="下个月"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
          <div className="mt-3 grid grid-cols-7 gap-1 text-center text-xs font-black text-muted">
            {weekDays.map((day) => (
              <span key={day}>{day}</span>
            ))}
          </div>
          <div className="mt-2 grid grid-cols-7 gap-1">
            {days.map((day, index) =>
              day ? (
                <button
                  key={dateKey(day)}
                  type="button"
                  onClick={() => setSelected(dateKey(day))}
                  className={cn(
                    'flex h-9 items-center justify-center rounded-xl text-sm font-black transition hover:bg-primary-soft',
                    dateKey(day) === selectedValue && 'bg-primary text-white hover:bg-primary',
                    dateKey(day) === todayKey && dateKey(day) !== selectedValue && 'text-primary'
                  )}
                >
                  {day.getUTCDate()}
                </button>
              ) : (
                <span key={`empty-${index}`} />
              )
            )}
          </div>
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
}

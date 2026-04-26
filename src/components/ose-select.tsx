'use client';

import { Check, ChevronDown } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

export type OSESelectOption = { value: string; label: string };
export type OSESelectGroup = { label: string; options: OSESelectOption[] };

export function OSESelect({
  value,
  options,
  groups,
  placeholder = '请选择',
  disabled,
  className,
  triggerClassName,
  onChange,
}: {
  value: string;
  options?: OSESelectOption[];
  groups?: OSESelectGroup[];
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  triggerClassName?: string;
  onChange: (value: string) => void;
}) {
  const flatOptions = [...(options ?? []), ...(groups ?? []).flatMap((group) => group.options)];
  const selected = flatOptions.find((option) => option.value === value);

  function renderOption(option: OSESelectOption) {
    const active = option.value === value;
    return (
      <DropdownMenuItem
        key={option.value}
        onSelect={() => onChange(option.value)}
        className="justify-between"
      >
        <span className="truncate">{option.label}</span>
        {active ? <Check className="h-4 w-4 shrink-0 text-primary" /> : null}
      </DropdownMenuItem>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild disabled={disabled}>
        <button
          type="button"
          disabled={disabled}
          className={cn(
            'ose-input flex h-12 w-full items-center justify-between gap-3 text-left text-sm font-extrabold disabled:pointer-events-none disabled:opacity-50',
            triggerClassName
          )}
        >
          <span className={cn('min-w-0 flex-1 truncate', !selected && 'text-muted')}>
            {selected?.label ?? placeholder}
          </span>
          <ChevronDown className="h-4 w-4 shrink-0 text-primary" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="start"
        className={cn(
          'max-h-80 w-[var(--radix-dropdown-menu-trigger-width)] overflow-y-auto',
          className
        )}
      >
        {options?.map(renderOption)}
        {groups?.map((group, index) => (
          <div key={group.label}>
            {index > 0 || options?.length ? <DropdownMenuSeparator /> : null}
            <DropdownMenuLabel>{group.label}</DropdownMenuLabel>
            {group.options.map(renderOption)}
          </div>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

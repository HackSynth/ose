import { describe, expect, it } from 'vitest';
import { normalizeIntervals } from '@/utils';

describe('normalizeIntervals', () => {
  it('filters invalid numbers', () => {
    expect(normalizeIntervals('1, 3, x, 7, -1')).toEqual([1, 3, 7]);
  });
});

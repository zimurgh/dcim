import { describe, expect, it } from 'vitest';
import * as fc from 'fast-check';

describe('fast-check smoke', () => {
  it('history ids stay positive', () => {
    fc.assert(
      fc.property(fc.integer({ min: 1, max: 10_000 }), (historyId) => {
        expect(historyId).toBeGreaterThan(0);
      }),
    );
  });
});

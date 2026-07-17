import { describe, expect, it } from "vitest";
import {
  DEDUCTION_TYPES,
  PAYMENT_RECORD_TYPES,
  VARIATION_TYPES,
  currentDay,
  currentMonth,
  deductionTypeText,
  labelOf,
  paymentRecordTypeText
} from "./dictionaries";

describe("dictionaries", () => {
  it("resolves known dictionary values to their labels", () => {
    expect(deductionTypeText("social_security")).toBe("社保扣款");
    expect(paymentRecordTypeText("received")).toBe("收款");
  });

  it("falls back to the raw value for unknown entries", () => {
    expect(deductionTypeText("not-a-type")).toBe("not-a-type");
    expect(paymentRecordTypeText("custom")).toBe("custom");
  });

  it("returns '-' for empty/null values", () => {
    expect(deductionTypeText(undefined)).toBe("-");
    expect(deductionTypeText(null)).toBe("-");
    expect(labelOf(DEDUCTION_TYPES, "")).toBe("-");
  });

  it("exposes stable option lists", () => {
    expect(DEDUCTION_TYPES).toHaveLength(5);
    expect(PAYMENT_RECORD_TYPES.length).toBeGreaterThan(0);
    expect(VARIATION_TYPES).toContain("设计变更");
  });

  it("formats current month and day deterministically", () => {
    const fixed = new Date("2026-07-17T12:00:00.000Z");
    expect(currentMonth(fixed)).toBe("2026-07");
    expect(currentDay(fixed)).toBe("2026-07-17");
  });
});

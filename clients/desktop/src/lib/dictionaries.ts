/**
 * Shared label dictionaries for status/type enumerations.
 *
 * These were previously declared ad-hoc inside individual page files. Keep them
 * in one place so the web client speaks the same language everywhere and the
 * list of options stays in sync with the backend state machine.
 */

export type DictionaryEntry = { value: string; label: string };

export const DEDUCTION_TYPES: DictionaryEntry[] = [
  { value: "social_security", label: "社保扣款" },
  { value: "housing_fund", label: "公积金扣款" },
  { value: "individual_tax", label: "个税扣款" },
  { value: "advance", label: "借支扣款" },
  { value: "other", label: "其他扣款" }
];

export const PAYMENT_RECORD_TYPES: DictionaryEntry[] = [
  { value: "received", label: "收款" },
  { value: "paid", label: "付款" },
  { value: "invoice", label: "开票" },
  { value: "retention", label: "质保金" }
];

export const VARIATION_TYPES: string[] = [
  "设计变更", "工程量增加", "障碍物处理", "管线迁改配合", "返工",
  "停工窝工", "夜间施工", "临时排水", "二次倒运", "零星用工", "其他"
];

export function labelOf(dictionary: DictionaryEntry[], value?: string | null): string {
  if (!value) return "-";
  return dictionary.find((entry) => entry.value === value)?.label ?? value;
}

export function deductionTypeText(type?: string | null): string {
  return labelOf(DEDUCTION_TYPES, type);
}

export function paymentRecordTypeText(type?: string | null): string {
  return labelOf(PAYMENT_RECORD_TYPES, type);
}

/** Current month in `yyyy-MM` form, for month-picker defaults. */
export function currentMonth(date = new Date()): string {
  return date.toISOString().slice(0, 7);
}

/** Current day in `yyyy-MM-dd` form, for date-picker defaults. */
export function currentDay(date = new Date()): string {
  return date.toISOString().slice(0, 10);
}

/**
 * Shared status pill used across list/detail tables.
 *
 * Replaces the per-feature `function Badge({status})` copies in Project/Compensation
 * pages and the inline `badge-green/yellow/red` class maps. A status is treated as
 * "good" (green) / "bad" (red) / "pending" (yellow) based on the conventions already
 * baked into `statusText`. Callers that need a custom color can pass `tone` directly.
 */
import { statusText } from "../../lib/utils";

export type BadgeTone = "green" | "yellow" | "red" | "gray" | "blue";

const GOOD_STATUSES = new Set(["active", "confirmed", "approved", "paid", "released", "completed", "settled", "confirmed"]);
const BAD_STATUSES = new Set(["rejected", "cancelled", "archived", "left", "inactive"]);
const PENDING_STATUSES = new Set([
  "draft", "pending_approval", "pending", "submitted", "unpaid", "not_started",
  "in_progress", "paused", "pm_confirmed", "allocated"
]);

export function toneForStatus(status?: string): BadgeTone {
  if (!status) return "gray";
  if (GOOD_STATUSES.has(status)) return "green";
  if (BAD_STATUSES.has(status)) return "red";
  if (PENDING_STATUSES.has(status)) return "yellow";
  return "blue";
}

export function badgeClass(tone: BadgeTone): string {
  return `badge badge-${tone}`;
}

export function Badge({ status, tone, children }: { status?: string; tone?: BadgeTone; children?: React.ReactNode }) {
  const resolvedTone = tone ?? toneForStatus(status);
  const label = children ?? statusText(status);
  return <span className={badgeClass(resolvedTone)}>{label}</span>;
}

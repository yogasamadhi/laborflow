/**
 * Resolves a display name from a list of `{id, name}` rows by id.
 *
 * Previously copy-pasted verbatim in ProjectPages.tsx and CompensationPages.tsx.
 */
export type NamedRow = { id: string | number; name?: string };

export function nameOf<T extends NamedRow>(rows: T[] | undefined, id?: string | number | null): string {
  if (id == null || id === "") return "-";
  const match = rows?.find((row) => String(row.id) === String(id));
  return match?.name?.trim() || "-";
}

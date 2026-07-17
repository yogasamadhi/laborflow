/**
 * Unified lookup-data hook.
 *
 * Previously Project and Compensation pages each declared their own
 * `useLookups`/`useCompLookups` that fetched the same endpoints (employees,
 * projects, positions, teams, ...) with different query keys — so react-query
 * cached them separately and the same data was re-fetched per page switch.
 *
 * This single hook uses a stable query key per resource so every page that
 * requests the same resource shares one cache entry.
 */
import { useQuery } from "@tanstack/react-query";
import { getData, type PageResult } from "./axios";

export type LookupRow = { id: string; name?: string };

export type LookupResource =
  | "employees"
  | "projects"
  | "positions"
  | "departments"
  | "teams"
  | "work-processes"
  | "project-sections"
  | "machinery-equipments"
  | "compensation/bonus-pools";

const RESOURCE_PATH: Record<LookupResource, string> = {
  employees: "/employees",
  projects: "/projects",
  positions: "/positions",
  departments: "/departments",
  teams: "/teams",
  "work-processes": "/work-processes",
  "project-sections": "/project-sections",
  "machinery-equipments": "/machinery-equipments",
  "compensation/bonus-pools": "/compensation/bonus-pools"
};

const LOOKUP_PAGE_SIZE = 500;

/**
 * Fetch the requested lookup resources once (cached by react-query's default
 * staleTime configured in providers.tsx). Returns a map keyed by resource.
 */
export function useLookups<T extends LookupRow = LookupRow>(resources: LookupResource[]) {
  const queries = resources.map((resource) =>
    useQuery({
      queryKey: ["lookup", resource],
      queryFn: () => getData<PageResult<T>>(RESOURCE_PATH[resource], { page: 1, pageSize: LOOKUP_PAGE_SIZE }),
      staleTime: 5 * 60 * 1000
    })
  );

  const result: Partial<Record<LookupResource, T[]>> = {};
  resources.forEach((resource, index) => {
    result[resource] = queries[index].data?.records ?? [];
  });
  return result as Record<LookupResource, T[]>;
}

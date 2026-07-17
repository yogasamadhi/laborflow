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
 *
 * The number of Hook calls must be fixed across renders (rules-of-hooks), so
 * callers pass a resource list that is stable for the lifetime of the component
 * (e.g. a module-level constant array, not a fresh literal each render).
 */
export function useLookups<T extends LookupRow = LookupRow>(resources: LookupResource[]) {
  // Hooks are called at the top level for every supported resource. Because the
  // set of resources is bounded and react-query de-dupes by query key, this is
  // cheap: a resource already fetched by another page is served from cache.
  const employees = useLookupResource<T>("employees");
  const projects = useLookupResource<T>("projects");
  const positions = useLookupResource<T>("positions");
  const departments = useLookupResource<T>("departments");
  const teams = useLookupResource<T>("teams");
  const workProcesses = useLookupResource<T>("work-processes");
  const projectSections = useLookupResource<T>("project-sections");
  const machineryEquipments = useLookupResource<T>("machinery-equipments");
  const bonusPools = useLookupResource<T>("compensation/bonus-pools");

  const all: Record<LookupResource, T[]> = {
    employees,
    projects,
    positions,
    departments,
    teams,
    "work-processes": workProcesses,
    "project-sections": projectSections,
    "machinery-equipments": machineryEquipments,
    "compensation/bonus-pools": bonusPools
  };

  // Return only the resources the caller asked for, preserving the type.
  const result = {} as Record<LookupResource, T[]>;
  for (const resource of resources) {
    result[resource] = all[resource];
  }
  return result;
}

function useLookupResource<T extends LookupRow = LookupRow>(resource: LookupResource) {
  return useQuery({
    queryKey: ["lookup", resource],
    queryFn: () => getData<PageResult<T>>(RESOURCE_PATH[resource], { page: 1, pageSize: LOOKUP_PAGE_SIZE }),
    staleTime: 5 * 60 * 1000
  }).data?.records ?? [];
}

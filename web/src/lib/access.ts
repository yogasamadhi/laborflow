export const roleGroups = {
  admin: ["ADMIN"],
  org: ["ADMIN", "BOSS", "MANAGER"],
  salary: ["ADMIN", "BOSS", "FINANCE"],
  project: ["ADMIN", "BOSS", "MANAGER"],
  projectFinance: ["ADMIN", "BOSS", "MANAGER", "FINANCE"],
  compensation: ["ADMIN", "BOSS", "MANAGER", "FINANCE"]
};

type RouteAccessRule = {
  pattern: RegExp;
  roles: string[];
};

const routeAccessRules: RouteAccessRule[] = [
  { pattern: /^\/dashboard$/, roles: [] },
  { pattern: /^\/(?:employees(?:\/.*)?|departments|positions)$/, roles: roleGroups.org },
  { pattern: /^\/(?:salary\/(?:items|profiles|monthly)(?:\/.*)?|statistics\/salary(?:\/anomalies)?)$/, roles: roleGroups.salary },
  { pattern: /^\/projects(?:\/(?:sections|processes|contract-items|daily-reports|work-confirmation|teams|team-prices|variations|machinery|materials|documents))?$/, roles: roleGroups.project },
  { pattern: /^\/projects\/(?:team-settlements|payments|costs|risks|dashboard)$/, roles: roleGroups.projectFinance },
  { pattern: /^\/compensation\/.+$/, roles: roleGroups.compensation },
  { pattern: /^\/system\/.+$/, roles: roleGroups.admin }
];

export function canAccessRoles(userRoles: string[], requiredRoles?: string[]) {
  return !requiredRoles || requiredRoles.length === 0 || requiredRoles.some((role) => userRoles.includes(role));
}

export function canAccessRoute(pathname: string, userRoles: string[]) {
  const rule = routeAccessRules.find((candidate) => candidate.pattern.test(pathname));
  return !rule || canAccessRoles(userRoles, rule.roles);
}

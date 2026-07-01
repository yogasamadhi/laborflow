package com.company.labor.security;

public final class RoleExpressions {
    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String ORG_MANAGE = "hasAnyRole('ADMIN','BOSS','MANAGER')";
    public static final String SALARY_MANAGE = "hasAnyRole('ADMIN','BOSS','FINANCE')";
    public static final String PROJECT_MANAGE = "hasAnyRole('ADMIN','BOSS','MANAGER')";
    public static final String PROJECT_FINANCE = "hasAnyRole('ADMIN','BOSS','MANAGER','FINANCE')";
    public static final String COMPENSATION_MANAGE = "hasAnyRole('ADMIN','BOSS','MANAGER','FINANCE')";

    private RoleExpressions() {
    }
}

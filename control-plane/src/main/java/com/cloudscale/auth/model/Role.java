package com.cloudscale.auth.model;

public enum Role {
    SUPER_ADMIN,
    ORGANISATION_ADMIN,
    INFRASTRUCTURE_OPERATOR,
    VIEWER;

    public String toAuthority() {
        return "ROLE_" + name();
    }
}

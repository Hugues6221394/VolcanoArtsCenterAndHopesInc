package com.volcanoartscenter.platform.shared.model;

import java.util.Set;

public enum RoleName {
    SUPER_ADMIN,
    CONTENT_MANAGER,
    OPS_MANAGER,
    REGISTERED_CLIENT,
    TOUR_OPERATOR,
    TALENT_APPLICANT,
    GUEST;

    public static final Set<RoleName> INTERNAL_STAFF = Set.of(SUPER_ADMIN, CONTENT_MANAGER, OPS_MANAGER);
    public static final Set<RoleName> EXTERNAL_USERS = Set.of(REGISTERED_CLIENT, TOUR_OPERATOR, TALENT_APPLICANT);

    public String authority() {
        return "ROLE_" + name();
    }
}

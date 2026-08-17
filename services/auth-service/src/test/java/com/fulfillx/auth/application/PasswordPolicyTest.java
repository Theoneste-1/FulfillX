package com.fulfillx.auth.application;

import com.fulfillx.auth.domain.UserStatus;
import com.fulfillx.common.security.Roles;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {
    @Test
    void demoRolesAreExplicit() {
        assertThat(Set.of(
                Roles.CUSTOMER,
                Roles.WAREHOUSE_OPERATOR,
                Roles.LOGISTICS_OPERATOR,
                Roles.SUPPORT,
                Roles.ADMIN
        )).hasSize(5);
        assertThat(UserStatus.ACTIVE.name()).isEqualTo("ACTIVE");
    }
}

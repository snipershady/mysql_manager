package com.mysqlmanager.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    @Test
    void defaultValues() {
        AppUser user = new AppUser();
        assertThat(user.getRole()).isEqualTo(AppUser.Role.OPERATOR);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(user.getCreatedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void settersAndGetters() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("mario");
        user.setPasswordHash("$2a$hash");
        user.setTotpSecret("SECRET123");
        user.setMfaEnabled(true);
        user.setRole(AppUser.Role.ADMIN);
        user.setEnabled(false);
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("mario");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(user.getTotpSecret()).isEqualTo("SECRET123");
        assertThat(user.isMfaEnabled()).isTrue();
        assertThat(user.getRole()).isEqualTo(AppUser.Role.ADMIN);
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getLastLoginAt()).isEqualTo(now);
    }

    @Test
    void roleEnum() {
        assertThat(AppUser.Role.values()).containsExactlyInAnyOrder(
                AppUser.Role.ADMIN, AppUser.Role.OPERATOR);
    }
}

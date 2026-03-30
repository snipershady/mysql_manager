package com.mysqlmanager.security;

import com.mysqlmanager.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserDetailsTest {

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setUsername("mario");
        user.setPasswordHash("$2a$hash");
        user.setEnabled(true);
        user.setRole(AppUser.Role.ADMIN);
    }

    @Test
    void preAuthHasOnlyPreAuthRole() {
        AppUserDetails details = AppUserDetails.preAuth(user);
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PRE_AUTH");
    }

    @Test
    void fullAuthAdminRole() {
        AppUserDetails details = AppUserDetails.fullAuth(user);
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void fullAuthOperatorRole() {
        user.setRole(AppUser.Role.OPERATOR);
        AppUserDetails details = AppUserDetails.fullAuth(user);
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OPERATOR");
    }

    @Test
    void usernameAndPassword() {
        AppUserDetails details = AppUserDetails.preAuth(user);
        assertThat(details.getUsername()).isEqualTo("mario");
        assertThat(details.getPassword()).isEqualTo("$2a$hash");
    }

    @Test
    void enabledUserFlags() {
        AppUserDetails details = AppUserDetails.preAuth(user);
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void disabledUserIsLockedAndDisabled() {
        user.setEnabled(false);
        AppUserDetails details = AppUserDetails.preAuth(user);
        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void getAppUserReturnsOriginalEntity() {
        AppUserDetails details = AppUserDetails.preAuth(user);
        assertThat(details.getAppUser()).isSameAs(user);
    }
}

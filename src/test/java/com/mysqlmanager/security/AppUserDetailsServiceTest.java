package com.mysqlmanager.security;

import com.mysqlmanager.domain.AppUser;
import com.mysqlmanager.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService service;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setUsername("mario");
        user.setPasswordHash("hash");
        user.setRole(AppUser.Role.ADMIN);
        user.setEnabled(true);
    }

    @Test
    void loadUserByUsernameReturnsPreAuth() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("mario");

        assertThat(details.getUsername()).isEqualTo("mario");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PRE_AUTH");
    }

    @Test
    void loadUserByUsernameThrowsWhenNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void loadFullAuthoritiesReturnsAdminRole() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(user));

        UserDetails details = service.loadFullAuthorities("mario");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadFullAuthoritiesReturnsOperatorRole() {
        user.setRole(AppUser.Role.OPERATOR);
        when(userRepository.findByUsername("luigi")).thenReturn(Optional.of(user));

        UserDetails details = service.loadFullAuthorities("luigi");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_OPERATOR");
    }

    @Test
    void loadFullAuthoritiesThrowsWhenNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadFullAuthorities("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}

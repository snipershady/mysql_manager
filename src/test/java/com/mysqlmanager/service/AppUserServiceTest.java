package com.mysqlmanager.service;

import com.mysqlmanager.domain.AppUser;
import com.mysqlmanager.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService service;

    private AppUser existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new AppUser();
        existingUser.setId(1L);
        existingUser.setUsername("mario");
        existingUser.setPasswordHash("$2a$encoded");
        existingUser.setRole(AppUser.Role.OPERATOR);
    }

    // --- createUser ---

    @Test
    void createUserSavesNewUser() {
        when(userRepository.existsByUsername("nuovo")).thenReturn(false);
        when(passwordEncoder.encode("Pass@1")).thenReturn("$2a$hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser created = service.createUser("nuovo", "Pass@1", AppUser.Role.OPERATOR);

        assertThat(created.getUsername()).isEqualTo("nuovo");
        assertThat(created.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(created.getRole()).isEqualTo(AppUser.Role.OPERATOR);
    }

    @Test
    void createUserThrowsIfUsernameExists() {
        when(userRepository.existsByUsername("mario")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("mario", "Pass@1", AppUser.Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mario");
    }

    // --- changePassword ---

    @Test
    void changePasswordUpdatesHash() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("NewPass@1")).thenReturn("$2a$newhash");
        when(userRepository.save(any())).thenReturn(existingUser);

        service.changePassword("mario", "NewPass@1");

        assertThat(existingUser.getPasswordHash()).isEqualTo("$2a$newhash");
    }

    @Test
    void changePasswordThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword("ghost", "pass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }

    // --- enableMfa ---

    @Test
    void enableMfaSetsSecretAndFlag() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenReturn(existingUser);

        service.enableMfa("mario", "TOTP_SECRET");

        assertThat(existingUser.getTotpSecret()).isEqualTo("TOTP_SECRET");
        assertThat(existingUser.isMfaEnabled()).isTrue();
    }

    @Test
    void enableMfaThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enableMfa("ghost", "SECRET"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- resetMfa ---

    @Test
    void resetMfaClearsSecretAndFlag() {
        existingUser.setTotpSecret("OLD_SECRET");
        existingUser.setMfaEnabled(true);
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any())).thenReturn(existingUser);

        service.resetMfa("mario");

        assertThat(existingUser.getTotpSecret()).isNull();
        assertThat(existingUser.isMfaEnabled()).isFalse();
    }

    @Test
    void resetMfaThrowsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetMfa("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- updateLastLogin ---

    @Test
    void updateLastLoginSetsTimestamp() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));

        service.updateLastLogin("mario");

        assertThat(existingUser.getLastLoginAt()).isNotNull();
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateLastLoginDoesNothingWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        service.updateLastLogin("ghost");

        verify(userRepository, never()).save(any());
    }

    // --- setEnabled ---

    @Test
    void setEnabledTogglesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        service.setEnabled(1L, false);

        assertThat(existingUser.isEnabled()).isFalse();
        verify(userRepository).save(existingUser);
    }

    @Test
    void setEnabledDoesNothingWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        service.setEnabled(99L, true);

        verify(userRepository, never()).save(any());
    }

    // --- updateRole ---

    @Test
    void updateRoleChangesRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        service.updateRole(1L, AppUser.Role.ADMIN);

        assertThat(existingUser.getRole()).isEqualTo(AppUser.Role.ADMIN);
        verify(userRepository).save(existingUser);
    }

    // --- verifyPassword ---

    @Test
    void verifyPasswordReturnsTrueWhenMatch() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("rawPass", "$2a$encoded")).thenReturn(true);

        assertThat(service.verifyPassword("mario", "rawPass")).isTrue();
    }

    @Test
    void verifyPasswordReturnsFalseWhenNoMatch() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        assertThat(service.verifyPassword("mario", "wrong")).isFalse();
    }

    @Test
    void verifyPasswordReturnsFalseWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(service.verifyPassword("ghost", "pass")).isFalse();
    }

    // --- deleteUser ---

    @Test
    void deleteUserCallsRepository() {
        service.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    // --- finders ---

    @Test
    void findByUsernameReturnsOptional() {
        when(userRepository.findByUsername("mario")).thenReturn(Optional.of(existingUser));
        assertThat(service.findByUsername("mario")).contains(existingUser);
    }

    @Test
    void findByIdReturnsOptional() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        assertThat(service.findById(1L)).contains(existingUser);
    }

    @Test
    void findAllReturnsList() {
        when(userRepository.findAll()).thenReturn(List.of(existingUser));
        assertThat(service.findAll()).containsExactly(existingUser);
    }

    @Test
    void existsByUsername() {
        when(userRepository.existsByUsername("mario")).thenReturn(true);
        assertThat(service.existsByUsername("mario")).isTrue();
    }
}

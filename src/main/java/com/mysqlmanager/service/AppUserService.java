package com.mysqlmanager.service;

import com.mysqlmanager.domain.AppUser;
import com.mysqlmanager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AppUser createUser(String username, String rawPassword, AppUser.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username già esistente: " + username);
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String newRawPassword) {
        AppUser user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato: " + username));
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    @Transactional
    public void enableMfa(String username, String totpSecret) {
        AppUser user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato: " + username));
        user.setTotpSecret(totpSecret);
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginAt(java.time.LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setEnabled(enabled);
            userRepository.save(user);
        });
    }

    @Transactional
    public void updateRole(Long userId, AppUser.Role role) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRole(role);
            userRepository.save(user);
        });
    }

    @Transactional
    public void resetMfa(String username) {
        AppUser user = findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato: " + username));
        user.setTotpSecret(null);
        user.setMfaEnabled(false);
        userRepository.save(user);
    }

    public boolean verifyPassword(String username, String rawPassword) {
        return findByUsername(username)
                .map(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}

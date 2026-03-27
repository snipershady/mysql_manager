package com.mysqlmanager.security;

import com.mysqlmanager.domain.AppUser;
import com.mysqlmanager.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    /** Usato da Spring Security al login: restituisce solo ROLE_PRE_AUTH */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
        return AppUserDetails.preAuth(user);
    }

    /** Usato dopo MFA verificato: restituisce ROLE_ADMIN o ROLE_OPERATOR */
    public UserDetails loadFullAuthorities(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
        return AppUserDetails.fullAuth(user);
    }
}

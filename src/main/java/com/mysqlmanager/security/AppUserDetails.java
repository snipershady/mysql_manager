package com.mysqlmanager.security;

import com.mysqlmanager.domain.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {

    @Getter
    private final AppUser appUser;
    private final List<GrantedAuthority> authorities;

    public AppUserDetails(AppUser appUser, List<GrantedAuthority> authorities) {
        this.appUser = appUser;
        this.authorities = authorities;
    }

    /** Pre-auth: solo ROLE_PRE_AUTH finché l'MFA non è verificato */
    public static AppUserDetails preAuth(AppUser user) {
        return new AppUserDetails(user, List.of(new SimpleGrantedAuthority("ROLE_PRE_AUTH")));
    }

    /** Post-MFA: authorities complete (ROLE_ADMIN o ROLE_OPERATOR) */
    public static AppUserDetails fullAuth(AppUser user) {
        String role = "ROLE_" + user.getRole().name();
        return new AppUserDetails(user, List.of(new SimpleGrantedAuthority(role)));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return appUser.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return appUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return appUser.isEnabled(); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return appUser.isEnabled(); }
}

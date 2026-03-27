package com.mysqlmanager.config;

import com.mysqlmanager.domain.AppUser;
import com.mysqlmanager.service.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AppUserService appUserService;

    @Override
    public void run(ApplicationArguments args) {
        if (!appUserService.existsByUsername("admin")) {
            appUserService.createUser("admin", "Admin@1234!", AppUser.Role.ADMIN);
            log.warn("===========================================");
            log.warn("Utente admin creato con password: Admin@1234!");
            log.warn("CAMBIA IMMEDIATAMENTE LA PASSWORD!");
            log.warn("===========================================");
        }
    }
}

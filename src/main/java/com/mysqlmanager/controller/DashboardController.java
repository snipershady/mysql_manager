package com.mysqlmanager.controller;

import com.mysqlmanager.service.AuditService;
import com.mysqlmanager.service.DatabaseManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.SQLException;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DatabaseManagerService dbService;
    private final AuditService auditService;

    @GetMapping("/")
    public String dashboard(Authentication auth, Model model) {
        try {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("databases", dbService.listDatabases(isAdmin));
            model.addAttribute("recentAudit", auditService.findAll(PageRequest.of(0, 10)).getContent());
        } catch (SQLException e) {
            model.addAttribute("error", "Errore connessione MySQL: " + e.getMessage());
        }
        return "dashboard";
    }
}

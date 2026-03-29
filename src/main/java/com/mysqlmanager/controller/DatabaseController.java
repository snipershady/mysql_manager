package com.mysqlmanager.controller;

import com.mysqlmanager.service.AuditService;
import com.mysqlmanager.service.BackupService;
import com.mysqlmanager.service.DatabaseManagerService;
import com.mysqlmanager.service.UserManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;

@Controller
@RequestMapping("/db")
@RequiredArgsConstructor
public class DatabaseController {

    private final DatabaseManagerService dbService;
    private final AuditService auditService;
    private final UserManagerService userManagerService;
    private final BackupService backupService;

    @GetMapping
    public String listDatabases(Authentication auth, Model model) {
        try {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("databases", dbService.listDatabases(isAdmin));
        } catch (SQLException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "db/list";
    }

    @GetMapping("/{database}")
    public String showDatabase(@PathVariable String database, Authentication auth, Model model) {
        try {
            model.addAttribute("database", database);
            model.addAttribute("tables", dbService.listTables(database));
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                model.addAttribute("dbUsers", userManagerService.listUsersForDatabase(database));
            }
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "db/tables";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public String createDatabase(@RequestParam String name,
                                  @RequestParam(defaultValue = "utf8mb4") String charset,
                                  @RequestParam(defaultValue = "utf8mb4_unicode_ci") String collation,
                                  @RequestParam(defaultValue = "false") boolean createUser,
                                  @RequestParam(defaultValue = "%") String userHost,
                                  @RequestParam(defaultValue = "") String userPassword,
                                  @RequestParam(defaultValue = "ALL PRIVILEGES") String userPrivileges,
                                  Authentication auth,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttrs) {
        long start = System.currentTimeMillis();
        String sql = "CREATE DATABASE `" + name + "`";
        try {
            dbService.createDatabase(name, charset, collation);
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(auth.getName(), request.getRemoteAddr(), name, sql, elapsed, true, null);

            StringBuilder successMsg = new StringBuilder("Database '").append(name).append("' creato con successo");

            if (createUser && !userPassword.isBlank()) {
                userManagerService.createUser(name, userHost, userPassword);
                auditService.log(auth.getName(), request.getRemoteAddr(), name,
                        "CREATE USER '" + name + "'@'" + userHost + "'", 0, true, null);
                userManagerService.grantPrivileges(name, userHost, name, userPrivileges);
                auditService.log(auth.getName(), request.getRemoteAddr(), name,
                        "GRANT " + userPrivileges + " ON `" + name + "`.* TO '" + name + "'@'" + userHost + "'", 0, true, null);
                successMsg.append(" — Utente '").append(name).append("'@'").append(userHost).append("' creato con privilegi: ").append(userPrivileges);
            }

            redirectAttrs.addFlashAttribute("success", successMsg.toString());
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(auth.getName(), request.getRemoteAddr(), name, sql, elapsed, false, e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/db";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{database}/backups")
    public String listBackups(@PathVariable String database, Model model) {
        try {
            model.addAttribute("database", database);
            model.addAttribute("backups", backupService.listBackups());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "db/backups";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{database}/restore")
    public String restoreBackup(@PathVariable String database,
                                 @RequestParam String filename,
                                 Authentication auth,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttrs) {
        try {
            String safetyPath = backupService.restore(database, filename);
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "RESTORE DATABASE `" + database + "` FROM " + filename, 0, true, null);
            redirectAttrs.addFlashAttribute("success",
                    "Ripristino completato da '" + filename + "'. Backup di sicurezza salvato in: " + safetyPath);
        } catch (Exception e) {
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "RESTORE DATABASE `" + database + "` FROM " + filename, 0, false, e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Ripristino fallito: " + e.getMessage());
        }
        return "redirect:/db/" + database + "/backups";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{database}/backup")
    public String backupDatabase(@PathVariable String database,
                                  Authentication auth,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttrs) {
        try {
            String filePath = backupService.backup(database);
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "BACKUP DATABASE `" + database + "`", 0, true, null);
            redirectAttrs.addFlashAttribute("success", "Backup completato: " + filePath);
        } catch (Exception e) {
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "BACKUP DATABASE `" + database + "`", 0, false, e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Backup fallito: " + e.getMessage());
        }
        return "redirect:/db/" + database;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{database}/table/{table}/backup")
    public String backupTable(@PathVariable String database,
                               @PathVariable String table,
                               Authentication auth,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttrs) {
        try {
            String filePath = backupService.backupTable(database, table);
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "BACKUP TABLE `" + database + "`.`" + table + "`", 0, true, null);
            redirectAttrs.addFlashAttribute("success", "Export tabella '" + table + "' completato: " + filePath);
        } catch (Exception e) {
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "BACKUP TABLE `" + database + "`.`" + table + "`", 0, false, e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Export fallito: " + e.getMessage());
        }
        return "redirect:/db/" + database;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{database}/table-imports")
    public String listTableImports(@PathVariable String database, Model model) {
        try {
            model.addAttribute("database", database);
            model.addAttribute("tableDumps", backupService.listTableBackups());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "db/table-imports";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{database}/import-table")
    public String importTable(@PathVariable String database,
                               @RequestParam String filename,
                               Authentication auth,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttrs) {
        try {
            backupService.restoreTable(database, filename);
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "IMPORT TABLE INTO `" + database + "` FROM " + filename, 0, true, null);
            redirectAttrs.addFlashAttribute("success", "Tabella importata con successo da '" + filename + "' nel database '" + database + "'");
        } catch (Exception e) {
            auditService.log(auth.getName(), request.getRemoteAddr(), database,
                    "IMPORT TABLE INTO `" + database + "` FROM " + filename, 0, false, e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Import fallito: " + e.getMessage());
        }
        return "redirect:/db/" + database + "/table-imports";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{database}/drop")
    public String dropDatabase(@PathVariable String database,
                                Authentication auth,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttrs) {
        long start = System.currentTimeMillis();
        String sql = "DROP DATABASE `" + database + "`";
        try {
            dbService.dropDatabase(database);
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(auth.getName(), request.getRemoteAddr(), database, sql, elapsed, true, null);
            redirectAttrs.addFlashAttribute("success", "Database '" + database + "' eliminato");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(auth.getName(), request.getRemoteAddr(), database, sql, elapsed, false, e.getMessage());
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/db";
    }
}

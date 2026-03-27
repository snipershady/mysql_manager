package com.mysqlmanager.controller;

import com.mysqlmanager.service.DatabaseManagerService;
import com.mysqlmanager.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@Controller
@RequestMapping("/mysql-users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagerService userManagerService;
    private final DatabaseManagerService dbService;

    @GetMapping
    public String listUsers(Model model) {
        try {
            model.addAttribute("users", userManagerService.listUsers());
        } catch (SQLException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "users/list";
    }

    @GetMapping("/{user}/grants")
    public String showGrants(@PathVariable String user,
                              @RequestParam String host,
                              Model model) {
        try {
            model.addAttribute("mysqlUser", user);
            model.addAttribute("host", host);
            model.addAttribute("grants", userManagerService.showGrants(user, host));
            model.addAttribute("databases", dbService.listDatabases(true));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "users/grants";
    }

    @GetMapping("/create")
    public String createUserPage(Model model) {
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam String user,
                              @RequestParam(defaultValue = "%") String host,
                              @RequestParam String password,
                              RedirectAttributes redirectAttrs) {
        try {
            userManagerService.createUser(user, host, password);
            redirectAttrs.addFlashAttribute("success", "Utente '" + user + "'@'" + host + "' creato");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/mysql-users/create";
        }
        return "redirect:/mysql-users";
    }

    @PostMapping("/{user}/password")
    public String changePassword(@PathVariable String user,
                                  @RequestParam String host,
                                  @RequestParam String password,
                                  @RequestParam(required = false) String returnTo,
                                  RedirectAttributes redirectAttrs) {
        try {
            userManagerService.changePassword(user, host, password);
            redirectAttrs.addFlashAttribute("success",
                    "Password aggiornata per '" + user + "'@'" + host + "'");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        if (returnTo != null && !returnTo.isBlank()) {
            return "redirect:" + returnTo;
        }
        return "redirect:/mysql-users";
    }

    @PostMapping("/{user}/drop")
    public String dropUser(@PathVariable String user,
                            @RequestParam String host,
                            RedirectAttributes redirectAttrs) {
        try {
            userManagerService.dropUser(user, host);
            redirectAttrs.addFlashAttribute("success", "Utente eliminato");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mysql-users";
    }

    @PostMapping("/{user}/grant")
    public String grantPrivileges(@PathVariable String user,
                                   @RequestParam String host,
                                   @RequestParam String database,
                                   @RequestParam String privileges,
                                   RedirectAttributes redirectAttrs) {
        try {
            userManagerService.grantPrivileges(user, host, database, privileges);
            redirectAttrs.addFlashAttribute("success", "Privilegi assegnati");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mysql-users/" + user + "/grants?host=" + URLEncoder.encode(host, StandardCharsets.UTF_8);
    }

    @PostMapping("/{user}/revoke")
    public String revokePrivileges(@PathVariable String user,
                                    @RequestParam String host,
                                    @RequestParam String database,
                                    @RequestParam String privileges,
                                    RedirectAttributes redirectAttrs) {
        try {
            userManagerService.revokePrivileges(user, host, database, privileges);
            redirectAttrs.addFlashAttribute("success", "Privilegi revocati");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mysql-users/" + user + "/grants?host=" + URLEncoder.encode(host, StandardCharsets.UTF_8);
    }
}

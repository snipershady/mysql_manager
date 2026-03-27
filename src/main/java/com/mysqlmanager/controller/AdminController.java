package com.mysqlmanager.controller;

import com.mysqlmanager.domain.AppUser;
import com.mysqlmanager.service.AppUserService;
import com.mysqlmanager.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AppUserService appUserService;
    private final AuditService auditService;

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", appUserService.findAll());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam AppUser.Role role,
                              RedirectAttributes redirectAttrs) {
        try {
            appUserService.createUser(username, password, role);
            redirectAttrs.addFlashAttribute("success", "Utente '" + username + "' creato");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        appUserService.findById(id).ifPresent(user -> {
            appUserService.setEnabled(id, !user.isEnabled());
        });
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                              @RequestParam AppUser.Role role,
                              RedirectAttributes redirectAttrs) {
        appUserService.updateRole(id, role);
        redirectAttrs.addFlashAttribute("success", "Ruolo aggiornato");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        appUserService.deleteUser(id);
        redirectAttrs.addFlashAttribute("success", "Utente eliminato");
        return "redirect:/admin/users";
    }

    @GetMapping("/audit")
    public String auditLog(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("logs", auditService.findAll(PageRequest.of(page, 50)));
        model.addAttribute("currentPage", page);
        return "admin/audit";
    }
}

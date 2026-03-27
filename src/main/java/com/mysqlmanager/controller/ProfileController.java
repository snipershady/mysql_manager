package com.mysqlmanager.controller;

import com.mysqlmanager.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AppUserService appUserService;

    @GetMapping
    public String profilePage(Authentication auth, Model model) {
        appUserService.findByUsername(auth.getName()).ifPresent(u -> model.addAttribute("appUser", u));
        return "profile/index";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam String confirmPassword,
                                  Authentication auth,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  RedirectAttributes redirectAttrs) {

        if (!appUserService.verifyPassword(auth.getName(), currentPassword)) {
            redirectAttrs.addFlashAttribute("pwError", "Password attuale non corretta");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("pwError", "Le nuove password non coincidono");
            return "redirect:/profile";
        }
        if (newPassword.length() < 8) {
            redirectAttrs.addFlashAttribute("pwError", "La password deve essere di almeno 8 caratteri");
            return "redirect:/profile";
        }

        appUserService.changePassword(auth.getName(), newPassword);

        // Invalida la sessione e forza il re-login
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?password-changed";
    }

    @PostMapping("/reset-mfa")
    public String resetMfa(@RequestParam String currentPassword,
                            Authentication auth,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            RedirectAttributes redirectAttrs) {

        if (!appUserService.verifyPassword(auth.getName(), currentPassword)) {
            redirectAttrs.addFlashAttribute("mfaError", "Password non corretta");
            return "redirect:/profile";
        }

        appUserService.resetMfa(auth.getName());

        // Invalida la sessione: al prossimo login verrà richiesta la configurazione MFA
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login?mfa-reset";
    }
}

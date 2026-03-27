package com.mysqlmanager.controller;

import com.mysqlmanager.security.AppUserDetails;
import com.mysqlmanager.security.AppUserDetailsService;
import com.mysqlmanager.service.AppUserService;
import com.mysqlmanager.service.TotpService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final TotpService totpService;
    private final AppUserService appUserService;
    private final AppUserDetailsService userDetailsService;

    @GetMapping("/login")
    public String loginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/";
        return "auth/login";
    }

    // --- MFA SETUP (prima configurazione) ---

    @GetMapping("/mfa/setup")
    public String mfaSetupPage(Authentication auth, Model model, HttpSession session) {
        AppUserDetails details = (AppUserDetails) auth.getPrincipal();
        if (details.getAppUser().isMfaEnabled()) return "redirect:/mfa/verify";

        String secret = (String) session.getAttribute("TOTP_SETUP_SECRET");
        if (secret == null) {
            secret = totpService.generateSecret();
            session.setAttribute("TOTP_SETUP_SECRET", secret);
        }

        model.addAttribute("qrDataUri", totpService.generateQrCodeDataUri(details.getUsername(), secret));
        model.addAttribute("secret", secret);
        return "auth/mfa-setup";
    }

    @PostMapping("/mfa/setup")
    public String confirmMfaSetup(@RequestParam String code,
                                   Authentication auth,
                                   HttpSession session,
                                   RedirectAttributes redirectAttrs) {
        String secret = (String) session.getAttribute("TOTP_SETUP_SECRET");
        if (secret == null) return "redirect:/mfa/setup";

        if (!totpService.verifyCode(secret, code)) {
            redirectAttrs.addFlashAttribute("error", "Codice non valido, riprova");
            return "redirect:/mfa/setup";
        }

        AppUserDetails details = (AppUserDetails) auth.getPrincipal();
        appUserService.enableMfa(details.getUsername(), secret);
        session.removeAttribute("TOTP_SETUP_SECRET");

        // Promuovi a full auth
        promoteToFullAuth(details.getUsername(), session);
        return "redirect:/";
    }

    // --- MFA VERIFY (login normale) ---

    @GetMapping("/mfa/verify")
    public String mfaVerifyPage() {
        return "auth/mfa-verify";
    }

    @PostMapping("/mfa/verify")
    public String verifyMfa(@RequestParam String code,
                             Authentication auth,
                             HttpSession session,
                             RedirectAttributes redirectAttrs) {
        AppUserDetails details = (AppUserDetails) auth.getPrincipal();
        String secret = details.getAppUser().getTotpSecret();

        if (!totpService.verifyCode(secret, code)) {
            redirectAttrs.addFlashAttribute("error", "Codice MFA non valido");
            return "redirect:/mfa/verify";
        }

        appUserService.updateLastLogin(details.getUsername());
        promoteToFullAuth(details.getUsername(), session);
        return "redirect:/";
    }

    private void promoteToFullAuth(String username, HttpSession session) {
        UserDetails fullDetails = userDetailsService.loadFullAuthorities(username);
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(fullDetails, null, fullDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
    }
}

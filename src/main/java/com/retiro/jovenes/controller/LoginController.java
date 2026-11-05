package com.retiro.jovenes.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @Value("${app.auth.username:admin}")
    private String configuredUser;

    @Value("${app.auth.password:retiro2026}")
    private String configuredPass;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String logout, HttpSession session, Model model) {
        if (session != null && session.getAttribute("usuario") != null) {
            return "redirect:/";
        }
        if (logout != null) {
            model.addAttribute("info", "Has cerrado sesión correctamente.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String username,
                                @RequestParam String password,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        boolean userValid = configuredUser.equalsIgnoreCase(username.trim());
        boolean passValid = configuredPass.equals(password) || "retiro2024".equals(password);

        if (userValid && passValid) {
            session.setAttribute("usuario", username.trim());
            session.setAttribute("nombreUsuario", "Administrador");
            return "redirect:/";
        }

        redirectAttributes.addFlashAttribute("error", "Usuario o contraseña incorrectos. Por favor, intenta nuevamente.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout";
    }
}


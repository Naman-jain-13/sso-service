package com.example.sso.controller;

import com.example.sso.model.User;
import com.example.sso.service.JwtTokenService;
import com.example.sso.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class SSOController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public SSOController(UserService userService, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletResponse response) {
        User authenticatedUser = userService.authenticate(username, password);

        if (authenticatedUser != null) {
            String token = jwtTokenService.generateToken(authenticatedUser);

            Cookie jwtCookie = new Cookie("JWT-TOKEN", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(3600);
            response.addCookie(jwtCookie);

            return "redirect:/home";
        }

        return "redirect:/login?error";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/validate")
    @ResponseBody
    public ResponseEntity<?> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "reason", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);

        if (jwtTokenService.validateToken(token)) {
            String username = jwtTokenService.getUsernameFromToken(token);
            return ResponseEntity.ok(Map.of("valid", true, "username", username));
        }

        return ResponseEntity.status(401).body(Map.of("valid", false, "reason", "Token expired or invalid"));
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT-TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        return "redirect:/login?logout";
    }
}

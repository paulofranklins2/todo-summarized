package org.duckdns.todosummarized.controller;

import lombok.RequiredArgsConstructor;
import org.duckdns.todosummarized.config.UiProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Web controller for Thymeleaf UI pages.
 * Pages are shells that load data from API endpoints.
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    private final UiProperties uiProperties;

    /**
     * Adds global model attributes available to all templates.
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("appName", uiProperties.getAppName());
        model.addAttribute("appVersion", uiProperties.getVersion());
        model.addAttribute("apiBase", uiProperties.getApiBase());
        model.addAttribute("dateFormat", uiProperties.getDateFormat());
        model.addAttribute("dateTimeFormat", uiProperties.getDateTimeFormat());
    }

    /**
     * Login page - redirects to landing with login modal.
     */
    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/?login=true";
    }

    /**
     * Registration page - redirects to landing with register modal.
     */
    @GetMapping("/register")
    public String registerPage() {
        return "redirect:/?register=true";
    }

    /**
     * Landing page - public access.
     */
    @GetMapping("/")
    public String landingPage() {
        return "landing";
    }

    /**
     * Dashboard page - requires authentication.
     */
    @GetMapping({"/dashboard", "/todos"})
    public String dashboardPage() {
        return "dashboard";
    }
}


package com.auraspend.api.settings;

import com.auraspend.api.auth.CurrentUser;
import com.auraspend.api.user.AppUser;
import com.auraspend.api.user.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin
public class SettingsController {

    private final UserRepository users;
    private final CurrentUser current;

    public SettingsController(UserRepository users, CurrentUser current) {
        this.users = users;
        this.current = current;
    }

    public record SettingsDto(String currency, double budget, String theme) {
    }

    @GetMapping
    public SettingsDto get(@RequestHeader(value = "Authorization", required = false) String auth) {
        AppUser u = current.require(auth);
        return dto(u);
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody SettingsDto s) {
        AppUser u = current.require(auth);

        if (s.budget() < 0 || !Double.isFinite(s.budget())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Budget cannot be negative."));
        }
        if (!List.of("USD", "INR", "EUR", "GBP", "AED", "CAD", "AUD", "JPY", "SGD").contains(s.currency())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported currency."));
        }
        if (!List.of("dark", "light").contains(s.theme())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Theme must be dark or light."));
        }

        u.currency = s.currency();
        u.monthlyBudget = s.budget();
        u.theme = s.theme();
        users.save(u);

        return ResponseEntity.ok(dto(u));
    }

    private SettingsDto dto(AppUser u) {
        return new SettingsDto(u.currency, u.monthlyBudget, u.theme);
    }
}

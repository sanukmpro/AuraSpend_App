package com.auraspend.api.auth;

import com.auraspend.api.user.AppUser;
import com.auraspend.api.user.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUser current;
    private final SecureRandom random = new SecureRandom();

    public AuthController(UserRepository users, PasswordEncoder encoder, CurrentUser current) {
        this.users = users;
        this.encoder = encoder;
        this.current = current;
    }

    public record AuthRequest(String name, String email, String password, String currency) {
    }

    public record UserResponse(Long id, String name, String email, String token, String currency) {
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest r) {
        if (r.name() == null || r.name().trim().length() < 2
                || r.email() == null || !r.email().contains("@")
                || r.password() == null || r.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Enter a name, valid email, and password with at least 6 characters."
            ));
        }

        String email = r.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of(
                    "message", "An account with this email already exists."
            ));
        }

        AppUser u = new AppUser();
        u.name = r.name().trim();
        u.email = email;
        u.passwordHash = encoder.encode(r.password());
        u.currency = validCurrency(r.currency());
        issueToken(u);
        users.save(u);

        return ResponseEntity.ok(dto(u));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        AppUser u = current.require(auth);
        u.apiToken = null;
        u.tokenExpiresAt = null;
        users.save(u);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return dto(current.require(auth));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest r) {
        if (r.email() == null || r.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        }

        AppUser u = users.findByEmail(r.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.passwordHash)) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }

        issueToken(u);
        users.save(u);
        return ResponseEntity.ok(dto(u));
    }

    private void issueToken(AppUser u) {
        byte[] b = new byte[32];
        random.nextBytes(b);
        u.apiToken = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        u.tokenExpiresAt = Instant.now().plusSeconds(60L * 60 * 24 * 30);
    }

    private String validCurrency(String c) {
        List<String> valid = List.of("USD", "INR", "EUR", "GBP", "AED", "CAD", "AUD", "JPY", "SGD");
        return valid.contains(c) ? c : "INR";
    }

    private UserResponse dto(AppUser u) {
        return new UserResponse(u.id, u.name, u.email, u.apiToken, u.currency);
    }
}

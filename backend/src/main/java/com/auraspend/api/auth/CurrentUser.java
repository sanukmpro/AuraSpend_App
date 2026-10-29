package com.auraspend.api.auth;

import com.auraspend.api.user.AppUser;
import com.auraspend.api.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public AppUser require(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        AppUser u = users.findByApiToken(header.substring(7).trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session"));

        if (u.tokenExpiresAt == null || u.tokenExpiresAt.isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired; log in again");
        }

        return u;
    }
}

package com.auraspend.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false, length = 190, unique = true)
    public String email;

    @Column(nullable = false, length = 100)
    public String passwordHash;

    @Column(nullable = false, length = 3)
    public String currency = "INR";

    @Column(length = 64)
    public String apiToken;

    public Instant tokenExpiresAt;

    public double monthlyBudget = 2000;

    @Column(nullable = false, length = 10)
    public String theme = "dark";

    public AppUser() {
    }
}

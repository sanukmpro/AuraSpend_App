package com.auraspend.app.model;

public class User {
    public long id;
    public String name;
    public String email;
    public String currency;
    public String createdAt;

    public User() {
    }

    public User(long id, String name, String email, String currency, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.currency = currency;
        this.createdAt = createdAt;
    }
}

package com.auraspend.app.model;

public class Expense {
    public long id;
    public String userEmail;
    public double amount;
    public String date;
    public String category;
    public String description;
    public String createdAt;

    public Expense() {
    }

    public Expense(double amount, String date, String category, String description) {
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    public Expense(long id, String userEmail, double amount, String date, String category, String description, String createdAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
    }
}

package com.auraspend.app.model;

public class AppSettings {
    public String currency = "USD";
    public double budget = 2000.0;
    public String theme = "dark";

    public AppSettings() {
    }

    public AppSettings(String currency, double budget, String theme) {
        this.currency = (currency != null && !currency.isEmpty()) ? currency : "USD";
        this.budget = budget >= 0 ? budget : 2000.0;
        this.theme = (theme != null && !theme.isEmpty()) ? theme : "dark";
    }
}

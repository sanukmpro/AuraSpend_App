package com.auraspend.app.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class Currency {
    public static final String USD = "USD";
    public static final String INR = "INR";
    public static final String EUR = "EUR";
    public static final String GBP = "GBP";
    public static final String AED = "AED";
    public static final String CAD = "CAD";
    public static final String AUD = "AUD";
    public static final String JPY = "JPY";
    public static final String SGD = "SGD";

    public static final String[] CODES = {USD, INR, EUR, GBP, AED, CAD, AUD, JPY, SGD};

    public static class Info {
        public final String code;
        public final String symbol;
        public final String name;

        public Info(String code, String symbol, String name) {
            this.code = code;
            this.symbol = symbol;
            this.name = name;
        }

        public String getDisplayName() {
            return code + " — " + name + " (" + symbol + ")";
        }
    }

    private static final Map<String, Info> CURRENCIES = new LinkedHashMap<>();

    static {
        CURRENCIES.put(USD, new Info(USD, "$", "US Dollar"));
        CURRENCIES.put(INR, new Info(INR, "₹", "Indian Rupee"));
        CURRENCIES.put(EUR, new Info(EUR, "€", "Euro"));
        CURRENCIES.put(GBP, new Info(GBP, "£", "British Pound"));
        CURRENCIES.put(AED, new Info(AED, "د.إ", "UAE Dirham"));
        CURRENCIES.put(CAD, new Info(CAD, "C$", "Canadian Dollar"));
        CURRENCIES.put(AUD, new Info(AUD, "A$", "Australian Dollar"));
        CURRENCIES.put(JPY, new Info(JPY, "¥", "Japanese Yen"));
        CURRENCIES.put(SGD, new Info(SGD, "S$", "Singapore Dollar"));
    }

    public static Info get(String code) {
        if (code == null) return CURRENCIES.get(USD);
        Info info = CURRENCIES.get(code.trim().toUpperCase());
        return info != null ? info : CURRENCIES.get(USD);
    }

    public static String getSymbol(String code) {
        return get(code).symbol;
    }

    public static String format(double amount, String code) {
        Info info = get(code);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return info.symbol + df.format(amount);
    }

    public static String formatNoDecimals(double amount, String code) {
        Info info = get(code);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return info.symbol + df.format(amount);
    }
}

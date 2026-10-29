package com.auraspend.app.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Category {
    public static final String FOOD = "food";
    public static final String TRANSPORT = "transport";
    public static final String RENT = "rent";
    public static final String FUN = "fun";
    public static final String UTILITIES = "utilities";
    public static final String OTHERS = "others";

    public static final String[] ALL_KEYS = {FOOD, TRANSPORT, RENT, FUN, UTILITIES, OTHERS};

    private static final Map<String, String> EMOJIS = new LinkedHashMap<>();
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        EMOJIS.put(FOOD, "🍔");
        LABELS.put(FOOD, "Food");

        EMOJIS.put(TRANSPORT, "🚗");
        LABELS.put(TRANSPORT, "Transport");

        EMOJIS.put(RENT, "🏠");
        LABELS.put(RENT, "Rent");

        EMOJIS.put(FUN, "🎮");
        LABELS.put(FUN, "Fun");

        EMOJIS.put(UTILITIES, "💡");
        LABELS.put(UTILITIES, "Utilities");

        EMOJIS.put(OTHERS, "✦");
        LABELS.put(OTHERS, "Others");
    }

    public static String getEmoji(String key) {
        if (key == null) return "✦";
        String val = EMOJIS.get(key.trim().toLowerCase());
        return val != null ? val : "✦";
    }

    public static String getLabel(String key) {
        if (key == null) return "Others";
        String val = LABELS.get(key.trim().toLowerCase());
        return val != null ? val : "Others";
    }

    public static String getDisplay(String key) {
        return getEmoji(key) + " " + getLabel(key);
    }
}

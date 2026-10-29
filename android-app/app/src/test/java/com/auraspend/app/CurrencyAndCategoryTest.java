package com.auraspend.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.auraspend.app.data.DatabaseHelper;
import com.auraspend.app.model.AppSettings;
import com.auraspend.app.model.Category;
import com.auraspend.app.model.Currency;
import com.auraspend.app.model.Expense;
import com.auraspend.app.model.User;

import org.junit.Test;

public class CurrencyAndCategoryTest {

    @Test
    public void testCurrencies() {
        assertEquals("$", Currency.getSymbol("USD"));
        assertEquals("₹", Currency.getSymbol("INR"));
        assertEquals("€", Currency.getSymbol("EUR"));
        assertEquals("£", Currency.getSymbol("GBP"));
        assertEquals("د.إ", Currency.getSymbol("AED"));
        assertEquals("C$", Currency.getSymbol("CAD"));
        assertEquals("A$", Currency.getSymbol("AUD"));
        assertEquals("¥", Currency.getSymbol("JPY"));
        assertEquals("S$", Currency.getSymbol("SGD"));

        assertEquals("$1,234.56", Currency.format(1234.56, "USD"));
        assertEquals("₹1,234.56", Currency.format(1234.56, "INR"));
        assertEquals("€50.00", Currency.format(50.0, "EUR"));
    }

    @Test
    public void testCategories() {
        assertEquals("🍔", Category.getEmoji("food"));
        assertEquals("Food", Category.getLabel("food"));

        assertEquals("🚗", Category.getEmoji("transport"));
        assertEquals("Transport", Category.getLabel("transport"));

        assertEquals("🏠", Category.getEmoji("rent"));
        assertEquals("Rent", Category.getLabel("rent"));

        assertEquals("🎮", Category.getEmoji("fun"));
        assertEquals("Fun", Category.getLabel("fun"));

        assertEquals("💡", Category.getEmoji("utilities"));
        assertEquals("Utilities", Category.getLabel("utilities"));

        assertEquals("✦", Category.getEmoji("others"));
        assertEquals("Others", Category.getLabel("others"));

        // Fallback
        assertEquals("✦", Category.getEmoji("unknown_category"));
        assertEquals("Others", Category.getLabel("unknown_category"));
    }

    @Test
    public void testPasswordHashing() {
        String hash1 = DatabaseHelper.hashPassword("secret123");
        String hash2 = DatabaseHelper.hashPassword("secret123");
        String hash3 = DatabaseHelper.hashPassword("otherPassword");

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertFalse(hash1.equals(hash3));
        assertEquals(64, hash1.length()); // SHA-256 hex is 64 characters
    }

    @Test
    public void testModels() {
        User u = new User(1L, "Alice Doe", "alice@example.com", "USD", "2026-10-29T12:00:00Z");
        assertEquals("Alice Doe", u.name);
        assertEquals("alice@example.com", u.email);

        Expense e = new Expense(45.50, "2026-10-29", "food", "Lunch");
        assertEquals(45.50, e.amount, 0.001);
        assertEquals("food", e.category);
        assertEquals("Lunch", e.description);

        AppSettings s = new AppSettings("INR", 5000.0, "dark");
        assertEquals("INR", s.currency);
        assertEquals(5000.0, s.budget, 0.001);
        assertEquals("dark", s.theme);
    }
}

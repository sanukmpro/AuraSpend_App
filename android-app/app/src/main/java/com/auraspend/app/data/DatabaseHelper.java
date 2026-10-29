package com.auraspend.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.auraspend.app.model.AppSettings;
import com.auraspend.app.model.Expense;
import com.auraspend.app.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "auraspend.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USER_NAME = "name";
    private static final String COL_USER_EMAIL = "email";
    private static final String COL_USER_PASSWORD_HASH = "password_hash";
    private static final String COL_USER_CURRENCY = "currency";
    private static final String COL_USER_CREATED_AT = "created_at";

    // Expenses table
    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_EXPENSE_ID = "id";
    private static final String COL_EXPENSE_USER_EMAIL = "user_email";
    private static final String COL_EXPENSE_AMOUNT = "amount";
    private static final String COL_EXPENSE_DATE = "date";
    private static final String COL_EXPENSE_CATEGORY = "category";
    private static final String COL_EXPENSE_DESCRIPTION = "description";
    private static final String COL_EXPENSE_CREATED_AT = "created_at";

    // Settings table
    private static final String TABLE_SETTINGS = "settings";
    private static final String COL_SETTING_USER_EMAIL = "user_email";
    private static final String COL_SETTING_CURRENCY = "currency";
    private static final String COL_SETTING_BUDGET = "budget";
    private static final String COL_SETTING_THEME = "theme";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_NAME + " TEXT NOT NULL, "
                + COL_USER_EMAIL + " TEXT NOT NULL UNIQUE, "
                + COL_USER_PASSWORD_HASH + " TEXT NOT NULL, "
                + COL_USER_CURRENCY + " TEXT DEFAULT 'USD', "
                + COL_USER_CREATED_AT + " TEXT);";

        String createExpenses = "CREATE TABLE " + TABLE_EXPENSES + " ("
                + COL_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EXPENSE_USER_EMAIL + " TEXT NOT NULL, "
                + COL_EXPENSE_AMOUNT + " REAL NOT NULL, "
                + COL_EXPENSE_DATE + " TEXT NOT NULL, "
                + COL_EXPENSE_CATEGORY + " TEXT NOT NULL, "
                + COL_EXPENSE_DESCRIPTION + " TEXT, "
                + COL_EXPENSE_CREATED_AT + " TEXT);";

        String createSettings = "CREATE TABLE " + TABLE_SETTINGS + " ("
                + COL_SETTING_USER_EMAIL + " TEXT PRIMARY KEY, "
                + COL_SETTING_CURRENCY + " TEXT DEFAULT 'USD', "
                + COL_SETTING_BUDGET + " REAL DEFAULT 2000.0, "
                + COL_SETTING_THEME + " TEXT DEFAULT 'dark');";

        db.execSQL(createUsers);
        db.execSQL(createExpenses);
        db.execSQL(createSettings);

        db.execSQL("CREATE INDEX idx_expenses_user ON " + TABLE_EXPENSES + " (" + COL_EXPENSE_USER_EMAIL + ");");
        db.execSQL("CREATE INDEX idx_expenses_date ON " + TABLE_EXPENSES + " (" + COL_EXPENSE_DATE + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(password.hashCode());
        }
    }

    private static String nowIso() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        return sdf.format(new Date());
    }

    // --- User Operations ---

    public boolean userExists(String email) {
        if (email == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USER_EMAIL + " = ?", new String[]{email.trim().toLowerCase(Locale.ROOT)},
                null, null, null);
        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        return exists;
    }

    public User registerUser(String name, String email, String password, String currency) throws Exception {
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userExists(cleanEmail)) {
            throw new Exception("An account with this email already exists");
        }
        if (password == null || password.length() < 6) {
            throw new Exception("Password must be at least 6 characters");
        }
        if (name == null || name.trim().length() < 2) {
            throw new Exception("Name must be at least 2 characters");
        }
        String cleanCurrency = (currency != null && !currency.isEmpty()) ? currency.trim().toUpperCase() : "USD";

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USER_NAME, name.trim());
        cv.put(COL_USER_EMAIL, cleanEmail);
        cv.put(COL_USER_PASSWORD_HASH, hashPassword(password));
        cv.put(COL_USER_CURRENCY, cleanCurrency);
        String now = nowIso();
        cv.put(COL_USER_CREATED_AT, now);

        long id = db.insert(TABLE_USERS, null, cv);
        if (id == -1) {
            throw new Exception("Registration failed. Please try again.");
        }

        // Initialize user settings
        ContentValues settingCv = new ContentValues();
        settingCv.put(COL_SETTING_USER_EMAIL, cleanEmail);
        settingCv.put(COL_SETTING_CURRENCY, cleanCurrency);
        settingCv.put(COL_SETTING_BUDGET, 2000.0);
        settingCv.put(COL_SETTING_THEME, "dark");
        db.insertWithOnConflict(TABLE_SETTINGS, null, settingCv, SQLiteDatabase.CONFLICT_REPLACE);

        return new User(id, name.trim(), cleanEmail, cleanCurrency, now);
    }

    public User loginUser(String email, String password) {
        if (email == null || password == null) return null;
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        String passHash = hashPassword(password);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_USER_EMAIL + " = ? AND " + COL_USER_PASSWORD_HASH + " = ?",
                new String[]{cleanEmail, passHash}, null, null, null);

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME));
            String cur = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CURRENCY));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT));
            user = new User(id, name, cleanEmail, cur, createdAt);
            cursor.close();
        } else if (cursor != null) {
            cursor.close();
        }
        return user;
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_USER_EMAIL + " = ?", new String[]{cleanEmail},
                null, null, null);

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME));
            String cur = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CURRENCY));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CREATED_AT));
            user = new User(id, name, cleanEmail, cur, createdAt);
            cursor.close();
        } else if (cursor != null) {
            cursor.close();
        }
        return user;
    }

    // --- Settings Operations ---

    public AppSettings getSettings(String userEmail) {
        String cleanEmail = userEmail != null ? userEmail.trim().toLowerCase(Locale.ROOT) : "";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, null,
                COL_SETTING_USER_EMAIL + " = ?", new String[]{cleanEmail},
                null, null, null);

        AppSettings settings = new AppSettings();
        if (cursor != null && cursor.moveToFirst()) {
            settings.currency = cursor.getString(cursor.getColumnIndexOrThrow(COL_SETTING_CURRENCY));
            settings.budget = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SETTING_BUDGET));
            settings.theme = cursor.getString(cursor.getColumnIndexOrThrow(COL_SETTING_THEME));
            cursor.close();
        } else {
            if (cursor != null) cursor.close();
            // Default settings for user
            User u = getUserByEmail(cleanEmail);
            if (u != null && u.currency != null) {
                settings.currency = u.currency;
            }
            saveSettings(cleanEmail, settings);
        }
        return settings;
    }

    public void saveSettings(String userEmail, AppSettings settings) {
        if (userEmail == null || settings == null) return;
        String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(COL_SETTING_USER_EMAIL, cleanEmail);
        cv.put(COL_SETTING_CURRENCY, settings.currency);
        cv.put(COL_SETTING_BUDGET, settings.budget);
        cv.put(COL_SETTING_THEME, settings.theme);

        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Also update currency in user table
        ContentValues userCv = new ContentValues();
        userCv.put(COL_USER_CURRENCY, settings.currency);
        db.update(TABLE_USERS, userCv, COL_USER_EMAIL + " = ?", new String[]{cleanEmail});
    }

    // --- Expense Operations ---

    public List<Expense> getExpenses(String userEmail) {
        List<Expense> list = new ArrayList<>();
        if (userEmail == null) return list;
        String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_EXPENSE_USER_EMAIL + " = ?", new String[]{cleanEmail},
                null, null, COL_EXPENSE_DATE + " DESC, " + COL_EXPENSE_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXPENSE_ID));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_EXPENSE_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_DATE));
                String cat = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_CATEGORY));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_DESCRIPTION));
                String created = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_CREATED_AT));

                list.add(new Expense(id, cleanEmail, amount, date, cat, desc, created));
            }
            cursor.close();
        }
        return list;
    }

    public Expense getExpenseById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_EXPENSE_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);

        Expense e = null;
        if (cursor != null && cursor.moveToFirst()) {
            String email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_USER_EMAIL));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_EXPENSE_AMOUNT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_DATE));
            String cat = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_CATEGORY));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_DESCRIPTION));
            String created = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPENSE_CREATED_AT));
            e = new Expense(id, email, amount, date, cat, desc, created);
            cursor.close();
        } else if (cursor != null) {
            cursor.close();
        }
        return e;
    }

    public long addExpense(String userEmail, Expense expense) {
        if (userEmail == null || expense == null) return -1;
        String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_USER_EMAIL, cleanEmail);
        cv.put(COL_EXPENSE_AMOUNT, expense.amount);
        cv.put(COL_EXPENSE_DATE, expense.date);
        cv.put(COL_EXPENSE_CATEGORY, expense.category != null ? expense.category.toLowerCase() : "others");
        cv.put(COL_EXPENSE_DESCRIPTION, expense.description != null ? expense.description.trim() : "");
        String now = nowIso();
        cv.put(COL_EXPENSE_CREATED_AT, now);

        long id = db.insert(TABLE_EXPENSES, null, cv);
        expense.id = id;
        expense.userEmail = cleanEmail;
        expense.createdAt = now;
        return id;
    }

    public boolean updateExpense(Expense expense) {
        if (expense == null || expense.id <= 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EXPENSE_AMOUNT, expense.amount);
        cv.put(COL_EXPENSE_DATE, expense.date);
        cv.put(COL_EXPENSE_CATEGORY, expense.category != null ? expense.category.toLowerCase() : "others");
        cv.put(COL_EXPENSE_DESCRIPTION, expense.description != null ? expense.description.trim() : "");

        int rows = db.update(TABLE_EXPENSES, cv, COL_EXPENSE_ID + " = ?", new String[]{String.valueOf(expense.id)});
        return rows > 0;
    }

    public boolean deleteExpense(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public void importBackup(String userEmail, List<Expense> importedExpenses, AppSettings importedSettings) {
        if (userEmail == null) return;
        String cleanEmail = userEmail.trim().toLowerCase(Locale.ROOT);
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            if (importedSettings != null) {
                saveSettings(cleanEmail, importedSettings);
            }

            if (importedExpenses != null) {
                for (Expense e : importedExpenses) {
                    if (e != null && e.amount > 0 && e.date != null && !e.date.isEmpty()) {
                        ContentValues cv = new ContentValues();
                        cv.put(COL_EXPENSE_USER_EMAIL, cleanEmail);
                        cv.put(COL_EXPENSE_AMOUNT, e.amount);
                        cv.put(COL_EXPENSE_DATE, e.date);
                        cv.put(COL_EXPENSE_CATEGORY, e.category != null ? e.category.toLowerCase() : "others");
                        cv.put(COL_EXPENSE_DESCRIPTION, e.description != null ? e.description : "");
                        cv.put(COL_EXPENSE_CREATED_AT, e.createdAt != null ? e.createdAt : nowIso());
                        db.insert(TABLE_EXPENSES, null, cv);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}

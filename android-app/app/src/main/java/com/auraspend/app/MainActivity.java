package com.auraspend.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;

import com.auraspend.app.data.DatabaseHelper;
import com.auraspend.app.data.SessionManager;
import com.auraspend.app.model.AppSettings;
import com.auraspend.app.model.Category;
import com.auraspend.app.model.Currency;
import com.auraspend.app.model.Expense;
import com.auraspend.app.model.User;
import com.auraspend.app.ui.CircularProgressView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private User currentUser;
    private AppSettings settings = new AppSettings();
    private List<Expense> expenses = new ArrayList<>();

    // Views
    private View mainRoot;
    private View authView;
    private View appView;
    private View topBar;
    private View topBarDivider;
    private TextView topGreeting;
    private TextView topProfileBtn;
    private FrameLayout contentFrame;
    private LinearLayout bottomNavBar;

    // Bottom nav items
    private TextView navDashboardIcon, navDashboardLabel;
    private TextView navHistoryIcon, navHistoryLabel;
    private TextView navReportsIcon, navReportsLabel;
    private TextView navSettingsIcon, navSettingsLabel;

    // Auth components
    private TextView authTabLogin, authTabSignup;
    private View loginForm, signupForm;
    private EditText loginEmail, loginPassword;
    private EditText signupName, signupEmail, signupPassword;
    private Spinner signupCurrencySpinner;
    private AppCompatButton loginSubmitBtn, signupSubmitBtn;

    // State
    private String activeView = "dashboard"; // dashboard, history, reports, settings
    private String activeFilter = "all";
    private String reportPeriod = "weekly"; // weekly, monthly, annual, all
    private String historySearchQuery = "";
    private boolean isLightTheme = false;

    private ActivityResultLauncher<String> jsonImportLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        bindAuth();
        bindNavigation();
        initImportPicker();

        // Check active session
        if (sessionManager.isLoggedIn()) {
            String email = sessionManager.getCurrentUserEmail();
            currentUser = dbHelper.getUserByEmail(email);
        }

        if (currentUser != null) {
            enterApp();
        } else {
            showAuth();
        }
    }

    private void initViews() {
        mainRoot = findViewById(R.id.mainRoot);
        authView = findViewById(R.id.authView);
        appView = findViewById(R.id.appView);
        topBar = findViewById(R.id.topBar);
        topBarDivider = findViewById(R.id.topBarDivider);
        topGreeting = findViewById(R.id.topGreeting);
        topProfileBtn = findViewById(R.id.topProfileBtn);
        contentFrame = findViewById(R.id.contentFrame);
        bottomNavBar = findViewById(R.id.bottomNavBar);

        navDashboardIcon = findViewById(R.id.navDashboardIcon);
        navDashboardLabel = findViewById(R.id.navDashboardLabel);
        navHistoryIcon = findViewById(R.id.navHistoryIcon);
        navHistoryLabel = findViewById(R.id.navHistoryLabel);
        navReportsIcon = findViewById(R.id.navReportsIcon);
        navReportsLabel = findViewById(R.id.navReportsLabel);
        navSettingsIcon = findViewById(R.id.navSettingsIcon);
        navSettingsLabel = findViewById(R.id.navSettingsLabel);

        authTabLogin = findViewById(R.id.authTabLogin);
        authTabSignup = findViewById(R.id.authTabSignup);
        loginForm = findViewById(R.id.loginForm);
        signupForm = findViewById(R.id.signupForm);
        loginEmail = findViewById(R.id.loginEmail);
        loginPassword = findViewById(R.id.loginPassword);
        signupName = findViewById(R.id.signupName);
        signupEmail = findViewById(R.id.signupEmail);
        signupPassword = findViewById(R.id.signupPassword);
        signupCurrencySpinner = findViewById(R.id.signupCurrencySpinner);
        loginSubmitBtn = findViewById(R.id.loginSubmitBtn);
        signupSubmitBtn = findViewById(R.id.signupSubmitBtn);

        populateCurrencySpinner();
    }

    private void populateCurrencySpinner() {
        List<String> currencyList = new ArrayList<>();
        for (String code : Currency.CODES) {
            currencyList.add(Currency.get(code).getDisplayName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencyList);
        signupCurrencySpinner.setAdapter(adapter);
    }

    private void bindAuth() {
        authTabLogin.setOnClickListener(v -> switchAuthTab(true));
        authTabSignup.setOnClickListener(v -> switchAuthTab(false));

        loginSubmitBtn.setOnClickListener(v -> handleLogin());
        signupSubmitBtn.setOnClickListener(v -> handleSignup());
    }

    private void switchAuthTab(boolean isLogin) {
        if (isLogin) {
            authTabLogin.setBackgroundResource(R.drawable.bg_chip_selected);
            authTabLogin.setTextColor(Color.WHITE);
            authTabLogin.setTypeface(null, Typeface.BOLD);

            authTabSignup.setBackground(null);
            authTabSignup.setTextColor(isLightTheme ? Color.parseColor("#786B81") : Color.parseColor("#A99DB8"));
            authTabSignup.setTypeface(null, Typeface.NORMAL);

            loginForm.setVisibility(View.VISIBLE);
            signupForm.setVisibility(View.GONE);
        } else {
            authTabSignup.setBackgroundResource(R.drawable.bg_chip_selected);
            authTabSignup.setTextColor(Color.WHITE);
            authTabSignup.setTypeface(null, Typeface.BOLD);

            authTabLogin.setBackground(null);
            authTabLogin.setTextColor(isLightTheme ? Color.parseColor("#786B81") : Color.parseColor("#A99DB8"));
            authTabLogin.setTypeface(null, Typeface.NORMAL);

            signupForm.setVisibility(View.VISIBLE);
            loginForm.setVisibility(View.GONE);
        }
    }

    private void handleLogin() {
        String email = loginEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String pass = loginPassword.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            toast("Please enter your email and password");
            return;
        }

        User user = dbHelper.loginUser(email, pass);
        if (user == null) {
            toast("Invalid email or password");
            return;
        }

        currentUser = user;
        sessionManager.saveSession(currentUser.email);
        loginEmail.setText("");
        loginPassword.setText("");
        enterApp();
    }

    private void handleSignup() {
        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        String pass = signupPassword.getText().toString();
        int selectedCurrencyIdx = signupCurrencySpinner.getSelectedItemPosition();
        String currency = Currency.CODES[Math.max(0, selectedCurrencyIdx)];

        if (name.length() < 2) {
            toast("Name must be at least 2 characters");
            return;
        }
        if (email.isEmpty() || !email.contains("@")) {
            toast("Enter a valid email address");
            return;
        }
        if (pass.length() < 6) {
            toast("Password must be at least 6 characters");
            return;
        }

        try {
            currentUser = dbHelper.registerUser(name, email, pass, currency);
            sessionManager.saveSession(currentUser.email);
            signupName.setText("");
            signupEmail.setText("");
            signupPassword.setText("");
            enterApp();
            toast("Account created successfully");
        } catch (Exception e) {
            toast(e.getMessage() != null ? e.getMessage() : "Registration failed");
        }
    }

    private void bindNavigation() {
        findViewById(R.id.navDashboard).setOnClickListener(v -> switchView("dashboard"));
        findViewById(R.id.navHistory).setOnClickListener(v -> switchView("history"));
        findViewById(R.id.navAdd).setOnClickListener(v -> openAddExpenseSheet());
        findViewById(R.id.navReports).setOnClickListener(v -> switchView("reports"));
        findViewById(R.id.navSettings).setOnClickListener(v -> switchView("settings"));
        topProfileBtn.setOnClickListener(v -> switchView("settings"));
    }

    private void initImportPicker() {
        jsonImportLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                importJsonBackup(uri);
            }
        });
    }

    private void showAuth() {
        authView.setVisibility(View.VISIBLE);
        appView.setVisibility(View.GONE);
        switchAuthTab(true);
    }

    private void enterApp() {
        loadUserData();
        authView.setVisibility(View.GONE);
        appView.setVisibility(View.VISIBLE);

        String firstName = currentUser.name.split(" ")[0];
        topGreeting.setText("Welcome back, " + firstName + ".");
        topProfileBtn.setText(currentUser.name.substring(0, 1).toUpperCase(Locale.ROOT));

        applyTheme();
        switchView("dashboard");
    }

    private void logout() {
        sessionManager.clearSession();
        currentUser = null;
        expenses.clear();
        showAuth();
        toast("Logged out");
    }

    private void loadUserData() {
        if (currentUser == null) return;
        settings = dbHelper.getSettings(currentUser.email);
        isLightTheme = "light".equalsIgnoreCase(settings.theme);
        expenses = dbHelper.getExpenses(currentUser.email);
    }

    private void switchView(String targetView) {
        activeView = targetView;
        updateNavIcons();
        renderActiveView();
    }

    private void updateNavIcons() {
        int activeColor = Color.parseColor("#D7B9FF");
        int inactiveColor = isLightTheme ? Color.parseColor("#8E809B") : Color.parseColor("#776B84");

        navDashboardIcon.setTextColor("dashboard".equals(activeView) ? activeColor : inactiveColor);
        navDashboardLabel.setTextColor("dashboard".equals(activeView) ? activeColor : inactiveColor);
        navDashboardLabel.setTypeface(null, "dashboard".equals(activeView) ? Typeface.BOLD : Typeface.NORMAL);

        navHistoryIcon.setTextColor("history".equals(activeView) ? activeColor : inactiveColor);
        navHistoryLabel.setTextColor("history".equals(activeView) ? activeColor : inactiveColor);
        navHistoryLabel.setTypeface(null, "history".equals(activeView) ? Typeface.BOLD : Typeface.NORMAL);

        navReportsIcon.setTextColor("reports".equals(activeView) ? activeColor : inactiveColor);
        navReportsLabel.setTextColor("reports".equals(activeView) ? activeColor : inactiveColor);
        navReportsLabel.setTypeface(null, "reports".equals(activeView) ? Typeface.BOLD : Typeface.NORMAL);

        navSettingsIcon.setTextColor("settings".equals(activeView) ? activeColor : inactiveColor);
        navSettingsLabel.setTextColor("settings".equals(activeView) ? activeColor : inactiveColor);
        navSettingsLabel.setTypeface(null, "settings".equals(activeView) ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void applyTheme() {
        int bgColor = isLightTheme ? Color.parseColor("#F5F1FB") : Color.parseColor("#090414");
        int navBgColor = isLightTheme ? Color.parseColor("#FFFFFF") : Color.parseColor("#0B0513");
        int dividerColor = isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#24163B");

        mainRoot.setBackgroundColor(bgColor);
        topBar.setBackgroundColor(bgColor);
        topBarDivider.setBackgroundColor(dividerColor);
        bottomNavBar.setBackgroundColor(navBgColor);
        contentFrame.setBackgroundColor(bgColor);

        topProfileBtn.setBackgroundResource(isLightTheme ? R.drawable.bg_category_item : R.drawable.bg_input);
        updateNavIcons();
    }

    private int colorBg() {
        return isLightTheme ? Color.parseColor("#F5F1FB") : Color.parseColor("#090414");
    }

    private int colorCard() {
        return isLightTheme ? Color.parseColor("#FFFFFF") : Color.parseColor("#130B22");
    }

    private int colorCardSurface() {
        return isLightTheme ? Color.parseColor("#F0E9F8") : Color.parseColor("#1B1030");
    }

    private int colorText() {
        return isLightTheme ? Color.parseColor("#20152A") : Color.parseColor("#FFFAFF");
    }

    private int colorMuted() {
        return isLightTheme ? Color.parseColor("#786B81") : Color.parseColor("#A99DB8");
    }

    private int colorPurple() {
        return Color.parseColor("#A56CFF");
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ==================== VIEW RENDERING ====================

    private void renderActiveView() {
        contentFrame.removeAllViews();

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scroll.setFillViewport(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(container);

        contentFrame.addView(scroll);

        if ("dashboard".equals(activeView)) {
            renderDashboard(container);
        } else if ("history".equals(activeView)) {
            renderHistory(container);
        } else if ("reports".equals(activeView)) {
            renderReports(container);
        } else if ("settings".equals(activeView)) {
            renderSettings(container);
        }
    }

    // -------------------- 1. DASHBOARD --------------------
    private void renderDashboard(LinearLayout container) {
        // Welcome row
        LinearLayout welcomeRow = new LinearLayout(this);
        welcomeRow.setOrientation(LinearLayout.HORIZONTAL);
        welcomeRow.setGravity(Gravity.CENTER_VERTICAL);
        welcomeRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView eyebrow = text("OVERVIEW", 10, Color.parseColor("#A990BF"), true);
        eyebrow.setLetterSpacing(0.15f);
        titleCol.addView(eyebrow);

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greetingWord = hour < 12 ? "Good morning 👋" : hour < 17 ? "Good afternoon 👋" : "Good evening 👋";
        titleCol.addView(text(greetingWord, 23, colorText(), true));
        welcomeRow.addView(titleCol);

        // Quick report button
        TextView quickReport = new TextView(this);
        quickReport.setText("▤");
        quickReport.setTextSize(18);
        quickReport.setTextColor(Color.parseColor("#DCC5F2"));
        quickReport.setGravity(Gravity.CENTER);
        quickReport.setBackground(roundDrawable(colorCardSurface(), 14));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        quickReport.setLayoutParams(btnLp);
        quickReport.setOnClickListener(v -> switchView("reports"));
        welcomeRow.addView(quickReport);

        container.addView(welcomeRow);

        // Space
        space(container, dp(16));

        // Calculate this month's stats
        String currentMonth = LocalDate.now().toString().substring(0, 7);
        List<Expense> monthItems = new ArrayList<>();
        double monthTotal = 0.0;
        Map<String, Double> categoryTotals = new LinkedHashMap<>();

        for (Expense e : expenses) {
            if (e.date != null && e.date.startsWith(currentMonth)) {
                monthItems.add(e);
                monthTotal += e.amount;
                categoryTotals.put(e.category, categoryTotals.getOrDefault(e.category, 0.0) + e.amount);
            }
        }

        double budget = Math.max(0, settings.budget);
        int percent = budget > 0 ? (int) Math.min(100, Math.round(monthTotal / budget * 100.0)) : 0;
        int currentDay = Math.max(1, LocalDate.now().getDayOfMonth());
        double dailyAvg = monthTotal / currentDay;

        // Find top category
        String topCat = "—";
        double maxCatVal = 0.0;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > maxCatVal) {
                maxCatVal = entry.getValue();
                topCat = Category.getLabel(entry.getKey());
            }
        }

        // Hero Card (Monthly total & Budget Ring)
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.HORIZONTAL);
        heroCard.setGravity(Gravity.CENTER_VERTICAL);
        heroCard.setPadding(dp(22), dp(22), dp(22), dp(22));
        heroCard.setBackground(isLightTheme ? roundDrawable(Color.parseColor("#FFFFFF"), 24) : getDrawable(R.drawable.bg_hero_card));
        heroCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout heroInfo = new LinearLayout(this);
        heroInfo.setOrientation(LinearLayout.VERTICAL);
        heroInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        heroInfo.addView(text("This month", 12, colorMuted()));
        heroInfo.addView(text(Currency.format(monthTotal, settings.currency), 28, colorText(), true));

        String compareStr;
        if (budget <= 0) {
            compareStr = "No budget set";
        } else if (monthTotal <= budget) {
            compareStr = Currency.format(budget - monthTotal, settings.currency) + " remaining";
        } else {
            compareStr = Currency.format(monthTotal - budget, settings.currency) + " over budget";
        }
        heroInfo.addView(text(compareStr, 12, Color.parseColor("#BDA9CA")));

        heroCard.addView(heroInfo);

        // Circular budget ring
        CircularProgressView ring = new CircularProgressView(this);
        ring.setTheme(isLightTheme);
        ring.setProgress(percent);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(90), dp(90));
        ring.setLayoutParams(ringLp);
        heroCard.addView(ring);

        container.addView(heroCard);

        space(container, dp(12));

        // Stat Grid: Transactions | Daily average | Top category
        LinearLayout statGrid = new LinearLayout(this);
        statGrid.setOrientation(LinearLayout.HORIZONTAL);
        statGrid.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statGrid.addView(createMiniCard("Transactions", String.valueOf(monthItems.size())));
        spaceHorizontal(statGrid, dp(8));
        statGrid.addView(createMiniCard("Daily avg", Currency.formatNoDecimals(dailyAvg, settings.currency)));
        spaceHorizontal(statGrid, dp(8));
        statGrid.addView(createMiniCard("Top category", topCat));

        container.addView(statGrid);

        space(container, dp(22));

        // Section Heading: Category breakdown + Clear filter button
        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        sectionHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout secTitles = new LinearLayout(this);
        secTitles.setOrientation(LinearLayout.VERTICAL);
        secTitles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView secEyebrow = text("SPENDING", 10, Color.parseColor("#A990BF"), true);
        secEyebrow.setLetterSpacing(0.15f);
        secTitles.addView(secEyebrow);
        secTitles.addView(text("Category breakdown", 17, colorText(), true));
        sectionHeader.addView(secTitles);

        if (!"all".equals(activeFilter)) {
            TextView clearBtn = text("Clear", 12, colorPurple(), true);
            clearBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
            clearBtn.setOnClickListener(v -> {
                activeFilter = "all";
                renderActiveView();
            });
            sectionHeader.addView(clearBtn);
        }
        container.addView(sectionHeader);

        space(container, dp(10));

        // Category Filter Chips
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipScroll.addView(chipRow);

        chipRow.addView(createFilterChip("all", "All"));
        for (String catKey : Category.ALL_KEYS) {
            chipRow.addView(createFilterChip(catKey, Category.getDisplay(catKey)));
        }
        container.addView(chipScroll);

        space(container, dp(14));

        // Filter items
        Map<String, Double> displayedTotals = new LinkedHashMap<>();
        if ("all".equals(activeFilter)) {
            displayedTotals.putAll(categoryTotals);
        } else if (categoryTotals.containsKey(activeFilter)) {
            displayedTotals.put(activeFilter, categoryTotals.get(activeFilter));
        }

        if (displayedTotals.isEmpty()) {
            container.addView(createEmptyState("◌", "No expenses for this period", "Add your first expense to see insights here."));
        } else {
            // Sort by amount descending
            List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(displayedTotals.entrySet());
            sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            double maxVal = Math.max(1.0, maxCatVal);

            for (Map.Entry<String, Double> entry : sortedEntries) {
                container.addView(createBreakdownItem(entry.getKey(), entry.getValue(), maxVal));
                space(container, dp(8));
            }
        }
    }

    private View createMiniCard(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(roundDrawable(colorCard(), 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        card.setLayoutParams(lp);

        card.addView(text(label, 10, colorMuted()));
        card.addView(text(value, 13, colorText(), true));
        return card;
    }

    private View createFilterChip(String key, String title) {
        boolean selected = key.equals(activeFilter);
        TextView chip = new TextView(this);
        chip.setText(title);
        chip.setTextSize(11);
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(selected ? Color.WHITE : colorMuted());
        chip.setBackground(roundDrawable(selected ? Color.parseColor("#361A5C") : colorCardSurface(), 999,
                selected ? Color.parseColor("#B37DFF") : (isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#2D2145"))));
        chip.setPadding(dp(14), dp(8), dp(14), dp(8));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);

        chip.setOnClickListener(v -> {
            activeFilter = key;
            renderActiveView();
        });
        return chip;
    }

    private View createBreakdownItem(String categoryKey, double amount, double maxVal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        row.setBackground(roundDrawable(colorCard(), 16));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Icon badge
        TextView icon = new TextView(this);
        icon.setText(Category.getEmoji(categoryKey));
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundDrawable(colorCardSurface(), 12));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconLp.setMarginEnd(dp(12));
        icon.setLayoutParams(iconLp);
        row.addView(icon);

        // Right details
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView name = text(Category.getLabel(categoryKey), 12, colorText(), true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topRow.addView(name);

        topRow.addView(text(Currency.format(amount, settings.currency), 12, colorPurple(), true));
        main.addView(topRow);

        space(main, dp(6));

        // Horizontal Progress bar
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        int progress = (int) Math.round((amount / maxVal) * 100.0);
        bar.setProgress(progress);
        bar.setProgressTintList(ColorStateList.valueOf(colorPurple()));
        main.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(5)));

        row.addView(main);
        return row;
    }

    // -------------------- 2. HISTORY --------------------
    private void renderHistory(LinearLayout container) {
        // Title row
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView eyebrow = text("ACTIVITY", 10, Color.parseColor("#A990BF"), true);
        eyebrow.setLetterSpacing(0.15f);
        titleCol.addView(eyebrow);
        titleCol.addView(text("History", 24, colorText(), true));
        header.addView(titleCol);

        container.addView(header);
        space(container, dp(14));

        // Search bar
        EditText searchBox = new EditText(this);
        searchBox.setHint("Search description or category...");
        searchBox.setSingleLine(true);
        searchBox.setText(historySearchQuery);
        searchBox.setTextColor(colorText());
        searchBox.setHintTextColor(isLightTheme ? Color.parseColor("#9C90A8") : Color.parseColor("#6D5D7E"));
        searchBox.setTextSize(13);
        searchBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        searchBox.setBackground(roundDrawable(colorCard(), 14, isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#2D2145")));
        searchBox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        container.addView(searchBox);

        space(container, dp(16));

        // History items container
        LinearLayout itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        itemsContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(itemsContainer);

        renderHistoryList(itemsContainer, historySearchQuery);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                historySearchQuery = s.toString();
                renderHistoryList(itemsContainer, historySearchQuery);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void renderHistoryList(LinearLayout itemsContainer, String query) {
        itemsContainer.removeAllViews();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";

        List<Expense> filtered = new ArrayList<>();
        for (Expense e : expenses) {
            String combined = (e.description != null ? e.description : "") + " " + Category.getLabel(e.category);
            if (q.isEmpty() || combined.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            itemsContainer.addView(createEmptyState("☷", "No transactions found", "Try another search or add an expense."));
            return;
        }

        // Group by Date
        Map<String, List<Expense>> grouped = new LinkedHashMap<>();
        for (Expense e : filtered) {
            String d = e.date != null ? e.date : "";
            if (!grouped.containsKey(d)) {
                grouped.put(d, new ArrayList<>());
            }
            grouped.get(d).add(e);
        }

        for (Map.Entry<String, List<Expense>> group : grouped.entrySet()) {
            String dateStr = group.getKey();
            List<Expense> dayItems = group.getValue();
            double dayTotal = 0;
            for (Expense e : dayItems) dayTotal += e.amount;

            // Date Header
            LinearLayout dateHeader = new LinearLayout(this);
            dateHeader.setOrientation(LinearLayout.HORIZONTAL);
            dateHeader.setGravity(Gravity.CENTER_VERTICAL);
            dateHeader.setPadding(dp(4), dp(10), dp(4), dp(6));
            dateHeader.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView dateText = text(formatDateHeading(dateStr), 11, colorMuted(), true);
            dateText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            dateHeader.addView(dateText);

            dateHeader.addView(text(Currency.format(dayTotal, settings.currency), 12, colorText(), true));
            itemsContainer.addView(dateHeader);

            // Day Cards
            for (Expense e : dayItems) {
                itemsContainer.addView(createHistoryCard(e));
                space(itemsContainer, dp(6));
            }
        }
    }

    private View createHistoryCard(Expense e) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(11), dp(12), dp(11));
        card.setBackground(roundDrawable(colorCard(), 16));
        card.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Category icon
        TextView icon = new TextView(this);
        icon.setText(Category.getEmoji(e.category));
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundDrawable(colorCardSurface(), 12));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconLp.setMarginEnd(dp(12));
        icon.setLayoutParams(iconLp);
        card.addView(icon);

        // Main text
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        String title = (e.description != null && !e.description.trim().isEmpty()) ? e.description : Category.getLabel(e.category);
        main.addView(text(title, 13, colorText(), true));
        main.addView(text(Category.getLabel(e.category), 10, colorMuted()));
        card.addView(main);

        // Amount: - $XX.XX
        TextView amountView = text("-" + Currency.format(e.amount, settings.currency), 13, Color.parseColor("#FFB3C6"), true);
        amountView.setGravity(Gravity.END);
        card.addView(amountView);

        card.setOnClickListener(v -> openEditExpenseSheet(e));
        return card;
    }

    // -------------------- 3. REPORTS --------------------
    private void renderReports(LinearLayout container) {
        // Title row + PDF button
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView eyebrow = text("INSIGHTS", 10, Color.parseColor("#A990BF"), true);
        eyebrow.setLetterSpacing(0.15f);
        titleCol.addView(eyebrow);
        titleCol.addView(text("Reports", 24, colorText(), true));
        header.addView(titleCol);

        // PDF Button
        AppCompatButton pdfBtn = new AppCompatButton(this);
        pdfBtn.setText("PDF ↓");
        pdfBtn.setTextSize(12);
        pdfBtn.setTypeface(null, Typeface.BOLD);
        pdfBtn.setTextColor(Color.WHITE);
        pdfBtn.setBackgroundResource(R.drawable.bg_primary_button);
        pdfBtn.setPadding(dp(14), dp(6), dp(14), dp(6));
        pdfBtn.setOnClickListener(v -> exportPdfReport(reportPeriod));
        header.addView(pdfBtn);

        container.addView(header);
        space(container, dp(14));

        // Report period tabs: Weekly | Monthly | Annual | All time
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        String[] periods = {"weekly", "monthly", "annual", "all"};
        String[] periodLabels = {"Weekly", "Monthly", "Annual", "All time"};

        for (int i = 0; i < periods.length; i++) {
            final String p = periods[i];
            boolean isSel = p.equals(reportPeriod);

            TextView tab = new TextView(this);
            tab.setText(periodLabels[i]);
            tab.setTextSize(10);
            tab.setGravity(Gravity.CENTER);
            tab.setTextColor(isSel ? Color.WHITE : colorMuted());
            tab.setTypeface(null, isSel ? Typeface.BOLD : Typeface.NORMAL);
            tab.setBackground(roundDrawable(isSel ? Color.parseColor("#361A5C") : colorCard(), 11,
                    isSel ? Color.parseColor("#8E5CD0") : (isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#2D2145"))));
            tab.setPadding(dp(4), dp(9), dp(4), dp(9));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(dp(6));
            tab.setLayoutParams(lp);

            tab.setOnClickListener(v -> {
                reportPeriod = p;
                renderActiveView();
            });
            tabs.addView(tab);
        }
        container.addView(tabs);

        space(container, dp(14));

        // Filter items for selected period
        List<Expense> periodItems = getReportExpenses(reportPeriod);
        double totalSpending = 0;
        Map<String, Double> categoryTotals = new LinkedHashMap<>();

        for (Expense e : periodItems) {
            totalSpending += e.amount;
            categoryTotals.put(e.category, categoryTotals.getOrDefault(e.category, 0.0) + e.amount);
        }

        // Summary Card
        LinearLayout sumCard = new LinearLayout(this);
        sumCard.setOrientation(LinearLayout.VERTICAL);
        sumCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        sumCard.setBackground(isLightTheme ? roundDrawable(Color.parseColor("#FFFFFF"), 20) : getDrawable(R.drawable.bg_hero_card));
        sumCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        sumCard.addView(text(reportPeriodLabel(reportPeriod), 11, colorMuted()));
        sumCard.addView(text(Currency.format(totalSpending, settings.currency), 28, colorText(), true));
        sumCard.addView(text(periodItems.size() + " transaction" + (periodItems.size() == 1 ? "" : "s"), 11, colorMuted()));
        container.addView(sumCard);

        space(container, dp(22));

        // Breakdown: Top categories
        TextView topCatEyebrow = text("BREAKDOWN", 10, Color.parseColor("#A990BF"), true);
        topCatEyebrow.setLetterSpacing(0.15f);
        container.addView(topCatEyebrow);
        container.addView(text("Top categories", 17, colorText(), true));

        space(container, dp(10));

        if (categoryTotals.isEmpty()) {
            container.addView(text("No transactions in this period.", 12, colorMuted()));
        } else {
            List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(categoryTotals.entrySet());
            sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            double maxCat = sortedEntries.get(0).getValue();

            for (Map.Entry<String, Double> entry : sortedEntries) {
                container.addView(createBreakdownItem(entry.getKey(), entry.getValue(), maxCat));
                space(container, dp(8));
            }
        }

        space(container, dp(22));

        // Detail: Transactions list
        TextView detailEyebrow = text("DETAIL", 10, Color.parseColor("#A990BF"), true);
        detailEyebrow.setLetterSpacing(0.15f);
        container.addView(detailEyebrow);
        container.addView(text("Transactions", 17, colorText(), true));

        space(container, dp(10));

        if (periodItems.isEmpty()) {
            container.addView(text("Nothing to report.", 12, colorMuted()));
        } else {
            for (Expense e : periodItems) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4), dp(10), dp(4), dp(10));
                row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                LinearLayout left = new LinearLayout(this);
                left.setOrientation(LinearLayout.VERTICAL);
                left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                left.addView(text(formatDateShort(e.date) + " · " + Category.getLabel(e.category), 10, colorMuted()));
                if (e.description != null && !e.description.trim().isEmpty()) {
                    left.addView(text(e.description, 12, colorText(), true));
                }
                row.addView(left);

                row.addView(text(Currency.format(e.amount, settings.currency), 13, colorPurple(), true));

                // Border separator at bottom
                container.addView(row);
                View div = new View(this);
                div.setBackgroundColor(isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#24163B"));
                container.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            }
        }
    }

    private List<Expense> getReportExpenses(String period) {
        if ("all".equals(period)) {
            return new ArrayList<>(expenses);
        }

        List<Expense> result = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (Expense e : expenses) {
            try {
                LocalDate d = LocalDate.parse(e.date);
                boolean matches = false;
                if ("weekly".equals(period)) {
                    LocalDate startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    matches = !d.isBefore(startOfWeek) && !d.isAfter(now);
                } else if ("monthly".equals(period)) {
                    matches = (d.getYear() == now.getYear() && d.getMonthValue() == now.getMonthValue());
                } else if ("annual".equals(period)) {
                    matches = (d.getYear() == now.getYear());
                }
                if (matches) {
                    result.add(e);
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    private String reportPeriodLabel(String period) {
        switch (period) {
            case "weekly": return "This week";
            case "monthly": return "This month";
            case "annual": return "This year";
            default: return "All time";
        }
    }

    // -------------------- 4. SETTINGS --------------------
    private void renderSettings(LinearLayout container) {
        // Title
        TextView eyebrow = text("ACCOUNT", 10, Color.parseColor("#A990BF"), true);
        eyebrow.setLetterSpacing(0.15f);
        container.addView(eyebrow);
        container.addView(text("Settings", 24, colorText(), true));

        space(container, dp(16));

        // Account Profile Card
        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.VERTICAL);
        profileCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        profileCard.setBackground(roundDrawable(colorCard(), 20));
        profileCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout userRow = new LinearLayout(this);
        userRow.setOrientation(LinearLayout.HORIZONTAL);
        userRow.setGravity(Gravity.CENTER_VERTICAL);
        userRow.setPadding(0, 0, 0, dp(14));

        TextView avatar = new TextView(this);
        avatar.setText(currentUser != null ? currentUser.name.substring(0, 1).toUpperCase(Locale.ROOT) : "A");
        avatar.setTextSize(20);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setTextColor(Color.WHITE);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_fab);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        avLp.setMarginEnd(dp(14));
        avatar.setLayoutParams(avLp);
        userRow.addView(avatar);

        LinearLayout userInfo = new LinearLayout(this);
        userInfo.setOrientation(LinearLayout.VERTICAL);
        userInfo.addView(text(currentUser != null ? currentUser.name : "User", 15, colorText(), true));
        userInfo.addView(text(currentUser != null ? currentUser.email : "", 11, colorMuted()));
        userRow.addView(userInfo);

        profileCard.addView(userRow);

        View div1 = new View(this);
        div1.setBackgroundColor(isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#24163B"));
        profileCard.addView(div1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // Setting 1: Currency
        profileCard.addView(createSettingRow("Currency", "Used for all amounts and reports",
                Currency.get(settings.currency).code + " (" + Currency.get(settings.currency).symbol + ")", v -> showCurrencyPicker()));

        View div2 = new View(this);
        div2.setBackgroundColor(isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#24163B"));
        profileCard.addView(div2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // Setting 2: Monthly Budget
        profileCard.addView(createSettingRow("Monthly budget", "Used for your dashboard budget ring",
                Currency.format(settings.budget, settings.currency), v -> showBudgetDialog()));

        View div3 = new View(this);
        div3.setBackgroundColor(isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#24163B"));
        profileCard.addView(div3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        // Setting 3: Theme Toggle
        profileCard.addView(createSettingRow("Appearance", "Switch between dark and light appearance",
                isLightTheme ? "Light" : "Dark", v -> toggleTheme()));

        container.addView(profileCard);

        space(container, dp(16));

        // Data & Backup Card
        LinearLayout dataCard = new LinearLayout(this);
        dataCard.setOrientation(LinearLayout.VERTICAL);
        dataCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        dataCard.setBackground(roundDrawable(colorCard(), 20));
        dataCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView dataEyebrow = text("DATA", 10, Color.parseColor("#A990BF"), true);
        dataEyebrow.setLetterSpacing(0.15f);
        dataCard.addView(dataEyebrow);
        dataCard.addView(text("Backup & reports", 16, colorText(), true));

        space(dataCard, dp(12));

        dataCard.addView(createActionBtn("Export data (.json)", v -> exportJsonBackup()));
        space(dataCard, dp(8));
        dataCard.addView(createActionBtn("Import data (.json)", v -> jsonImportLauncher.launch("application/json")));
        space(dataCard, dp(8));
        dataCard.addView(createActionBtn("Generate all-time PDF", v -> exportPdfReport("all")));

        container.addView(dataCard);

        space(container, dp(16));

        // Logout Button
        AppCompatButton logoutBtn = new AppCompatButton(this);
        logoutBtn.setText("Log out");
        logoutBtn.setTextSize(14);
        logoutBtn.setTypeface(null, Typeface.BOLD);
        logoutBtn.setTextColor(Color.parseColor("#FF91AD"));
        logoutBtn.setBackground(roundDrawable(Color.parseColor("#261421"), 14, Color.parseColor("#5C1D2D")));
        logoutBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        logoutBtn.setOnClickListener(v -> logout());
        container.addView(logoutBtn);
    }

    private View createSettingRow(String title, String desc, String value, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(onClick);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        left.addView(text(title, 13, colorText(), true));
        left.addView(text(desc, 10, colorMuted()));
        row.addView(left);

        TextView valView = text(value + "  ›", 12, colorPurple(), true);
        row.addView(valView);
        return row;
    }

    private View createActionBtn(String title, View.OnClickListener onClick) {
        AppCompatButton btn = new AppCompatButton(this);
        btn.setText(title);
        btn.setTextSize(13);
        btn.setTextColor(colorText());
        btn.setBackground(roundDrawable(colorCardSurface(), 14, isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#2D2145")));
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
        btn.setOnClickListener(onClick);
        return btn;
    }

    private void showCurrencyPicker() {
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < Currency.CODES.length; i++) {
            names.add(Currency.get(Currency.CODES[i]).getDisplayName());
            if (Currency.CODES[i].equalsIgnoreCase(settings.currency)) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Currency")
                .setSingleChoiceItems(names.toArray(new String[0]), selectedIndex, (dialog, which) -> {
                    settings.currency = Currency.CODES[which];
                    dbHelper.saveSettings(currentUser.email, settings);
                    dialog.dismiss();
                    renderActiveView();
                    toast("Currency changed to " + settings.currency);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBudgetDialog() {
        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(settings.budget));
        input.setPadding(dp(16), dp(12), dp(16), dp(12));

        new AlertDialog.Builder(this)
                .setTitle("Monthly Budget")
                .setMessage("Enter your monthly budget amount:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double val = Double.parseDouble(input.getText().toString().trim());
                        if (val >= 0) {
                            settings.budget = val;
                            dbHelper.saveSettings(currentUser.email, settings);
                            renderActiveView();
                            toast("Budget updated");
                        }
                    } catch (Exception e) {
                        toast("Please enter a valid amount");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleTheme() {
        isLightTheme = !isLightTheme;
        settings.theme = isLightTheme ? "light" : "dark";
        dbHelper.saveSettings(currentUser.email, settings);
        applyTheme();
        renderActiveView();
        toast("Theme switched to " + (isLightTheme ? "Light" : "Dark"));
    }

    // ==================== ADD / EDIT BOTTOM SHEETS ====================

    private void openAddExpenseSheet() {
        if (currentUser == null) {
            toast("Please log in first");
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_add_expense, null);
        dialog.setContentView(sheet);

        TextView amountCurrency = sheet.findViewById(R.id.addAmountCurrency);
        EditText amountInput = sheet.findViewById(R.id.addAmountInput);
        EditText dateInput = sheet.findViewById(R.id.addDateInput);
        EditText descInput = sheet.findViewById(R.id.addDescriptionInput);
        TextView errorText = sheet.findViewById(R.id.addFormError);
        AppCompatButton submitBtn = sheet.findViewById(R.id.addSubmitBtn);
        View closeBtn = sheet.findViewById(R.id.addSheetClose);

        amountCurrency.setText(Currency.getSymbol(settings.currency));
        dateInput.setText(LocalDate.now().toString());

        final String[] selectedCategory = {""};

        int[] catIds = {R.id.catFood, R.id.catTransport, R.id.catRent, R.id.catFun, R.id.catUtilities, R.id.catOthers};
        String[] catKeys = Category.ALL_KEYS;

        View.OnClickListener catListener = v -> {
            for (int i = 0; i < catIds.length; i++) {
                View catView = sheet.findViewById(catIds[i]);
                if (catView == v) {
                    selectedCategory[0] = catKeys[i];
                    catView.setBackgroundResource(R.drawable.bg_category_item_selected);
                } else {
                    catView.setBackgroundResource(R.drawable.bg_category_item);
                }
            }
            errorText.setVisibility(View.GONE);
        };

        for (int id : catIds) {
            sheet.findViewById(id).setOnClickListener(catListener);
        }

        dateInput.setOnClickListener(v -> showDatePicker(dateInput));
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        submitBtn.setOnClickListener(v -> {
            String amtStr = amountInput.getText().toString().trim();
            String dateStr = dateInput.getText().toString().trim();
            String descStr = descInput.getText().toString().trim();

            double amount = 0;
            try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}

            if (amount <= 0 || dateStr.isEmpty()) {
                errorText.setText("Enter a valid amount and date.");
                errorText.setVisibility(View.VISIBLE);
                return;
            }

            if (selectedCategory[0].isEmpty()) {
                errorText.setText("Please select a category.");
                errorText.setVisibility(View.VISIBLE);
                return;
            }

            Expense expense = new Expense(amount, dateStr, selectedCategory[0], descStr);
            dbHelper.addExpense(currentUser.email, expense);
            expenses = dbHelper.getExpenses(currentUser.email);

            dialog.dismiss();
            renderActiveView();
            toast("Expense added successfully");
        });

        dialog.show();
    }

    private void openEditExpenseSheet(Expense expense) {
        if (expense == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_expense, null);
        dialog.setContentView(sheet);

        TextView amountCurrency = sheet.findViewById(R.id.editAmountCurrency);
        EditText amountInput = sheet.findViewById(R.id.editAmountInput);
        EditText dateInput = sheet.findViewById(R.id.editDateInput);
        EditText descInput = sheet.findViewById(R.id.editDescriptionInput);
        TextView errorText = sheet.findViewById(R.id.editFormError);
        AppCompatButton submitBtn = sheet.findViewById(R.id.editSubmitBtn);
        AppCompatButton deleteBtn = sheet.findViewById(R.id.editDeleteBtn);
        View closeBtn = sheet.findViewById(R.id.editSheetClose);

        amountCurrency.setText(Currency.getSymbol(settings.currency));
        amountInput.setText(String.format(Locale.US, "%.2f", expense.amount));
        dateInput.setText(expense.date);
        descInput.setText(expense.description != null ? expense.description : "");

        final String[] selectedCategory = {expense.category != null ? expense.category.toLowerCase() : "others"};

        int[] catIds = {R.id.editCatFood, R.id.editCatTransport, R.id.editCatRent, R.id.editCatFun, R.id.editCatUtilities, R.id.editCatOthers};
        String[] catKeys = Category.ALL_KEYS;

        // Pre-select category
        for (int i = 0; i < catKeys.length; i++) {
            View catView = sheet.findViewById(catIds[i]);
            if (catKeys[i].equalsIgnoreCase(selectedCategory[0])) {
                catView.setBackgroundResource(R.drawable.bg_category_item_selected);
            } else {
                catView.setBackgroundResource(R.drawable.bg_category_item);
            }
        }

        View.OnClickListener catListener = v -> {
            for (int i = 0; i < catIds.length; i++) {
                View catView = sheet.findViewById(catIds[i]);
                if (catView == v) {
                    selectedCategory[0] = catKeys[i];
                    catView.setBackgroundResource(R.drawable.bg_category_item_selected);
                } else {
                    catView.setBackgroundResource(R.drawable.bg_category_item);
                }
            }
            errorText.setVisibility(View.GONE);
        };

        for (int id : catIds) {
            sheet.findViewById(id).setOnClickListener(catListener);
        }

        dateInput.setOnClickListener(v -> showDatePicker(dateInput));
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        submitBtn.setOnClickListener(v -> {
            String amtStr = amountInput.getText().toString().trim();
            String dateStr = dateInput.getText().toString().trim();
            String descStr = descInput.getText().toString().trim();

            double amount = 0;
            try { amount = Double.parseDouble(amtStr); } catch (Exception ignored) {}

            if (amount <= 0 || dateStr.isEmpty()) {
                errorText.setText("Enter a valid amount and date.");
                errorText.setVisibility(View.VISIBLE);
                return;
            }

            expense.amount = amount;
            expense.date = dateStr;
            expense.category = selectedCategory[0];
            expense.description = descStr;

            dbHelper.updateExpense(expense);
            expenses = dbHelper.getExpenses(currentUser.email);

            dialog.dismiss();
            renderActiveView();
            toast("Expense updated");
        });

        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete transaction?")
                    .setMessage("Are you sure you want to delete this transaction? This cannot be undone.")
                    .setPositiveButton("Delete", (d, which) -> {
                        dbHelper.deleteExpense(expense.id);
                        expenses = dbHelper.getExpenses(currentUser.email);
                        dialog.dismiss();
                        renderActiveView();
                        toast("Transaction deleted");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        try {
            String cur = target.getText().toString();
            if (!cur.isEmpty()) {
                LocalDate ld = LocalDate.parse(cur);
                c.set(ld.getYear(), ld.getMonthValue() - 1, ld.getDayOfMonth());
            }
        } catch (Exception ignored) {}

        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            target.setText(formatted);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    // ==================== PDF & JSON BACKUP ====================

    private void exportPdfReport(String period) {
        List<Expense> items = getReportExpenses(period);
        Collections.sort(items, (a, b) -> (b.date != null ? b.date : "").compareTo(a.date != null ? a.date : ""));

        double total = 0;
        for (Expense e : items) total += e.amount;

        try {
            PdfDocument pdf = new PdfDocument();
            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(18);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            subtitlePaint.setColor(Color.DKGRAY);
            subtitlePaint.setTextSize(11);

            Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            headerPaint.setColor(Color.BLACK);
            headerPaint.setTextSize(10);
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(9);

            Paint linePaint = new Paint();
            linePaint.setColor(Color.LTGRAY);
            linePaint.setStrokeWidth(1);

            int pageNum = 1;
            int y = 45;
            PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, pageNum).create());
            Canvas canvas = page.getCanvas();

            // Header
            canvas.drawText("✦ AuraSpend", 40, y, titlePaint);
            y += 20;
            canvas.drawText("Report: " + reportPeriodLabel(period) + " Report", 40, y, subtitlePaint);
            y += 15;
            canvas.drawText("Account: " + (currentUser != null ? currentUser.name + " (" + currentUser.email + ")" : "Local Account"), 40, y, subtitlePaint);
            y += 15;
            canvas.drawText("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()), 40, y, subtitlePaint);
            y += 15;
            canvas.drawText("Total: " + Currency.format(total, settings.currency) + "  ·  Transactions: " + items.size(), 40, y, headerPaint);
            y += 24;

            // Table headers
            canvas.drawLine(40, y - 5, 555, y - 5, linePaint);
            canvas.drawText("Date", 42, y + 8, headerPaint);
            canvas.drawText("Category", 130, y + 8, headerPaint);
            canvas.drawText("Description", 230, y + 8, headerPaint);
            canvas.drawText("Amount", 480, y + 8, headerPaint);
            y += 18;
            canvas.drawLine(40, y, 555, y, linePaint);
            y += 14;

            for (Expense e : items) {
                if (y > 800) {
                    pdf.finishPage(page);
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++pageNum).create());
                    canvas = page.getCanvas();
                    y = 45;
                }

                String dateStr = e.date != null ? e.date : "";
                String catStr = Category.getLabel(e.category);
                String descStr = (e.description != null && !e.description.trim().isEmpty()) ? e.description : "—";
                if (descStr.length() > 36) descStr = descStr.substring(0, 34) + "…";
                String amtStr = Currency.format(e.amount, settings.currency);

                canvas.drawText(dateStr, 42, y, textPaint);
                canvas.drawText(catStr, 130, y, textPaint);
                canvas.drawText(descStr, 230, y, textPaint);
                canvas.drawText(amtStr, 480, y, textPaint);
                y += 16;
            }

            y += 8;
            canvas.drawLine(40, y, 555, y, linePaint);
            y += 16;
            canvas.drawText("End of report. Total: " + Currency.format(total, settings.currency), 40, y, headerPaint);

            pdf.finishPage(page);

            File pdfFile = new File(getCacheDir(), "auraspend-" + period + "-report-" + LocalDate.now() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                pdf.writeTo(fos);
            }
            pdf.close();

            shareExportFile(pdfFile, "application/pdf", "AuraSpend " + reportPeriodLabel(period) + " Report");
            toast("PDF generated");
        } catch (Exception e) {
            toast("PDF generation failed: " + e.getMessage());
        }
    }

    private void exportJsonBackup() {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("app", "AuraSpend");
            data.put("version", 2);
            data.put("exportedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));
            data.put("expenses", expenses);
            data.put("settings", settings);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonStr = gson.toJson(data);

            File jsonFile = new File(getCacheDir(), "auraspend-backup-" + LocalDate.now() + ".json");
            try (FileWriter writer = new FileWriter(jsonFile)) {
                writer.write(jsonStr);
            }

            shareExportFile(jsonFile, "application/json", "AuraSpend Backup");
            toast("Backup exported");
        } catch (Exception e) {
            toast("Export failed: " + e.getMessage());
        }
    }

    private void importJsonBackup(Uri uri) {
        if (currentUser == null) return;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }

            String content = out.toString(StandardCharsets.UTF_8.name());
            JSONObject obj = new JSONObject(content);

            AppSettings importedSettings = null;
            if (obj.has("settings")) {
                JSONObject s = obj.getJSONObject("settings");
                importedSettings = new AppSettings(
                        s.optString("currency", settings.currency),
                        s.optDouble("budget", settings.budget),
                        s.optString("theme", settings.theme)
                );
            }

            List<Expense> importedExpenses = new ArrayList<>();
            if (obj.has("expenses")) {
                JSONArray arr = obj.getJSONArray("expenses");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    double amount = item.optDouble("amount", 0);
                    String date = item.optString("date", "");
                    String category = item.optString("category", "others");
                    String description = item.optString("description", "");
                    if (amount > 0 && !date.isEmpty()) {
                        importedExpenses.add(new Expense(amount, date, category, description));
                    }
                }
            }

            dbHelper.importBackup(currentUser.email, importedExpenses, importedSettings);
            loadUserData();
            applyTheme();
            renderActiveView();
            toast("Backup imported successfully (" + importedExpenses.size() + " expenses)");
        } catch (Exception e) {
            toast("Invalid AuraSpend backup file");
        }
    }

    private void shareExportFile(File file, String mimeType, String subject) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(mimeType);
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            share.putExtra(Intent.EXTRA_SUBJECT, subject);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share " + subject));
        } catch (Exception e) {
            toast("Sharing failed: " + e.getMessage());
        }
    }

    // ==================== UI HELPERS ====================

    private TextView text(String s, int size, int color) {
        return text(s, size, color, false);
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) {
            t.setTypeface(null, Typeface.BOLD);
        }
        return t;
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
    }

    private void spaceHorizontal(LinearLayout parent, int width) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private GradientDrawable roundDrawable(int color, int radiusDp) {
        return roundDrawable(color, radiusDp, 0);
    }

    private GradientDrawable roundDrawable(int color, int radiusDp, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) {
            gd.setStroke(dp(1), strokeColor);
        }
        return gd;
    }

    private View createEmptyState(String iconStr, String titleStr, String descStr) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(20), dp(36), dp(20), dp(36));
        box.setBackground(roundDrawable(colorCard(), 20, isLightTheme ? Color.parseColor("#E4DCEF") : Color.parseColor("#2D2145")));
        box.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView icon = text(iconStr, 28, colorPurple());
        icon.setGravity(Gravity.CENTER);
        box.addView(icon);

        space(box, dp(6));

        TextView title = text(titleStr, 14, colorText(), true);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        space(box, dp(2));

        TextView desc = text(descStr, 11, colorMuted());
        desc.setGravity(Gravity.CENTER);
        box.addView(desc);

        return box;
    }

    private String formatDateHeading(String dateStr) {
        try {
            LocalDate ld = LocalDate.parse(dateStr);
            return ld.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String formatDateShort(String dateStr) {
        try {
            LocalDate ld = LocalDate.parse(dateStr);
            return ld.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

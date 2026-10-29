# ✦ AuraSpend — Standalone Android Expense Tracker & Backend API

AuraSpend is a native Android expense tracking and budget management application built with **Java**, **Android Material Design**, **embedded standalone SQLite**, and an optional **Spring Boot REST API** backend.

---

## ✨ Features

- **100% Standalone & Offline Support**: All user accounts, expenses, and settings can be stored locally on the Android device in SQLite (`auraspend.db`) with zero external backend required, or synced via the Spring Boot REST API.
- **Multi-User Account System**:
  - Secure local registration and login with SHA-256 password hashing.
  - Independent expense lists, monthly budget, preferred currency, and theme for each user account.
  - Automatic session restore on app launch.
- **Dashboard Overview**:
  - Current-month total spending and transactions count.
  - Real-time comparison to monthly budget (amount remaining or over budget).
  - Custom circular budget progress ring (`CircularProgressView`) showing percentage of budget spent.
  - Stat cards: Daily average spending and top spending category.
  - Spending breakdown by category with proportional progress bars.
  - Category filter chips (`All`, `🍔 Food`, `🚗 Transport`, `🏠 Rent`, `🎮 Fun`, `💡 Utilities`, `✦ Others`) with quick clear.
- **Activity & History**:
  - Real-time search by description or category name.
  - Transactions grouped by date with formatted weekday headings and daily totals.
  - Tap any transaction card to open the **Edit Expense** bottom sheet.
- **Add & Edit Expense Bottom Sheet**:
  - Slide-up bottom sheets with 3x2 interactive category selection grid.
  - Amount input with the user's active currency symbol.
  - Date input with native Android calendar `DatePickerDialog`.
  - Optional description field.
  - Transaction delete option with confirmation prompt.
- **Reports & PDF Export**:
  - Summary views for **Weekly**, **Monthly**, **Annual**, and **All time** periods.
  - Top categories breakdown and transaction item list.
  - **PDF Export**: Generates reports via Android's native `PdfDocument` and opens the Android share sheet.
- **Settings & Preferences**:
  - **9 Supported Currencies**: USD (`$`), INR (`₹`), EUR (`€`), GBP (`£`), AED (`د.إ`), CAD (`C$`), AUD (`A$`), JPY (`¥`), SGD (`S$`). Switching currency instantly updates all screens.
  - **Monthly Budget**: Configurable monthly budget.
  - **Appearance / Themes**: Instant toggle between Dark Obsidian and Light Lavender modes.
  - **JSON Backup & Restore**: Full backup export and import compatible with AuraSpend v2 backups.

---

## 🛠️ Project Structure & Architecture

```
AuraSpendJava/
├── android-app/
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/auraspend/app/
│   │   │   │   │   ├── api/                     # Retrofit2 API Client & Services
│   │   │   │   │   ├── data/                    # Standalone SQLite database & CRUD
│   │   │   │   │   ├── model/                   # User, Expense, AppSettings models
│   │   │   │   │   ├── ui/                      # Custom CircularProgressView ring
│   │   │   │   │   └── MainActivity.java        # Main Activity & controller logic
│   │   │   │   ├── res/                         # XML layouts, drawables, themes, colors
│   │   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle.properties
├── backend/                                     # Spring Boot REST API Service
│   ├── src/main/java/com/auraspend/api/
│   └── pom.xml
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🚀 How to Open and Run in Android Studio

1. Open **Android Studio**.
2. Select **File → Open...** and choose this project directory:
   ```
   C:\SAN\AuraSpendJava\AuraSpendJava
   ```
3. Android Studio will automatically perform Gradle sync.
4. Select an Android Emulator (API 26+) or a connected physical Android device.
5. Click **Run (Shift + F10)**.

---

## 💾 Local SQLite Database Schema

The standalone database is stored in Android's private app storage:
`/data/data/com.auraspend.app/databases/auraspend.db`

### Tables:
- **`users`**:
  - `id` INTEGER PRIMARY KEY AUTOINCREMENT
  - `name` TEXT NOT NULL
  - `email` TEXT NOT NULL UNIQUE
  - `password_hash` TEXT NOT NULL (SHA-256)
  - `currency` TEXT DEFAULT 'USD'
  - `created_at` TEXT
- **`expenses`**:
  - `id` INTEGER PRIMARY KEY AUTOINCREMENT
  - `user_email` TEXT NOT NULL
  - `amount` REAL NOT NULL
  - `date` TEXT NOT NULL (YYYY-MM-DD)
  - `category` TEXT NOT NULL
  - `description` TEXT
  - `created_at` TEXT
- **`settings`**:
  - `user_email` TEXT PRIMARY KEY
  - `currency` TEXT DEFAULT 'USD'
  - `budget` REAL DEFAULT 2000.0
  - `theme` TEXT DEFAULT 'dark'

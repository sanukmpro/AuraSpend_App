# ✦ AuraSpend — Android Expense Tracker & Personal Finance Manager

AuraSpend is a native Android expense tracking and budget management application built with **Java**, **Android Material Design**, and **Spring Boot REST API** with offline-first **SQLite** fallback storage.

## ✨ Highlights & Features

- 📱 **Native Android UI**: Built with Material Design 3, slide-up bottom sheets, custom progress indicators (`CircularProgressView`), and responsive dark/light themes.
- 🔐 **Authentication & Multi-User Support**: Secure login and signup with encrypted password hashing and token-based sessions.
- 📊 **Dashboard & Analytics**:
  - Monthly budget tracking with progress indicators.
  - Daily spending averages and top category breakdowns.
  - Quick category filter chips (`🍔 Food`, `🚗 Transport`, `🏠 Rent`, `🎮 Fun`, `💡 Utilities`, `✦ Others`).
- ⏱️ **Transaction History**: Real-time fuzzy search by category or description, grouped by date with full edit/delete bottom sheets.
- 📈 **Reports & PDF Export**: Filterable summary reports (Weekly, Monthly, Annual, All Time) with native PDF document generation and Android share integration.
- 💾 **Backup & Sync**: Complete JSON backup export/import and optional Spring Boot backend integration.
- 🌍 **Multi-Currency Support**: Instant formatting for 9 global currencies (`USD $`, `INR ₹`, `EUR €`, `GBP £`, `AED د.إ`, `CAD C$`, `AUD A$`, `JPY ¥`, `SGD S$`).

## 🛠️ Tech Stack & Architecture

- **Android Client**: Java, AndroidX, Material Components, Retrofit 2, OkHttp 3, Gson, SQLite.
- **Backend API**: Java 17+, Spring Boot, Spring Security, Spring Data JPA / Hibernate, H2/MySQL.
- **Build System**: Gradle 8.9+, Android AGP 8.6.1.

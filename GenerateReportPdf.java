import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GenerateReportPdf {

    public static void main(String[] args) {
        try {
            System.out.println("Generating AuraSpend Comprehensive Project Report PDF...");
            byte[] pdfBytes = buildReportPdf();
            try (FileOutputStream fos = new FileOutputStream("AuraSpend_Project_Report.pdf")) {
                fos.write(pdfBytes);
            }
            System.out.println("Successfully generated AuraSpend_Project_Report.pdf (" + pdfBytes.length + " bytes)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] buildReportPdf() {
        ReportDoc doc = new ReportDoc();

        // COVER / TITLE
        doc.addTitle("AuraSpend — Android Native Java Migration");
        doc.addSubTitle("Complete Technical Project Report & Java Implementation Guide");
        doc.addMeta("Platform: Android (Java 17, Min SDK 26, Target SDK 35)  |  Engine: Standalone SQLite");
        doc.addDivider();

        // 1. EXECUTIVE SUMMARY
        doc.addHeading1("1. Executive Summary");
        doc.addParagraph("AuraSpend is a personal finance and expense tracking Android application written in native Java with XML layouts and an embedded standalone SQLite database. The project was successfully transitioned from an earlier prototype requiring a PC-hosted Spring Boot and MySQL server to a 100% self-contained, offline-first mobile application.");
        doc.addParagraph("Key Achievements:");
        doc.addBullet("100% Standalone Operation: Requires zero external servers, zero cloud dependency, and zero PC-hosted database. All data resides securely on the device.");
        doc.addBullet("Multi-User Support: Multiple user accounts can register and authenticate on the same phone with isolated data and SHA-256 hashed passwords.");
        doc.addBullet("Custom UI & Boot Animation: Crafted luxury obsidian-purple theme (#090414), 3D squircle app logo, and a choreographed startup boot animation with halo pulse.");
        doc.addBullet("Full Feature Parity: Dashboard budget progress ring, searchable history, 3x2 category sheets, on-device PDF export, 9 currencies, and JSON backup/restore.");

        // 2. ARCHITECTURE
        doc.addHeading1("2. System Architecture");
        doc.addParagraph("The application implements a clean layered MVC architecture tailored for Android:");
        doc.addBullet("Presentation Layer: SplashActivity.java (boot animation), MainActivity.java (view orchestration), CircularProgressView.java (custom Canvas 2D ring), BottomSheetDialogs, and Material Components.");
        doc.addBullet("Domain & Model Layer: User.java, Expense.java, AppSettings.java, Category.java (6 categories with emojis), and Currency.java (9 currencies with symbol formatting).");
        doc.addBullet("Data Layer: DatabaseHelper.java (SQLiteOpenHelper managing auraspend.db) and SessionManager.java (SharedPreferences persistent session store).");
        doc.addBullet("Security & Export: Android FileProvider providing secure URI grants for on-device PDF report exports and JSON backups.");

        // 3. DATABASE DESIGN
        doc.addHeading1("3. Standalone SQLite Database Architecture");
        doc.addParagraph("The database is embedded inside the Android app private storage at /data/data/com.auraspend.app/databases/auraspend.db.");
        doc.addParagraph("Relational Database Schema (3 Tables):");

        String[] dbHeaders = {"Table Name", "Primary Key", "Key Columns", "Purpose"};
        String[][] dbRows = {
            {"users", "id (AUTOINCREMENT)", "name, email (UNIQUE), password_hash, currency", "Multi-user authentication & profile"},
            {"expenses", "id (AUTOINCREMENT)", "user_email (FK), amount, date, category, desc", "Financial transaction records"},
            {"settings", "user_email (PK)", "currency, budget, theme", "User-specific budget & appearance"}
        };
        doc.addTable(dbHeaders, dbRows);

        doc.addParagraph("Database Implementation Details (DatabaseHelper.java):");
        doc.addBullet("Thread-Safe Singleton: DatabaseHelper.getInstance(context) prevents connection leaks and database lock contention.");
        doc.addBullet("SHA-256 Cryptographic Hashing: Passwords are encrypted with SHA-256 message digests prior to storage; plaintext is never retained.");
        doc.addBullet("Multi-User Isolation: All queries for expenses and settings strictly enforce WHERE user_email = ? clauses, isolating each user's finances.");
        doc.addBullet("Performance Indexing: Indexes idx_expenses_user and idx_expenses_date guarantee sub-millisecond lookups for history and reports.");
        doc.addBullet("Atomic Transactions: importBackup() wraps batch insertions in beginTransaction() and setTransactionSuccessful(), guaranteeing ACID integrity.");

        // 4. JAVA CODE WALKTHROUGH
        doc.addHeading1("4. Screen-by-Screen Java Code Walkthrough");

        // 4.1 Splash
        doc.addHeading2("4.1 Boot-Up Splash Screen (SplashActivity.java)");
        doc.addParagraph("SplashActivity serves as the application's launcher activity, orchestrating a multi-stage startup sequence before transitioning to MainActivity:");
        doc.addBullet("Stage 1 (0ms): Ambient background glow orbs (#090414) fade in.");
        doc.addBullet("Stage 2 (0-750ms): The 3D squircle logo springs in from scale 0.5 to 1.0 using an OvershootInterpolator(1.4f) for a tactile bounce.");
        doc.addBullet("Stage 3 (100-800ms): A radial halo expands behind the logo and transitions into an infinite ValueAnimator breathing pulse.");
        doc.addBullet("Stage 4 (300-900ms): The brand title 'AuraSpend' and subtitle 'Smart money, simple life.' slide up 40px and fade in with DecelerateInterpolator.");
        doc.addBullet("Stage 5 (1850ms): Smooth crossfade transition into MainActivity with activity finish() so users cannot back-navigate to the splash screen.");

        // 4.2 Auth
        doc.addHeading2("4.2 Authentication Flow (MainActivity.java)");
        doc.addParagraph("Authentication manages local login and registration without network reliance:");
        doc.addBullet("Tab Switching: Toggles between loginForm and signupForm with visual chip highlights.");
        doc.addBullet("Login: Queries dbHelper.loginUser(email, password). If valid, stores session in SessionManager and triggers enterApp().");
        doc.addBullet("Sign Up: Validates name length, email format, and password length (min 6 chars). Calls dbHelper.registerUser(), which creates the user and seeds default settings.");
        doc.addBullet("Session Persistence: On startup, SessionManager checks SharedPreferences for an active email. If found, auto-logs into the Dashboard.");

        // 4.3 Dashboard
        doc.addHeading2("4.3 Dashboard & Canvas Budget Ring");
        doc.addParagraph("The dashboard aggregates real-time spending insights for the current calendar month:");
        doc.addBullet("Hero Card: Displays monthly spending total, remaining budget, and the budget progress ring.");
        doc.addBullet("CircularProgressView.java: Custom Android Canvas 2D view rendering the background track, progress arc, percentage, and 'budget' subtext.");
        doc.addBullet("Stat Cards: 3 compact cards displaying total monthly transactions, daily average spending, and top expense category.");
        doc.addBullet("Filter Chips: Interactive horizontally scrollable chips (All, Food, Transport, Rent, Fun, Utilities, Others) to filter category breakdown.");
        doc.addBullet("Category Breakdown: Proportional progress bars scaled to the highest spending category with emoji badges.");

        // 4.4 History
        doc.addHeading2("4.4 Searchable History & Activity");
        doc.addParagraph("The history screen provides chronological transaction logs with real-time search:");
        doc.addBullet("Real-Time Search: TextWatcher monitors user keystrokes in searchBox, filtering both descriptions and category names simultaneously.");
        doc.addBullet("Date Grouping: Transactions are bucketed by date (YYYY-MM-DD), rendering human-readable headers (e.g. 'Thursday, Oct 29') and daily subtotals.");
        doc.addBullet("Tap to Edit: Clicking any transaction card opens the Edit Expense bottom sheet pre-filled with the item's existing data.");

        // 4.5 Bottom Sheets
        doc.addHeading2("4.5 Add & Edit Expense Bottom Sheets");
        doc.addParagraph("Interactive slide-up bottom sheets with Material BottomSheetDialog:");
        doc.addBullet("Category Grid: 3x2 interactive tile grid (Food, Transport, Rent, Fun, Utilities, Others) with selected state highlights.");
        doc.addBullet("Dynamic Currency Prefix: Automatically displays the user's active currency symbol ($ / Rs. / E / L) next to the amount input.");
        doc.addBullet("Native DatePicker: Tapping the date field opens Android's native calendar DatePickerDialog defaulting to the current date.");
        doc.addBullet("Deletion Flow: Edit sheet provides a danger-styled 'Delete transaction' button with an AlertDialog confirmation prompt.");

        // 4.6 Reports & PDF
        doc.addHeading2("4.6 Reports & On-Device PDF Engine");
        doc.addParagraph("Comprehensive financial insights with native PDF generation:");
        doc.addBullet("Period Tabs: Weekly (Monday-current), Monthly (current month), Annual (current year), and All-time.");
        doc.addBullet("Summary Card: Displays timeframe total spending and transaction count.");
        doc.addBullet("Native PDF Generator: Built using Android's native android.graphics.pdf.PdfDocument API. Generates clean multi-page tabular reports directly on the device.");
        doc.addBullet("Sharing: Opens the Android Share Sheet via FileProvider with temporary URI read permissions.");

        // 4.7 Settings & Backup
        doc.addHeading2("4.7 Settings, Appearance & Data Portability");
        doc.addParagraph("User preferences, theme customization, and data backup:");
        doc.addBullet("Currency Selector: Modal chooser for 9 currencies (USD, INR, EUR, GBP, AED, CAD, AUD, JPY, SGD), updating all screens immediately.");
        doc.addBullet("Budget Adjustment: Dialog to adjust monthly budget threshold, updating the budget ring.");
        doc.addBullet("Appearance Toggle: Instant toggle between Dark Obsidian and Light Lavender themes, persisted in SQLite.");
        doc.addBullet("JSON Export/Import: Full backup compatibility with AuraSpend v2 backups. Imports validate JSON structure and transactionally insert records.");

        // 5. SECURITY & VERIFICATION
        doc.addHeading1("5. Security, Build Quality & Verification");
        doc.addParagraph("Quality assurance and security measures:");
        doc.addBullet("Data Privacy: Internal app storage sandboxing prevents cross-app access without device root.");
        doc.addBullet("Password Protection: SHA-256 one-way cryptographic hashing protects credentials.");
        doc.addBullet("FileProvider Security: Content URIs with FLAG_GRANT_READ_URI_PERMISSION avoid broad external storage permissions.");
        doc.addBullet("Compilation: Verified with Gradle 8.9 and JDK 17 (BUILD SUCCESSFUL, app-debug.apk generated).");
        doc.addBullet("Automated Unit Tests: CurrencyAndCategoryTest passes 100% of test assertions for calculations, currencies, and hashing.");
        doc.addBullet("Rootless Inspection: Full compatibility with Android Studio Database Inspector and Termux Wireless ADB.");

        return doc.buildPdf();
    }

    // ==================== PDF BUILDER ENGINE ====================

    private static class ReportDoc {
        private final List<String> objects = new ArrayList<>();
        private final List<Integer> pageObjIds = new ArrayList<>();
        private StringBuilder currentStream = new StringBuilder();
        private int currentPage = 1;
        private int y = 780; // top margin for A4 portrait (595 x 842)

        private final int FONT_BOLD = 3;
        private final int FONT_NORMAL = 4;
        private final int FONT_OBLIQUE = 5;

        ReportDoc() {
            objects.add(""); // placeholder 1: Catalog
            objects.add(""); // placeholder 2: Pages
            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"); // obj 3
            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"); // obj 4
            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique >>"); // obj 5

            initPage();
        }

        private void initPage() {
            currentStream = new StringBuilder();
            // Background fill (#FFFFFF)
            currentStream.append("1.0 1.0 1.0 rg\n0 0 595 842 re f\n");

            // Top decorative bar (#A56CFF)
            currentStream.append("0.647 0.423 1.0 rg\n0 834 595 8 re f\n");

            // Running header
            if (currentPage > 1) {
                currentStream.append("BT\n/F2 8 Tf\n0.55 0.55 0.60 rg\n");
                currentStream.append("45 815 Td\n(AuraSpend - Android Technical Report & Implementation Guide) Tj\nET\n");
                currentStream.append("0.85 0.85 0.90 RG\n0.5 w\n45 808 m 550 808 l S\n");
            }

            // Running footer
            currentStream.append("BT\n/F2 8 Tf\n0.55 0.55 0.60 rg\n");
            currentStream.append("45 30 Td\n(Confidential - Generated for AuraSpend Android Project) Tj\nET\n");
            currentStream.append("0.85 0.85 0.90 RG\n0.5 w\n45 42 m 550 42 l S\n");

            currentStream.append("BT\n/F1 8 Tf\n0.647 0.423 1.0 rg\n");
            currentStream.append("500 30 Td\n(Page ").append(currentPage).append(") Tj\nET\n");

            y = 780;
        }

        private void checkPageBreak(int spaceNeeded) {
            if (y - spaceNeeded < 60) {
                flushPage();
                currentPage++;
                initPage();
            }
        }

        private void flushPage() {
            String streamContent = currentStream.toString();
            byte[] streamBytes = streamContent.getBytes(StandardCharsets.US_ASCII);

            int streamObjId = objects.size() + 1;
            String streamObj = "<< /Length " + streamBytes.length + " >>\nstream\n" + streamContent + "\nendstream";
            objects.add(streamObj);

            int pageObjId = objects.size() + 1;
            pageObjIds.add(pageObjId);
            String pageObj = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 " 
                + FONT_BOLD + " 0 R /F2 " + FONT_NORMAL + " 0 R /F3 " + FONT_OBLIQUE + " 0 R >> >> /Contents " + streamObjId + " 0 R >>";
            objects.add(pageObj);
        }

        void addTitle(String text) {
            checkPageBreak(40);
            currentStream.append("BT\n/F1 20 Tf\n0.12 0.06 0.22 rg\n");
            currentStream.append("45 ").append(y).append(" Td\n(").append(esc(text)).append(") Tj\nET\n");
            y -= 26;
        }

        void addSubTitle(String text) {
            checkPageBreak(25);
            currentStream.append("BT\n/F1 12 Tf\n0.52 0.28 0.85 rg\n");
            currentStream.append("45 ").append(y).append(" Td\n(").append(esc(text)).append(") Tj\nET\n");
            y -= 18;
        }

        void addMeta(String text) {
            checkPageBreak(20);
            currentStream.append("BT\n/F3 9 Tf\n0.50 0.50 0.55 rg\n");
            currentStream.append("45 ").append(y).append(" Td\n(").append(esc(text)).append(") Tj\nET\n");
            y -= 16;
        }

        void addDivider() {
            checkPageBreak(15);
            currentStream.append("0.80 0.75 0.90 RG\n1 w\n");
            currentStream.append("45 ").append(y).append(" m 550 ").append(y).append(" l S\n");
            y -= 18;
        }

        void addHeading1(String text) {
            checkPageBreak(35);
            y -= 8;
            currentStream.append("0.95 0.92 1.0 rg\n");
            currentStream.append("45 ").append(y - 4).append(" 505 22 re f\n");
            currentStream.append("0.647 0.423 1.0 rg\n");
            currentStream.append("45 ").append(y - 4).append(" 4 22 re f\n");

            currentStream.append("BT\n/F1 13 Tf\n0.18 0.08 0.32 rg\n");
            currentStream.append("56 ").append(y + 2).append(" Td\n(").append(esc(text)).append(") Tj\nET\n");
            y -= 26;
        }

        void addHeading2(String text) {
            checkPageBreak(25);
            y -= 4;
            currentStream.append("BT\n/F1 11 Tf\n0.35 0.18 0.55 rg\n");
            currentStream.append("45 ").append(y).append(" Td\n(").append(esc(text)).append(") Tj\nET\n");
            y -= 16;
        }

        void addParagraph(String text) {
            List<String> lines = wrapText(text, 95);
            for (String line : lines) {
                checkPageBreak(15);
                currentStream.append("BT\n/F2 9.5 Tf\n0.22 0.22 0.25 rg\n");
                currentStream.append("45 ").append(y).append(" Td\n(").append(esc(line)).append(") Tj\nET\n");
                y -= 13;
            }
            y -= 4;
        }

        void addBullet(String text) {
            List<String> lines = wrapText(text, 90);
            for (int i = 0; i < lines.size(); i++) {
                checkPageBreak(15);
                if (i == 0) {
                    currentStream.append("0.647 0.423 1.0 rg\n");
                    currentStream.append("50 ").append(y + 2).append(" 4 4 re f\n");
                }
                currentStream.append("BT\n/F2 9.5 Tf\n0.22 0.22 0.25 rg\n");
                currentStream.append("62 ").append(y).append(" Td\n(").append(esc(lines.get(i))).append(") Tj\nET\n");
                y -= 13;
            }
            y -= 2;
        }

        void addTable(String[] headers, String[][] rows) {
            checkPageBreak(25 + rows.length * 18);
            int rowH = 18;
            int startX = 45;
            int[] colW = {80, 115, 170, 140};

            // Header background
            currentStream.append("0.90 0.85 0.98 rg\n");
            currentStream.append(startX).append(" ").append(y - 4).append(" 505 ").append(rowH).append(" re f\n");

            currentStream.append("BT\n/F1 9 Tf\n0.18 0.08 0.32 rg\n");
            int curX = startX + 6;
            for (int c = 0; c < headers.length; c++) {
                currentStream.append(curX).append(" ").append(y + 1).append(" Td\n(").append(esc(headers[c])).append(") Tj\n");
                curX += colW[c];
            }
            currentStream.append("ET\n");
            y -= rowH;

            // Rows
            for (int r = 0; r < rows.length; r++) {
                checkPageBreak(rowH);
                if (r % 2 == 1) {
                    currentStream.append("0.98 0.97 1.0 rg\n");
                    currentStream.append(startX).append(" ").append(y - 4).append(" 505 ").append(rowH).append(" re f\n");
                }
                currentStream.append("0.85 0.85 0.90 RG\n0.5 w\n");
                currentStream.append(startX).append(" ").append(y - 4).append(" m 550 ").append(y - 4).append(" l S\n");

                currentStream.append("BT\n/F2 8.5 Tf\n0.25 0.25 0.28 rg\n");
                curX = startX + 6;
                for (int c = 0; c < rows[r].length; c++) {
                    currentStream.append(curX).append(" ").append(y + 1).append(" Td\n(").append(esc(rows[r][c])).append(") Tj\n");
                    curX += colW[c];
                }
                currentStream.append("ET\n");
                y -= rowH;
            }
            y -= 8;
        }

        byte[] buildPdf() {
            flushPage(); // flush the final page

            // Build Pages object (obj 2)
            StringBuilder kids = new StringBuilder();
            for (int id : pageObjIds) {
                kids.append(id).append(" 0 R ");
            }
            objects.set(1, "<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pageObjIds.size() + " >>");

            // Build Catalog object (obj 1)
            objects.set(0, "<< /Type /Catalog /Pages 2 0 R >>");

            // Assemble PDF
            StringBuilder sb = new StringBuilder();
            sb.append("%PDF-1.4\n%\u00FF\u00FF\u00FF\u00FF\n");

            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);

            int currentOffset = sb.length();
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(currentOffset);
                String objStr = (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n";
                sb.append(objStr);
                currentOffset += objStr.getBytes(StandardCharsets.US_ASCII).length;
            }

            int startXref = currentOffset;
            sb.append("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
            for (int i = 1; i <= objects.size(); i++) {
                sb.append(String.format("%010d 00000 n \n", offsets.get(i)));
            }
            sb.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
              .append(startXref).append("\n%%EOF\n");

            return sb.toString().getBytes(StandardCharsets.US_ASCII);
        }

        private static List<String> wrapText(String text, int maxChars) {
            List<String> result = new ArrayList<>();
            if (text == null || text.isEmpty()) {
                result.add("");
                return result;
            }
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.length() + word.length() + 1 <= maxChars) {
                    if (line.length() > 0) line.append(" ");
                    line.append(word);
                } else {
                    result.add(line.toString());
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) result.add(line.toString());
            return result;
        }

        private static String esc(String text) {
            if (text == null) return "";
            return text.replace("\\", "\\\\")
                       .replace("(", "\\(")
                       .replace(")", "\\)")
                       .replaceAll("[^\\x20-\\x7E]", " ");
        }
    }
}

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeneratePresentationPdf {

    public static void main(String[] args) {
        try {
            System.out.println("Generating AuraSpend Presentation PDF...");
            byte[] pdfBytes = buildPresentationPdf();
            try (FileOutputStream fos = new FileOutputStream("AuraSpend_Presentation.pdf")) {
                fos.write(pdfBytes);
            }
            System.out.println("Successfully generated AuraSpend_Presentation.pdf (" + pdfBytes.length + " bytes)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] buildPresentationPdf() {
        List<String> objects = new ArrayList<>();
        // Object index starts at 1
        int pagesObjId = 1;
        int fontHelveticaBold = 2;
        int fontHelvetica = 3;

        // Allocate placeholder for catalog and pages
        objects.add(""); // placeholder for catalog (will be obj 1)
        objects.add(""); // placeholder for pages (will be obj 2)
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"); // obj 3
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"); // obj 4

        int fontBoldId = 3;
        int fontNormalId = 4;

        List<Integer> pageObjIds = new ArrayList<>();

        // Slide definitions (Width: 842, Height: 595 - A4 Landscape)
        Slide[] slides = {
            new Slide("AuraSpend", "Native Android App & Standalone Database Architecture", 
                "Project Overview & Java Implementation Guide",
                new String[]{
                    "Transitioned from PC MySQL/Spring Boot dependency to 100% standalone Android app.",
                    "Written in pure Java with XML layouts and native Android Material Components.",
                    "Embedded SQLite database (auraspend.db) provides complete offline data independence.",
                    "Identical feature set and visual parity with the luxury AuraSpend dark violet theme.",
                    "Custom animated boot-up splash screen with pulsating halo and 3D squircle logo."
                }),
            new Slide("Executive Transformation", "From Server-Dependent to 100% Standalone",
                "Key Architectural Shifts",
                new String[]{
                    "OLD PROTOTYPE: Required running Spring Boot backend and MySQL 8 server on a host PC.",
                    "LIMITATION: Android emulator or device failed with 'Can't reach server' if PC was offline.",
                    "NEW NATIVE ARCHITECTURE: All accounts, expenses, and settings stored in device SQLite.",
                    "ZERO EXTERNAL DEPENDENCIES: Zero cloud reliance, zero PC database, works fully offline.",
                    "DATA PORTABILITY: Full JSON backup import/export compatible with web AuraSpend v2."
                }),
            new Slide("Database Architecture", "Embedded SQLite Database (auraspend.db)",
                "Relational Schema & Multi-User Isolation",
                new String[]{
                    "DATABASE HELPER: DatabaseHelper.java extends SQLiteOpenHelper with singleton pattern.",
                    "TABLE 'users': Stores unique email, display name, currency, and SHA-256 hashed password.",
                    "TABLE 'expenses': Stores user_email, amount, date (YYYY-MM-DD), category, and description.",
                    "TABLE 'settings': 1-to-1 user settings storing preferred currency, budget, and theme mode.",
                    "INDEXES: idx_expenses_user and idx_expenses_date guarantee sub-millisecond query performance.",
                    "TRANSACTION SAFETY: importBackup uses beginTransaction() and setTransactionSuccessful()."
                }),
            new Slide("Domain & Business Logic", "Models, Currencies and Categories",
                "Java Class Design",
                new String[]{
                    "CURRENCY (Currency.java): Supports 9 global currencies with dynamic symbols and formatting:",
                    "  - USD ($), INR (Rs.), EUR (E), GBP (L), AED (AED), CAD (C$), AUD (A$), JPY (Y), SGD (S$).",
                    "CATEGORY (Category.java): 6 categorized spending buckets with emojis and labels:",
                    "  - Food (Burger), Transport (Car), Rent (House), Fun (Game), Utilities (Bulb), Others (Star).",
                    "USER & EXPENSE (User.java, Expense.java): Clean POJOs for financial entity mapping.",
                    "SESSION (SessionManager.java): Persistent SharedPreferences session store for auto-login."
                }),
            new Slide("UI Engine: Boot & Dashboard", "Splash Choreography & Canvas Graphics",
                "Interactive Screen Implementations",
                new String[]{
                    "BOOT ANIMATION (SplashActivity.java): Multi-stage startup choreography:",
                    "  - Ambient glow orbs fade in -> 3D logo springs with OvershootInterpolator bounce.",
                    "  - Radial halo breathing pulse animation -> Title & subtitle slide up -> Crossfade transition.",
                    "CIRCULAR BUDGET RING (CircularProgressView.java): Custom Android Canvas 2D view:",
                    "  - Draws background ring track, progress arc, percentage text, and 'budget' label in center.",
                    "DASHBOARD METRICS: Current month spending, remaining budget, daily average, top category.",
                    "FILTER CHIPS: Interactive horizontally scrollable chips to filter category breakdown list."
                }),
            new Slide("UI Engine: History & Sheets", "Searchable History & Slide-Up Sheets",
                "Transaction Management Workflows",
                new String[]{
                    "HISTORY SCREEN: Real-time search by description or category using TextWatcher.",
                    "DATE GROUPING: Automatically groups transactions into day buckets with weekday headers.",
                    "ADD EXPENSE SHEET: Slide-up BottomSheetDialog with 3x2 interactive category selection grid.",
                    "DATE PICKER: Native Android calendar DatePickerDialog integration defaults to current date.",
                    "EDIT EXPENSE SHEET: Pre-fills values, allows in-place updating or deletion with confirmation alert."
                }),
            new Slide("Reports & Data Portability", "On-Device PDF Engine & Backup System",
                "Insights & Export Capabilities",
                new String[]{
                    "PERIOD TABS: Instant filtering for Weekly, Monthly, Annual, and All-time timeframes.",
                    "PDF GENERATION: Uses Android's native android.graphics.pdf.PdfDocument engine directly on device.",
                    "SHARING INTEGRATION: Generates formatted report with table and shares via FileProvider intent.",
                    "JSON BACKUP: Exports complete backup file (auraspend-backup.json) for cross-device migration.",
                    "JSON RESTORE: Parses backup files, validates fields, and transactionally imports into SQLite."
                }),
            new Slide("Settings, Security & Verification", "Theming, Protection and Build Quality",
                "Implementation Quality & Testing",
                new String[]{
                    "APPEARANCE TOGGLE: Instant switching between Dark Obsidian and Light Lavender themes.",
                    "LOCAL SECURITY: Data sandboxed in private app directory; SHA-256 one-way password hashing.",
                    "SECURE SHARING: FileProvider grants temporary URI read permission without broad storage access.",
                    "VERIFICATION: Built and tested with Gradle 8.9 and JDK 17 (BUILD SUCCESSFUL).",
                    "UNIT TESTS: CurrencyAndCategoryTest passes 100% of test cases for calculations and hashing."
                })
        };

        for (int i = 0; i < slides.length; i++) {
            Slide slide = slides[i];
            String contentStream = slide.render(i + 1, slides.length);
            byte[] streamBytes = contentStream.getBytes(StandardCharsets.US_ASCII);

            int streamObjId = objects.size() + 1;
            String streamObj = "<< /Length " + streamBytes.length + " >>\nstream\n" + contentStream + "\nendstream";
            objects.add(streamObj);

            int pageObjId = objects.size() + 1;
            pageObjIds.add(pageObjId);
            String pageObj = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 " 
                + fontBoldId + " 0 R /F2 " + fontNormalId + " 0 R >> >> /Contents " + streamObjId + " 0 R >>";
            objects.add(pageObj);
        }

        // Build Pages object (obj 2)
        StringBuilder kids = new StringBuilder();
        for (int id : pageObjIds) {
            kids.append(id).append(" 0 R ");
        }
        objects.set(1, "<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pageObjIds.size() + " >>");

        // Build Catalog object (obj 1)
        objects.set(0, "<< /Type /Catalog /Pages 2 0 R >>");

        // Assemble entire PDF document with xref table
        StringBuilder sb = new StringBuilder();
        sb.append("%PDF-1.4\n%\u00FF\u00FF\u00FF\u00FF\n");

        List<Integer> offsets = new ArrayList<>();
        offsets.add(0); // dummy for index 0

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

    private static class Slide {
        String tag;
        String title;
        String subtitle;
        String[] points;

        Slide(String tag, String title, String subtitle, String[] points) {
            this.tag = tag;
            this.title = title;
            this.subtitle = subtitle;
            this.points = points;
        }

        String render(int slideNum, int totalSlides) {
            StringBuilder s = new StringBuilder();

            // Background dark gradient / rectangle (#090414)
            s.append("0.035 0.015 0.078 rg\n"); // #090414
            s.append("0 0 842 595 re f\n");

            // Top decorative bar (#130B22)
            s.append("0.074 0.043 0.133 rg\n"); // #130B22
            s.append("0 587 842 8 re f\n");

            // Accent purple strip (#A56CFF)
            s.append("0.647 0.423 1.0 rg\n"); // #A56CFF
            s.append("0 585 842 2 re f\n");

            // Card panel in center (#130B22)
            s.append("0.074 0.043 0.133 rg\n");
            s.append("40 45 762 505 re f\n");

            // Card subtle border (#2A1647)
            s.append("0.165 0.086 0.278 RG\n");
            s.append("1.5 w\n");
            s.append("40 45 762 505 re S\n");

            // Tag badge (#361A5C background)
            s.append("0.211 0.102 0.360 rg\n");
            s.append("65 505 130 26 re f\n");
            s.append("0.647 0.423 1.0 RG\n");
            s.append("1 w\n");
            s.append("65 505 130 26 re S\n");

            // Tag text
            s.append("BT\n/F1 10 Tf\n0.85 0.72 1.0 rg\n");
            s.append("78 513 Td\n(").append(escapePdf(tag)).append(") Tj\nET\n");

            // Slide Title (Helvetica-Bold 22pt)
            s.append("BT\n/F1 22 Tf\n1.0 0.98 1.0 rg\n");
            s.append("65 470 Td\n(").append(escapePdf(title)).append(") Tj\nET\n");

            // Slide Subtitle (Helvetica 12pt)
            s.append("BT\n/F2 12 Tf\n0.66 0.61 0.72 rg\n");
            s.append("65 448 Td\n(").append(escapePdf(subtitle)).append(") Tj\nET\n");

            // Divider line
            s.append("0.165 0.086 0.278 RG\n1 w\n");
            s.append("65 432 m 777 432 l S\n");

            // Bullet points inside cards
            int y = 390;
            for (String pt : points) {
                // Bullet icon box
                s.append("0.647 0.423 1.0 rg\n");
                s.append("70 ").append(y + 3).append(" 8 8 re f\n");

                // Text
                s.append("BT\n/F2 13 Tf\n0.95 0.92 1.0 rg\n");
                s.append("90 ").append(y).append(" Td\n(").append(escapePdf(pt)).append(") Tj\nET\n");

                y -= 38;
            }

            // Footer bar
            s.append("BT\n/F2 10 Tf\n0.52 0.47 0.58 rg\n");
            s.append("65 65 Td\n(AuraSpend Android Architecture Report | 100% Standalone SQLite) Tj\nET\n");

            s.append("BT\n/F1 10 Tf\n0.647 0.423 1.0 rg\n");
            s.append("740 65 Td\n(Slide ").append(slideNum).append(" / ").append(totalSlides).append(") Tj\nET\n");

            return s.toString();
        }

        private String escapePdf(String text) {
            if (text == null) return "";
            return text.replace("\\", "\\\\")
                       .replace("(", "\\(")
                       .replace(")", "\\)")
                       .replaceAll("[^\\x20-\\x7E]", " ");
        }
    }
}

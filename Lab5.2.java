import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Program 2: Car Database
 */
class Program2_CarDatabase {

    private static final String FILE_PATH = "cars.dat";
    private static final String DELIMITER = "|";

    // Deserializes a file line into 4 groups: brand | plate | color | owner
    private static final Pattern RECORD_PATTERN =
            Pattern.compile("^([^|]+)\\|([^|]+)\\|([^|]+)\\|([^|]+)$");

    // Validates and parses a plate number into 3 groups: AA | 1234 | BB
    private static final Pattern PLATE_PATTERN =
            Pattern.compile(
                    "^([A-ZА-ЯҐЄІЇ]{2})\\s?(\\d{4})\\s?([A-ZА-ЯҐЄІЇ]{2})$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );

    // Parses a partial plate query — each group is optional: AA | 1234 | BB
    private static final Pattern PLATE_QUERY_PATTERN =
            Pattern.compile(
                    "^([A-ZА-ЯҐЄІЇ]{0,2})?(\\d{0,4})?([A-ZА-ЯҐЄІЇ]{0,2})?$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
            );


    static class Car {
        String brand;
        String plate;
        String color;
        String owner;

        Car(String brand, String plate, String color, String owner) {
            this.brand = brand.trim();
            this.plate = plate.trim().toUpperCase();
            this.color = color.trim();
            this.owner = owner.trim();
        }

        String serialize() {
            return brand + DELIMITER + plate + DELIMITER + color + DELIMITER + owner;
        }

        static Car deserialize(String line) {
            Matcher matcher = RECORD_PATTERN.matcher(line.trim());
            if (!matcher.matches()) return null;
            return new Car(
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4)
            );
        }
    }


    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║           Program 2: Car Database                ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Your choice: ");
            String choice = input.nextLine().trim();

            switch (choice) {
                case "1" -> createDatabase(input);
                case "2" -> appendRecord(input);
                case "3" -> editRecord(input);
                case "4" -> viewAllRecords();
                case "5" -> searchByBrandAndColor(input);
                case "6" -> searchByPlate(input);
                case "0" -> {
                    running = false;
                    System.out.println("Goodbye!");
                }
                default -> System.out.println("⚠  Invalid choice, try again.");
            }
        }

        input.close();
    }


    private static void printMenu() {
        System.out.println("\n┌──────────────────────────────────────────┐");
        System.out.println("│                  MENU                    │");
        System.out.println("├──────────────────────────────────────────┤");
        System.out.println("│  1.  Create database from keyboard       │");
        System.out.println("│  2.  Append new record                   │");
        System.out.println("│  3.  Edit existing record                │");
        System.out.println("│  4.  View all records                    │");
        System.out.println("│  5.  Search by brand and color           │");
        System.out.println("│  6.  Search by plate number              │");
        System.out.println("│  0.  Exit                                │");
        System.out.println("└──────────────────────────────────────────┘");
    }


    private static void createDatabase(Scanner input) {
        System.out.println("\n📝 Enter car records (leave brand empty to stop):");
        List<Car> cars = new ArrayList<>();

        while (true) {
            System.out.println("\n--- Car #" + (cars.size() + 1) + " ---");
            Car car = readCarFromInput(input);
            if (car == null) break;
            cars.add(car);
        }

        if (cars.isEmpty()) {
            System.out.println("⚠  No data entered.");
            return;
        }

        saveAllCars(cars, false);
        System.out.println("✅ Database created. Records saved: " + cars.size());
    }


    private static void appendRecord(Scanner input) {
        System.out.println("\n➕ New record:");
        Car car = readCarFromInput(input);

        if (car == null) {
            System.out.println("⚠  Cancelled.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH, true), "UTF-8"))) {
            writer.write(car.serialize());
            writer.newLine();
            System.out.println("✅ Record appended.");
        } catch (IOException e) {
            System.out.println("❌ Write error: " + e.getMessage());
        }
    }


    private static void editRecord(Scanner input) {
        List<Car> cars = loadAllCars();
        if (cars == null || cars.isEmpty()) {
            System.out.println("⚠  Database is empty or does not exist.");
            return;
        }

        printCarTable(cars, "All Cars");

        System.out.print("\nEnter record number to edit (0 = cancel): ");
        int index = parseIntOrZero(input.nextLine());

        if (index == 0) return;
        if (index < 1 || index > cars.size()) {
            System.out.println("⚠  Record not found.");
            return;
        }

        Car car = cars.get(index - 1);
        System.out.println("Editing record #" + index);
        System.out.println("(Press Enter to keep current value)");

        System.out.print("Brand  [" + car.brand + "]: ");
        String brand = input.nextLine().trim();
        if (!brand.isEmpty()) car.brand = brand;

        car.plate = readValidPlate(input, car.plate);

        System.out.print("Color  [" + car.color + "]: ");
        String color = input.nextLine().trim();
        if (!color.isEmpty()) car.color = color;

        System.out.print("Owner  [" + car.owner + "]: ");
        String owner = input.nextLine().trim();
        if (!owner.isEmpty()) car.owner = owner;

        saveAllCars(cars, false);
        System.out.println("✅ Record #" + index + " updated.");
    }


    private static void viewAllRecords() {
        List<Car> cars = loadAllCars();
        if (cars == null) return;

        if (cars.isEmpty()) {
            System.out.println("📄 Database is empty.");
        } else {
            printCarTable(cars, "All Cars");
        }
    }


    private static void searchByBrandAndColor(Scanner input) {
        List<Car> cars = loadAllCars();
        if (cars == null || cars.isEmpty()) {
            System.out.println("⚠  Database is empty or does not exist.");
            return;
        }

        System.out.print("\n🔍 Brand to search: ");
        String targetBrand = input.nextLine().trim();

        System.out.print("🎨 Color to search: ");
        String targetColor = input.nextLine().trim();

        if (targetBrand.isEmpty() || targetColor.isEmpty()) {
            System.out.println("⚠  Brand and color cannot be empty.");
            return;
        }

        List<Car> results = new ArrayList<>();
        for (Car car : cars) {
            if (car.brand.equalsIgnoreCase(targetBrand) &&
                    car.color.equalsIgnoreCase(targetColor)) {
                results.add(car);
            }
        }

        printSearchHeader("brand = '" + targetBrand + "',  color = '" + targetColor + "'");

        if (results.isEmpty()) {
            System.out.println("  No matching cars found.");
        } else {
            System.out.printf("  %-5s  %-15s  %-20s%n", "#", "Plate Number", "Owner");
            System.out.println("  " + "─".repeat(44));
            for (int i = 0; i < results.size(); i++) {
                System.out.printf("  %-5d  %-15s  %-20s%n",
                        i + 1, results.get(i).plate, results.get(i).owner);
            }
            System.out.println("  " + "─".repeat(44));
            System.out.println("  Found: " + results.size() + " car(s).");
        }
    }


    private static void searchByPlate(Scanner input) {
        List<Car> cars = loadAllCars();
        if (cars == null || cars.isEmpty()) {
            System.out.println("⚠  Database is empty or does not exist.");
            return;
        }

        System.out.println("\nEnter what you know about the plate:");
        System.out.println("  AA        → search by first letters");
        System.out.println("  1234      → search by digits");
        System.out.println("  BB        → search by last letters");
        System.out.println("  AA1234BB  → full plate");
        System.out.print("Query: ");
        String query = input.nextLine().trim().toUpperCase();

        if (query.isEmpty()) {
            System.out.println("⚠  Query cannot be empty.");
            return;
        }

        // Parse the query into up to 3 groups — each is optional
        Matcher queryMatcher = PLATE_QUERY_PATTERN.matcher(query);
        if (!queryMatcher.matches()) {
            System.out.println("⚠  Invalid query format.");
            return;
        }

        String queryFirst  = queryMatcher.group(1) != null ? queryMatcher.group(1) : "";
        String queryDigits = queryMatcher.group(2) != null ? queryMatcher.group(2) : "";
        String queryLast   = queryMatcher.group(3) != null ? queryMatcher.group(3) : "";

        // Show user what we are actually searching for
        System.out.println("\nSearching where:");
        if (!queryFirst.isEmpty())  System.out.println("  first letters contain  → '" + queryFirst  + "'");
        if (!queryDigits.isEmpty()) System.out.println("  digits        contain  → '" + queryDigits + "'");
        if (!queryLast.isEmpty())   System.out.println("  last letters  contain  → '" + queryLast   + "'");

        List<Car> results = new ArrayList<>();

        for (Car car : cars) {
            // Split each stored plate into its 3 groups via PLATE_PATTERN
            Matcher plateMatcher = PLATE_PATTERN.matcher(car.plate);
            if (!plateMatcher.matches()) continue;

            String plateFirst  = plateMatcher.group(1).toUpperCase(); // "AA"
            String plateDigits = plateMatcher.group(2);               // "1234"
            String plateLast   = plateMatcher.group(3).toUpperCase(); // "BB"

            // Only check groups that the user actually filled in
            boolean matchFirst  = queryFirst.isEmpty()  || plateFirst.contains(queryFirst);
            boolean matchDigits = queryDigits.isEmpty() || plateDigits.contains(queryDigits);
            boolean matchLast   = queryLast.isEmpty()   || plateLast.contains(queryLast);

            if (matchFirst && matchDigits && matchLast) {
                results.add(car);
            }
        }

        printSearchHeader("plate query = '" + query + "'");

        if (results.isEmpty()) {
            System.out.println("  No cars found.");
        } else {
            System.out.printf("  %-5s  %-14s  %-12s  %-12s  %-15s%n",
                    "#", "Plate", "Brand", "Color", "Owner");
            System.out.println("  " + "─".repeat(62));
            for (int i = 0; i < results.size(); i++) {
                Car c = results.get(i);
                System.out.printf("  %-5d  %-14s  %-12s  %-12s  %-15s%n",
                        i + 1, c.plate, c.brand, c.color, c.owner);
            }
            System.out.println("  " + "─".repeat(62));
            System.out.println("  Found: " + results.size() + " car(s).");
        }
    }

    private static Car readCarFromInput(Scanner input) {
        System.out.print("Brand  (empty = stop): ");
        String brand = input.nextLine().trim();
        if (brand.isEmpty()) return null;

        String plate = readValidPlate(input, null);

        System.out.print("Color : ");
        String color = input.nextLine().trim();

        System.out.print("Owner : ");
        String owner = input.nextLine().trim();

        return new Car(brand, plate, color, owner);
    }

    /**
     *
     * Regex groups on a valid input:
     *   group(1) = first 2 letters  →  "AA"
     *   group(2) = 4 digits         →  "1234"
     *   group(3) = last 2 letters   →  "BB"
     * Normalizes result to uppercase with no spaces: AA1234BB
     */
    private static String readValidPlate(Scanner input, String fallback) {
        while (true) {
            String prompt = (fallback != null)
                    ? "Plate  [" + fallback + "] (e.g. AA1234BB): "
                    : "Plate  (e.g. AA1234BB): ";
            System.out.print(prompt);

            String raw = input.nextLine().trim();

            if (raw.isEmpty() && fallback != null) return fallback;

            Matcher matcher = PLATE_PATTERN.matcher(raw);
            if (matcher.matches()) {
                // Reconstruct normalized plate from captured groups
                return matcher.group(1).toUpperCase()
                        + matcher.group(2)
                        + matcher.group(3).toUpperCase();
            }

            System.out.println("  ⚠  Invalid format. Expected: AA1234BB");
        }
    }

    private static List<Car> loadAllCars() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            System.out.println("⚠  File '" + FILE_PATH + "' not found. Create the database first (option 1).");
            return null;
        }

        List<Car> cars    = new ArrayList<>();
        int       lineNum = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(FILE_PATH), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;

                Car car = Car.deserialize(line);
                if (car != null) {
                    cars.add(car);
                } else {
                    System.out.println("⚠  Line " + lineNum + " is malformed — skipped: [" + line + "]");
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Read error: " + e.getMessage());
            return null;
        }

        return cars;
    }

    private static void saveAllCars(List<Car> cars, boolean append) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_PATH, append), "UTF-8"))) {
            for (Car car : cars) {
                writer.write(car.serialize());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Write error: " + e.getMessage());
        }
    }

    private static void printCarTable(List<Car> cars, String title) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.printf ("║  %-64s║%n", " " + title + "  (" + cars.size() + " records)");
        System.out.println("╠═══╦══════════════╦════════════╦════════════╦════════════════════╣");
        System.out.println("║ # ║ Brand        ║ Plate      ║ Color      ║ Owner              ║");
        System.out.println("╠═══╬══════════════╬════════════╬════════════╬════════════════════╣");
        for (int i = 0; i < cars.size(); i++) {
            Car c = cars.get(i);
            System.out.printf("║%2d ║ %-12s ║ %-10s ║ %-10s ║ %-18s ║%n",
                    i + 1, c.brand, c.plate, c.color, c.owner);
        }
        System.out.println("╚═══╩══════════════╩════════════╩════════════╩════════════════════╝");
    }

    private static void printSearchHeader(String criteria) {
        System.out.println("\n" + "═".repeat(65));
        System.out.println("  Search:  " + criteria);
        System.out.println("═".repeat(65));
    }

    private static int parseIntOrZero(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}


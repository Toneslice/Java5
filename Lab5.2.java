import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Program 2: Car Database
 */
class Program2_CarDatabase {

    private static final String FILE_PATH   = "cars.dat";
    private static final String DELIMITER   = "|";

    // Regex: captures 4 fields separated by '|'
    // Group 1 = brand, 2 = plate, 3 = color, 4 = owner
    private static final Pattern RECORD_PATTERN =
            Pattern.compile("^([^|]+)\\|([^|]+)\\|([^|]+)\\|([^|]+)$");

    // Regex: Ukrainian plate format — AA1234BB or AA 1234 BB
    // Group 1 = first letters, 2 = digits, 3 = last letters
    private static final Pattern PLATE_PATTERN =
            Pattern.compile("^([A-ZА-ЯҐЄІЇ]{2})\\s?(\\d{4})\\s?([A-ZА-ЯҐЄІЇ]{2})$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);


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
                    matcher.group(1),   // brand
                    matcher.group(2),   // plate
                    matcher.group(3),   // color
                    matcher.group(4)    // owner
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
                case "0" -> {
                    running = false;
                    System.out.println("Goodbye!");
                }
                default  -> System.out.println("⚠  Invalid choice, try again.");
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
        System.out.println("Editing: " + car.serialize());
        System.out.println("(Press Enter to keep current value)");

        System.out.print("Brand   [" + car.brand + "]: ");
        String brand = input.nextLine().trim();
        if (!brand.isEmpty()) car.brand = brand;

        String plate = readValidPlate(input, car.plate);
        car.plate = plate;

        System.out.print("Color   [" + car.color + "]: ");
        String color = input.nextLine().trim();
        if (!color.isEmpty()) car.color = color;

        System.out.print("Owner   [" + car.owner + "]: ");
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

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.printf( "  Search: brand = '%s',  color = '%s'%n", targetBrand, targetColor);
        System.out.println("═══════════════════════════════════════════════════════");

        if (results.isEmpty()) {
            System.out.println("  No matching cars found.");
        } else {
            System.out.printf("  %-5s  %-15s  %-20s%n", "#", "Plate Number", "Owner");
            System.out.println("  " + "─".repeat(44));
            for (int i = 0; i < results.size(); i++) {
                Car car = results.get(i);
                System.out.printf("  %-5d  %-15s  %-20s%n", i + 1, car.plate, car.owner);
            }
            System.out.println("  " + "─".repeat(44));
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
     * Regex groups on a valid plate:
     *   group(1) = first 2 letters   e.g. "AA"
     *   group(2) = 4 digits          e.g. "1234"
     *   group(3) = last 2 letters    e.g. "BB"
     */
    private static String readValidPlate(Scanner input, String fallback) {
        while (true) {
            String prompt = (fallback != null)
                    ? "Plate  [" + fallback + "] (e.g. AA1234BB): "
                    : "Plate  (e.g. AA1234BB): ";
            System.out.print(prompt);

            String raw = input.nextLine().trim();

            // Keep existing value if editing and user pressed Enter
            if (raw.isEmpty() && fallback != null) return fallback;

            Matcher matcher = PLATE_PATTERN.matcher(raw);
            if (matcher.matches()) {
                // Reconstruct normalized plate from captured groups: AA1234BB
                String normalized = matcher.group(1).toUpperCase()
                        + matcher.group(2)
                        + matcher.group(3).toUpperCase();
                return normalized;
            }

            System.out.println("  ⚠  Invalid plate format. Expected: AA1234BB");
        }
    }

    private static List<Car> loadAllCars() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            System.out.println("⚠  File '" + FILE_PATH + "' not found. Create the database first (option 1).");
            return null;
        }

        List<Car> cars     = new ArrayList<>();
        int       lineNum  = 0;

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

    private static int parseIntOrZero(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}

import java.io.*;
import java.util.*;

public class Lab5 {

    private static final String FILE_NAME = "text_english.txt";

    private static final Set<Character> VOWELS = new HashSet<>(Arrays.asList(

            'a','e','i','o','u',
            'A','E','I','O','U',

            'а','е','є','и','і','ї','о','у','ю','я',

            'А','Е','Є','И','І','Ї','О','У','Ю','Я'
    ));


    private static final String WORD_SPLIT_REGEX = "[^a-zA-Zа-яА-ЯґҐєЄіІїЇ']+";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Ваш вибір: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> fillFileFromKeyboard(scanner);
                case "2" -> appendToFile(scanner);
                case "3" -> editFile(scanner);
                case "4" -> viewFile();
                case "5" -> findVowelWords();
                case "0" -> {
                    running = false;
                    System.out.println("До побачення!");
                }
                default -> System.out.println("⚠ Невірний вибір. Спробуйте ще раз.");
            }
        }
        scanner.close();
    }


    private static void printMenu() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│              МЕНЮ                   │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│ 1. Заповнити файл з клавіатури      │");
        System.out.println("│ 2. Доповнити файл новим текстом     │");
        System.out.println("│ 3. Редагувати рядок у файлі         │");
        System.out.println("│ 4. Переглянути вміст файлу          │");
        System.out.println("│ 5. Знайти слова (голосна→голосна)   │");
        System.out.println("│ 0. Вихід                            │");
        System.out.println("└─────────────────────────────────────┘");
    }



    private static void fillFileFromKeyboard(Scanner scanner) {
        System.out.println("\n📝 Введіть текст (порожній рядок — завершення):");
        List<String> lines = new ArrayList<>();

        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) break;
            lines.add(line);
        }

        if (lines.isEmpty()) {
            System.out.println("⚠ Текст не введено.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_NAME, false), "UTF-8"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("✅ Файл '" + FILE_NAME + "' створено (" + lines.size() + " рядків).");
        } catch (IOException e) {
            System.out.println("❌ Помилка запису: " + e.getMessage());
        }
    }



    private static void appendToFile(Scanner scanner) {
        System.out.println("\n➕ Введіть новий текст (порожній рядок — завершення):");
        List<String> lines = new ArrayList<>();

        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) break;
            lines.add(line);
        }

        if (lines.isEmpty()) {
            System.out.println("⚠ Нічого не додано.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_NAME, true), "UTF-8"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("✅ Додано " + lines.size() + " рядків до файлу.");
        } catch (IOException e) {
            System.out.println("❌ Помилка: " + e.getMessage());
        }
    }



    private static void editFile(Scanner scanner) {
        List<String> lines = readAllLines();
        if (lines == null) return;

        if (lines.isEmpty()) {
            System.out.println("⚠ Файл порожній.");
            return;
        }

        System.out.println("\n✏ Поточний вміст файлу:");
        for (int i = 0; i < lines.size(); i++) {
            System.out.printf("  [%d] %s%n", i + 1, lines.get(i));
        }

        System.out.print("Введіть номер рядка для редагування (0 — скасувати): ");
        int lineNum;
        try {
            lineNum = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("⚠ Невірний номер.");
            return;
        }

        if (lineNum == 0) return;
        if (lineNum < 1 || lineNum > lines.size()) {
            System.out.println("⚠ Рядок не існує.");
            return;
        }

        System.out.println("Старий рядок: " + lines.get(lineNum - 1));
        System.out.print("Новий рядок : ");
        String newLine = scanner.nextLine();
        lines.set(lineNum - 1, newLine);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE_NAME, false), "UTF-8"))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("✅ Рядок " + lineNum + " оновлено.");
        } catch (IOException e) {
            System.out.println("❌ Помилка: " + e.getMessage());
        }
    }


    private static void viewFile() {
        List<String> lines = readAllLines();
        if (lines == null) return;

        if (lines.isEmpty()) {
            System.out.println("📄 Файл порожній.");
            return;
        }

        System.out.println("\n📄 Вміст файлу '" + FILE_NAME + "':");
        System.out.println("─".repeat(60));
        for (int i = 0; i < lines.size(); i++) {
            System.out.printf("  %2d | %s%n", i + 1, lines.get(i));
        }
        System.out.println("─".repeat(60));
        System.out.println("Всього рядків: " + lines.size());
    }


    private static void findVowelWords() {
        List<String> lines = readAllLines();
        if (lines == null) return;

        if (lines.isEmpty()) {
            System.out.println("⚠ Файл порожній.");
            return;
        }

        List<String> allFound    = new ArrayList<>();
        List<String> uniqueFound = new ArrayList<>();
        Set<String>  seenLower   = new LinkedHashSet<>();

        for (String line : lines) {
            String[] words = line.split(WORD_SPLIT_REGEX);
            for (String word : words) {
                if (word.isEmpty()) continue;

                char first = word.charAt(0);
                char last  = word.charAt(word.length() - 1);

                if (VOWELS.contains(first) && VOWELS.contains(last)) {
                    allFound.add(word);
                    if (seenLower.add(word.toLowerCase())) {
                        uniqueFound.add(word);
                    }
                }
            }
        }

        System.out.println("\n🔍 Слова, що починаються І закінчуються на голосну:");
        System.out.println("─".repeat(62));

        if (allFound.isEmpty()) {
            System.out.println("  (таких слів не знайдено)");
        } else {
            System.out.printf("  %-5s  %-22s  %-12s  %-12s%n",
                    "№", "Слово", "Перша літера", "Остання літера");
            System.out.println("  " + "─".repeat(58));

            for (int i = 0; i < uniqueFound.size(); i++) {
                String w   = uniqueFound.get(i);
                char first = w.charAt(0);
                char last  = w.charAt(w.length() - 1);
                System.out.printf("  %-5d  %-22s  %-12s  %-12s%n",
                        i + 1,
                        w,
                        "'" + first + "' — голосна",
                        "'" + last  + "' — голосна");
            }

            System.out.println("  " + "─".repeat(58));
            System.out.printf("  Знайдено: %d входжень, %d унікальних слів.%n",
                    allFound.size(), uniqueFound.size());
        }
    }


    private static List<String> readAllLines() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("⚠ Файл '" + FILE_NAME + "' не існує. Спочатку заповніть його (пункт 1).");
            return null;
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(FILE_NAME), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.out.println("❌ Помилка читання: " + e.getMessage());
            return null;
        }
        return lines;
    }

}

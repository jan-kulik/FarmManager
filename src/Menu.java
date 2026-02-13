import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Supplier;

public class Menu {

    private final String title;
    private final Scanner sc;
    private final boolean isMainMenu;
    private Supplier<String> statusLine;

    private final Map<Integer, MenuItem> items = new LinkedHashMap<>();

    private Menu(String title, Scanner sc, boolean isMainMenu) {
        this.title = title;
        this.sc = sc;
        this.isMainMenu = isMainMenu;
    }

    public static Menu main(String title, Scanner sc) {
        return new Menu(title, sc, true);
    }

    public static Menu sub(String title, Scanner sc) {
        return new Menu(title, sc, false);
    }

    public void add(int number, String text, Runnable action) {
        items.put(number, new MenuItem(text, action, false));
    }

    public void setStatusLine(Supplier<String> statusLine) {
        this.statusLine = statusLine;
    }

    public void open() {
        while (true) {
            print();
            int choice = readInt("Auswahl: ");

            if (choice == 0) {
                return;
            }

            MenuItem item = items.get(choice);
            if (item == null) {
                System.out.println("Ungültige Auswahl.");
                continue;
            }

            System.out.println();
            item.run();
            System.out.println();
        }
    }

    private void print() {
        System.out.println("=== " + title + " ===");
        if (statusLine != null) {
            System.out.println(statusLine.get());
        }
        for (Map.Entry<Integer, MenuItem> e : items.entrySet()) {
            System.out.println(e.getKey() + ") " + e.getValue().text());
        }
        System.out.println("0) " + (isMainMenu ? "Programm beenden" : "Zurück"));
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Bitte Zahl eingeben.");
            }
        }
    }
}

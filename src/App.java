import java.util.Scanner;

public class App {

    public void start() {
        Scanner sc = new Scanner(System.in);

        Inventory inventory = new Inventory();
        InventoryRepository repo = new InventoryRepository("inventory.csv", "inventory_config.txt");
        repo.loadOrCreate(inventory);

        Menu mainMenu = Menu.main("Hauptmenü", sc);
        Menu inventoryMenu = Menu.sub("Lagerverwaltung", sc);
        Menu inventoryConfigMenu = Menu.sub("Lagerkonfiguration", sc);

        mainMenu.add(1, "Lagerverwaltung", inventoryMenu::open);

        // Lager \\

        inventoryMenu.add(1, "Lagerbestand anzeigen", () -> {
           System.out.println("Maximale Kapazität: " + (inventory.getMaxCapacity() < 0 ? "unbegrenzt" : inventory.getMaxCapacity()));
           System.out.println("Benutzte Kapazität: " + inventory.getUsedCapacity());
           if (inventory.getMaxCapacity() > 0)
               System.out.println("Freie Kapazität: " + inventory.getFreeCapacity());
           for (String id : inventory.getItemIdsSorted())
               System.out.println("-" + id + ": " + inventory.getAmount(id));
           System.out.print("Enter drücken, um zum Menü zurückzukehren.");
           sc.nextLine();
        });

        inventoryMenu.add(2, "Artikel hinzufügen", () -> {
            System.out.print("Artikel-ID: ");
            String id = sc.nextLine();
            System.out.print("Anzahl: ");
            int amount = readInt(sc, "");
            boolean ok = inventory.addItem(id, amount);
            System.out.println(ok ? "Artikel hinzugefügt." : "Artikel konnte nicht hinzugefügt werden.");
            repo.save(inventory);
        });

        inventoryMenu.add(3, "Artikel erstellen", () -> {
            System.out.print("Artikel-ID: ");
            String id = sc.nextLine();
            boolean ok = inventory.createItem(id);
            System.out.println(ok ? "Artikel erstellt." : "Artikel konnte nicht erstellt werden.");
            repo.save(inventory);
        });

        inventoryMenu.add(4, "Artikel entfernen", () -> {
            System.out.print("Artikel-ID: ");
            String id = sc.nextLine();
            if (inventory.getAmount(id) > 0) {
                System.out.println("Der Artikel ist noch im Lager. Sicher das du ihn löschen möchtest?");
                System.out.println("Ja oder Nein?");
                if (sc.nextLine().trim().equalsIgnoreCase("ja")) {
                } else {
                    System.out.println("Löschen abgebrochen.");
                    return;
                }
            }
            boolean ok = inventory.deleteItem(id);
            System.out.println(ok ? "Artikel entfernt." : "Artikel konnte nicht entfernt werden.");
            repo.save(inventory);
        });

        inventoryMenu.add(9, "Lagerkonfiguration", inventoryConfigMenu::open);

        inventoryConfigMenu.add(1, "Maximale Kapazität festlegen", () -> {
            int maxCapacity = readInt(sc, "Maximale Kapazität (-1 für unbegrenzt): ");
            inventory.setMaxCapacity(maxCapacity);
            repo.save(inventory);
            System.out.println("Maximale Kapazität aktualisiert.");
        });

        mainMenu.open();

        System.out.println("Programm beendet.");
        sc.close();
    }

    private int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Bitte Nummer eingeben.");
            }
        }
    }
}

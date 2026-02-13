import java.util.Scanner;
import java.util.List;

public class App {

    public void start() {
        Scanner sc = new Scanner(System.in);

        ItemCatalog catalog = new ItemCatalog();
        try {
            catalog.loadFromCsv("items_catalog.csv");
        } catch (Exception e) {
            System.out.println("Katalog konnte nicht geladen werden. " + e.getMessage());
        }

        Inventory inventory = new Inventory(catalog);
        DataStore dataStore = new DataStore("data.txt");
        InventoryRepository repo = new InventoryRepository("inventory.csv", dataStore);
        if (!dataStore.hasKey("balance") || !dataStore.hasKey("maxCapacity")) {
            System.out.println("Ersteinrichtung:");
            double startBalance = readDouble(sc, "Start-Geld: ");
            int maxCapacity = readInt(sc, "Maximale Kapazität (-1 für unbegrenzt): ");

            dataStore.setDouble("balance", startBalance);
            dataStore.setInt("maxCapacity", maxCapacity);
        }
        repo.loadOrCreate(inventory);
        Balance balance = new Balance(dataStore);

        // ===  Menüs === \\

        Menu mainMenu = Menu.main("Hauptmenü", sc);
        Menu settingsAndConfigMenu = Menu.sub("Einstellungen & Konfiguration", sc);

        mainMenu.setStatusLine(() -> "Kontostand: " + balance.getBalance());
        mainMenu.add(1, "Lagerbestand anzeigen", () -> {
            System.out.println("Maximale Kapazität: " + (inventory.getMaxCapacity() < 0 ? "unbegrenzt" : inventory.getMaxCapacity()));
            if (inventory.getMaxCapacity() > 0) {
                System.out.println("Benutzte Kapazität: " + inventory.getUsedCapacity());
                System.out.println("Freie Kapazität: " + inventory.getFreeCapacity());
            }
            for (String id : inventory.getItemIdsSorted()) {
                String idCapitalized = id.substring(0, 1).toUpperCase() + id.substring(1);
                System.out.println("- " + idCapitalized + ": " + inventory.getAmount(id) + " / " + inventory.getMaxItemCapacity(id));
            }
            System.out.print("Enter drücken, um zum Menü zurückzukehren.");
            sc.nextLine();
        });
        mainMenu.add(9, "Einstellungen & Konfiguration", settingsAndConfigMenu::open);

        // ===  Einstellungen & Konfiguration === \\

        Menu moneySettingMenu = Menu.sub("Geldkonfiguration", sc);
        Menu inventorySettingsMenu = Menu.sub("Lagerkonfiguration", sc);

        settingsAndConfigMenu.add(1, "Geldkonfiguration", moneySettingMenu::open);
        settingsAndConfigMenu.add(2, "Lagerkonfiguration", inventorySettingsMenu::open);

        moneySettingMenu.setStatusLine(() -> "Kontostand: " + balance.getBalance());
        moneySettingMenu.add(1, "Kontostand festlegen", () -> {
            double newBalance = readDouble(sc, "Neuer Kontostand: ");
            balance.setBalance(newBalance);
            System.out.println("Kontostand aktualisiert.");
        });
        moneySettingMenu.add(2, "Geld hinzufügen", () -> {
            double amount = readDouble(sc, "Betrag: ");
            balance.deposit(amount);
            System.out.println("Geld hinzugefügt.");
        });
        moneySettingMenu.add(3, "Geld abziehen", () -> {
            double amount = readDouble(sc, "Betrag: ");
            boolean ok = balance.withdraw(amount);
            System.out.println(ok ? "Geld abgehoben." : "Nicht genügend Geld auf dem Konto.");
        });

        inventorySettingsMenu.add(1, "Artikel hinzufügen", () -> {
            System.out.print("Artikel-ID: ");
            String id = sc.nextLine();
            System.out.print("Anzahl: ");
            int amount = readInt(sc, "");
            boolean ok = inventory.addItem(id, amount);
            System.out.println(ok ? "Artikel hinzugefügt." : "Artikel konnte nicht hinzugefügt werden.");
            repo.save(inventory);
        });

        inventorySettingsMenu.add(2, "Artikel erstellen", () -> {
            int index = 1;
            List<ItemDefinition> list = catalog.getAllSorted();

            for (ItemDefinition item : list) {
                System.out.println(index + ") " + item.getDisplayName());
                index++;
            }

            int choice = readInt(sc, "Artikel auswählen: ");

            if (choice < 1 || choice > list.size()) {
                System.out.println("Wähle eine Zahl.");
                return;
            }

            int cap = readInt(sc, "Maximale Kapazität für dieses Item: ");
            String id = list.get(choice - 1).getItemId();
            boolean ok = inventory.createItem(id, cap);

            System.out.println(ok ? "Artikel erstellt" : "Bereits vorhanden oder ungültig.");
            repo.save(inventory);
        });

        inventorySettingsMenu.add(3, "Artikel entfernen", () -> {
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

        inventorySettingsMenu.add(5, "Maximale Kapazität festlegen", () -> {
            int maxCapacity = readInt(sc, "Maximale Kapazität (-1 für unbegrenzt): ");
            inventory.setMaxCapacity(maxCapacity);
            repo.save(inventory);
            System.out.println("Maximale Kapazität aktualisiert.");
        });


        // hier stopp
        mainMenu.open();

        System.out.println("Programm beendet.");
        sc.close();
    }


    // ===  Hilfsfunktionen === \\

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

    private double readDouble(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim().replace(',', '.');
            try {
                double v = Double.parseDouble(s);
                if (v < 0) {
                    System.out.println("Bitte keine negative Zahl.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Bitte Zahl eingeben (z.B. 1000 oder 1000.50).");
            }
        }
    }
}

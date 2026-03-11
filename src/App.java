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
            System.out.println("Keine vorherigen Daten gefunden.");
            System.out.println("Ersteinrichtung:");
            System.out.println(" ");
            double startBalance = readDouble(sc, "Start-Geld: ");
            int maxCapacity = readInt(sc, "Maximale Kapazität (-1 für unbegrenzt): ");

            dataStore.setDouble("balance", startBalance);
            dataStore.setInt("maxCapacity", maxCapacity);
        }
        repo.loadOrCreate(inventory);
        Balance balance = new Balance(dataStore);

        AnimalRepository animalRepo = new AnimalRepository("animals.csv", dataStore);
        AnimalService animalService = new AnimalService(animalRepo);

        CropRepository cropRepo = new CropRepository("crops.csv", dataStore);
        Crops crops = new Crops(cropRepo);
        crops.setCatalog(catalog);

        MarketRepository marketRepo = new MarketRepository("market.csv");
        Market market = new Market(marketRepo, catalog);

        // === Menüs === \\

        Menu mainMenu = Menu.main("Hauptmenü", sc);
        Menu settingsAndConfigMenu = Menu.sub("Einstellungen & Konfiguration", sc);

        mainMenu.setStatusLine(() -> "Kontostand: " + String.format("%.2f", balance.getBalance()) + " €");

        mainMenu.add(1, "Lagerbestand anzeigen", () -> {
            System.out.println("Maximale Kapazität: " + (inventory.getMaxCapacity() < 0 ? "unbegrenzt" : inventory.getMaxCapacity()));
            if (inventory.getMaxCapacity() > 0) {
                System.out.println("Benutzte Kapazität: " + inventory.getUsedCapacity());
                System.out.println("Freie Kapazität: " + inventory.getFreeCapacity());
            }
            for (String id : inventory.getItemIdsSorted()) {
                System.out.println("- " + inventory.getDisplayName(id) + ": " + inventory.getAmount(id) + " / " + inventory.getMaxItemCapacity(id));
            }
            System.out.print("Enter drücken, um zum Menü zurückzukehren.");
            sc.nextLine();
        });

        mainMenu.add(2, "Markt (Kaufen / Verkaufen)", () -> market.openMenu(sc, inventory, balance, repo));

        mainMenu.add(3, "Tiere anzeigen", () -> animalService.openBrowser(sc, inventory, repo));

        mainMenu.add(4, "Felder & Ackerbau", () -> crops.openBrowser(sc, inventory, repo));

        mainMenu.add(5, "Tag beenden", () -> {
            int day = dataStore.getInt("currentDay", 1);

            System.out.println("=== Tagesabschluss – Tag " + day + " ===");
            System.out.println(" ");

            // Tiere produzieren
            animalService.endDayAll(inventory);
            System.out.println("Tiere haben produziert");

            // Felder wachsen
            crops.endDayAll();
            System.out.println("Felder sind um einen Tag gewachsen.");

            // Marktpreise aktualisieren
            market.endOfDay(inventory);
            System.out.println("Marktpreise wurden aktualisiert.");

            // Lager speichern
            repo.save(inventory);

            // Tageszähler erhöhen
            dataStore.setInt("currentDay", day + 1);

            System.out.println(" ");
            System.out.println("Tag " + day + " abgeschlossen. Willkommen an Tag " + (day + 1) + "!");
            System.out.print("Enter drücken, um zum Menü zurückzukehren.");
            sc.nextLine();
        });

        mainMenu.add(9, "Einstellungen & Konfiguration", settingsAndConfigMenu::open);


        // ===  Einstellungen & Konfiguration === \\

        Menu moneySettingMenu = Menu.sub("Geldkonfiguration", sc);
        Menu inventorySettingsMenu = Menu.sub("Lagerkonfiguration", sc);
        Menu animalSettingsMenu = Menu.sub("Tierkonfiguration", sc);

        settingsAndConfigMenu.add(1, "Geldkonfiguration", moneySettingMenu::open);
        settingsAndConfigMenu.add(2, "Lagerkonfiguration", inventorySettingsMenu::open);
        settingsAndConfigMenu.add(3, "Tierkonfiguration", animalSettingsMenu::open);

        // Geldkonfiguration
        moneySettingMenu.setStatusLine(() -> "Kontostand: " + String.format("%.2f", balance.getBalance()) + " €");
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

        // Lagerkonfiguration
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

        // Tierkonfiguration
        animalSettingsMenu.add(1, "Tier hinzufügen", () -> {
            AnimalType[] types = AnimalType.values();
            for (int i = 0; i < types.length; i++) {
                System.out.println((i + 1) + ") " + types[i]);
            }
            int choice = readInt(sc, "Tierart wählen: ");
            if (choice < 1 || choice > types.length) {
                System.out.println("Ungültige Auswahl.");
                return;
            }
            System.out.print("Name des Tieres: ");
            String name = sc.nextLine().trim();
            if (name.isEmpty()) {
                System.out.println("Name darf nicht leer sein.");
                return;
            }
            Animal created = animalService.create(types[choice - 1], name);
            System.out.println("Tier hinzugefügt: " + created.getName() + " (ID: " + created.getId() + ")");
        });
        animalSettingsMenu.add(2, "Tier füttern", () -> {
            List<Animal> animals = animalService.getAnimals();
            if (animals.isEmpty()) {
                System.out.println("Keine Tiere vorhanden.");
                return;
            }
            int id = readInt(sc, "Tier-ID: ");
            int amount = readInt(sc, "Futtermenge: ");
            boolean ok = animalService.feed(id, amount);
            System.out.println(ok ? "Tier gefüttert." : "Tier nicht gefunden.");
        });
        animalSettingsMenu.add(3, "Tier entfernen", () -> {
            int id = readInt(sc, "Tier-ID: ");
            boolean ok = animalService.deleteById(id);
            System.out.println(ok ? "Tier entfernt." : "Tier nicht gefunden.");
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
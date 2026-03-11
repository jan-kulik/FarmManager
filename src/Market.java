import java.util.*;

/**
 * Preisberechnung:
 *   1) Rohpreis = Basispreis * demandFactor * angebotsFaktor
 *   2) angebotsFaktor: sinkt wenn Lager voll ist, steigt wenn Lager leer ist
 *   3) demandFactor: steigt beim Verkaufen (+0.05 pro Item), sinkt beim Kaufen (-0.05)
 *   4) Preisglätte: smoothedPrice bewegt sich langsam Richtung Rohpreis (30% je Tag)
 *   5) Preis ist auf ±50% des Basispreises begrenzt
 *
 * Beim Kauf: Item wird ggf automatisch im Lager angelegt (Standardkapazität 1000).
 */
public class Market {

    // Maximale Abweichung vom Basispreis (50 %)
    private static final double MAX_DEVIATION = 0.50;

    // Wie stark Kauf/Verkauf den demandFactor verschiebt
    private static final double DEMAND_SHIFT_PER_ITEM = 0.02;

    // Wie schnell der smooth Preis dem Basispreis folgt (0–1)
    private static final double SMOOTH_FACTOR = 0.30;

    // Standard-Kapazität, wenn ein Item beim Kauf neu angelegt wird
    private static final int DEFAULT_ITEM_CAPACITY = 1000;

    private final MarketRepository    repository;
    private final ItemCatalog         catalog;
    private final Map<String, MarketEntry> entries;

    public Market(MarketRepository repository, ItemCatalog catalog) {
        this.repository = repository;
        this.catalog    = catalog;
        this.entries    = new LinkedHashMap<>();
        this.repository.loadOrCreate(this.entries);
    }

    // Preisberechnung

    //Gibt den aktuellen Preis zurück(geglättet)
    public double getBuyPrice(String itemId, Inventory inventory) {
        return getSmoothedPrice(itemId, inventory);
    }

    // Verkaufspreis ist 80% des Kaufpreises
    public double getSellPrice(String itemId, Inventory inventory) {
        return getSmoothedPrice(itemId, inventory) * 0.80;
    }

    // Holt den geglätteten Preis für ein Item, berechnet ihn ggf. neu
    private double getSmoothedPrice(String itemId, Inventory inventory) {
        MarketEntry entry = getOrCreate(itemId);
        return entry.getSmoothedPrice();
    }

    // Kaufen (Spieler kauft vom Markt = Lager füllt sich)

    // Kauft eine Menge eines Items vom Markt. Gibt den Gesamtpreis zurück oder -1 bei Fehler.
    public double buyFromMarket(String itemId, int amount,
                                Inventory inventory, Balance balance) {
        if (amount <= 0) return -1;

        ItemDefinition def = catalog.get(itemId);
        if (def == null) return -1;

        // Item wenn nicht existent neu anlegen (Standardkapazität)
        if (!inventory.itemExists(itemId)) {
            inventory.createItem(itemId, DEFAULT_ITEM_CAPACITY);
        }

        double pricePerUnit = getBuyPrice(itemId, inventory);
        double total        = pricePerUnit * amount;

        if (!balance.withdraw(total)) return -1;

        boolean added = inventory.addItem(itemId, amount);
        if (!added) {
            // Kauf zurückabwickeln, falls das lager voll ist
            balance.deposit(total);
            return -1;
        }

        // Nachfrage anpassen = Bei kauf senken
        MarketEntry entry = getOrCreate(itemId);
        entry.recordBuy(amount);
        double newDemand = entry.getDemandFactor() - DEMAND_SHIFT_PER_ITEM * amount;
        entry.setDemandFactor(Math.max(0.5, newDemand));

        repository.save(entries);
        return total;
    }


    // Verkaufen (Spieler verkauft an Markt = Lager leert sich)

   // Verkauft eine Menge eines Items an den Markt
    public double sellToMarket(String itemId, int amount,
                               Inventory inventory, Balance balance) {
        if (amount <= 0) return -1;
        if (!inventory.hasEnough(itemId, amount)) return -1;

        double pricePerUnit = getSellPrice(itemId, inventory);
        double total        = pricePerUnit * amount;

        boolean removed = inventory.removeItem(itemId, amount);
        if (!removed) return -1;

        balance.deposit(total);

        // Nachfrage anpassen = Verkaufen erhöht Nachfrage
        MarketEntry entry = getOrCreate(itemId);
        entry.recordSell(amount);
        double newDemand = entry.getDemandFactor() + DEMAND_SHIFT_PER_ITEM * amount;
        entry.setDemandFactor(Math.min(1.5, newDemand));

        repository.save(entries);
        return total;
    }


    // Tagesabschluss

    // Berechnet die neuen Preise basierend auf Nachfrage und Lagerbestand, wird am Ende eines Tages aufgerufen
    public void endOfDay(Inventory inventory) {
        List<ItemDefinition> allItems = catalog.getAllSorted();

        for (ItemDefinition def : allItems) {
            String itemId   = def.getItemId();
            double base     = def.getBasePrice();
            if (base <= 0) continue;

            MarketEntry entry = getOrCreate(itemId);

            // Angebotsfaktor: volle Lager = niedriger Preis, leeres Lager = hoher Preis
            double angebotsFaktor = calcAngebotsFaktor(itemId, inventory);

            // Basepreis, Nachfrage und Angebot = Rohpreis
            double rawPrice = base * entry.getDemandFactor() * angebotsFaktor;

            // Begrenzung des Basispreises aus Zeile 16
            double minPrice = base * (1.0 - MAX_DEVIATION);
            double maxPrice = base * (1.0 + MAX_DEVIATION);
            rawPrice = Math.max(minPrice, Math.min(maxPrice, rawPrice));

            // smoothedPrice bewegt sich langsam Richtung rawPrice
            double oldSmoothed = entry.getSmoothedPrice();
            double newSmoothed = oldSmoothed + SMOOTH_FACTOR * (rawPrice - oldSmoothed);
            entry.setSmoothedPrice(newSmoothed);

            // Nachfrage langsam Richtung Basis
            double demand = entry.getDemandFactor();
            demand = demand + 0.05 * (1.0 - demand);
            entry.setDemandFactor(demand);

            // Tageswerte zurücksetzen
            entry.resetDailyCounters();
        }

        repository.save(entries);
    }


    // Menü

    public void openMenu(Scanner sc, Inventory inventory, Balance balance,
                         InventoryRepository invRepo) {
        while (true) {
            System.out.println("=== Markt ===");
            System.out.println("Kontostand: " + String.format("%.2f", balance.getBalance()) + " €");
            System.out.println(" ");
            System.out.println("1) Verkaufen");
            System.out.println("2) Kaufen");
            System.out.println("3) Preisübersicht");
            System.out.println("0) Zurück");
            System.out.print("Auswahl: ");

            String input = sc.nextLine().trim();
            System.out.println();

            if (input.equals("0")) return;
            else if (input.equals("1")) openSellMenu(sc, inventory, balance, invRepo);
            else if (input.equals("2")) openBuyMenu(sc, inventory, balance, invRepo);
            else if (input.equals("3")) showPriceOverview(inventory);
            else System.out.println("Ungültige Auswahl.");
        }
    }

    // Verkaufsmenü
    private void openSellMenu(Scanner sc, Inventory inventory, Balance balance,
                              InventoryRepository invRepo) {
        // Nur Items im Lager anzeigen
        List<String> available = new ArrayList<>();
        for (String id : inventory.getItemIdsSorted()) {
            if (inventory.getAmount(id) > 0) {
                available.add(id);
            }
        }

        if (available.isEmpty()) {
            System.out.println("Lager ist leer, nichts zu verkaufen.");
            return;
        }

        System.out.println("=== Verkaufen ===");
        System.out.println("  #  Name                       Bestand   Preis/Stk");
        System.out.println("  ---------------------------------------------------");
        for (int i = 0; i < available.size(); i++) {
            String id          = available.get(i);
            String name        = inventory.getDisplayName(id);
            int    amount      = inventory.getAmount(id);
            double pricePerUnit = getSellPrice(id, inventory);
            System.out.printf("  %2d) %-26s %5d     %6.2f €%n",
                    i + 1, name, amount, pricePerUnit);
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > available.size()) {
            System.out.println("Ungültige Auswahl.");
            return;
        }

        String chosenId = available.get(choice - 1);
        int maxAmount   = inventory.getAmount(chosenId);
        System.out.print("Menge (max " + maxAmount + "): ");
        int amount = readInt(sc);
        if (amount <= 0 || amount > maxAmount) {
            System.out.println("Ungültige Menge.");
            return;
        }

        double earned = sellToMarket(chosenId, amount, inventory, balance);
        if (earned < 0) {
            System.out.println("Verkauf fehlgeschlagen.");
        } else {
            invRepo.save(inventory);
            System.out.printf("Verkauft: %dx %s  →  +%.2f €%n",
                    amount, inventory.getDisplayName(chosenId), earned);
            System.out.printf("Neuer Kontostand: %.2f €%n", balance.getBalance());
        }
    }

    // Kaufmenü
    private void openBuyMenu(Scanner sc, Inventory inventory, Balance balance,
                             InventoryRepository invRepo) {
        List<ItemDefinition> allItems = catalog.getAllSorted();

        System.out.println("=== Kaufen ===");
        System.out.println("  #  Name                       Kaufpreis   Im Lager");
        System.out.println("  -----------------------------------------------------");
        for (int i = 0; i < allItems.size(); i++) {
            ItemDefinition def  = allItems.get(i);
            String         id   = def.getItemId();
            double price        = getBuyPrice(id, inventory);
            int    inStock      = inventory.getAmount(id);
            System.out.printf("  %2d) %-26s %8.2f €   %5d%n",
                    i + 1, def.getDisplayName(), price, inStock);
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > allItems.size()) {
            System.out.println("Ungültige Auswahl.");
            return;
        }

        ItemDefinition chosen = allItems.get(choice - 1);
        System.out.print("Menge: ");
        int amount = readInt(sc);
        if (amount <= 0) {
            System.out.println("Ungültige Menge.");
            return;
        }

        double total = buyFromMarket(chosen.getItemId(), amount, inventory, balance);
        if (total < 0) {
            System.out.println("Kauf fehlgeschlagen. (Zu wenig Geld oder Lager voll?)");
        } else {
            invRepo.save(inventory);
            System.out.printf("Gekauft: %dx %s  →  -%.2f €%n",
                    amount, chosen.getDisplayName(), total);
            System.out.printf("Neuer Kontostand: %.2f €%n", balance.getBalance());
        }
    }

    // Preisübersicht
    private void showPriceOverview(Inventory inventory) {
        System.out.println("=== Preisübersicht ===");
        System.out.printf("  %-26s  %8s  %8s  %8s  %8s%n",
                "Name", "Basis", "Kaufen", "Verkaufen", "Nachfrage");
        System.out.println("  " + "-".repeat(70));

        List<ItemDefinition> allItems = catalog.getAllSorted();
        for (ItemDefinition def : allItems) {
            String id        = def.getItemId();
            double base      = def.getBasePrice();
            double buy       = getBuyPrice(id, inventory);
            double sell      = getSellPrice(id, inventory);
            MarketEntry entry = getOrCreate(id);
            double demand    = entry.getDemandFactor();

            // Trend-Pfeil: steigend, fallend, neutral
            String trend;
            if (demand > 1.05)       trend = "↑";
            else if (demand < 0.95)  trend = "↓";
            else                     trend = "–";

            System.out.printf("  %-26s  %7.2f€  %7.2f€  %8.2f€  %6.2f %s%n",
                    def.getDisplayName(), base, buy, sell, demand, trend);
        }
        System.out.println();
    }

    // Hilfsmethoden

    // Gibt bestehenden Eintrag zurück oder erstellt einen neuen mit Basispreis.
    private MarketEntry getOrCreate(String itemId) {
        if (!entries.containsKey(itemId)) {
            ItemDefinition def = catalog.get(itemId);
            double base = (def != null) ? def.getBasePrice() : 1.0;
            entries.put(itemId, new MarketEntry(itemId, 1.0, base));
        }
        return entries.get(itemId);
    }

    /**
     * Berechnet den Angebotsfaktor abhängig vom Lagerstand.
     * Voller Lager = Faktor < 1 (sinkender Preis)
     * Leeres Lager = Faktor > 1 (steigender Preis)
     */
    private double calcAngebotsFaktor(String itemId, Inventory inventory) {
        int current = inventory.getAmount(itemId);
        int cap     = inventory.getMaxItemCapacity(itemId);

        if (cap <= 0) return 1.0; // Keine Kapazitätsdaten = neutral

        double ratio = (double) current / cap;
        // ratio=0 = 1.3 (teurer), ratio=1 = 0.7 (günstiger)
        return 1.3 - 0.6 * ratio;
    }

    private int readInt(Scanner sc) {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Bitte eine Zahl eingeben: ");
            }
        }
    }
}
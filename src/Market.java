import java.util.*;

// Markt zum Kaufen und Verkaufen von Waren. Preise aendern sich taeglich
// basierend auf Angebot und Nachfrage, werden aber geglaettet damit sie
// nicht zu sehr springen. Ausserdem gibt es einen Tiershop mit
// taeglich neuem Angebot.
public class Market {

    // Maximale Abweichung vom Basispreis (65 %)
    private static final double MAX_DEVIATION = 0.65;

    // Wie stark Kauf/Verkauf den demandFactor verschiebt
    private static final double DEMAND_SHIFT_PER_ITEM = 0.02;

    // Wie schnell der smooth Preis dem Basispreis folgt (0–1)
    private static final double SMOOTH_FACTOR = 0.30;

    // Standard-Kapazität, wenn ein Item beim Kauf neu angelegt wird
    private static final int DEFAULT_ITEM_CAPACITY = 1000;

    // Tiershop-Konstanten

    // Basispreise pro Tierart
    private static final Map<AnimalType, Double> ANIMAL_BASE_PRICES = new LinkedHashMap<>();
    static {
        ANIMAL_BASE_PRICES.put(AnimalType.CHICKEN,  15.0);
        ANIMAL_BASE_PRICES.put(AnimalType.COW,     2150.0);
        ANIMAL_BASE_PRICES.put(AnimalType.PIG,     215.0);
        ANIMAL_BASE_PRICES.put(AnimalType.SHEEP,    210.0);
        ANIMAL_BASE_PRICES.put(AnimalType.BEE,      295.0);
    }

    // Zufällige Tiernamen
    private static final String[] ANIMAL_NAMES = {
            "Max", "Bruno", "Hansi", "Fritz", "Karl", "Benny", "Otto", "Moritz",
            "Gustav", "Heinrich", "Berta", "Hilde", "Emma", "Klara", "Rosa",
            "Grete", "Liesel", "Anna", "Mathilde", "Frieda", "Emil", "Willi"
    };

    // Internes Datenobjekt für ein Tier im Shop-Angebot
    private static class ShopAnimalEntry {
        final AnimalType type;
        final String name;
        final double price;

        ShopAnimalEntry(AnimalType type, String name, double price) {
            this.type  = type;
            this.name  = name;
            this.price = price;
        }
    }

    private final MarketRepository    repository;
    private final ItemCatalog         catalog;
    private final Map<String, MarketEntry> entries;
    private final Random              random = new Random();

    // Aktuelles Tierangebot im Shop
    private final List<ShopAnimalEntry> animalOffer = new ArrayList<>();

    public Market(MarketRepository repository, ItemCatalog catalog) {
        this.repository = repository;
        this.catalog    = catalog;
        this.entries    = new LinkedHashMap<>();
        this.repository.loadOrCreate(this.entries);
        generateAnimalOffer();
    }

    // Preisberechnung

    public double getBuyPrice(String itemId, Inventory inventory) {
        return getSmoothedPrice(itemId, inventory) * 1.07;
    }

    public double getSellPrice(String itemId, Inventory inventory) {
        return getSmoothedPrice(itemId, inventory) * 0.80 * 1.09;
    }

    private double getSmoothedPrice(String itemId, Inventory inventory) {
        MarketEntry entry = getOrCreate(itemId);
        return entry.getSmoothedPrice();
    }

    // Kaufen / Verkaufen (Waren)

    public double buyFromMarket(String itemId, int amount,
                                Inventory inventory, Balance balance) {
        if (amount <= 0) return -1;

        ItemDefinition def = catalog.get(itemId);
        if (def == null) return -1;

        if (!inventory.itemExists(itemId)) {
            inventory.createItem(itemId, DEFAULT_ITEM_CAPACITY);
        }

        double pricePerUnit = getBuyPrice(itemId, inventory);
        double total        = pricePerUnit * amount;

        if (!balance.withdraw(total)) return -1;

        boolean added = inventory.addItem(itemId, amount);
        if (!added) {
            balance.deposit(total);
            return -1;
        }

        MarketEntry entry = getOrCreate(itemId);
        entry.recordBuy(amount);
        double newDemand = entry.getDemandFactor() - DEMAND_SHIFT_PER_ITEM * amount;
        entry.setDemandFactor(Math.max(0.5, newDemand));

        repository.save(entries);
        return total;
    }

    public double sellToMarket(String itemId, int amount,
                               Inventory inventory, Balance balance) {
        if (amount <= 0) return -1;
        if (!inventory.hasEnough(itemId, amount)) return -1;

        double pricePerUnit = getSellPrice(itemId, inventory);
        double total        = pricePerUnit * amount;

        boolean removed = inventory.removeItem(itemId, amount);
        if (!removed) return -1;

        balance.deposit(total);

        MarketEntry entry = getOrCreate(itemId);
        entry.recordSell(amount);
        double newDemand = entry.getDemandFactor() + DEMAND_SHIFT_PER_ITEM * amount;
        entry.setDemandFactor(Math.min(1.5, newDemand));

        repository.save(entries);
        return total;
    }

    // Tagesabschluss

    public void endOfDay(Inventory inventory) {
        List<ItemDefinition> allItems = catalog.getAllSorted();

        for (ItemDefinition def : allItems) {
            String itemId   = def.getItemId();
            double base     = def.getBasePrice();
            if (base <= 0) continue;

            MarketEntry entry = getOrCreate(itemId);

            double angebotsFaktor = calcAngebotsFaktor(itemId, inventory);
            double rawPrice = base * entry.getDemandFactor() * angebotsFaktor;

            double minPrice = base * (1.0 - MAX_DEVIATION);
            double maxPrice = base * (1.0 + MAX_DEVIATION);
            rawPrice = Math.max(minPrice, Math.min(maxPrice, rawPrice));

            double oldSmoothed = entry.getSmoothedPrice();
            double newSmoothed = oldSmoothed + SMOOTH_FACTOR * (rawPrice - oldSmoothed);
            entry.setSmoothedPrice(newSmoothed);

            double demand = entry.getDemandFactor();
            demand = demand + 0.05 * (1.0 - demand);
            entry.setDemandFactor(demand);

            entry.resetDailyCounters();
        }
        repository.save(entries);
        generateAnimalOffer();         // Tierangebot täglich neu generieren
    }

    // Tiershop – Angebot generieren

    // generiert 3 bis 6 zufaellige Tiere mit leicht schwankenden Preisen
    private void generateAnimalOffer() {
        animalOffer.clear();
        int count = 3 + random.nextInt(4); // 3 bis 6 Tiere
        AnimalType[] types = AnimalType.values();

        for (int i = 0; i < count; i++) {
            AnimalType type = types[random.nextInt(types.length)];
            String name     = ANIMAL_NAMES[random.nextInt(ANIMAL_NAMES.length)];
            double base     = ANIMAL_BASE_PRICES.getOrDefault(type, 100.0);
            double factor   = 0.80 + random.nextDouble() * 0.40; // ±20%
            double price    = Math.round(base * factor * 100.0) / 100.0;

            animalOffer.add(new ShopAnimalEntry(type, name, price));
        }
    }

    // Menü

    public void openMenu(Scanner sc, Inventory inventory, Balance balance,
                         InventoryRepository invRepo, AnimalService animalService) {
        while (true) {
            System.out.println("=== Markt ===");
            System.out.println("Kontostand: " + String.format("%.2f", balance.getBalance()) + " €");
            System.out.println(" ");
            System.out.println("1) Verkaufen");
            System.out.println("2) Kaufen");
            System.out.println("3) Preisübersicht");
            System.out.println("4) Tiere kaufen");
            System.out.println("0) Zurück");
            System.out.print("Auswahl: ");

            String input = sc.nextLine().trim();
            System.out.println();

            if (input.equals("0")) return;
            else if (input.equals("1")) openSellMenu(sc, inventory, balance, invRepo);
            else if (input.equals("2")) openBuyMenu(sc, inventory, balance, invRepo);
            else if (input.equals("3")) showPriceOverview(inventory);
            else if (input.equals("4")) openAnimalShopMenu(sc, balance, animalService);
            else System.out.println("Ungültige Auswahl.");
        }
    }

    // Tiershop-Menü

    private void openAnimalShopMenu(Scanner sc, Balance balance, AnimalService animalService) {
        while (true) {
            System.out.println("=== Tiere kaufen ===");
            System.out.println("Kontostand: " + String.format("%.2f", balance.getBalance()) + " €");
            System.out.println("(Das Angebot wechselt täglich)");
            System.out.println();

            if (animalOffer.isEmpty()) {
                System.out.println("Heute sind keine Tiere verfügbar. Komm morgen wieder!");
                System.out.println();
                System.out.println("0) Zurück");
                System.out.print("Auswahl: ");
                sc.nextLine();
                return;
            }

            System.out.printf("  %-4s %-12s %-20s %s%n", "#", "Tierart", "Name", "Preis");
            System.out.println("  " + "-".repeat(52));
            for (int i = 0; i < animalOffer.size(); i++) {
                ShopAnimalEntry e = animalOffer.get(i);
                System.out.printf("  %2d) %-12s %-20s %8.2f €%n",
                        i + 1,
                        e.type.name(),
                        e.name,
                        e.price);
            }

            System.out.println();
            System.out.println("0) Zurück");
            System.out.print("Auswahl: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return;

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Bitte eine Zahl eingeben.");
                continue;
            }

            if (choice < 1 || choice > animalOffer.size()) {
                System.out.println("Ungültige Auswahl.");
                continue;
            }

            ShopAnimalEntry chosen = animalOffer.get(choice - 1);

            System.out.printf("Kaufen: %s (%s) für %.2f €? (ja/nein): ",
                    chosen.name, chosen.type.name(), chosen.price);
            String confirm = sc.nextLine().trim();

            if (!confirm.equalsIgnoreCase("ja") && !confirm.equalsIgnoreCase("j")) {
                System.out.println("Kauf abgebrochen.");
                continue;
            }

            if (!balance.withdraw(chosen.price)) {
                System.out.println("Nicht genug Geld auf dem Konto.");
                continue;
            }

            Animal bought = animalService.create(chosen.type, chosen.name);
            animalOffer.remove(choice - 1);

            System.out.printf("Gekauft: %s (%s, ID: %d)  −%.2f €%n",
                    bought.getName(), bought.getType().name(),
                    bought.getId(), chosen.price);
            System.out.printf("Neuer Kontostand: %.2f €%n", balance.getBalance());
            System.out.println();
        }
    }

    // Warenmenüs

    private void openSellMenu(Scanner sc, Inventory inventory, Balance balance,
                              InventoryRepository invRepo) {
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
        System.out.printf("  %-2s  %-26s  %-6s  %-8s  %s%n", "#", "Name", "Einheit", "Bestand", "Preis/Stk");
        System.out.println("  " + "-".repeat(58));
        for (int i = 0; i < available.size(); i++) {
            String id           = available.get(i);
            String name         = inventory.getDisplayName(id);
            String unit         = inventory.getUnit(id);
            int    amount       = inventory.getAmount(id);
            double pricePerUnit = getSellPrice(id, inventory);
            System.out.printf("  %2d) %-26s  %-6s  %5d     %6.2f €%n",
                    i + 1, name, unit, amount, pricePerUnit);
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
        String unit     = inventory.getUnit(chosenId);
        System.out.print("Menge in " + unit + " (max " + maxAmount + "): ");
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
            System.out.printf("Verkauft: %d %s %s  →  +%.2f €%n",
                    amount, unit, inventory.getDisplayName(chosenId), earned);
            System.out.printf("Neuer Kontostand: %.2f €%n", balance.getBalance());
        }
    }

    private void openBuyMenu(Scanner sc, Inventory inventory, Balance balance,
                             InventoryRepository invRepo) {
        List<ItemDefinition> allItems = catalog.getAllSorted();

        System.out.println("=== Kaufen ===");
        System.out.printf("  %-2s  %-26s  %-6s  %-10s  %s%n", "#", "Name", "Einheit", "Kaufpreis", "Im Lager");
        System.out.println("  " + "-".repeat(62));
        for (int i = 0; i < allItems.size(); i++) {
            ItemDefinition def  = allItems.get(i);
            String         id   = def.getItemId();
            String         unit = def.getUnit();
            double price        = getBuyPrice(id, inventory);
            int    inStock      = inventory.getAmount(id);
            System.out.printf("  %2d) %-26s  %-6s  %8.2f €   %5d%n",
                    i + 1, def.getDisplayName(), unit, price, inStock);
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
        String unit = chosen.getUnit();
        System.out.print("Menge in " + unit + ": ");
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
            System.out.printf("Gekauft: %d %s %s  →  -%.2f €%n",
                    amount, unit, chosen.getDisplayName(), total);
            System.out.printf("Neuer Kontostand: %.2f €%n", balance.getBalance());
        }
    }

    private void showPriceOverview(Inventory inventory) {
        System.out.println("=== Preisübersicht ===");
        System.out.printf("  %-26s  %-6s  %8s  %8s  %9s  %8s%n",
                "Name", "Einheit", "Basis", "Kaufen", "Verkaufen", "Nachfrage");
        System.out.println("  " + "-".repeat(78));

        List<ItemDefinition> allItems = catalog.getAllSorted();
        for (ItemDefinition def : allItems) {
            String id        = def.getItemId();
            String unit      = def.getUnit();
            double base      = def.getBasePrice();
            double buy       = getBuyPrice(id, inventory);
            double sell      = getSellPrice(id, inventory);
            MarketEntry entry = getOrCreate(id);
            double demand    = entry.getDemandFactor();

            String trend;
            if (demand > 1.05)       trend = "↑";
            else if (demand < 0.95)  trend = "↓";
            else                     trend = "–";

            System.out.printf("  %-26s  %-6s  %7.2f€  %7.2f€  %8.2f€  %6.2f %s%n",
                    def.getDisplayName(), unit, base, buy, sell, demand, trend);
        }
        System.out.println();
    }

    // Hilfsmethoden

    private MarketEntry getOrCreate(String itemId) {
        if (!entries.containsKey(itemId)) {
            ItemDefinition def = catalog.get(itemId);
            double base = (def != null) ? def.getBasePrice() : 1.0;
            entries.put(itemId, new MarketEntry(itemId, 1.0, base));
        }
        return entries.get(itemId);
    }

    private double calcAngebotsFaktor(String itemId, Inventory inventory) {
        int current = inventory.getAmount(itemId);
        int cap     = inventory.getMaxItemCapacity(itemId);

        if (cap <= 0) return 1.0;

        double ratio = (double) current / cap;
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
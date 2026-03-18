import java.util.*;

/**
 * Hofladen: Der Farmer kann eigene Produkte zu selbst gewählten Preisen anbieten.
 * Beim Tagesabschluss (endOfDay) wird berechnet, wie viele Kunden kaufen.
 * Preislogik:
 *   - ratio = farmerPrice / basePrice
 *   - ratio <= 1.0  → hohe Nachfrage (bis zu MAX_CUSTOMERS Kunden)
 *   - ratio > 1.0   → Nachfrage sinkt linear
 *   - ratio >= 5.0  → niemand kauft mehr
 */
public class FarmShop {

    // Artikel die nicht im Hofladen angeboten werden können
    private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
            "slurry", "water", "diesel", "wheat_seeds", "corn_seeds",
            "barley_seeds", "rapeseed_seeds", "fertilizer", "biogas",
            "compost", "packaging", "label", "straw", "animal_feed",
            "hay", "corn_meal", "protein_mix", "silage"
    ));

    // Maximale Kundenanzahl pro Artikel pro Tag bei optimalem Preis
    private static final int MAX_CUSTOMERS = 17;

    // Ab 6,7 * Basispreis kauft niemand mehr
    private static final double MAX_PRICE_RATIO = 6.7;

    public static class ShopEntry {
        private final String itemId;
        private double farmerPrice;
        private boolean active;

        public ShopEntry(String itemId, double farmerPrice) {
            this.itemId = itemId;
            this.farmerPrice = farmerPrice;
            this.active = true;
        }

        public String getItemId()        { return itemId; }
        public double getFarmerPrice()   { return farmerPrice; }
        public boolean isActive()        { return active; }

        public void setFarmerPrice(double price) { if (price > 0) this.farmerPrice = price; }
        public void setActive(boolean active)    { this.active = active; }
    }

    // Ergebnis eines Tagesabschlusses für die Anzeige
    public static class SaleResult {
        public final String itemId;
        public final String displayName;
        public final int unitsSold;
        public final double revenue;
        public final String reason; // null = normaler Verkauf, sonst Grund warum nichts verkauft

        public SaleResult(String itemId, String displayName, int unitsSold, double revenue, String reason) {
            this.itemId      = itemId;
            this.displayName = displayName;
            this.unitsSold   = unitsSold;
            this.revenue     = revenue;
            this.reason      = reason;
        }
    }

    private final Map<String, ShopEntry> entries = new LinkedHashMap<>();
    private final ItemCatalog catalog;
    private final Random random = new Random();

    public FarmShop(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    // Gibt true zurück wenn der Artikel im Hofladen angeboten werden darf
    public boolean isAllowed(String itemId) {
        if (itemId == null) return false;
        if (BLACKLIST.contains(itemId)) return false;
        return catalog.exists(itemId);
    }

    public boolean addEntry(String itemId, double price) {
        if (!isAllowed(itemId)) return false;
        if (price <= 0) return false;
        entries.put(itemId, new ShopEntry(itemId, price));
        return true;
    }

    public boolean removeEntry(String itemId) {
        return entries.remove(itemId) != null;
    }

    public boolean setPrice(String itemId, double price) {
        ShopEntry e = entries.get(itemId);
        if (e == null || price <= 0) return false;
        e.setFarmerPrice(price);
        return true;
    }

    public Map<String, ShopEntry> getEntries() {
        return entries;
    }

    //Tagesabschluss: Berechnet Kundenverkäufe für alle aktiven Angebote.
    // Zieht verkaufte Waren aus dem Lager, zahlt Gewinn auf Konto ein.
    public List<SaleResult> endOfDay(Inventory inventory, Balance balance) {
        List<SaleResult> results = new ArrayList<>();

        for (ShopEntry entry : entries.values()) {
            if (!entry.isActive()) continue;

            String itemId     = entry.getItemId();
            String name       = inventory.getDisplayName(itemId);
            double farmerPrice = entry.getFarmerPrice();

            ItemDefinition def = catalog.get(itemId);
            if (def == null) continue;

            double basePrice = def.getBasePrice();
            double ratio     = farmerPrice / basePrice;

            // Preis zu hoch = niemand kauft
            if (ratio >= MAX_PRICE_RATIO) {
                results.add(new SaleResult(itemId, name, 0, 0,
                        "Preis zu hoch (>" + (int)(MAX_PRICE_RATIO) + "x Basispreis) – keine Kunden"));
                continue;
            }

            // Kein Bestand
            int stock = inventory.getAmount(itemId);
            if (stock <= 0) {
                results.add(new SaleResult(itemId, name, 0, 0, "Nicht auf Lager"));
                continue;
            }

            // Kaufwahrscheinlichkeit berechnen: linear von 1.0 (ratio=0) bis 0.0 (ratio=MAX)
            double buyChance = Math.max(0.0, 1.0 - (ratio / MAX_PRICE_RATIO));

            // Anzahl potentieller Kunden sinkt auch mit steigendem Preis
            int potentialCustomers = (int) Math.round(MAX_CUSTOMERS * buyChance);
            if (potentialCustomers < 1 && ratio < MAX_PRICE_RATIO) potentialCustomers = 1;

            // Jeder Kunde würfelt ob er kauft
            int unitsSold = 0;
            for (int i = 0; i < potentialCustomers; i++) {
                if (random.nextDouble() < buyChance) {
                    unitsSold++;
                }
            }

            // Maximal so viel wie auf Lager
            unitsSold = Math.min(unitsSold, stock);

            if (unitsSold == 0) {
                results.add(new SaleResult(itemId, name, 0, 0, "Heute kein Interesse"));
                continue;
            }

            // Aus Lager nehmen und Geld einzahlen
            inventory.removeItem(itemId, unitsSold);
            double revenue = unitsSold * farmerPrice;
            balance.deposit(revenue);

            results.add(new SaleResult(itemId, name, unitsSold, revenue, null));
        }

        return results;
    }

    // === Menü ===

    public void openMenu(Scanner sc, Inventory inventory, Balance balance, InventoryRepository repo) {
        while (true) {
            System.out.println("=== Hofladen ===");
            System.out.println("Kontostand: " + String.format("%.2f", balance.getBalance()) + " €");
            System.out.println();

            if (entries.isEmpty()) {
                System.out.println("Noch keine Artikel im Angebot.");
            } else {
                System.out.printf("  %-2s  %-24s  %-8s  %-10s  %s%n",
                        "#", "Artikel", "Lager", "Dein Preis", "Basispreis");
                System.out.println("  " + "-".repeat(58));

                int i = 1;
                for (ShopEntry e : entries.values()) {
                    String name  = inventory.getDisplayName(e.getItemId());
                    int stock    = inventory.getAmount(e.getItemId());
                    double base  = getBasePrice(e.getItemId());
                    double ratio = e.getFarmerPrice() / base;
                    String warn  = ratio >= MAX_PRICE_RATIO ? " !!ZU HOCH!!" : ratio > 2.0 ? " (wenig Nachfrage)" : "";
                    System.out.printf("  %2d) %-24s  %5d     %7.2f €   %7.2f €%s%n",
                            i++, name, stock, e.getFarmerPrice(), base, warn);
                }
            }

            System.out.println();
            System.out.println("1) Artikel hinzufügen");
            System.out.println("2) Preis ändern");
            System.out.println("3) Artikel entfernen");
            System.out.println("0) Zurück");
            System.out.print("Auswahl: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return;
            else if (input.equals("1")) openAddMenu(sc, inventory);
            else if (input.equals("2")) openPriceMenu(sc, inventory);
            else if (input.equals("3")) openRemoveMenu(sc, inventory);
            else System.out.println("Ungültige Auswahl.");
        }
    }

    private void openAddMenu(Scanner sc, Inventory inventory) {
        // Alle Artikel die im Lager sind, erlaubt sind und noch nicht im Angebot
        List<String> available = new ArrayList<>();
        for (String id : inventory.getItemIdsSorted()) {
            if (isAllowed(id) && !entries.containsKey(id)) {
                available.add(id);
            }
        }

        if (available.isEmpty()) {
            System.out.println("Keine weiteren Artikel verfügbar.");
            return;
        }

        System.out.println("=== Artikel hinzufügen ===");
        for (int i = 0; i < available.size(); i++) {
            String id   = available.get(i);
            double base = getBasePrice(id);
            System.out.printf("  %2d) %-24s  Basispreis: %.2f €%n",
                    i + 1, inventory.getDisplayName(id), base);
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > available.size()) { System.out.println("Ungültige Auswahl."); return; }

        String chosen   = available.get(choice - 1);
        double base     = getBasePrice(chosen);
        System.out.printf("Dein Verkaufspreis (Basispreis: %.2f €): ", base);
        double price = readDouble(sc);
        if (price <= 0) { System.out.println("Ungültiger Preis."); return; }

        addEntry(chosen, price);
        System.out.println(inventory.getDisplayName(chosen) + " zum Preis von " + String.format("%.2f", price) + " € im Hofladen eingestellt.");
    }

    private void openPriceMenu(Scanner sc, Inventory inventory) {
        if (entries.isEmpty()) { System.out.println("Keine Artikel im Angebot."); return; }

        List<ShopEntry> list = new ArrayList<>(entries.values());
        System.out.println("=== Preis ändern ===");
        for (int i = 0; i < list.size(); i++) {
            ShopEntry e = list.get(i);
            System.out.printf("  %2d) %-24s  aktuell: %.2f €%n",
                    i + 1, inventory.getDisplayName(e.getItemId()), e.getFarmerPrice());
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > list.size()) { System.out.println("Ungültige Auswahl."); return; }

        ShopEntry chosen = list.get(choice - 1);
        double base = getBasePrice(chosen.getItemId());
        System.out.printf("Neuer Preis für %s (Basispreis: %.2f €): ",
                inventory.getDisplayName(chosen.getItemId()), base);
        double price = readDouble(sc);
        if (price <= 0) { System.out.println("Ungültiger Preis."); return; }

        chosen.setFarmerPrice(price);
        System.out.println("Preis aktualisiert.");
    }

    private void openRemoveMenu(Scanner sc, Inventory inventory) {
        if (entries.isEmpty()) { System.out.println("Keine Artikel im Angebot."); return; }

        List<ShopEntry> list = new ArrayList<>(entries.values());
        System.out.println("=== Artikel entfernen ===");
        for (int i = 0; i < list.size(); i++) {
            System.out.printf("  %2d) %s%n", i + 1,
                    inventory.getDisplayName(list.get(i).getItemId()));
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > list.size()) { System.out.println("Ungültige Auswahl."); return; }

        String removed = list.get(choice - 1).getItemId();
        removeEntry(removed);
        System.out.println(inventory.getDisplayName(removed) + " aus dem Hofladen entfernt.");
    }

    // Hilfsmethoden

    private double getBasePrice(String itemId) {
        ItemDefinition def = catalog.get(itemId);
        return (def != null) ? def.getBasePrice() : 1.0;
    }

    private int readInt(Scanner sc) {
        while (true) {
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.print("Bitte Zahl eingeben: "); }
        }
    }

    private double readDouble(Scanner sc) {
        while (true) {
            try {
                double v = Double.parseDouble(sc.nextLine().trim().replace(',', '.'));
                if (v > 0) return v;
                System.out.print("Bitte positive Zahl eingeben: ");
            } catch (NumberFormatException e) {
                System.out.print("Bitte Zahl eingeben: ");
            }
        }
    }
}
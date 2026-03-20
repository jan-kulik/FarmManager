import java.util.*;

// Hofladen, hier kann der Spieler eigene Produkte zu selbstgewaehlten Preisen verkaufen.
// Je naeher der Preis am Basispreis liegt desto mehr Kunden kommen.
// Ab dem ~6.7-fachen Basispreis kauft keiner mehr.
public class FarmShop {

    // Artikel die nicht im Hofladen angeboten werden koennen (z.B. Rohstoffe, Futter)
    private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
            "slurry", "water", "diesel", "wheat_seeds", "corn_seeds",
            "barley_seeds", "rapeseed_seeds", "fertilizer", "biogas",
            "compost", "packaging", "label", "straw", "animal_feed",
            "hay", "corn_meal", "protein_mix", "silage"
    ));

    // Maximale Kundenanzahl pro Artikel pro Tag bei optimalem Preis
    private static final int MAX_CUSTOMERS = 17;

    // Ab diesem Vielfachen des Basispreises kauft niemand mehr
    private static final double MAX_PRICE_RATIO = 6.7;

    // Ein einzelnes Angebot im Hofladen
    public static class ShopEntry {
        private final String itemId;
        private double farmerPrice;
        private boolean active;

        public ShopEntry(String itemId, double farmerPrice) {
            this.itemId      = itemId;
            this.farmerPrice = farmerPrice;
            this.active      = true;
        }

        public String  getItemId()      { return itemId; }
        public double  getFarmerPrice() { return farmerPrice; }
        public boolean isActive()       { return active; }

        public void setFarmerPrice(double price) { if (price > 0) this.farmerPrice = price; }
        public void setActive(boolean active)    { this.active = active; }
    }

    // Ergebnis eines Tagesabschlusses
    public static class SaleResult {
        public final String itemId;
        public final String displayName;
        public final int    unitsSold;
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
    private final ItemCatalog            catalog;
    private final FarmShopRepository     repository;
    private final Random                 random = new Random();

    public FarmShop(ItemCatalog catalog, FarmShopRepository repository) {
        this.catalog    = catalog;
        this.repository = repository;
        // Gespeicherte Angebote beim Start laden
        this.repository.loadOrCreate(this.entries, catalog);
    }

    // prueft ob ein Artikel ueberhaupt im Hofladen angeboten werden darf
    public boolean isAllowed(String itemId) {
        if (itemId == null) return false;
        if (BLACKLIST.contains(itemId)) return false;
        return catalog.exists(itemId);
    }

    public boolean addEntry(String itemId, double price) {
        if (!isAllowed(itemId)) return false;
        if (price <= 0) return false;
        entries.put(itemId, new ShopEntry(itemId, price));
        repository.save(entries);
        return true;
    }

    public boolean removeEntry(String itemId) {
        boolean removed = entries.remove(itemId) != null;
        if (removed) repository.save(entries);
        return removed;
    }

    public boolean setPrice(String itemId, double price) {
        ShopEntry e = entries.get(itemId);
        if (e == null || price <= 0) return false;
        e.setFarmerPrice(price);
        repository.save(entries);
        return true;
    }

    public Map<String, ShopEntry> getEntries() {
        return entries;
    }

    // Tagesabschluss, berechnet fuer jeden Artikel wie viele Kunden kaufen
    // und schreibt die Einnahmen direkt aufs Konto.
    public List<SaleResult> endOfDay(Inventory inventory, Balance balance) {
        List<SaleResult> results = new ArrayList<>();

        for (ShopEntry entry : entries.values()) {
            if (!entry.isActive()) continue;

            String itemId      = entry.getItemId();
            String name        = inventory.getDisplayName(itemId);
            double farmerPrice = entry.getFarmerPrice();

            ItemDefinition def = catalog.get(itemId);
            if (def == null) continue;

            double basePrice = def.getBasePrice();
            double ratio     = farmerPrice / basePrice;

            // Preis zu hoch: niemand kauft
            if (ratio >= MAX_PRICE_RATIO) {
                results.add(new SaleResult(itemId, name, 0, 0,
                        "Preis zu hoch (>" + (int)(MAX_PRICE_RATIO) + "x Basispreis) - keine Kunden"));
                continue;
            }

            // Kein Bestand vorhanden
            int stock = inventory.getAmount(itemId);
            if (stock <= 0) {
                results.add(new SaleResult(itemId, name, 0, 0, "Nicht auf Lager"));
                continue;
            }

            // Kaufwahrscheinlichkeit: linear von 1.0 (ratio=0) bis 0.0 (ratio=MAX)
            double buyChance = Math.max(0.0, 1.0 - (ratio / MAX_PRICE_RATIO));

            // Potenzielle Kundenzahl sinkt ebenfalls mit steigendem Preis
            int potentialCustomers = (int) Math.round(MAX_CUSTOMERS * buyChance);
            if (potentialCustomers < 1 && ratio < MAX_PRICE_RATIO) potentialCustomers = 1;

            // Jeder Kunde wuerfelt ob er kauft
            int unitsSold = 0;
            for (int i = 0; i < potentialCustomers; i++) {
                if (random.nextDouble() < buyChance) {
                    unitsSold++;
                }
            }

            // Maximal so viel wie im Lager vorhanden
            unitsSold = Math.min(unitsSold, stock);

            if (unitsSold == 0) {
                results.add(new SaleResult(itemId, name, 0, 0, "Heute kein Interesse"));
                continue;
            }

            // Lager reduzieren und Einnahmen verbuchen
            inventory.removeItem(itemId, unitsSold);
            double revenue = unitsSold * farmerPrice;
            balance.deposit(revenue);

            results.add(new SaleResult(itemId, name, unitsSold, revenue, null));
        }

        // Stand nach Tagesabschluss speichern
        repository.save(entries);
        return results;
    }

    // Menue

    public void openMenu(Scanner sc, Inventory inventory, Balance balance, InventoryRepository repo) {
        while (true) {
            System.out.println("=== Hofladen ===");
            System.out.println("Kontostand: " + String.format("%.2f", balance.getBalance()) + " EUR");
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
                    int    stock = inventory.getAmount(e.getItemId());
                    double base  = getBasePrice(e.getItemId());
                    double ratio = e.getFarmerPrice() / base;
                    // Warnung wenn Preis zu hoch oder sehr unattraktiv
                    String warn  = ratio >= MAX_PRICE_RATIO ? " !!ZU HOCH!!" : ratio > 2.0 ? " (wenig Nachfrage)" : "";
                    System.out.printf("  %2d) %-24s  %5d     %7.2f EUR   %7.2f EUR%s%n",
                            i++, name, stock, e.getFarmerPrice(), base, warn);
                }
            }

            System.out.println();
            System.out.println("1) Artikel hinzufuegen");
            System.out.println("2) Preis aendern");
            System.out.println("3) Artikel entfernen");
            System.out.println("0) Zurueck");
            System.out.print("Auswahl: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return;
            else if (input.equals("1")) openAddMenu(sc, inventory);
            else if (input.equals("2")) openPriceMenu(sc, inventory);
            else if (input.equals("3")) openRemoveMenu(sc, inventory);
            else System.out.println("Ungueltige Auswahl.");
        }
    }

    private void openAddMenu(Scanner sc, Inventory inventory) {
        // Nur Artikel anzeigen die erlaubt und noch nicht im Angebot sind
        List<String> available = new ArrayList<>();
        for (String id : inventory.getItemIdsSorted()) {
            if (isAllowed(id) && !entries.containsKey(id)) {
                available.add(id);
            }
        }

        if (available.isEmpty()) {
            System.out.println("Keine weiteren Artikel verfuegbar.");
            return;
        }

        System.out.println("=== Artikel hinzufuegen ===");
        for (int i = 0; i < available.size(); i++) {
            String id   = available.get(i);
            double base = getBasePrice(id);
            System.out.printf("  %2d) %-24s  Basispreis: %.2f EUR%n",
                    i + 1, inventory.getDisplayName(id), base);
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > available.size()) { System.out.println("Ungueltige Auswahl."); return; }

        String chosen = available.get(choice - 1);
        double base   = getBasePrice(chosen);
        System.out.printf("Dein Verkaufspreis (Basispreis: %.2f EUR): ", base);
        double price = readDouble(sc);
        if (price <= 0) { System.out.println("Ungueltiger Preis."); return; }

        addEntry(chosen, price);
        System.out.println(inventory.getDisplayName(chosen) + " zum Preis von "
                + String.format("%.2f", price) + " EUR im Hofladen eingestellt.");
    }

    private void openPriceMenu(Scanner sc, Inventory inventory) {
        if (entries.isEmpty()) { System.out.println("Keine Artikel im Angebot."); return; }

        List<ShopEntry> list = new ArrayList<>(entries.values());
        System.out.println("=== Preis aendern ===");
        for (int i = 0; i < list.size(); i++) {
            ShopEntry e = list.get(i);
            System.out.printf("  %2d) %-24s  aktuell: %.2f EUR%n",
                    i + 1, inventory.getDisplayName(e.getItemId()), e.getFarmerPrice());
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice = readInt(sc);
        if (choice == 0) return;
        if (choice < 1 || choice > list.size()) { System.out.println("Ungueltige Auswahl."); return; }

        ShopEntry chosen = list.get(choice - 1);
        double base = getBasePrice(chosen.getItemId());
        System.out.printf("Neuer Preis fuer %s (Basispreis: %.2f EUR): ",
                inventory.getDisplayName(chosen.getItemId()), base);
        double price = readDouble(sc);
        if (price <= 0) { System.out.println("Ungueltiger Preis."); return; }

        setPrice(chosen.getItemId(), price);
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
        if (choice < 1 || choice > list.size()) { System.out.println("Ungueltige Auswahl."); return; }

        String removed = list.get(choice - 1).getItemId();
        removeEntry(removed);
        System.out.println(inventory.getDisplayName(removed) + " aus dem Hofladen entfernt.");
    }

    // Hilfsmethoden

    // Gibt den Basispreis zurueck
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
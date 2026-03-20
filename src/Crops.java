import java.util.*;

// Verwaltet alle bepflanzten Felder. Felder wachsen taeglich um einen Tag
// und koennen geerntet werden sobald die noetige Wachstumszeit erreicht ist.
public class Crops {

    public static class CropEntry {
        private final int id;
        private final String seedItemId;
        private final String harvestItemId;
        private final int amount;
        private final int growthDaysNeeded;
        private int growthDays;

        public CropEntry(int id, String seedItemId, String harvestItemId,
                         int amount, int growthDaysNeeded, int growthDays) {
            this.id = id;
            this.seedItemId = seedItemId;
            this.harvestItemId = harvestItemId;
            this.amount = Math.max(1, amount);
            this.growthDaysNeeded = Math.max(1, growthDaysNeeded);
            this.growthDays = Math.max(0, growthDays);
        }

        public int getId()              { return id; }
        public String getSeedItemId()   { return seedItemId; }
        public String getHarvestItemId(){ return harvestItemId; }
        public int getAmount()          { return amount; }
        public int getGrowthDays()      { return growthDays; }
        public int getGrowthDaysNeeded(){ return growthDaysNeeded; }

        public void growOneDay() { growthDays++; }

        public boolean isReady() { return growthDays >= growthDaysNeeded; }

        public String toCsvRow() {
            return id + "," + seedItemId + "," + harvestItemId + ","
                    + amount + "," + growthDaysNeeded + "," + growthDays;
        }
    }

    private static final String[][] CROP_TYPES = {
            { "wheat_seeds",    "wheat",    "7",  "10" },
            { "corn_seeds",     "corn",     "10", "8"  },
            { "barley_seeds",   "barley",   "6",  "9"  },
            { "rapeseed_seeds", "rapeseed", "12", "7"  },
    };

    private final CropRepository repository;
    private final List<CropEntry> crops;
    private ItemCatalog catalog;

    public Crops(CropRepository repository) {
        this.repository = repository;
        this.crops = new ArrayList<>();
        this.repository.loadOrCreate(this.crops);
        sortById();
    }

    public void setCatalog(ItemCatalog catalog) {
        this.catalog = catalog;
    }

    public List<CropEntry> getCrops() {
        return crops;
    }

    public void endDayAll() {
        for (CropEntry c : crops) {
            c.growOneDay();
        }
        repository.save(crops);
    }

    // Ab hier Menüs

    public void openBrowser(Scanner sc, Inventory inventory, InventoryRepository repo) {
        while (true) {
            System.out.println("=== Felder & Ackerbau ===");
            System.out.println(" ");

            if (crops.isEmpty()) {
                System.out.println("Keine Felder bepflanzt.");
            } else {
                for (int i = 0; i < crops.size(); i++) {
                    CropEntry c = crops.get(i);
                    String name = inventory.getDisplayName(c.getSeedItemId());
                    String status = c.isReady()
                            ? "ERNTEREIF"
                            : ("Tag " + c.getGrowthDays() + "/" + c.getGrowthDaysNeeded());
                    System.out.println((i + 1) + ") [ID:" + c.getId() + "]  "
                            + c.getAmount() + "x " + name
                            + "  |  " + status);
                }
            }

            System.out.println(" ");
            System.out.println("1) Pflanzen");
            System.out.println("2) Ernten");
            System.out.println("0) Zurück");
            System.out.print("Auswahl: ");
            String input = sc.nextLine().trim();

            if (input.equals("0")) return;
            else if (input.equals("1")) openPlantMenu(sc, inventory, repo);
            else if (input.equals("2")) openHarvestMenu(sc, inventory, repo);
            else System.out.println("Ungültige Auswahl.");
        }
    }

    private void openPlantMenu(Scanner sc, Inventory inventory, InventoryRepository repo) {
        System.out.println("=== Pflanzen ===");

        // Nur Samen anzeigen die im Lager vorhanden sind
        List<String[]> available = new ArrayList<>();
        for (String[] type : CROP_TYPES) {
            int seedAmount = inventory.getAmount(type[0]);
            if (seedAmount > 0) {
                available.add(type);
                System.out.println(available.size() + ") " + inventory.getDisplayName(type[0])
                        + "  |  Samen: " + seedAmount
                        + "  |  Wächst in " + type[2] + " Tagen"
                        + "  |  Ergibt: " + type[3] + "x " + inventory.getDisplayName(type[1]) + " pro Samen");
            }
        }

        if (available.isEmpty()) {
            System.out.println("Keine Samen im Lager vorhanden.");
            return;
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice;
        try { choice = Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Bitte eine Zahl eingeben."); return; }

        if (choice == 0) return;
        if (choice < 1 || choice > available.size()) { System.out.println("Ungültige Auswahl."); return; }

        String[] chosen = available.get(choice - 1);
        int maxSeeds = inventory.getAmount(chosen[0]);

        System.out.print("Anzahl pflanzen (max " + maxSeeds + "): ");
        int amount;
        try { amount = Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Bitte eine Zahl eingeben."); return; }

        if (amount <= 0 || amount > maxSeeds) {
            System.out.println("Ungültige Menge.");
            return;
        }

        // Samen verbrauchen
        inventory.removeItem(chosen[0], amount);
        repo.save(inventory);

        // Feld anlegen
        int id = repository.createId();
        int growthDays = Integer.parseInt(chosen[2]);
        CropEntry entry = new CropEntry(id, chosen[0], chosen[1], amount, growthDays, 0);
        crops.add(entry);
        sortById();
        repository.save(crops);

        System.out.println(amount + "x " + inventory.getDisplayName(chosen[0]) + " gepflanzt. (Feld-ID: " + id + ")");
    }

    private void openHarvestMenu(Scanner sc, Inventory inventory, InventoryRepository repo) {
        List<CropEntry> ready = new ArrayList<>();
        for (CropEntry c : crops) {
            if (c.isReady()) ready.add(c);
        }

        if (ready.isEmpty()) {
            System.out.println("Keine reifen Felder vorhanden.");
            return;
        }

        System.out.println("=== Ernte ===");
        for (int i = 0; i < ready.size(); i++) {
            CropEntry c = ready.get(i);
            int harvestPerSeed = getHarvestAmount(c.getSeedItemId());
            int total = c.getAmount() * harvestPerSeed;
            System.out.println((i + 1) + ") [ID:" + c.getId() + "]  "
                    + c.getAmount() + "x " + inventory.getDisplayName(c.getSeedItemId())
                    + "  →  Ernte: " + total + "x " + inventory.getDisplayName(c.getHarvestItemId()));
        }

        System.out.println("0) Abbrechen");
        System.out.print("Auswahl: ");
        int choice;
        try { choice = Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Bitte eine Zahl eingeben."); return; }

        if (choice == 0) return;
        if (choice < 1 || choice > ready.size()) { System.out.println("Ungültige Auswahl."); return; }

        CropEntry c = ready.get(choice - 1);
        int total = c.getAmount() * getHarvestAmount(c.getSeedItemId());

        boolean ok = inventory.addItem(c.getHarvestItemId(), total);
        if (!ok) {
            System.out.println("Lager voll oder Artikel nicht im Lager. Ernte: " + total + "x " + c.getHarvestItemId());
        } else {
            repo.save(inventory);
            System.out.println("Geerntet: " + total + "x " + inventory.getDisplayName(c.getHarvestItemId()));
        }

        crops.remove(c);
        repository.save(crops);
    }

    // Hilfsfunktionen

    private int getHarvestAmount(String seedItemId) {
        for (String[] type : CROP_TYPES) {
            if (type[0].equals(seedItemId)) return Integer.parseInt(type[3]);
        }
        return 1;
    }

    private void sortById() {
        crops.sort(Comparator.comparingInt(CropEntry::getId));
    }
}
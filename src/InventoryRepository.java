import java.io.*;
import java.util.*;

// Laedt und speichert das Inventar als CSV-Datei.
// Die Gesamtkapazitaet wird zusaetzlich im DataStore gehalten.
public class InventoryRepository {

    private final String inventoryFile;
    private final DataStore dataStore;

    public InventoryRepository(String inventoryFile, DataStore dataStore) {
        this.inventoryFile = inventoryFile;
        this.dataStore = dataStore;
    }

    public void loadOrCreate(Inventory inventory) {
        int maxCapacity = dataStore.getInt("maxCapacity", -1);
        inventory.setMaxCapacity(maxCapacity);

        File f = new File(inventoryFile);
        if (f.exists()) {
            loadCsv(inventory);
        } else {
            save(inventory);
        }
    }

    public void save(Inventory inventory) {
        dataStore.setInt("maxCapacity", inventory.getMaxCapacity());
        dataStore.saveNow();
        saveCsv(inventory);
    }

    /*
     * Benutztes Format:
     * itemId, amount, maxItemCapacity, basePrice
     * eggs, 120, 300
     * milk, 50, 67
     *
     * immer so weiter
     */

    private void saveCsv(Inventory inventory) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inventoryFile))) {
            writer.write("itemId,amount,maxItemCapacity");
            writer.newLine();
            for (String id : inventory.getItemIdsSorted()) {
                writer.write(id + "," + inventory.getAmount(id) + "," + inventory.getMaxItemCapacity(id));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Sichern fehlgeschlagen: " + e.getMessage());
        }
    }

    private void loadCsv(Inventory inventory) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inventoryFile))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                if (firstLine && t.toLowerCase().startsWith("itemid")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = t.split(",");
                if (parts.length < 3) continue;

                int amount, cap;
                try {
                    amount = Math.max(0, Integer.parseInt(parts[1].trim()));
                    cap    = Math.max(0, Integer.parseInt(parts[2].trim()));
                } catch (NumberFormatException e) {
                    continue;
                }

                inventory.createItem(parts[0].trim(), cap);
                inventory.addItem(parts[0].trim(), amount);
            }
        } catch (IOException e) {
            System.out.println("Laden fehlgeschlagen: " + e.getMessage());
        }
    }
}
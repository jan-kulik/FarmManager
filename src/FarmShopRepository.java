import java.io.*;
import java.util.*;

/**
 * Lädt und speichert die Hofladen-Angebote in einer CSV-Datei.
 * Format: itemId,farmerPrice,active
 */
public class FarmShopRepository {

    private final String filePath;

    public FarmShopRepository(String filePath) {
        this.filePath = filePath;
    }

    // Lädt bestehende Einträge aus der CSV-Datei in die übergebene Map.
    public void loadOrCreate(Map<String, FarmShop.ShopEntry> entries, ItemCatalog catalog) {
        entries.clear();

        File f = new File(filePath);
        if (!f.exists()) {
            save(entries);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                // Kopfzeile überspringen
                if (firstLine && t.toLowerCase().startsWith("itemid")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = t.split(",", -1);
                if (parts.length < 3) continue;

                String itemId = parts[0].trim();
                if (itemId.isEmpty()) continue;

                // Artikel muss im Katalog vorhanden sein
                if (!catalog.exists(itemId)) continue;

                double farmerPrice;
                boolean active;
                try {
                    farmerPrice = Double.parseDouble(parts[1].trim());
                    active      = Boolean.parseBoolean(parts[2].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                if (farmerPrice <= 0) continue;

                FarmShop.ShopEntry entry = new FarmShop.ShopEntry(itemId, farmerPrice);
                entry.setActive(active);
                entries.put(itemId, entry);
            }

        } catch (IOException ignored) {
        }
    }

    //
    public void save(Map<String, FarmShop.ShopEntry> entries) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("itemId,farmerPrice,active");
            writer.newLine();

            List<String> keys = new ArrayList<>(entries.keySet());
            Collections.sort(keys);

            for (String key : keys) {
                FarmShop.ShopEntry e = entries.get(key);
                writer.write(e.getItemId() + "," + e.getFarmerPrice() + "," + e.isActive());
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }
}
 
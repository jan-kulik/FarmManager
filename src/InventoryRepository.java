import java.io.File;
import java.io.IOException;

public class InventoryRepository {

    private final String inventoryFile;
    private final DataStore dataStore;

    public InventoryRepository(String inventoryFile, DataStore dataStore) {
        this.inventoryFile = inventoryFile;
        this.dataStore = dataStore;
    }

    public void loadOrCreate(Inventory inventory) {
        try {
            int maxCapacity = dataStore.getInt("maxCapacity", -1);
            inventory.setMaxCapacity(maxCapacity);

            if (new File(inventoryFile).exists()) {
                inventory.loadInventoryCsv(inventoryFile);
            } else {
                inventory.saveInventoryCsv(inventoryFile);
            }
        } catch (IOException e) {
            System.out.println("Laden fehlgeschlagen: " + e.getMessage());
        }
    }

    public void save(Inventory inventory) {
        try {
            dataStore.setInt("maxCapacity", inventory.getMaxCapacity());
            inventory.saveInventoryCsv(inventoryFile);
        } catch (IOException e) {
            System.out.println("Sichern fehlgeschlagen: " + e.getMessage());
        }
    }
}
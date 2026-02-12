import java.io.File;
import java.io.IOException;

public class InventoryRepository {

    private final String inventoryFile;
    private final String configFile;

    public InventoryRepository(String inventoryFile, String configFile) {
        this.inventoryFile = inventoryFile;
        this.configFile = configFile;
    }

    public void loadOrCreate(Inventory inventory) {
        try {
            if (new File(configFile).exists()) {
                inventory.loadConfig(configFile);
            } else {
                inventory.setMaxCapacity(-1);
                inventory.saveConfig(configFile);
            }

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
            inventory.saveConfig(configFile);
            inventory.saveInventoryCsv(inventoryFile);
        } catch (IOException e) {
            System.out.println("Sichern fehlgeschlagen: " + e.getMessage());
        }
    }
}

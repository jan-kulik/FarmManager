import java.io.*;
import java.util.*;

// Laedt und speichert alle Felder als CSV-Datei.
// Feld-IDs werden wie Tier-IDs ueber den DataStore verwaltet.
public class CropRepository {

    private final String filePath;
    private final DataStore dataStore;

    public CropRepository(String filePath, DataStore dataStore) {
        this.filePath = filePath;
        this.dataStore = dataStore;
    }

    public void loadOrCreate(List<Crops.CropEntry> crops) {
        crops.clear();

        File f = new File(filePath);
        if (!f.exists()) {
            save(crops);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                if (firstLine && t.toLowerCase().startsWith("id,")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                // Format: id,seedItemId,harvestItemId,amount,growthDaysNeeded,growthDays
                String[] parts = t.split(",", -1);
                if (parts.length < 6) continue;

                int id, amount, growthDaysNeeded, growthDays;
                try {
                    id               = Integer.parseInt(parts[0].trim());
                    amount           = Integer.parseInt(parts[3].trim());
                    growthDaysNeeded = Integer.parseInt(parts[4].trim());
                    growthDays       = Integer.parseInt(parts[5].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                String seedItemId    = parts[1].trim();
                String harvestItemId = parts[2].trim();
                if (seedItemId.isEmpty() || harvestItemId.isEmpty()) continue;

                crops.add(new Crops.CropEntry(id, seedItemId, harvestItemId,
                        amount, growthDaysNeeded, growthDays));
            }

        } catch (IOException ignored) {
        }

        // Nächste ID korrekt setzen
        int maxId = 0;
        for (Crops.CropEntry c : crops) {
            if (c.getId() > maxId) maxId = c.getId();
        }
        int next = Math.max(getNextCropId(), maxId + 1);
        setNextCropId(next);
    }

    public void save(List<Crops.CropEntry> crops) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("id,seedItemId,harvestItemId,amount,growthDaysNeeded,growthDays");
            writer.newLine();

            List<Crops.CropEntry> copy = new ArrayList<>(crops);
            copy.sort(Comparator.comparingInt(Crops.CropEntry::getId));

            for (Crops.CropEntry c : copy) {
                writer.write(c.toCsvRow());
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public int createId() {
        int id = getNextCropId();
        setNextCropId(id + 1);
        return id;
    }

    private int getNextCropId() {
        return dataStore.getInt("nextCropId", 1);
    }

    private void setNextCropId(int next) {
        dataStore.setInt("nextCropId", Math.max(1, next));
        dataStore.saveNow();
    }
}
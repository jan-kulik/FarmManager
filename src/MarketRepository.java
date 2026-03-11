import java.io.*;
import java.util.*;

public class MarketRepository {

    private final String filePath;

    public MarketRepository(String filePath) {
        this.filePath = filePath;
    }

    public void loadOrCreate(Map<String, MarketEntry> entries) {
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

                if (firstLine && t.toLowerCase().startsWith("itemid")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;

                String[] parts = t.split(",", -1);
                if (parts.length < 3) continue;

                String itemId = parts[0].trim();
                if (itemId.isEmpty()) continue;

                double demandFactor;
                double smoothedPrice;
                try {
                    demandFactor  = Double.parseDouble(parts[1].trim());
                    smoothedPrice = Double.parseDouble(parts[2].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                entries.put(itemId, new MarketEntry(itemId, demandFactor, smoothedPrice));
            }

        } catch (IOException ignored) {
        }
    }

    public void save(Map<String, MarketEntry> entries) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("itemId,demandFactor,smoothedPrice");
            writer.newLine();

            List<String> keys = new ArrayList<>(entries.keySet());
            Collections.sort(keys);

            for (String key : keys) {
                writer.write(entries.get(key).toCsvRow());
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }
}
import java.io.*;
import java.util.*;

public class ItemCatalog {

    private final Map<String, ItemDefinition> definitions = new HashMap<>();

    public void loadFromCsv(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));

        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (firstLine) {
                firstLine = false;
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length<4) continue;

            String itemId = normalizeId(parts[0]);
            if (itemId == null) continue;

            String displayName = parts[1].trim();
            if (displayName.isEmpty()) continue;

            ItemCategory category;
            try {
                category = ItemCategory.valueOf(parts[2].trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                continue;
            }

            int itemLimit;
            try {
                itemLimit = Integer.parseInt(parts[3].trim());
            } catch (NumberFormatException ex) {
                continue;
            }

            if (itemLimit < 0) itemLimit = 0;

            definitions.put(itemId, new ItemDefinition(itemId, displayName, category, itemLimit));
        }

        reader.close();
    }

    public boolean exists(String itemId) {
        String id = normalizeId(itemId);
        if (id ==null) return false;
        return definitions.containsKey(id);
    }

    public ItemDefinition get(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return null;
        return definitions.get(id);
    }

    public List<ItemDefinition> getAllSorted() {
        List<ItemDefinition> list= new ArrayList<>(definitions.values());
        list.sort(Comparator.comparing(ItemDefinition::getItemId));
        return list;
    }

    private String normalizeId(String itemId) {
        if (itemId == null) return null;
        String id = itemId.trim().toLowerCase();
        if (id.isEmpty()) return null;

        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9')
                      || c == '_' || c == '-';
            if (!ok) return null;
        }
        return id;
    }
}

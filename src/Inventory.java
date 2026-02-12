import java.io.*;
import java.util.*;

public class Inventory {
    private final Map<String, Integer> items;
    private int maxCapacity;

    public Inventory() {
        this.items = new HashMap<>();
        this.maxCapacity = -1;
    }

    public boolean createItem(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return false;

        if (items.containsKey(id)) return false;
        items.put(id, 0);
        return true;
    }

    public boolean deleteItem(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return false;

        return items.remove(id) != null;
    }

    public boolean itemExists(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return false;

        return items.containsKey(id);
    }

    public boolean addItem(String itemId, int amount) {
        String id = normalizeId(itemId);
        if (id == null) return false;
        if(amount <= 0) return false;
        if (!items.containsKey(id)) return false;

        if(!canAdd(amount)) return false;

        items.put(id, items.get(id) + amount);
        return true;
    }

    public boolean removeItem(String itemId, int amount) {
        String id = normalizeId(itemId);
        if (id == null) return false;
        if (amount <= 0) return false;

        Integer current = items.get(id);
        if (current == null || current < amount) return false;
        items.put(id, current - amount);
        return true;
    }

    public int getAmount(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return 0;

        Integer v = items.get(id);
        return (v == null) ? 0 : v;
    }

    public boolean hasEnough(String itemId, int amount) {
        if (amount <= 0) return false;
        return getAmount(itemId) >= amount;
    }

    public List<String> getItemIdsSorted() {
        List<String> ids = new ArrayList<>(items.keySet());
        Collections.sort(ids);
        return ids;
    }


    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        if (maxCapacity < -1) return;
        this.maxCapacity = maxCapacity;
    }

    public int getUsedCapacity() {
        int sum = 0;
        for (int amount : items.values()) {
            sum += amount;
        }
        return sum;
    }

    public int getFreeCapacity() {
        if (maxCapacity < 0) return Integer.MAX_VALUE;
        return Math.max(0, maxCapacity - getUsedCapacity());
    }

    private boolean canAdd(int amountToAdd) {
        if (maxCapacity == -1) return true;
        return getUsedCapacity() + amountToAdd <= maxCapacity;
    }

    /*
     * Benutztes Format:
     * itemId, amount
     * eggs, 120
     * milk, 50
     *
     * immer so weiter
     */

    public void saveInventoryCsv(String path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));

        writer.write("itemId,amount");
        writer.newLine();

        for (String id : getItemIdsSorted()) {
            writer.write(id + "," + items.get(id));
            writer.newLine();
        }

        writer.close();
    }

    public void loadInventoryCsv(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        items.clear();

        String line;
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (firstLine && line.toLowerCase().startsWith("itemid")) {
                firstLine = false;
                continue;
            }
            firstLine = false;

            String[] parts = line.split(",");
            if(parts.length < 2) continue;
            String id = normalizeId(parts[0]);
            if (id == null) continue;

            int amount;
            try {
                amount = Integer.parseInt(parts[1].trim());
            }catch (NumberFormatException ex) {
                continue;
            }

            if (amount < 0) amount = 0;
            items.put(id,amount);
        }
        reader.close();
    }

    public void saveConfig(String path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.write("maxCapacity=" + maxCapacity);
        writer.newLine();
        writer.close();
    }

    public void loadConfig(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;

            String[] parts = line.split("=");
            if (parts.length < 2) continue;

            String key = parts[0].trim();
            String value = parts[1].trim();

            if (key.equalsIgnoreCase("maxCapacity")) {
                try {
                    int cap = Integer.parseInt(value);
                    setMaxCapacity(cap);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        reader.close();
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



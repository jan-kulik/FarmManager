import java.io.*;
import java.util.*;

public class Inventory {
    private final Map<String, Integer> items;
    private final Map<String, Integer> itemCaps;
    private final ItemCatalog catalog;
    private int maxCapacity;

    public Inventory(ItemCatalog catalog) {
        this.catalog = catalog;
        this.items = new HashMap<>();
        this.itemCaps = new HashMap<>();
        this.maxCapacity = -1;
    }



    public boolean createItem(String itemId, int maxItemCapacity) {
        String id = normalizeId(itemId);
        if (id== null) return false;

        if (!catalog.exists(id)) return false;
        if (items.containsKey(id)) return false;

        if (maxItemCapacity< 0) return false;

        items.put(id, 0);
        itemCaps.put(id, maxItemCapacity);
        return true;
    }


    public boolean deleteItem(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return false;

        boolean existed = items.remove(id) != null;
        itemCaps.remove(id);
        return existed;
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
        ItemDefinition def = catalog.get(id);
        if (def == null) return false;
        int current = items.get(id);
        int cap = itemCaps.getOrDefault(id, 0);
        if (current + amount > cap) return false;

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

    public int getMaxItemCapacity(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return 0;
        return itemCaps.getOrDefault(id, 0);
    }

    // Gibt den "displayName" aus dem Katalog zuruck
    public String getDisplayName(String itemId) {
        String id = normalizeId(itemId);
        if (id == null) return null;
        ItemDefinition def = catalog.get(id);
        return (def != null) ? def.getDisplayName(): id;
    }

    // Prüft, ob die Menge hinzugefügt werden kann(Kpazität des Lagers und vom Item)
    private boolean canAdd(int amountToAdd) {
        if (maxCapacity == -1) return true;
        return getUsedCapacity() + amountToAdd <= maxCapacity;
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
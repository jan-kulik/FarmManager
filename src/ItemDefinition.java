// Ein Item aus dem Katalog, hat ID, Name, Kategorie, Basispreis und Einheit.
public class ItemDefinition {
    private final String itemId;
    private final String displayName;
    private final ItemCategory category;
    private final double basePrice;
    private final String unit;

    public ItemDefinition(String itemId, String displayName, ItemCategory category, double basePrice,  String unit) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.category = category;
        this.basePrice = Math.max(0.0, basePrice);
        this.unit = (unit == null || unit.isBlank()) ? "" : unit.trim();
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public String getUnit() { return unit; }
}
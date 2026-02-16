public class ItemDefinition {
    private final String itemId;
    private final String displayName;
    private final ItemCategory category;
    private final int basePrice;

    public ItemDefinition(String itemId, String displayName, ItemCategory category, int basePrice) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.category = category;
        this.basePrice = Math.max(0, basePrice);
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

    public int getBasePrice() {
        return basePrice;
    }
}
public class ItemDefinition {
    private final String itemId;
    private final String displayName;
    private final ItemCategory category;
    private final int itemLimit;

    public ItemDefinition(String itemId, String displayName, ItemCategory category, int itemLimit){
        this.itemId = itemId;
        this.displayName = displayName;
        this.category =category;
        this.itemLimit = itemLimit;
    }
    public String getItemId(){
        return itemId;
    }
    public String getDisplayName(){
        return displayName;
    }
    public ItemCategory getCategory() {
        return category;
    }
    public int getItemLimit() {
        return itemLimit;
    }
}

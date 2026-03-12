public abstract class Animal {

    private final int id;
    private String name;

    private int ageDays;
    private int hunger;
    private int productionCounterDays;

    protected Animal(int id, String name, int ageDays, int hunger, int productionCounterDays) {
        this.id = id;
        this.name = (name == null || name.isBlank()) ? ("Animal " + id) : name.trim();
        this.ageDays = Math.max(0, ageDays);
        this.hunger = clamp(hunger, 0, 100);
        this.productionCounterDays = Math.max(0, productionCounterDays);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) return;
        this.name = name.trim();
    }

    public int getAgeDays() {
        return ageDays;
    }

    public int getHunger() {
        return hunger;
    }

    public int getProductionCounterDays() {
        return productionCounterDays;
    }

    public void feed(int amount) {
        if (amount <= 0) return;
        hunger = clamp(hunger + amount, 0, 100);
    }

    public boolean feedWithItem(String itemId, Inventory inventory) {
        String[] allowed = getAllowedFeedItems();
        boolean isAllowed = false;
        for (String s : allowed) {
            if (s.equals(itemId)) { isAllowed = true; break; }
        }
        if (!isAllowed) return false;
        if (!inventory.hasEnough(itemId, 1)) return false;
        inventory.removeItem(itemId, 1);
        hunger = clamp(hunger + getFeedHungerValue(itemId), 0, 100);
        return true;
    }

    public int getFeedHungerValue(String itemId) {
        return 25;
    }

    public abstract String[] getAllowedFeedItems();

    public boolean needsFeeding() {
        return true;
    }

    public final void endOfDay(Inventory inventory) {
        ageDays++;

        int loss = getDailyHungerLoss();
        if (loss < 0) loss = 0;
        hunger = clamp(hunger - loss, 0, 100);

        productionCounterDays++;

        int interval = getProductionIntervalDays();
        if (interval < 1) interval = 1;

        if (hunger >= getMinHungerToProduce() && productionCounterDays >= interval) {
            String productId = getProductItemId();
            int amount = getProductAmount();
            if (productId != null && !productId.isBlank() && amount > 0) {
                boolean produced = inventory.addItem(productId, amount);
                if (!produced) {
                    System.out.println(getName() + " konnte nicht produzieren (Lager voll?)");
                }
            }
            productionCounterDays = 0;
        }
    }

    public String toCsvRow() {
        String safeName = name.replace(",", " ").trim();
        return id + "," + getType().name() + "," + safeName + "," + ageDays + "," + hunger + "," + productionCounterDays;
    }

    public abstract AnimalType getType();

    public abstract int getDailyHungerLoss();

    public abstract int getMinHungerToProduce();

    public abstract String getProductItemId();

    public abstract int getProductAmount();

    public abstract int getProductionIntervalDays();

    protected int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    protected void resetProductionCounter() {
        this.productionCounterDays = 0;
    }

    // Erstellt eine Textdarstellung des Hungerbalkens, z.B. [#####.....] für 50% Hunger
    public static String hungerBar(int hunger) {
        int filled = hunger / 10;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "#" : ".");
        }
        bar.append("]");
        return bar.toString();
    }
}
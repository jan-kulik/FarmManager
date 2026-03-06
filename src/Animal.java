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

    public final void endOfDay(Inventory inventory, Balance balance) {
        ageDays++;

        int loss = getDailyHungerLoss();
        if (loss < 0) loss = 0;
        hunger = clamp(hunger - loss, 0, 100);

        double cost = getDailyFeedCost();
        if (cost < 0) cost = 0;
        if (cost > 0) {
            balance.withdraw(cost);
        }

        productionCounterDays++;

        int interval = getProductionIntervalDays();
        if (interval < 1) interval = 1;

        if (hunger >= getMinHungerToProduce() && productionCounterDays >= interval) {
            String productId = getProductItemId();
            int amount = getProductAmount();
            if (productId != null && !productId.isBlank() && amount > 0) {
                inventory.addItem(productId, amount);
            }
            productionCounterDays = 0;
        }
    }

    public String toCsvRow() {
        String safeName = name.replace(",", " ").trim();
        return id + "," + getType().name() + "," + safeName + "," + ageDays + "," + hunger + "," + productionCounterDays;
    }

    public abstract AnimalType getType();

    public abstract double getDailyFeedCost();

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
}
import java.util.Random;

public class Sheep extends Animal {

    // productionCounterDays aus Animal wird als "daysSinceShearing" wiederverwendet.
    // Kein eigenes Feld nötig, kein eigenes toCsvRow nötig.

    private static final int SHEARING_INTERVAL = 182;
    private static final Random random = new Random();

    public Sheep(int id, String name) {
        super(id, name, 0, 100, 0);
    }

    public Sheep(int id, String name, int ageDays, int hunger, int productionCounterDays) {
        super(id, name, ageDays, hunger, productionCounterDays);
    }

    @Override
    public AnimalType getType() {
        return AnimalType.SHEEP;
    }

    @Override
    public int getDailyHungerLoss() {
        return 10;
    }

    @Override
    public int getMinHungerToProduce() {
        return 40;
    }

    // Automatische Produktion deaktiviert – Schafe werden manuell geschoren.
    @Override
    public String getProductItemId() {
        return "wool";
    }

    @Override
    public int getProductAmount() {
        return 0;
    }

    @Override
    public int getProductionIntervalDays() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String[] getAllowedFeedItems() {
        return new String[]{ "hay", "animal_feed" };
    }

    @Override
    public int getFeedHungerValue(String itemId) {
        switch (itemId) {
            case "hay": return 35;
            case "animal_feed": return 20;
            default: return 25;
        }
    }

    /** productionCounterDays wird hier als Tage-seit-Schur verwendet. */
    public boolean canBeSheared() {
        return getProductionCounterDays() >= SHEARING_INTERVAL;
    }

    public int getDaysUntilShearing() {
        return Math.max(0, SHEARING_INTERVAL - getProductionCounterDays());
    }

    /**
     * Schert das Schaf manuell. Legt 2–5 Wolle ins Lager.
     * Setzt productionCounterDays auf 0 zurück.
     */
    public boolean shear(Inventory inventory) {
        if (!canBeSheared()) return false;
        if (getHunger() < getMinHungerToProduce()) return false;

        int amount = 2 + random.nextInt(4); // 2, 3, 4 oder 5
        boolean added = inventory.addItem("wool", amount);
        if (!added) return false;

        resetProductionCounter();
        return true;
    }
}
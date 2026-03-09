/**
 * Bienen werden als Gruppe verwaltet und benötigen kein Futter. Sie produzieren alle 30 Tage Honig.
 * Die Menge des produzierten Honigs hängt von der Größe der Kolonie ab.
 * Honig wird in Gläsern gespeichert.
 */
public class Bee extends Animal {

    private int colonySize;

    public Bee(int id, String name) {
        super(id, name, 0, 100, 0);
        this.colonySize = 10000;
    }

    public Bee(int id, String name, int ageDays, int hunger, int productionCounterDays) {
        super(id, name, ageDays, hunger, productionCounterDays);
        this.colonySize = 10000;
    }

    public Bee(int id, String name, int ageDays, int hunger, int productionCounterDays, int colonySize) {
        super(id, name, ageDays, hunger, productionCounterDays);
        this.colonySize = Math.max(1, colonySize);
    }

    public int getColonySize() { return colonySize; }
    public void setColonySize(int size) { if (size > 0) this.colonySize = size; }

    @Override public AnimalType getType() { return AnimalType.BEE; }
    @Override public int getDailyHungerLoss() { return 0; }
    @Override public int getMinHungerToProduce() { return 0; }
    @Override public String getProductItemId() { return "honey"; }
    @Override public int getProductionIntervalDays() { return 30; }

    @Override
    public int getProductAmount() {
        return Math.max(1, colonySize / 5000);
    }

    @Override
    public String[] getAllowedFeedItems() {
        return new String[0];
    }

    @Override
    public boolean needsFeeding() {
        return false;
    }

    @Override
    public String toCsvRow() {
        String safeName = getName().replace(",", " ").trim();
        return getId() + "," + getType().name() + "," + safeName + ","
                + getAgeDays() + "," + getHunger() + "," + getProductionCounterDays()
                + "," + colonySize;
    }
}
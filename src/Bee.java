public class Bee extends Animal {

    public Bee(int id, String name) {
        super(id, name, 0, 100, 0);
    }

    public Bee(int id, String name, int ageDays, int hunger, int productionCounterDays) {
        super(id, name, ageDays, hunger, productionCounterDays);
    }

    @Override
    public AnimalType getType() {
        return AnimalType.BEE;
    }

    @Override
    public double getDailyFeedCost() {
        return 1.5;
    }

    @Override
    public int getDailyHungerLoss() {
        return 0;
    }

    @Override
    public int getMinHungerToProduce() {
        return 0;
    }

    @Override
    public String getProductItemId() {
        return "honey";
    }

    @Override
    public int getProductAmount() {
        return 1;
    }

    @Override
    public int getProductionIntervalDays() {
        return 100;
    }
}
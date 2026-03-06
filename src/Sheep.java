public class Sheep extends Animal {

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
    public double getDailyFeedCost() {
        return 1.5;
    }

    @Override
    public int getDailyHungerLoss() {
        return 10;
    }

    @Override
    public int getMinHungerToProduce() {
        return 40;
    }

    @Override
    public String getProductItemId() {
        return "wool";
    }

    @Override
    public int getProductAmount() {
        return 3;
    }

    @Override
    public int getProductionIntervalDays() {
        return 182;
    }
}
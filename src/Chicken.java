public class Chicken extends Animal {

    public Chicken(int id, String name) {
        super(id, name, 0, 100, 0);
    }

    public Chicken(int id, String name, int ageDays, int hunger, int productionCounterDays) {
        super(id, name, ageDays, hunger, productionCounterDays);
    }

    @Override
    public AnimalType getType() {
        return AnimalType.CHICKEN;
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
        return "eggs";
    }

    @Override
    public int getProductAmount() {
        return 1;
    }

    @Override
    public int getProductionIntervalDays() {
        return 1;
    }

    @Override
    public String[] getAllowedFeedItems() {
        return new String[]{ "corn_meal", "animal_feed" };
    }

    @Override
    public int getFeedHungerValue(String itemId) {
        switch (itemId) {
            case "corn_meal": return 30;
            case "animal_feed": return 25;
            default: return 25;
        }
    }

}
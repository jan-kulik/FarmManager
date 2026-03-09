public class Pig extends Animal {

    public Pig(int id, String name) {
        super(id, name, 0, 100, 0);
    }

    public Pig(int id, String name, int ageDays, int hunger, int productionCounterDays) {
        super(id, name, ageDays, hunger, productionCounterDays);
    }

    @Override
    public AnimalType getType() {
        return AnimalType.PIG;
    }

    @Override
    public int getDailyHungerLoss() {
        return 5;
    }

    @Override
    public int getMinHungerToProduce() {
        return 90;
    }

    @Override
    public String getProductItemId() {
        return "slurry";
    }

    @Override
    public int getProductAmount() {
        return 1;
    }

    @Override
    public int getProductionIntervalDays() {
        return 2;
    }

    @Override
    public String[] getAllowedFeedItems() {
        return new String[]{ "corn_meal", "protein_mix", "animal_feed" };
    }

    @Override
    public int getFeedHungerValue(String itemId) {
        switch (itemId) {
            case "corn_meal": return 25;
            case "protein_mix": return 40;
            case "animal_feed": return 20;
            default: return 25;
        }
    }

}
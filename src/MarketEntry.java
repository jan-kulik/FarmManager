public class MarketEntry {

    private final String itemId;

    // steigt beim Verkaufen, sinkt beim Kaufen (1.0 = neutral)
    private double demandFactor;

    // Wie oft heute gekauft / verkauft wurde (wird täglich zurückgesetzt)
    private int dailyBought;
    private int dailySold;

    // Geglätteter aktueller Preis (wird taglich neu berechnet)
    private double smoothedPrice;

    public MarketEntry(String itemId, double demandFactor, double smoothedPrice) {
        this.itemId       = itemId;
        this.demandFactor = demandFactor;
        this.smoothedPrice = smoothedPrice;
        this.dailyBought  = 0;
        this.dailySold    = 0;
    }

    // Getter
    public String  getItemId()       { return itemId; }
    public double  getDemandFactor() { return demandFactor; }
    public int     getDailyBought()  { return dailyBought; }
    public int     getDailySold()    { return dailySold; }
    public double  getSmoothedPrice(){ return smoothedPrice; }

    // Setter / Updater
    public void setDemandFactor(double demandFactor) {
        this.demandFactor = Math.max(0.1, demandFactor);
    }

    public void setSmoothedPrice(double price) {
        if (price > 0) this.smoothedPrice = price;
    }

    public void recordBuy(int amount) {
        if (amount > 0) dailyBought += amount;
    }

    public void recordSell(int amount) {
        if (amount > 0) dailySold += amount;
    }

    // taeglich werte zuruecksetzen
    public void resetDailyCounters() {
        dailyBought = 0;
        dailySold   = 0;
    }

    // Format: itemId,demandFactor,smoothedPrice
    public String toCsvRow() {
        return itemId + "," + demandFactor + "," + smoothedPrice;
    }
}
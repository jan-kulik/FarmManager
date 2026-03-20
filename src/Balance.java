// Spielerkonto. Kontostand wird beim Start aus dem DataStore geladen
// und nach jeder Aenderung sofort gespeichert damit nix verloren geht.
public class Balance {
    private final DataStore dataStore;
    private double balance;

    public Balance(DataStore dataStore) {
        this.dataStore = dataStore;
        // Kontostand beim Start aus der Datei laden
        this.balance = dataStore.getDouble("balance", 0.0);
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) return;
        balance += amount;
        save();
    }

    public boolean withdraw(double amount) {
        if(amount <= 0) return false;
        if (balance < amount) return false; // nicht genug geld
        balance -= amount;
        save();
        return true;
    }

    public void setBalance(double newBalance) {
        if (newBalance < 0) return;
        balance = newBalance;
        save();
    }

    // sofort speichern nach jeder Änderung
    private void save() {
        dataStore.setDouble("balance", balance);
        dataStore.saveNow();
    }
}

public class Balance {
    private final DataStore dataStore;
    private double balance;

    public Balance(DataStore dataStore) {
        this.dataStore = dataStore;
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
        if (balance < amount) return false;
        balance -= amount;
        save();
        return true;
    }

    public void setBalance(double newBalance) {
        if (newBalance < 0) return;
        balance = newBalance;
        save();
    }

    private void save() {
        dataStore.setDouble("balance", balance);
    }
}

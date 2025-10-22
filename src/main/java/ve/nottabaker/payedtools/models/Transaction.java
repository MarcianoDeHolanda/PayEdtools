package ve.nottabaker.payedtools.models;

import java.util.UUID;

/**
 * Represents a currency transaction
 */
public class Transaction {
    
    private final UUID id;
    private final UUID sender;
    private final UUID receiver;
    private final String currency;
    private final double amount;
    private final long timestamp;
    private double tax;
    
    public Transaction(UUID id, UUID sender, UUID receiver, String currency, double amount, long timestamp) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.currency = currency;
        this.amount = amount;
        this.timestamp = timestamp;
        this.tax = 0;
    }
    
    // Getters
    
    public UUID getId() {
        return id;
    }
    
    public UUID getSender() {
        return sender;
    }
    
    public UUID getReceiver() {
        return receiver;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public double getTax() {
        return tax;
    }
    
    public void setTax(double tax) {
        this.tax = tax;
    }
    
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", sender=" + sender +
                ", receiver=" + receiver +
                ", currency='" + currency + '\'' +
                ", amount=" + amount +
                ", tax=" + tax +
                ", timestamp=" + timestamp +
                '}';
    }
}

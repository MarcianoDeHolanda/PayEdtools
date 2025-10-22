package ve.nottabaker.payedtools.models;

import java.util.UUID;

/**
 * Represents a pending transaction awaiting confirmation
 */
public class PendingTransaction {
    
    private final UUID sender;
    private final UUID receiver;
    private final String currency;
    private final double amount;
    private final long createdAt;
    
    public PendingTransaction(UUID sender, UUID receiver, String currency, double amount, long createdAt) {
        this.sender = sender;
        this.receiver = receiver;
        this.currency = currency;
        this.amount = amount;
        this.createdAt = createdAt;
    }
    
    // Getters
    
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
    
    public long getCreatedAt() {
        return createdAt;
    }
}

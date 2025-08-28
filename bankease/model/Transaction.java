package com.bankease.model;

import java.time.LocalDateTime;

public class Transaction {
    private int id; // db id
    private String fromAccount;
    private String toAccount;
    private double amount;
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER
    private String description;
    private LocalDateTime timestamp = LocalDateTime.now();

    public Transaction() {}

    public Transaction(String fromAccount, String toAccount, double amount, String type, String description) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}


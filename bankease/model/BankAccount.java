package com.bankease.model;

import java.time.LocalDateTime;

// bank account holds balance and number, linked to user by accountNumber
public class BankAccount {
    private String accountNumber;
    private double balance;
    private int userId; // db user id
    private LocalDateTime createdDate = LocalDateTime.now();

    public BankAccount() {}

    public BankAccount(String accountNumber, double balance, int userId) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.userId = userId;
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public synchronized double getBalance() { return balance; }
    public synchronized void setBalance(double balance) { this.balance = balance; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}


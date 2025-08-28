package com.bankease.model;

// Admin extends User with priviliges
public class Admin extends User {
    public Admin() { super(); }

    public Admin(String name, String email, String password, String accountNumber, double balance) {
        super(name, email, password, accountNumber, balance);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}


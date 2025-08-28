package com.bankease.service;

import com.bankease.util.DatabaseConfig;

import java.sql.*;


public class DatabaseManager {

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.URL, DatabaseConfig.USER, DatabaseConfig.PASSWORD);
    }

    // create tables if not exist
    public static void init() {
        String usersSql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100), " +
                "email VARCHAR(100) UNIQUE, " +
                "password VARCHAR(100), " +
                "account_number VARCHAR(30) UNIQUE, " +
                "balance DOUBLE DEFAULT 0, " +
                "is_active BOOLEAN DEFAULT TRUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        String txSql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "from_account VARCHAR(30), " +
                "to_account VARCHAR(30), " +
                "amount DOUBLE, " +
                "type VARCHAR(20), " +
                "description VARCHAR(255), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute(usersSql);
            st.execute(txSql);
        } catch (SQLException e) {
            System.err.println("DB init faild: " + e.getMessage());
        }
    }

    public static void saveUser(String name, String email, String password, String accountNumber, double balance, boolean active) {
        String sql = "INSERT INTO users(name,email,password,account_number,balance,is_active) VALUES(?,?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, accountNumber);
            ps.setDouble(5, balance);
            ps.setBoolean(6, active);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("saveUser error: " + e.getMessage());
        }
    }

    public static ResultSet findUserByEmail(Connection c, String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=?";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, email);
        return ps.executeQuery();
    }

    public static ResultSet findUserByAccount(Connection c, String account) throws SQLException {
        String sql = "SELECT * FROM users WHERE account_number=?";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, account);
        return ps.executeQuery();
    }

    public static void updateBalance(String account, double newBalance) {
        String sql = "UPDATE users SET balance=? WHERE account_number=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, account);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateBalance error: " + e.getMessage());
        }
    }

    public static void insertTransaction(String from, String to, double amount, String type, String desc) {
        String sql = "INSERT INTO transactions(from_account,to_account,amount,type,description) VALUES(?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from);
            ps.setString(2, to);
            ps.setDouble(3, amount);
            ps.setString(4, type);
            ps.setString(5, desc);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("insertTransaction error: " + e.getMessage());
        }
    }
}


package com.bankease.service;

import com.bankease.exception.AccountFrozenException;
import com.bankease.exception.InsufficientFundsException;
import com.bankease.exception.InvalidAccountException;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class BankSystem {
    private final Scanner scanner = new Scanner(System.in);

    private final List<Map<String, Object>> usersCache = new ArrayList<>();
    private final List<Map<String, Object>> txCache = new ArrayList<>();

    public void start() {
        showBanner();
        DatabaseManager.init();
        ensureDefaultData();
        mainMenu();
    }

    private void showBanner() {
        System.out.println("==============================");
        System.out.println("   Welcome to BankEase  ");
        System.out.println("==============================");
    }

    private void mainMenu() {
        while (true) {
            System.out.println("\n1) Register  2) Login  3) Admin Login  0) Exit");
            System.out.print("Choose option: ");
            String ch = scanner.nextLine();
            switch (ch) {
                case "1": register(); break;
                case "2": login(false); break;
                case "3": login(true); break;
                case "0": System.out.println("Bye!"); return;
                default: System.out.println("Invalid choice");
            }
        }
    }

    private void register() {
        try (Connection c = DatabaseManager.getConnection()) {
            System.out.print("Full name: ");
            String name = scanner.nextLine();
            System.out.print("Email: ");
            String email = scanner.nextLine();
            if (!Pattern.compile("^[^@]+@[^@]+\\.[^@]+Rs - ").matcher(email).matches()) {
                System.out.println("Bad email format");
                return;
            }
            System.out.print("Password: ");
            String pass = scanner.nextLine();
            System.out.print("Initial deposit (min 10): ");
            double dep = Double.parseDouble(scanner.nextLine());
            if (dep < 10) {
                System.out.println("Minimum is 10");
                return;
            }
            String acc = genAccountNumber();
            DatabaseManager.saveUser(name, email, pass, acc, dep, true);
            DatabaseManager.insertTransaction(null, acc, dep, "DEPOSIT", "initial deposit");
            System.out.println("Account created! Number: " + acc);
        } catch (Exception e) {
            System.out.println("Could not register: " + e.getMessage());
        }
    }

    private void login(boolean admin) {
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet rs = DatabaseManager.findUserByEmail(c, email);
            if (rs.next()) {
                boolean isActive = rs.getBoolean("is_active");
                boolean isAdmin = rs.getString("email").equalsIgnoreCase("admin@bankease.com");
                if (admin && !isAdmin) { System.out.println("Not admin"); return; }
                if (!pass.equals(rs.getString("password"))) { System.out.println("Wrong password"); return; }
                if (!isActive) { System.out.println("Account frozen"); return; }
                if (admin) adminMenu(); else userMenu(rs.getString("account_number"));
            } else {
                System.out.println("No user found");
            }
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
    }

    private void userMenu(String account) {
        while (true) {
            System.out.println("\n1) Deposit 2) Withdraw 3) Transfer 4) Balance 5) Last 10 Tx 9) Back");
            System.out.print("Choose: ");
            String ch = scanner.nextLine();
            try {
                switch (ch) {
                    case "1": deposit(account); break;
                    case "2": withdraw(account); break;
                    case "3": transfer(account); break;
                    case "4": showBalance(account); break;
                    case "5": showLastTransactions(account); break;
                    case "9": return;
                    default: System.out.println("Bad choice");
                }
            } catch (Exception ex) {
                System.out.println("Action failed: " + ex.getMessage());
            }
        }
    }

    private void adminMenu() {
        while (true) {
            System.out.println("\nAdmin: 1) View Users 2) Search User 3) Freeze/Unfreeze 4) All Tx 5) Report 9) Back");
            System.out.print("Choose: ");
            String ch = scanner.nextLine();
            switch (ch) {
                case "1": listUsers(); break;
                case "2": searchUser(); break;
                case "3": toggleFreeze(); break;
                case "4": listAllTransactions(); break;
                case "5": report(); break;
                case "9": return;
                default: System.out.println("Bad choice");
            }
        }
    }

    private void deposit(String account) throws AccountFrozenException, InvalidAccountException {
        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine());
        if (amount <= 0) { System.out.println("Amount must be positive"); return; }
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet rs = DatabaseManager.findUserByAccount(c, account);
            if (!rs.next()) throw new InvalidAccountException("Account not found");
            if (!rs.getBoolean("is_active")) throw new AccountFrozenException("Account frozen");
            double bal = rs.getDouble("balance");
            double newBal;
            synchronized (this) {
                newBal = bal + amount;
            }
            DatabaseManager.updateBalance(account, newBal);
            DatabaseManager.insertTransaction(null, account, amount, "DEPOSIT", "cash deposit");
            appendTxToFile(account + ": +" + amount + " at " + LocalDateTime.now());
            System.out.println("Deposited. New balance: " + newBal);
        } catch (SQLException e) {
            System.out.println("Deposit error: " + e.getMessage());
        }
    }

    private void withdraw(String account) throws AccountFrozenException, InvalidAccountException, InsufficientFundsException {
        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine());
        if (amount <= 0) { System.out.println("Amount must be positive"); return; }
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet rs = DatabaseManager.findUserByAccount(c, account);
            if (!rs.next()) throw new InvalidAccountException("Account not found");
            if (!rs.getBoolean("is_active")) throw new AccountFrozenException("Account frozen");
            double bal = rs.getDouble("balance");
            if (bal < amount) throw new InsufficientFundsException("Not enough money");
            double newBal;
            synchronized (this) {
                newBal = bal - amount;
            }
            DatabaseManager.updateBalance(account, newBal);
            DatabaseManager.insertTransaction(account, null, amount, "WITHDRAWAL", "cash withdraw");
            appendTxToFile(account + ": -" + amount + " at " + LocalDateTime.now());
            System.out.println("Withdrawn. New balance: " + newBal);
        } catch (SQLException e) {
            System.out.println("Withdraw error: " + e.getMessage());
        }
    }

    private void transfer(String from) throws AccountFrozenException, InvalidAccountException, InsufficientFundsException {
        System.out.print("Send to account: ");
        String to = scanner.nextLine();
        System.out.print("Amount: ");
        double amount = Double.parseDouble(scanner.nextLine());
        if (amount <= 0) { System.out.println("Amount must be positive"); return; }
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet rsFrom = DatabaseManager.findUserByAccount(c, from);
            if (!rsFrom.next()) throw new InvalidAccountException("From account not found");
            if (!rsFrom.getBoolean("is_active")) throw new AccountFrozenException("Your account frozen");
            double balFrom = rsFrom.getDouble("balance");
            if (balFrom < amount) throw new InsufficientFundsException("Not enough money");

            ResultSet rsTo = DatabaseManager.findUserByAccount(c, to);
            if (!rsTo.next()) throw new InvalidAccountException("To account not found");
            if (!rsTo.getBoolean("is_active")) throw new AccountFrozenException("Target account frozen");
            double balTo = rsTo.getDouble("balance");

            double newFrom, newTo;
            synchronized (this) {
                newFrom = balFrom - amount;
                newTo = balTo + amount;
            }
            DatabaseManager.updateBalance(from, newFrom);
            DatabaseManager.updateBalance(to, newTo);
            DatabaseManager.insertTransaction(from, to, amount, "TRANSFER", "simple transfer");
            appendTxToFile(from + " -> " + to + ": -" + amount + " at " + LocalDateTime.now());
            System.out.println("Transfered. Your new balance: " + newFrom);
        } catch (SQLException e) {
            System.out.println("Transfer error: " + e.getMessage());
        }
    }

    private void showBalance(String account) {
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet rs = DatabaseManager.findUserByAccount(c, account);
            if (rs.next()) {
                System.out.println("Balance: " + rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            System.out.println("Balance error: " + e.getMessage());
        }
    }

    private void showLastTransactions(String account) {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions WHERE from_account=? OR to_account=? ORDER BY id DESC LIMIT 10")) {
            ps.setString(1, account);
            ps.setString(2, account);
            ResultSet rs = ps.executeQuery();
            List<String> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(rs.getString("type") + " Rs - " + rs.getDouble("amount") + " at " + rs.getTimestamp("created_at"));
            }
            // example: sort by text or filter large ones using streams/lambdas
            List<String> sorted = rows.stream().sorted().collect(Collectors.toList());
            sorted.forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println("History error: " + e.getMessage());
        }
    }

    private void listUsers() {
        try (Connection c = DatabaseManager.getConnection(); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT name,email,account_number,balance,is_active FROM users");
            while (rs.next()) {
                System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | Rs - " + rs.getDouble(4) + " | active:" + rs.getBoolean(5));
            }
        } catch (SQLException e) {
            System.out.println("List users error: " + e.getMessage());
        }
    }

    private void searchUser() {
        System.out.print("Enter email or account: ");
        String key = scanner.nextLine();
        String sql = key.contains("@") ? "SELECT * FROM users WHERE email=?" : "SELECT * FROM users WHERE account_number=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getString("name") + " | " + rs.getString("email") + " | " + rs.getString("account_number") + " | Rs - " + rs.getDouble("balance"));
            } else {
                System.out.println("No user");
            }
        } catch (SQLException e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    private void toggleFreeze() {
        System.out.print("Account number: ");
        String acc = scanner.nextLine();
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet rs = DatabaseManager.findUserByAccount(c, acc);
            if (!rs.next()) { System.out.println("Not found"); return; }
            boolean active = rs.getBoolean("is_active");
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET is_active=? WHERE account_number=?")) {
                ps.setBoolean(1, !active);
                ps.setString(2, acc);
                ps.executeUpdate();
                System.out.println((!active ? "Unfrozen" : "Frozen") + " succes");
            }
        } catch (SQLException e) {
            System.out.println("Freeze error: " + e.getMessage());
        }
    }

    private void listAllTransactions() {
        try (Connection c = DatabaseManager.getConnection(); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT from_account,to_account,amount,type,created_at FROM transactions ORDER BY id DESC LIMIT 50");
            while (rs.next()) {
                System.out.println(rs.getString(1) + " -> " + rs.getString(2) + " | Rs - " + rs.getDouble(3) + " | " + rs.getString(4) + " | " + rs.getTimestamp(5));
            }
        } catch (SQLException e) {
            System.out.println("Audit error: " + e.getMessage());
        }
    }

    private void report() {
        try (Connection c = DatabaseManager.getConnection(); Statement st = c.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT type, SUM(amount) FROM transactions GROUP BY type");
            while (rs.next()) {
                System.out.println(rs.getString(1) + ": Rs-" + rs.getDouble(2));
            }
        } catch (SQLException e) {
            System.out.println("Report error: " + e.getMessage());
        }
    }

    private void ensureDefaultData() {
        // create default admin and sample users if not present
        try (Connection c = DatabaseManager.getConnection()) {
            if (!existsEmail(c, "admin@bankease.com")) {
                DatabaseManager.saveUser("Admin", "admin@bankease.com", "admin123", "0000000001", 0, true);
            }
           
        } catch (SQLException e) {
            System.out.println("Seed data error: " + e.getMessage());
        }
    }

    private boolean existsEmail(Connection c, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email=?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    private String genAccountNumber() {
        // very simple generator using time, good enough for demo
        return String.valueOf(System.currentTimeMillis()).substring(3);
    }

    private void appendTxToFile(String line) {
        try (FileWriter fw = new FileWriter("transactions.txt", true)) {
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Could not write file: " + e.getMessage());
        }
    }
}


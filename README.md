# 🏦 BankEase – Java Console Banking System

![Java](https://img.shields.io/badge/Java-17-blue?logo=java&logoColor=white)
![JDBC](https://img.shields.io/badge/Database-JDBC-green?logo=mysql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**BankEase** is a console-based banking system written in **Java** with **database integration (JDBC)**.  
It simulates real-world banking operations such as deposits, withdrawals, transfers, account management, and transaction reporting.

---

## ✨ Features

### 👤 User
- Register with **name, email, password, and initial deposit**  
- Login with email & password  
- Deposit & withdraw funds  
- Transfer money to another account  
- View account balance  
- Show last **10 transactions**  

### 🛡️ Admin
- Admin login (`admin@bankease.com / admin123`)  
- View all users & search by email/account number  
- Freeze / unfreeze user accounts  
- View all transactions (audit trail)  
- Generate reports (transaction totals by type)  

### 🧾 Extra
- Transaction logs stored in both **Database** + `transactions.txt` file  
- Exception handling for **Insufficient Funds, Invalid Account, Frozen Account**  
- Balance updates synchronized for **data integrity**  

---

## 🛠️ Tech Stack

- **Language:** Java (OOP, Collections, Streams, Exception Handling)  
- **Database:** MySQL (via JDBC)  
- **Logging:** FileWriter (`transactions.txt`)  
- **Structure:** Modular service-based (`BankSystem`, `DatabaseManager`, `Exceptions`)  

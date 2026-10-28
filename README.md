# 🔄 SkillSwap — Peer-to-Peer Skill Matching & Communication Platform

> **OOP Micro-Project (Semester 3 Java)**  
> A distributed, client-server desktop application designed for collaborative peer learning. Users register skills they can teach and topics they want to learn; a matchmaking engine pairs complementary users and allows them to exchange messages and collaborate.

---

## 📌 Table of Contents
- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Object-Oriented Programming (OOP) Concepts](#-object-oriented-programming-oop-concepts)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [How to Run](#-how-to-run)
  - [Option A: Running on a Single Laptop (Localhost)](#option-a-running-on-a-single-laptop-localhost)
  - [Option B: Running Across Two Laptops (LAN / Wi-Fi)](#option-b-running-across-two-laptops-lan--wi-fi)
  - [Optional: MySQL Database Setup](#optional-mysql-database-setup)
- [Step-by-Step Usage & Demo Scenario](#-step-by-step-usage--demo-scenario)
- [Communication Protocol](#-communication-protocol)
- [Future Enhancements](#-future-enhancements)

---

## 📖 Overview

In academic and developer communities, students frequently want to learn new tools or concepts (e.g., Godot, Flutter, Data Structures, Graphic Design) while possessing proficiencies in other subjects (e.g., Unity, Java, Web Development). 

**SkillSwap** bridges this gap:
1. Each user submits their profile specifying:
   - **Offered Skills:** Subjects/tools they are confident teaching.
   - **Wanted Skills / Interests:** Subjects/tools they want to learn.
2. The server-side matching engine analyzes interest intersections across the user base.
3. Mutual matches are calculated with affinity scores, enabling students with reciprocal learning goals to connect directly and exchange messages.

---

## ✨ Key Features

- **🎯 Reciprocal Matchmaking Algorithm**: Identifies 100% two-way matches where User A teaches what User B wants and User B teaches what User A wants, sorted by match affinity percentage.
- **🔍 Partial / Demand Discovery**: View peers who want skills you offer, facilitating mentorship and skill discovery even when a 1:1 reciprocal match isn't available.
- **👥 Directory Listing**: Inspect all registered users and their learning goals across the network.
- **💬 Direct In-App Messaging**: Real-time communication and inbox management between peers to plan study sessions and exchange notes.
- **🌐 Cross-Device Networking**: Socket-based client-server design allows execution across multiple laptops connected to the same Wi-Fi or mobile hotspot.
- **💾 Hybrid Persistence (Zero-Config or MySQL)**: Runs completely in-memory with zero setup required, with automatic fallback and optional persistent storage using MySQL through JDBC.

---

## 🏗 System Architecture

The application adopts a 3-tier modular architecture:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Tier                    │
│      SkillSwapClient (Java Swing GUI Application)       │
└───────────────────────────▲────────────────────────────┘
                            │ Raw TCP Sockets (Port 5050)
┌───────────────────────────▼────────────────────────────┐
│                    Application Tier                    │
│    SkillSwapServer (Multithreaded Executor Pool)       │
│                           │                            │
│           ┌───────────────┴───────────────┐            │
│           ▼                               ▼            │
│      MatchEngine                    Data Router        │
│  (Reciprocal Matching,            (Direct Inboxes,     │
│   Affinity Scoring)                Message Queues)     │
└───────────────────────────▲────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                       Data Tier                        │
│   In-Memory Collections  ◄───Fallback───►  MySQL (JDBC)│
│ (ConcurrentHashMap & Sets)                (users table)│
└────────────────────────────────────────────────────────┘
```

---

## 🧩 Object-Oriented Programming (OOP) Concepts

This micro-project leverages core Java OOP and design principles:

1. **Encapsulation**:
   - `User`: Private attributes (`name`, `contact`, `offered`, `wanted`) accessed via read-only getters.
   - Collections are guarded against outside mutation using `Collections.unmodifiableSet()`.
2. **Separation of Concerns & Modularity**:
   - Clear decoupling into distinct responsibilities: GUI (`SkillSwapClient`), network protocol handling (`SkillSwapServer`), business & matching algorithms (`MatchEngine`), data model (`User`, `MatchResult`), and database bridge (`Database`).
3. **Abstraction & Interfaces**:
   - Extensive usage of Java Collections Framework interfaces (`Map`, `Set`, `List`) hiding underlying data structures (`ConcurrentHashMap`, `LinkedHashSet`, `CopyOnWriteArrayList`).
4. **Polymorphism & Natural Ordering**:
   - `MatchResult` implements `Comparable<MatchResult>`, overriding `compareTo()` to automatically order candidates by descending match affinity score.
5. **Concurrency & Thread Safety**:
   - Multi-client handling using `ExecutorService` thread pool.
   - Concurrent collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) prevent race conditions during simultaneous user registrations and message exchanges.

---

## 📁 Project Structure

| File | Description |
| :--- | :--- |
| [`User.java`](User.java) | Domain model encapsulating user identity, contact details, offered skills, and learning interests. |
| [`MatchResult.java`](MatchResult.java) | Result transfer object encapsulating peer match details, skill overlaps, and affinity score; implements `Comparable`. |
| [`MatchEngine.java`](MatchEngine.java) | Core matching algorithms (reciprocal 2-way matching, demand discovery) and in-memory message routing. |
| [`SkillSwapServer.java`](SkillSwapServer.java) | Multithreaded TCP socket server listening on port 5050; detects host IPs and handles client requests. |
| [`SkillSwapClient.java`](SkillSwapClient.java) | Swing-based graphical user interface for profile registration, matchmaking queries, directory viewing, and direct messaging. |
| [`Database.java`](Database.java) | JDBC persistence layer connecting to MySQL, with graceful fallback to in-memory mode if MySQL is not available. |

---

## ⚙️ Prerequisites

- **Java Development Kit (JDK 11 or higher)**: Make sure `javac` and `java` are available in your path.
  ```bash
  java -version
  javac -version
  ```
- *(Optional)* **MySQL Server & Connector/J**: Only required if persistent database storage is desired.

---

## 🚀 How to Run

### Step 1: Compile the Project

Open a terminal in the project directory:

```bash
javac *.java
```

---

### Option A: Running on a Single Laptop (Localhost)

1. **Start the Server**:
   ```bash
   java SkillSwapServer
   ```
   The server will start on port `5050` and display your local IP addresses.

2. **Start Client 1 (User 1)**:
   In a new terminal window:
   ```bash
   java SkillSwapClient
   ```
   Leave Server IP as `localhost`.

3. **Start Client 2 (User 2)**:
   In another terminal window:
   ```bash
   java SkillSwapClient
   ```
   Leave Server IP as `localhost`.

---

### Option B: Running Across Two Laptops (LAN / Wi-Fi)

1. **Network Setup**:
   Connect both Laptop 1 and Laptop 2 to the same Wi-Fi network (or one can connect to the other's mobile hotspot).

2. **Laptop 1 (Host / Server)**:
   Start the server:
   ```bash
   java SkillSwapServer
   ```
   Note down the IP address shown in the terminal (e.g., `192.168.1.15`).
   
   Start a client on Laptop 1:
   ```bash
   java SkillSwapClient
   ```

3. **Laptop 2 (Peer / Client)**:
   Run the client:
   ```bash
   java SkillSwapClient
   ```
   In the GUI's **Server IP** field, replace `localhost` with Laptop 1's IP address (e.g., `192.168.1.15`), and leave Port as `5050`.

---

### Optional: MySQL Database Setup

If you want user profiles to persist across server restarts:

1. Create a MySQL database:
   ```sql
   CREATE DATABASE skillswap;
   ```
2. In `Database.java`, ensure your MySQL `USER` and `PASS` credentials match your local MySQL configuration.
3. Compile and run with the MySQL JDBC Connector jar:
   ```bash
   javac -cp ".:mysql-connector-j.jar" *.java
   java -cp ".:mysql-connector-j.jar" SkillSwapServer
   ```
*(Note: On Windows, use `;` instead of `:` for classpath separator).*

---

## 🧪 Step-by-Step Usage & Demo Scenario

### 1. Register User 1
- **Name:** Dave
- **Contact:** `dave@college.edu`
- **Skills I Can Teach:** `Unity, C#`
- **Skills I Want To Learn:** `Godot Engine, Python`
- Click **"Save Profile"**.

### 2. Register User 2
- **Name:** Elena
- **Contact:** `elena@college.edu`
- **Skills I Can Teach:** `Godot Engine, Blender`
- **Skills I Want To Learn:** `Unity`
- Click **"Save Profile"**.

### 3. Match & Discover
- On Elena's client, click **"Find Matches"**:
  - The engine identifies Dave as a **100% Mutual Match**!
  - Details shown:
    - What Elena learns from Dave: `Unity`
    - What Dave learns from Elena: `Godot Engine`
    - Compatibility score and Dave's contact info.
- Click **"View All Users & Wanted Skills"** to view what everyone on the network wants to learn.

### 4. Exchange Messages
- On Elena's client:
  - In **"Send Message To"**, enter: `Dave`
  - In **"Message"**, enter: `Hi Dave! I saw you want to learn Godot. Let's do a skill swap session!`
  - Click **"Send Message"**.
- On Dave's client:
  - Click **"Check Inbox"**.
  - Dave sees the incoming message with timestamp.

---

## 📡 Communication Protocol

The server and clients communicate over plain-text line-based TCP socket commands:

| Command | Syntax | Description |
| :--- | :--- | :--- |
| `REGISTER` | `REGISTER <name>\|<contact>\|<offered,...>\|<wanted,...>` | Saves/updates a user's profile and skills. |
| `MATCH` | `MATCH <name>` | Computes 2-way reciprocal matches and 1-way demand for user. |
| `USERS` | `USERS` | Returns a list of all users and their wanted skills. |
| `SEND_MSG` | `SEND_MSG <from>\|<to>\|<message>` | Routes a direct message to recipient's inbox. |
| `GET_MSGS` | `GET_MSGS <name>` | Retrieves all pending/saved inbox messages for a user. |

---

## 🔮 Future Enhancements

- [ ] Real-time push notifications using persistent client listener threads (replacing pull-based "Check Inbox").
- [ ] User authentication with secure password hashing.
- [ ] Skill tags categorization (e.g., Programming, Music, Design, Academics).
- [ ] Rating and review system for completed skill-swap sessions.
- [ ] Session scheduling calendar integration.

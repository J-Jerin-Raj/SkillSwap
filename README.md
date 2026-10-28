# SkillSwap - Peer-to-Peer Skill Matching & Communication Platform

> **OOP Micro-Project (Semester 3 Java)**  
> A distributed, client-server desktop application designed for collaborative peer learning. Users register skills they can teach and topics they want to learn; a matchmaking engine pairs complementary users and allows them to exchange messages and collaborate.

---

## Table of Contents
- [Overview](#overview)
- [Course Outcomes (CO) Mapping](#course-outcomes-co-mapping)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Object-Oriented Programming (OOP) Concepts](#object-oriented-programming-oop-concepts)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [How to Run](#how-to-run)
  - [Option A: Running on a Single Laptop (Localhost)](#option-a-running-on-a-single-laptop-localhost)
  - [Option B: Running Across Two Laptops (LAN / Wi-Fi)](#option-b-running-across-two-laptops-lan--wi-fi)
  - [Optional: MySQL Database Setup](#optional-mysql-database-setup)
- [Step-by-Step Usage & Demo Scenario](#step-by-step-usage--demo-scenario)
- [Communication Protocol](#communication-protocol)
- [Future Enhancements](#future-enhancements)

---

## Overview

In academic and developer communities, students frequently want to learn new tools or concepts (e.g., Godot, Flutter, Data Structures, Graphic Design) while possessing proficiencies in other subjects (e.g., Unity, Java, Web Development). 

**SkillSwap** bridges this gap:
1. Each user submits their profile specifying:
   - **Credentials:** Username and Password authentication.
   - **Offered Skills:** Subjects/tools they are confident teaching.
   - **Wanted Skills / Interests:** Subjects/tools they want to learn.
2. The server-side matching engine analyzes interest intersections across the user base.
3. Mutual matches are calculated with affinity scores, enabling students with reciprocal learning goals to connect directly and exchange messages.

---

## Course Outcomes (CO) Mapping

This project incorporates all five Course Outcomes (CO1 to CO5) defined in the curriculum:

| Course Outcome | Description | Implementation in SkillSwap |
| :--- | :--- | :--- |
| **CO1** | **Structural Elements & Basic Constructs** | Enhanced switch expressions (`MatchResult.getCompatibilityLabel()`), control flow statements, for-each looping over skill collections, string normalization, and array splitting. |
| **CO2** | **OOP Principles & Hierarchy** | Abstract class (`AbstractAccount`), interfaces (`Authenticatable`, `Matchable`), inheritance (`User extends AbstractAccount implements Matchable`), encapsulation (`private final` fields, defensive copying with `Collections.unmodifiableSet()`), and static/final keywords. |
| **CO3** | **Robust Solutions & Concurrency** | User-defined exceptions (`AuthenticationException`, `InvalidProfileException`), multithreaded socket server (`ExecutorService`), thread synchronization (`synchronized` methods in `Database`), and 1-second background auto inbox poller (`javax.swing.Timer`). |
| **CO4** | **Collections, Generics & File I/O** | Java Collections Framework (`List`, `Set`, `Map`, `Queue`), generic class with type parameters (`ServiceResult<T>`), stream and lambda operations for skill filtering, and file I/O operations (`exportChatLogToFile()` using `BufferedWriter`). |
| **CO5** | **Networking, GUI & JDBC** | Multi-client TCP socket programming (`ServerSocket`, `Socket`), Swing GUI design (`JFrame`, `JTabbedPane`, `CardLayout`, `BorderLayout`, `BoxLayout`, `GridBagLayout`), event handling (`ActionListener`, `DocumentListener`), and JDBC database connectivity (`Database.java`). |

---

## Key Features

- **Modern Slate Dark UI**: Clean, contemporary desktop interface with rounded cards, avatar initials, colorful skill badges, and real-time interactive input chips (no emojis).
- **Startup Authentication Flow**: Dedicated Sign In and Sign Up views on client launch; requires username and password validation.
- **Multi-Skill Affinity Scoring**: Calculates comprehensive compatibility percentages (0-100%) incorporating reciprocal trades, mentor matches, learner demand, and shared study interests.
- **Auto-Checking Live Inbox**: Checks for new messages automatically every 1 second in the background without freezing the GUI.
- **Direct Messaging Integration**: Clicking "Message User" on any peer card directly switches to the chat module and focuses the input field.
- **Group Chat Module with 5-Letter Codes**: Dedicated "Group Chat" tab positioned between Direct Messaging and My Profile. Direct conversations automatically create a group room with a unique 5-letter code (e.g. `=== CHAT WITH ALICE [ABCDE] (3 message(s)) ===`). Any user can enter the 5-letter code to join the conversation.
- **Multi-Recipient Messaging**: Supports sending messages to multiple users simultaneously using comma-separated usernames (e.g. `Cara, Nate`), logged as `[timestamp] To Cara, Nate: hi (Sent Message)` and `[timestamp] From Dave: hi (Received Message)` without quotation marks.
- **Mirrored Profile Editor**: Allows users to update password, contact info, offered skills, and wanted interests with permanent greyed-out username.
- **File Export**: One-click export of chat history to a local text file.
- **Cross-Device Networking**: Socket-based client-server design allows execution across multiple laptops connected to the same Wi-Fi or mobile hotspot (Port 5050).
- **Hybrid Persistence (Zero-Config or MySQL)**: Runs completely in-memory with zero setup required, with automatic fallback and optional persistent storage using MySQL through JDBC.

---

## System Architecture

The application adopts a 3-tier modular architecture:

```
+--------------------------------------------------------+
|                   Presentation Tier                    |
|      SkillSwapClient (Java Swing GUI Application)       |
+---------------------------+----------------------------+
                            | Raw TCP Sockets (Port 5050)
+---------------------------v----------------------------+
|                    Application Tier                    |
|    SkillSwapServer (Multithreaded Executor Pool)       |
|                           |                            |
|           +---------------+---------------+            |
|           v                               v            |
|      MatchEngine                    Data Router        |
|  (Reciprocal Matching,            (Direct Inboxes,     |
|   Affinity Scoring)                Message Queues)     |
+---------------------------+----------------------------+
                            |
+---------------------------v----------------------------+
|                       Data Tier                        |
|   In-Memory Collections  <---Fallback--->  MySQL (JDBC)|
| (ConcurrentHashMap & Sets)                (users table)|
+--------------------------------------------------------+
```

---

## Object-Oriented Programming (OOP) Concepts

This micro-project leverages core Java OOP and design principles:

1. **Encapsulation**:
   - `User`: Private attributes accessed via read-only getters.
   - Collections are guarded against outside mutation using `Collections.unmodifiableSet()`.
2. **Inheritance & Abstraction**:
   - `AbstractAccount` serves as the base class for accounts, implementing `Authenticatable`.
   - `User` extends `AbstractAccount` and implements `Matchable`.
3. **Polymorphism & Natural Ordering**:
   - `MatchResult` implements `Comparable<MatchResult>`, overriding `compareTo()` to automatically order candidates by descending match affinity score.
4. **Exception Handling**:
   - Custom exceptions `AuthenticationException` and `InvalidProfileException` ensure strict validation.
5. **Concurrency & Thread Safety**:
   - Multi-client handling using `ExecutorService` thread pool.
   - Concurrent collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `ConcurrentLinkedQueue`) prevent race conditions during simultaneous user registrations and message exchanges.

---

## Project Structure

| File | Description |
| :--- | :--- |
| `AbstractAccount.java` | [CO2] Base abstract class encapsulating user identity, credentials, and contact info. |
| `Authenticatable.java` | [CO2] Interface defining credential verification contracts. |
| `Matchable.java` | [CO2] Interface defining skill supply and demand contracts. |
| `AuthenticationException.java` | [CO3] User-defined exception for invalid login attempts. |
| `InvalidProfileException.java` | [CO3] User-defined exception for validation errors in profiles. |
| `ServiceResult.java` | [CO4] Generic response wrapper demonstrating type parameters and static generic methods. |
| `GroupChat.java` | [CO2/CO4] Thread-safe multi-user chat room model managing unique 5-letter codes, member sets, and conversation history. |
| `User.java` | [CO2] Domain model inheriting `AbstractAccount` and implementing `Matchable`. |
| `MatchResult.java` | [CO1/CO4] Ranked match card with switch-based affinity categorization, implementing `Comparable`. |
| `MatchEngine.java` | [CO3/CO4] Matching engine calculating affinity scores, managing thread-safe inboxes, group chat rooms, and authentication. |
| `SkillSwapServer.java` | [CO3/CO5] Multithreaded TCP socket server listening on port 5050; handles client requests, group chats, and audit logging. |
| `SkillSwapClient.java` | [CO4/CO5] Modern Swing GUI with authentication, live tag previews, 1-sec auto inbox poller, Group Chat tab with 5-letter codes, and file export. |
| `Database.java` | [CO5] JDBC persistence layer connecting to MySQL with automatic fallback to in-memory mode. |

---

## Prerequisites

- **Java Development Kit (JDK 11 or higher)**: Make sure `javac` and `java` are available in your path.
  ```bash
  java -version
  javac -version
  ```
- *(Optional)* **MySQL Server & Connector/J**: Only required if persistent database storage is desired.

---

## How to Run

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
   Leave Server IP as `localhost`. Click on demo user **Alice** or register a new account.

3. **Start Client 2 (User 2)**:
   In another terminal window:
   ```bash
   java SkillSwapClient
   ```
   Leave Server IP as `localhost`. Click on demo user **Bob** or register a new account.

---

### Option B: Running Across Two Laptops (LAN / Wi-Fi)

1. **Network Setup**:
   Connect both Laptop 1 and Laptop 2 to the same Wi-Fi network (or mobile hotspot).

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

## Step-by-Step Usage & Demo Scenario

### 1. Register User 1
- Switch to **Create Profile (Sign Up)**:
  - **Name:** Dave
  - **Password:** `dave123`
  - **Contact:** `dave@college.edu`
  - **Skills I Can Teach:** `Unity, C#`
  - **Skills I Want To Learn:** `Godot Engine, Python`
- Click **Complete Sign Up & Enter Dashboard**.

### 2. Register User 2
- On Client 2, switch to **Create Profile (Sign Up)**:
  - **Name:** Elena
  - **Password:** `elena123`
  - **Contact:** `elena@college.edu`
  - **Skills I Can Teach:** `Godot Engine, Blender`
  - **Skills I Want To Learn:** `Unity`
- Click **Complete Sign Up & Enter Dashboard**.

### 3. Match & Discover
- On Elena's client, click **Find Matches**:
  - The engine identifies Dave as a **Reciprocal Match** with high affinity score.
  - Elena sees what Dave teaches (`Unity`) and what she teaches Dave (`Godot Engine`).
- Click **Message Dave**:
  - The client immediately transitions to the **Direct Messaging & Inbox** tab with Dave prefilled as the recipient.

### 4. Exchange Messages & Live Auto-Polling
- On Elena's client:
  - Type message: `Hi Dave! Let's swap skills on Unity and Godot.`
  - Click **Send Message**.
- On Dave's client:
  - Dave's inbox updates automatically within 1 second without clicking anything.
- Dave clicks **Export Chat Log** to save the conversation to a local file.

---

## Communication Protocol

The server and clients communicate over plain-text line-based TCP socket commands:

| Command | Syntax | Description |
| :--- | :--- | :--- |
| `LOGIN` | `LOGIN <name>\|<password>` | Authenticates user credentials; returns profile details if valid. |
| `REGISTER` | `REGISTER <name>\|<password>\|<contact>\|<offered,...>\|<wanted,...>` | Saves or updates a user profile with multiple skills & interests. |
| `MATCH_DATA` | `MATCH_DATA <name>` | Returns structured match cards for GUI rendering with Affinity Scores. |
| `MATCH` | `MATCH <name>` | Computes complete text report with affinity rankings. |
| `USERS` | `USERS` | Returns a list of all users and their offered & wanted skills. |
| `SEND_MSG` | `SEND_MSG <from>\|<to>\|<message>` | Routes message to single user, comma-separated users (`Cara, Nate`), or 5-letter group code. |
| `GET_MSGS` | `GET_MSGS <name>[\|<partnerOrCode>]` | Retrieves inbox messages with header `=== CHAT WITH <TARGET> [CODE] (<count> message(s)) ===`. |
| `JOIN_GROUP` | `JOIN_GROUP <name>\|<code5>` | Adds a user to an active conversation using its 5-letter code. |
| `GET_GROUP_MSGS` | `GET_GROUP_MSGS <name>\|<code5>` | Retrieves complete multi-user chat room history. |
| `GET_USER_GROUPS` | `GET_USER_GROUPS <name>` | Returns all active group rooms that the user belongs to. |

---

## Future Enhancements

- WebSocket push notifications for sub-second latency.
- SHA-256 password salting and hashing.
- Skill categories and tags taxonomy.
- Peer rating and feedback system.

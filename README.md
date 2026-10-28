========================================================================
SkillSwap - Peer-to-Peer Skill Matching & Communication Across Laptops
========================================================================

1. COMPILE:
   javac *.java

   (Optional: To use MySQL:
    CREATE DATABASE skillswap;
    javac -cp ".;mysql-connector-j.jar" *.java
    If MySQL is not installed, the server automatically runs in-memory!)

2. RUNNING ON TWO LAPTOPS:
   ----------------------------------------------------------------------
   STEP 1: Connect both laptops to the same Wi-Fi network (or mobile hotspot).

   STEP 2 (LAPTOP 1 - Server):
   Open a terminal and start the server:
       java SkillSwapServer

   Look at the terminal output. It will show your Laptop IP, for example:
       Your Laptop IP Address(es):
          -> 192.168.1.15   (or 10.2.5.239)

   STEP 3 (LAPTOP 1 - Client):
   Start the client on Laptop 1:
       java SkillSwapClient
   (Leaves Server IP as 'localhost' or your IP)

   STEP 4 (LAPTOP 2 - Other Person):
   Start the client on Laptop 2:
       java SkillSwapClient
   In the "Server IP" field at the top, type Laptop 1's IP address
   (e.g., 192.168.1.15).

3. HOW TO SEE WHAT THE OTHER GUY WANTS:
   ----------------------------------------------------------------------
   - Click "View All Users & Wanted Skills":
     Displays all users on the network along with the exact skills they
     WANT to learn and skills they OFFER to teach.

   - Click "Find Matches":
     Finds 100% mutual matches with match affinity score, and also shows
     partial matches: users who want the skills you offer!

   - Direct Messaging:
     Enter the other person's name in "Send Message To", type your message,
     and click "Send Message".
     The other person clicks "Check Inbox" to read your messages.

4. DEMO TEST:
   - Laptop 1 registers "Dave", offering "Unity", wanting "Godot Engine".
   - Laptop 2 registers "Elena", offering "Godot Engine", wanting "Unity".
   - Click "Find Matches" -> Instant 100% match!
   - Elena can see Dave's wanted skills, Dave can see Elena's wanted skills,
     and both can exchange messages in real-time.
========================================================================

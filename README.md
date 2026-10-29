# SkillSwap
A Java application made for Communication between Users to Learn what you want to learn and provide others with the knowledge you have, It's a application of Knowledge Exchange.

SkillSwap - run steps
1. Compile:   javac *.java
   (To use MySQL: download mysql-connector-j.jar, create DB:  CREATE DATABASE skillswap;
    edit password in Database.java, then compile/run with  -cp ".:mysql-connector-j.jar"  (Windows: ";"))
2. Start server:   java SkillSwapServer
3. Start client(s):   java SkillSwapClient     (open several to test multi-client)
4. Demo: register "Dave" offering "Unity", wanting "Godot Engine" -> matches Alice 100%.

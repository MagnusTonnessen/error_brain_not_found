# INF112 Maven template 

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/79c6c521bd28435980909af2043e6c5e)](https://www.codacy.com/gh/MagnusTonnessen/error_brain_not_found/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=MagnusTonnessen/error_brain_not_found&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.com/inf112-v20/error_brain_not_found.svg?branch=develop)](https://travis-ci.com/inf112-v20/error_brain_not_found)  
Simple skeleton with libgdx. 

## How to run
The program runs `java` and `maven`, these need to be installed.  
Every text-editor that can run version control system, can be used. We recommend IntelliJ IDEA.  
Clone the repo from github to you computer: `git clone https://github.com/inf112-v20/error_brain_not_found.git`  
Then open `pom.xml` in IntelliJ and the IntelliJ will build the project for you.  
Then open the repo and go to `src/main/java/inf112/skeleton/app/Main.java` and run `main` function.

## How to set up the game  
Multiplayer:  
The game runs over local area networks with IPv4, so you need to be connected to the same network in order to play. 
If you want to start a game, you press "Create". Your IP address will be displayed 
 to you.  There needs to be one person hosting the game before anyone can join.  
 If you are hosting the game give your IP address to you friend(s) and tell them to 
 join the game with this IP address. When everyone has joined the game you can press start.


## Known bugs
Currently throws "WARNING: An illegal reflective access operation has occurred", 
when the java version used is >8. This has no effect on function or performance, and is just a warning.

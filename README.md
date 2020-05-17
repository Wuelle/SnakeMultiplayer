# SnakeMultiplayer
Java implementation of a snake-like multiplayer game.:snake::computer:

__Usage:__
1. Compile the .java files to get .class files using `javac filename.java`

2. Start the server. The 'Server.class' program expects two parameters: mapsize and maxPlayers. It can be launched by running
`java Server mapsize maxPlayers`

3. Connect the correct amount of clients to the server. A client can be started using `java Game`.
  Once started, a popup will ask you to enter your username and the Server IP. Duplicate names may cause problems.
  If you did that, you should see the waiting screen. The Game will automatically start once enough players are connected.

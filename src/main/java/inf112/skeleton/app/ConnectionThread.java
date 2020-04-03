package inf112.skeleton.app;

import java.util.Scanner;

public class ConnectionThread extends Thread {

    private int numberOfPlayers;

    public ConnectionThread(int numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }

    public void run(){
        GameServer server = new GameServer(this.numberOfPlayers-1);

        server.connect(9000);
        // Let all clients start game with same amount of players.
        server.broadcastMessage(numberOfPlayers+"");
    }


}

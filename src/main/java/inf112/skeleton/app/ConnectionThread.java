package inf112.skeleton.app;

/**
 * Make a new thread to make server connect to clients so that game.create()
 * can finish, and the menu-screen is shown.
 * @author  Jenny
 */
public class ConnectionThread extends Thread {

    private int numberOfPlayers;
    private GameServer server;
    private RallyGame game;

    public ConnectionThread(RallyGame game, int numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
        this.game = game;
    }

    /**
     * What the thread is doing when it is started.
     */
    public void run(){
        this.server = new GameServer(game);
        int numberOfClients = this.numberOfPlayers-1;
        server.connect(9000, numberOfClients);
    }

    /**
     *
     * @return the gameserver handling the connections.
     */
    public GameServer getServer() {
        return server;
    }
}

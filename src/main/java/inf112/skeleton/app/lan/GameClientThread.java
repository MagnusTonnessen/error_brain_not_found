package inf112.skeleton.app.lan;

import inf112.skeleton.app.RallyGame;
import inf112.skeleton.app.cards.ProgramCard;
import inf112.skeleton.app.enums.Messages;
import inf112.skeleton.app.objects.player.Player;
import inf112.skeleton.app.screens.menuscreen.MenuScreenActors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Stack;
import java.util.concurrent.Semaphore;

/**
 * Own thread for a client so client can get continous updates from server.
 */
public class GameClientThread extends Thread {

    private final Socket clientSideSocket;
    private int myPlayerNumber;
    private PrintWriter writer;
    private BufferedReader reader;
    private final RallyGame game;
    private final Converter converter;
    private final Semaphore continueListening;
    private Stack<ProgramCard> stack;
    private boolean receivingDeck;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public GameClientThread(RallyGame game, Socket clientSideSocket) {
        this.clientSideSocket = clientSideSocket;
        this.game = game;
        this.converter = new Converter();
        this.continueListening = new Semaphore(1);
        continueListening.tryAcquire();
        try {
            this.writer = new PrintWriter(clientSideSocket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(clientSideSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listen for messages from server. If no messages match, it is a {@link ProgramCard} and should
     * be added to the player is belongs to (it is discarded if it is yours, since you have selected cards)
     *
     */
    @Override
    public void run() {

        while (true) {
            String message = getMessage();
            if (message == null) {
                break;
            }
            if (message.equals(Messages.STOP_THREAD.toString())) {
                return;
            }
            if (message.equals(Messages.HOST_LEAVES.toString())) {
                printMessageFinishTurnAndCloseSocket();
                return;
            }
            if (message.equals(Messages.CONTINUE_TURN.toString())) {
                game.continueTurn();
                waitForTurnToFinish();
            }
            else if (message.equals(Messages.START_TURN.toString())) {
                game.startTurn();
                waitForTurnToFinish();
            }
            else if (message.contains(Messages.HERE_IS_MAP.toString())){
                giveMapToGameAndTellMapIsReceived(message);
            }
            else if (message.equals(Messages.DECK_BEGIN.toString())) {
                createNewDeckAndWaitForCardsForThisDeck();
            }
            else if (message.equals(Messages.DECK_END.toString())){
                giveDeckToGameAndTellStartValuesAreReceived();
            }
            else if (message.contains(Messages.YOUR_NUMBER.toString())) {
                myPlayerNumber = converter.getMyPlayerNumber(message);
                game.setPlayerNumber(myPlayerNumber);
            }
            else if (message.contains(Messages.PLAYERS.toString())) {
                game.setNumberOfPlayers(converter.getNumbersOfPlayers(message));
            }
            else if (receivingDeck) {
                addReceivedCardToDeck(message);
            }
            else if (converter.isMessageFromAnotherPlayer(message)) {
                int playerNumber = converter.getPlayerNumberFromMessage(message);
                Player player = game.getBoard().getPlayer(playerNumber);
                String messageFromPlayer = converter.getMessageFromPlayer(message);
                if (messageFromPlayer.equals(Messages.QUIT.toString())) {
                    printMessageFinishTurnAndCloseSocket(playerNumber);
                    return;
                }
                if (messageFromPlayer.equals(Messages.POWERING_DOWN.toString())) {
                    player.setPoweringDown(true);
                    game.displayPlayersPoweringDown();
                }
                else if (messageFromPlayer.equals(Messages.POWER_UP.toString())) {
                    player.setPoweredDown(false);
                    game.removePoweredDownPlayer(player);
                }
                else {
                    try {
                        ProgramCard card = converter.convertToCard(messageFromPlayer);
                        // Your player have already selected cards
                        if (myPlayerNumber != playerNumber) {
                            player.addSelectedCard(card);
                        }
                    } catch (NotProgramCardException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void createNewDeckAndWaitForCardsForThisDeck() {
        this.stack = new Stack<>();
        this.receivingDeck = true;
    }

    /**
     * When startValues have been received call on {@link MenuScreenActors#haveReceivedStartValues()}
     * so that we can receive the mapPath.
     */
    public void giveDeckToGameAndTellStartValuesAreReceived() {
        this.receivingDeck = false;
        game.setDeck(this.stack);
        System.out.println("Received deck.");
        if (game.getMenuScreenActors() != null) {
            game.getMenuScreenActors().haveReceivedStartValues();
        }
    }

    /**
     * Get the card from the message using {@link Converter#convertToCard(String)} and add it to Deck
     *
     * @param message to interpret
     */
    public void addReceivedCardToDeck(String message) {
        try {
            ProgramCard card = converter.convertToCard(message);
            this.stack.add(card);
        } catch (NotProgramCardException e) {
            e.printStackTrace();
        }
    }

    /**
     * When mapPath is received we can release {@link MenuScreenActors#haveReceivedMapPath()} and game can begin.
     *
     * @param message from server
     */
    public void giveMapToGameAndTellMapIsReceived(String message) {
        game.setMapPath(converter.getMapPath(message));
        System.out.println("Got map");
        game.getMenuScreenActors().haveReceivedMapPath();
    }

    /**
     * When another player leaves then finish turn and close.
     *
     * @param playernumber player who leaves
     */
    private void printMessageFinishTurnAndCloseSocket(int playernumber) {
        System.out.println(playernumber + " left game.");
        game.quitPlaying();
        close();
    }

    /**
     * When host has left game then finish turn and close.
     */
    public void printMessageFinishTurnAndCloseSocket() {
        System.out.println("Host left game.");
        game.quitPlaying();
        close();
    }

    /**
     * Wait for doTurn to realease in game.
     */
    private void waitForTurnToFinish() {
        try {
            continueListening.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Let client continue loop
     */
    public void continueListening() {
        continueListening.release();
    }

    /**
     *
     * @return message from this socket. Close socket if error.
     */
    public String getMessage() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            try {
                // Close socket if exception
                clientSideSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * Send a message to server.
     * @param message message to be sent
     */
    public void sendMessage(String message) {
        writer.println(message);
    }

    /**
     * Close the socket.
     */
    public void close() {
        try {
            this.clientSideSocket.close();
            System.out.println(Messages.CLOSED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return the deck received from server
     */
    public Stack<ProgramCard> getStackOfDeck() {
        return stack;
    }
}

package inf112.skeleton.app;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import inf112.skeleton.app.board.Board;
import inf112.skeleton.app.cards.Deck;
import inf112.skeleton.app.cards.ProgramCard;
import inf112.skeleton.app.enums.Direction;
import inf112.skeleton.app.objects.Belt;
import inf112.skeleton.app.objects.Laser;
import inf112.skeleton.app.objects.RotatePad;
import inf112.skeleton.app.objects.player.Player;
import inf112.skeleton.app.objects.player.PlayerSorter;
import inf112.skeleton.app.screens.ButtonSkin;
import inf112.skeleton.app.screens.gifscreen.GifScreen;
import inf112.skeleton.app.screens.menuscreen.MenuScreen;
import inf112.skeleton.app.screens.standardscreen.StandardScreen;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class RallyGame extends Game {

    public Board board;
    public Deck deck;

    public ArrayList<Player> players;
    public ArrayList<Player> respawnPlayers;
    public Semaphore waitForCards;
    public boolean playing;
    public Sound laserSound;
    public Music gameMusic;
    public Player mainPlayer;

    public ButtonSkin buttonSkins;

    public static float volume = 0.5f;

    public void create() {
        this.buttonSkins = new ButtonSkin();
        this.setScreen(new MenuScreen(this));
        startMusic();
    }

    public void setupGame(String mapPath) {
        this.board = new Board(mapPath, 1);
        this.deck = new Deck();
        this.players = board.getPlayers();
        this.mainPlayer = board.getPlayer1();
        this.respawnPlayers = new ArrayList<>();

        this.waitForCards = new Semaphore(1);
        this.waitForCards.tryAcquire();
        this.playing = true;

        this.laserSound = Gdx.audio.newSound(Gdx.files.internal("assets/Sound/LaserShot.mp3"));

        new Thread(this::doTurn).start();

        dealCards();
    }

    public void confirmCards() {
        if (!mainPlayer.getRegisters().hasRegistersWithoutCard()) {
            cardsReady();
        }
    }

    @Override
    public void setScreen(Screen screen) {
        if (this.screen != null) {
            this.screen.dispose();
        }
        super.setScreen(screen);
    }

    public StandardScreen getScreen() {
        return (StandardScreen) super.getScreen();
    }

    public void muteMusic() {
        gameMusic.setVolume(gameMusic.getVolume() == 0 ? 0.5f : 0);
    }

    public void muteSounds() {
        volume = volume == 0 ? 0.5f : 0;
    }

    public void loadMusic() {
        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("assets/sound/menu_music.mp3"));
    }

    public void startMusic() {
        loadMusic();
        gameMusic.setVolume(0.5f);
        gameMusic.setLooping(true);
        gameMusic.play();
    }

    private void cardsReady() {
        waitForCards.release();
    }

    public void doTurn() {
        /*
        1. Deal the Program cards.
        2. Arrange your Program cards face down among your
           five registers.
        3. Announce intent to power down or continue running
           NEXT turn.
        4. Complete each register in order:
                A. Reveal Program Cards
                B. Robots Move
                C. board Elements Move (Gears, Express belt, normal belt)
                    1. Express conveyor belts move 1 space in the direction of the arrows.
                    2. Express conveyor belts and normal conveyor belts move 1 space in the
                       direction of the arrows.
                    3. Pushers push if active.
                    4. Gears rotate 90° in the direction of the arrows.
                D. Lasers Fire (player, then board)
                E. Touch Checkpoints (Flag, Repair)
        5. Clean up any end-of-turn effects
        */
        while (playing) {
            try {
                waitForCards.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (Thread.interrupted()) {
                return;
            }
            for (int i = 0; i < 5; i++) {

                System.out.println("Runde " + (i + 1));

                // All players play one card in the correct order
                allPlayersPlayCard(i);
                sleep(250);

                // Express belts move 1
                activateBelts(true);
                sleep(250);

                decreaseLives();

                // All belts move 1
                activateBelts(false);
                sleep(250);

                // Rotate pads rotate
                activateRotatePads();
                sleep(250);

                // Fire lasers for 250 ms
                firePlayerLaser();
                sleep(250);
                removeLasers();
                sleep(500);

                decreaseLives();

                // Fire lasers for 250 ms
                if (!board.getLasers().isEmpty()) {
                    fireLasers();
                    sleep(250);
                    removeLasers();
                    sleep(500);

                    decreaseLives();
                }

                pickUpFlags();
                sleep(500);

                sleep(1000);
            }
            respawnPlayers();
            discardCards();
            dealCards();
        }
    }

    private void pickUpFlags() {
        board.pickUpFlags();
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void dealCards() {
        for (Player player : players) {
            player.drawCards(deck);
        }
    }

    public void discardCards() {
        for (Player player : players) {
            player.discardAllCards(deck);
        }
    }

    /**
     * Decrease life tokens to each player that has collected 10 damage tokens.
     * Reset damage tokens, remove player from board and discard all cards
     */
    public void decreaseLives() {
        ArrayList<Player> removedPlayers = new ArrayList<>();
        for (Player player : players) {
            if (player.getDamageTokens() >= 10 || board.getBoardLogic().outsideBoard(player, board)) {
                player.decrementLifeTokens();
                player.resetDamageTokens();
                player.discardAllCards(deck);
                board.removePlayerFromBoard(player);
                removedPlayers.add(player);
            }
        }
        players.removeAll(removedPlayers);
        respawnPlayers.addAll(removedPlayers);
    }

    public void respawnPlayers() {
        for (Player player : respawnPlayers) {
            if (!player.isDead()) {
                players.add(player);
                board.respawn(player);
            }
        }
        respawnPlayers.clear();
    }

    public void allPlayersPlayCard(int cardNumber) {
        ArrayList<Player> playerOrder = new ArrayList<>(players);
        // Add all players to order list, and remove players with no cards left
        // playerOrder.removeIf(p -> p.getSelectedCards().isEmpty());
        playerOrder.sort(new PlayerSorter());
        for (Player player : playerOrder) {
            playCard(player, cardNumber);
            // Wait 1 second for each player
            sleep(500);
            decreaseLives();
            sleep(500);
        }
    }

    public void playCard(Player player, int cardNumber) {
        ProgramCard card = player.getRegisters().getCard(cardNumber);
        System.out.println(player.toString() + " played " + card.toString());
        switch (card.getRotate()) {
            case RIGHT:
                player.setDirection(player.getDirection().turnRight());
                break;
            case LEFT:
                player.setDirection(player.getDirection().turnLeft());
                break;
            case UTURN:
                player.setDirection(player.getDirection().turnAround());
                break;
            case NONE:
                for (int i = 0; i < card.getDistance(); i++) {
                    board.movePlayer(player);
                    // Wait 300 ms for each move except last one
                    if (i < card.getDistance() - 1) {
                        sleep(300);
                    }
                }
                break;
            default:
                break;
        }
        board.addPlayer(player);
        deck.addCardToDiscardPile(card);
    }

    public void setWinScreen() {
        setScreen(new GifScreen(this));
    }

    public void removeLasers() {
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                board.getLaserLayer().setCell(x, y, null);
            }
        }
    }

    public void firePlayerLaser() {
        if (!players.isEmpty()) {
            for (Player player : players) {
                player.fire(this);
            }
            laserSound.play(volume);
        }
    }

    public void fireLasers() {
        if (!board.getLasers().isEmpty()) {
            for (Laser laser : board.getLasers()) {
                laser.fire(this);
            }
            laserSound.play(volume);
        }
    }

    public void activateRotatePads() {
        for (Player player : board.getPlayers()) {
            for (RotatePad pad : board.getRotatePads()) {
                Vector2 playerPosition = player.getPosition();
                Vector2 padPosition = pad.getPosition();

                if (playerPosition.equals(padPosition)) {
                    pad.rotate(player);
                    board.addPlayer(player);
                    sleep(500);
                }
            }
        }
    }

    /**
     * <p>
     *     Activate the belts on the map, so they pushes the player in the direction of the belt.
     * </p>
     * @param onlyExpress if true then the pool of belts should be set to expressBelts
     */
    public void activateBelts(boolean onlyExpress) {
        ArrayList<Belt> belts = onlyExpress ? board.getExpressBelts() : board.getBelts();
        for (Player player : board.getPlayers()) {
            for (Belt belt : belts) {
                if (player.getPosition().equals(belt.getPosition())) {
                    beltPush(player, belt);
                }
            }
        }
        validateBeltPushPos();
        updateBoard();
    }

    private void updateBoard() {
        board.removePlayersFromBoard();
        updatePositionsAfterBeltPush();
        board.updateBoard();
    }

    public void validateBeltPushPos() {
        for (Player player : players) {
            for (Player otherPlayer : players) {
                if (!player.equals(otherPlayer) && player.getBeltPushPos().equals(otherPlayer.getBeltPushPos())) {
                    player.setBeltPushPos(null);
                    otherPlayer.setBeltPushPos(null);
                }
            }
        }
    }

    public void updatePositionsAfterBeltPush() {
        for (Player player : players) {
            if (player.getBeltPushPos() != null) {
                player.setPosition(player.getBeltPushPos());
                player.setBeltPushPos(null);
            }
        }
    }

    public void beltPush(Player player, Belt belt) {
        Direction lastPush = player.getBeltPushDir();
        Direction beltDirection = belt.getDirection();
        if (lastPush != null) {
            switch (lastPush) {
                case NORTH:
                    setPlayerDirectionAfterBeltPush(player, beltDirection, Direction.EAST, Direction.WEST);
                    break;
                case SOUTH:
                    setPlayerDirectionAfterBeltPush(player, beltDirection, Direction.WEST, Direction.EAST);
                    break;
                case EAST:
                    setPlayerDirectionAfterBeltPush(player, beltDirection, Direction.SOUTH, Direction.NORTH);
                    break;
                case WEST:
                    setPlayerDirectionAfterBeltPush(player, beltDirection, Direction.NORTH, Direction.SOUTH);
                    break;
                default:
                    break;
            }
        }
        player.setBeltPushDir(beltDirection);
        player.setBeltPushPos(board.getNeighbourPosition(player.getPosition(), beltDirection));
    }

    public void setPlayerDirectionAfterBeltPush(Player player, Direction beltDirection, Direction turnRight, Direction leftTurn) {
        if (beltDirection.equals(turnRight)) {
            player.setDirection(player.getDirection().turnRight());
        } else if (beltDirection.equals(leftTurn)) {
            player.setDirection(player.getDirection().turnLeft());
        }
    }

    public void dispose() {
        try {
            gameMusic.dispose();
            laserSound.dispose();
            screen.dispose();
            board.dispose();
        } catch (Exception ignored) {
        }
    }

    public Board getBoard() {
        return this.board;
    }
}

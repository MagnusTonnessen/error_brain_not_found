package inf112.skeleton.app.objects.player;


import com.badlogic.gdx.math.Vector2;
import inf112.skeleton.app.RallyGame;
import inf112.skeleton.app.board.Board;
import inf112.skeleton.app.cards.Deck;
import inf112.skeleton.app.cards.ProgramCard;
import inf112.skeleton.app.cards.Registers;
import inf112.skeleton.app.enums.Direction;
import inf112.skeleton.app.enums.TileID;
import inf112.skeleton.app.objects.Flag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Player {

    private final int playerNumber;
    private final Registers registers;
    private boolean confirmedPowerUpOrContinuePowerDown;
    private Vector2 backupPosition;
    private Direction backupDirection;
    private Vector2 alternativeBackupPosition;
    private Direction alternativeBackupDirection;
    private Vector2 position;
    private Direction direction;
    private final ArrayList<Flag> flagsCollected;
    private final ArrayList<ProgramCard> cardsOnHand;
    private int programCardsDealt;
    private Direction beltPushDir;
    private Vector2 beltPushPos;
    private boolean poweringDown;
    private boolean poweredDown;
    private boolean powerDownNextRound;
    private boolean powerUpNextRound;
    private final HashMap<Direction, Integer> tiles;

    private int damageTokens;
    private int lifeTokens;
    private final String color;

    public Player(Vector2 position, int playerNumber) {
        this(position, playerNumber, "white");
    }

    public Player(Vector2 position, int playerNumber, String color) {
        this.position = position;
        this.direction = Direction.EAST;
        this.playerNumber = playerNumber;
        this.flagsCollected = new ArrayList<>();
        this.registers = new Registers();
        this.cardsOnHand = new ArrayList<>();
        this.damageTokens = 0;
        this.lifeTokens = 3;
        this.beltPushDir = null;
        this.beltPushPos = null;
        this.programCardsDealt = 9;
        this.poweringDown = false;
        this.poweredDown = false;
        this.powerUpNextRound = false;
        this.powerDownNextRound = false;
        this.tiles = TileID.getRobotId(color);
        this.confirmedPowerUpOrContinuePowerDown = false;
        this.color = color;

        setBackup(this.position, this.direction);
    }

    public int getTileInt() {
        return tiles.get(direction);
    }

    public ArrayList<ProgramCard> getCardsOnHand() {
        return cardsOnHand;
    }

    public int getProgramCardsDealt() {
        updateProgramCardsDealt();
        return programCardsDealt;
    }

    public Registers getRegisters() {
        return registers;
    }

    public void updateRegisters() {
        registers.updateRegisters(damageTokens);
    }

    public void updateProgramCardsDealt() {
        this.programCardsDealt = 9 - damageTokens;
    }

    /**
     * Select cards from all cards.
     */
    public void selectCard(ProgramCard card) {
        if (registers.contains(card) && registers.hasRegistersWithoutCard()) {
            registers.addCard(card);
        } else {
            registers.remove(card);
        }
    }

    public void selectCards() {
        for (int i = 0; i < registers.getOpenRegisters(); i++) {
            registers.addCard(cardsOnHand.get(i));
        }
    }

    /**
     * Add a card to your program if program is not full.
     * @param card to add.
     */
    public void addSelectedCard(ProgramCard card) {
        registers.addCard(card);
    }

    public void drawCards(Deck deck) {
        updateProgramCardsDealt();
        while (cardsOnHand.size() < programCardsDealt) {
            cardsOnHand.add(deck.drawCard());
        }
    }

    public Direction getBeltPushDir() {
        return beltPushDir;
    }

    public void setBeltPushDir(Direction direction) {
        this.beltPushDir = direction;
    }

    public Vector2 getBeltPushPos() {
        return beltPushPos;
    }

    public void setBeltPushPos(Vector2 position) {
        this.beltPushPos = position;
    }

    public ArrayList<ProgramCard> discardCards(Deck deck) {
        for (ProgramCard card : cardsOnHand) {
            if (registers.contains(card) || registers.getRegister(card).isOpen()) {
                deck.addCardToDiscardPile(card);
            }
        }
        cardsOnHand.clear();
        registers.clear(true);
        return registers.getCards();
    }

    /**
     * a int on how many damageTokens
     *
     * @return your damageTokens
     */
    public int getDamageTokens() {
        return damageTokens;
    }

    public void resetDamageTokens() {
        this.damageTokens = 0;
    }

    public void decrementDamageTokens() {
        this.damageTokens--;
    }

    public int getLifeTokens() {
        return lifeTokens;
    }

    public void decrementLifeTokens() {
        this.lifeTokens--;
    }

    public boolean isDead() {
        return lifeTokens <= 0;
    }

    public void handleDamage() {
        this.damageTokens++;
    }

    /**
     * Set new backup position and direction
     *
     * @param backupPosition  respawn position when damaged
     * @param backupDirection respawn direction when damaged
     */
    public void setBackup(Vector2 backupPosition, Direction backupDirection) {
        if (this.backupPosition == null) {
            this.backupPosition = new Vector2(backupPosition);
        } else {
            this.backupPosition.set(backupPosition.x, backupPosition.y);
        }
        this.backupDirection = backupDirection;
    }

    /**
     * @return backup position
     */
    public Vector2 getBackupPosition() {
        return backupPosition;
    }

    /**
     * @return backup direction
     */
    public Direction getBackupDirection() {
        return backupDirection;
    }

    public void setAlternativeBackup(Vector2 alternativeBackupPosition, Direction alternativeBackupDirection) {
        if (this.alternativeBackupPosition == null) {
            this.alternativeBackupPosition = new Vector2(alternativeBackupPosition);
        } else {
            this.alternativeBackupPosition.set(alternativeBackupPosition.x, alternativeBackupPosition.y);
        }
        this.alternativeBackupDirection = alternativeBackupDirection;
    }

    /**
     * @return alternative backup position
     */
    public Vector2 getAlternativeBackupPosition() {
        return alternativeBackupPosition;
    }

    /**
     * @return alternative backup direction
     */
    public Direction getAlternativeBackupDirection() {
        return alternativeBackupDirection;
    }


    public void chooseAlternativeBackupPosition(Board board, Vector2 position) {
        ArrayList<Vector2> possiblePositions = board.getNeighbourhood(position);
        Collections.shuffle(possiblePositions);
        for (Vector2 pos : possiblePositions) {
            for (Direction dir : Direction.getDirectionRandomOrder()) {
                if (board.validRespawnPosition(pos, dir)) {
                    setAlternativeBackup(pos, dir);
                    return;
                }
            }
        }
        setAlternativeBackup(board.getStartPosition(getPlayerNumber()), Direction.EAST);
        if (board.hasPlayer(board.getStartPosition(getPlayerNumber()))) {
            chooseAlternativeBackupPosition(board, alternativeBackupPosition);
        }
    }

    /**
     * @return number of player
     */
    public int getPlayerNumber() {
        return playerNumber;
    }

    /**
     * @return the {@link Vector2} to the player
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Set's the position to the player
     */
    public void setPosition(Vector2 pos) {
        this.position = new Vector2(pos);
    }

    /**
     * @return the direction the player are facing
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Set's the direction the player are suppose to face
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean shouldPickUpFlag(Flag flag) {
        return flag.getFlagnr() == flagsCollected.size() + 1;
    }

    public void pickUpFlag(Flag flag) {
        System.out.println("Player " + playerNumber + " picked up flag " + flag.getFlagnr());
        flagsCollected.add(flag);
    }

    public void tryToPickUpFlag(Flag flag) {
        if (shouldPickUpFlag(flag)) {
            pickUpFlag(flag);
        }
    }

    public void fire(RallyGame game) {
        if (game.getBoard().canFire(position, direction)) {
            fire(game, game.getBoard().getNeighbourPosition(position, direction));
        }
    }

    public void fire(RallyGame game, Vector2 position) {
        game.getBoard().addLaser(position, direction);
        if (game.getBoard().hasPlayer(position)) {
            game.hitByLaser.play(game.getSoundVolume());
            game.getBoard().getPlayer(position).handleDamage();
        } else if (game.getBoard().canFire(position, direction)) {
            fire(game, game.getBoard().getNeighbourPosition(position, direction));
        }
    }

    /**
     * Update the selected cards for this player.
     * Used for testing, so that we can decide what card the player is going to play, and then test that the player
     * does what the card says.
     *
     * @param cards one or more cards (separated by comma) or a list of cards.
     */
    public void setSelectedCards(ProgramCard... cards) {
        registers.setSelectedCards(cards);
    }

    public ArrayList<Flag> getFlagsCollected() {
        return flagsCollected;
    }

    public boolean hasAllFlags(int numberOfFlags) {
        return flagsCollected.size() == numberOfFlags;
    }

    public boolean equals(Player other) {
        return this.getPlayerNumber() == other.getPlayerNumber();
    }

    /**
     *
     * @return true if player is in backup position and backup direction
     */
    public boolean isInBackupState() {
        return getPosition().equals(getBackupPosition()) && getDirection() == getBackupDirection();
    }

    public String toString() {
        return "Player " + getPlayerNumber();
    }

    public boolean isPoweringDown() {
        return poweringDown;
    }

    public void setPoweringDown(boolean poweringDown) {
        this.poweringDown = poweringDown;
    }

    public boolean isPoweredDown() {
        return poweredDown;
    }

    public void setPoweredDown(boolean poweredDown) {
        this.poweredDown = poweredDown;
    }

    public boolean getPowerDownNextRound() {
        return powerDownNextRound;
    }

    public boolean getPowerUpNextRound() {
        return powerUpNextRound;
    }

    public void setPowerDownNextRound(boolean powerDownNextRound) {
        this.powerDownNextRound = powerDownNextRound;
    }

    public void setPowerUpNextRound(boolean powerUpNextRound) {
        this.powerUpNextRound = powerUpNextRound;
    }

    public void togglePowerDownOrUpNextRound() {
        if (poweredDown) {
            setPowerUpNextRound(!getPowerUpNextRound());
        } else {
            setPowerDownNextRound(!getPowerDownNextRound());
        }
    }

    public boolean hasConfirmedPowerUpOrContinuePowerDown() {
        return !confirmedPowerUpOrContinuePowerDown;
    }

    public void setConfirmedPowerUpOrContinuePowerDown(boolean confirmedPowerUpOrContinuePowerDown) {
        this.confirmedPowerUpOrContinuePowerDown = confirmedPowerUpOrContinuePowerDown;
    }

    public String getColor() {
        return this.color;
    }
}

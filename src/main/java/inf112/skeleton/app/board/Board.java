package inf112.skeleton.app.board;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Vector2;
import inf112.skeleton.app.RallyGame;
import inf112.skeleton.app.enums.Direction;
import inf112.skeleton.app.enums.TileID;
import inf112.skeleton.app.objects.Flag;
import inf112.skeleton.app.objects.player.Player;

import java.util.ArrayList;

public class Board extends BoardLayers {

    private final ArrayList<Player> players;

    private final Sound wallImpact = Gdx.audio.newSound(Gdx.files.internal("assets/Sound/ImpactWall.mp3"));

    private final BoardLogic boardLogic = new BoardLogic(this);

    public Board(String mapPath) {
        super(mapPath);

        players = new ArrayList<>();
    }

    private TiledMapTile getRobotTile(Player player) {
        TiledMapTileSet tileSet = tiledMap.getTileSets().getTileSet("robots");
        switch (player.getDirection()) {
            case SOUTH:
                return tileSet.getTile(TileID.PLAYER_SOUTH.getId());
            case NORTH:
                return tileSet.getTile(TileID.PLAYER_NORTH.getId());
            case EAST:
                return tileSet.getTile(TileID.PLAYER_EAST.getId());
            case WEST:
                return tileSet.getTile(TileID.PLAYER_WEST.getId());
            default:
                return null;
        }
    }

    /**
     * Add a player to the player layer in coordinate (x, y) and
     * add that player to the list of players
     *
     * @param player to add to game and board
     */
    public void addPlayer(Player player) {
        TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
        cell.setTile(getRobotTile(player));
        playerLayer.setCell((int) player.getPosition().x, (int) player.getPosition().y, cell);
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    /**
     * Check all cells on map for start positions with {@link TileID} and add a new player to that
     * position based on number of players
     *
     * @param players list of robots playing, between 1-8
     */
    public void addPlayersToStartPositions(ArrayList<Player> players) {
        for (Player player : players) {
            player.setPosition(getStartPosition(player.getPlayerNumber()));
            addPlayer(player);
        }
    }

    public Vector2 getStartPosition(int number) {
        for (int x = 0; x < groundLayer.getWidth(); x++) {
            for (int y = 0; y < groundLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = groundLayer.getCell(x, y);
                int ID = cell.getTile().getId();
                if (number == 1 && ID == TileID.START_POS1.getId()) {
                    return new Vector2(x, y);
                } else if (number == 2 && ID == TileID.START_POS2.getId()) {
                    return new Vector2(x, y);
                } else if (number == 3 && ID == TileID.START_POS3.getId()) {
                    return new Vector2(x, y);
                } else if (number == 4 && ID == TileID.START_POS4.getId()) {
                    return new Vector2(x, y);
                } else if (number == 5 && ID == TileID.START_POS5.getId()) {
                    return new Vector2(x, y);
                } else if (number == 6 && ID == TileID.START_POS6.getId()) {
                    return new Vector2(x, y);
                } else if (number == 7 && ID == TileID.START_POS7.getId()) {
                    return new Vector2(x, y);
                } else if (number == 8 && ID == TileID.START_POS8.getId()) {
                    return new Vector2(x, y);
                }
            }
        }
        return null;
    }

    /**
     * Add laser in position in the right direction
     *
     * @param position  to add laser
     * @param direction of laser
     */
    public void addLaser(Vector2 position, Direction direction) {
        TiledMapTileLayer.Cell cell = laserLayer.getCell((int) position.x, (int) position.y);
        TiledMapTileSet tileSet = tiledMap.getTileSets().getTileSet("tiles");
        if (cell == null) {
            cell = new TiledMapTileLayer.Cell();
        }
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            if (cell.getTile() == null) {
                cell.setTile(tileSet.getTile(TileID.VERTICAL_LASER.getId()));
            } else if (cell.getTile().getId() == TileID.HORIZONTAL_LASER.getId()) {
                cell.setTile(tileSet.getTile(TileID.CROSSED_LASER.getId()));
            }
        } else {
            if (cell.getTile() == null) {
                cell.setTile(tileSet.getTile(TileID.HORIZONTAL_LASER.getId()));
            } else if (cell.getTile().getId() == TileID.VERTICAL_LASER.getId()) {
                cell.setTile(tileSet.getTile(TileID.CROSSED_LASER.getId()));
            }
        }
        laserLayer.setCell((int) position.x, (int) position.y, cell);
    }

    /**
     * Places a player in backup position or alternative position
     *
     * @param player to respawn
     */
    public void respawn(Player player) {
        removePlayerFromBoard(player);
        if (hasPlayer(player.getBackupPosition())) {
            player.chooseAlternativeBackupPosition(this, player.getBackupPosition(), boardLogic);
            player.setPosition(new Vector2(player.getAlternativeBackupPosition().x, player.getAlternativeBackupPosition().y));
            player.setDirection(player.getAlternativeBackupDirection());
        } else {
            player.setPosition(new Vector2(player.getBackupPosition().x, player.getBackupPosition().y));
            player.setDirection(player.getBackupDirection());
        }
        addPlayer(player);
    }

    /**
     * Check if it is possible to move in the direction the player are facing.
     * Check if player should and can push another player, if not return
     * Remove player from board
     * Update player position according to direction
     * Add player to cell that corresponds to player position
     *
     * @param player that is suppose to move
     */
    public void movePlayer(Player player, boolean backUp) {
        Vector2 position = player.getPosition();
        Direction direction = backUp ? player.getDirection().turnAround() : player.getDirection();

        if (!boardLogic.canGo(position, direction)) {
            wallImpact.play(RallyGame.volume);
            addPlayer(player);
            return;
        }
        if (boardLogic.shouldPush(player)) {
            Player enemyPlayer = getPlayer(getNeighbourPosition(player.getPosition(), direction));
            if (boardLogic.canPush(enemyPlayer, direction)) {
                boardLogic.pushPlayer(enemyPlayer, direction);
            } else {
                addPlayer(player);
                return;
            }
        }

        removePlayerFromBoard(player);

        switch (direction) {
            case NORTH:
                position.y++;
                break;
            case EAST:
                position.x++;
                break;
            case WEST:
                position.x--;
                break;
            case SOUTH:
                position.y--;
                break;
            default:
                break;
        }

        player.setPosition(position);
        addPlayer(player);
        player.setBeltPushDir(null);
    }

    public void updateBoard() {
        for (Player player : players) {
            addPlayer(player);
        }
    }

    public void removePlayersFromBoard() {
        for (Player player : players) {
            removePlayerFromBoard(player);
        }
    }

    public ArrayList<Vector2> getNeighbourhood(Vector2 position) {
        ArrayList<Vector2> positions = new ArrayList<>();
        for (int yi = -1; yi <= 1; yi++) {
            for (int xi = -1; xi <= 1; xi++) {
                int x = (int) (position.x + xi);
                int y = (int) (position.y + yi);
                if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                    positions.add(new Vector2(x, y));
                }
            }
        }
        return positions;
    }

    /**
     * @param position  to go from
     * @param direction to go in
     * @return neighbour position in direction from position
     */
    public Vector2 getNeighbourPosition(Vector2 position, Direction direction) {
        Vector2 neighbourPosition = new Vector2(position);
        switch (direction) {
            case EAST:
                neighbourPosition.x++;
                break;
            case WEST:
                neighbourPosition.x--;
                break;
            case NORTH:
                neighbourPosition.y++;
                break;
            case SOUTH:
                neighbourPosition.y--;
                break;
            default:
                break;
        }
        return neighbourPosition;
    }

    public boolean hasPlayer(Vector2 position) {
        for (Player enemyPlayer : players) {
            if (enemyPlayer.getPosition().equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return player if there is a player in that position
     *
     * @param position to check
     * @return player in position
     */
    public Player getPlayer(Vector2 position) {
        for (Player enemyPlayer : players) {
            if (enemyPlayer.getPosition().equals(position)) {
                return enemyPlayer;
            }
        }
        return null;
    }

    /**
     * Remove player from board
     *
     * @param player to remove from board
     */
    public void removePlayerFromBoard(Player player) {
        playerLayer.setCell((int) player.getPosition().x, (int) player.getPosition().y, null);
    }

    // TODO: Denne brukes ikke
    public boolean hasFlag(Vector2 position) {
        return flagLayer.getCell((int) position.x, (int) position.y) != null;
    }

    // TODO: Denne brukes ikke
    public Flag getFlag(Vector2 position) {
        for (Flag flag : flags) {
            if (flag.getPosition().equals(position)) {
                return flag;
            }
        }
        return null;
    }

    // TODO: Denne brukes ikke
    public void tryToPickUpFlag(Player player) {
        Flag flag = getFlag(player.getPosition());
        if (player.shouldPickUpFlag(flag)) {
            player.pickUpFlag(flag);
        }
    }

    // TODO: Denne brukes ikke
    public void pickUpFlags(Player player) {
        if (hasFlag(player.getPosition())) {
            tryToPickUpFlag(player);
        }
    }

    // TODO: Denne brukes ikke, flyttet lyden over til RallyGame::decreaseLives
    public void respawnPlayers() {
        for (Player player : players) {
            if (boardLogic.outsideBoard(player)) {
                player.decrementLifeTokens();
                respawn(player);
            }
        }
    }

    // TODO: Denne kan slettes når LAN merges inn
    /**
     * @return player that should be moved with arrows
     */
    public Player getPlayer1() {
        for (Player player : players) {
            if (player.getPlayerNumber() == 1) {
                return player;
            }
        }
        return null;
    }

    // TODO: Denne kan slettes
    /**
     * Make new player and add player to game and board
     *
     * @param x            coordinate
     * @param y            coordinate
     * @param playerNumber of player
     */
    public void addPlayer(int x, int y, int playerNumber) {
        if (!boardLogic.validPlayerNumber(playerNumber, players)) {
            return;
        }
        Player player = new Player(new Vector2(x, y), playerNumber);
        addPlayer(player);
    }

    public BoardLogic getBoardLogic() {
        return boardLogic;
    }

    /**
     * @return list of all players
     */
    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void dispose() {
        wallImpact.dispose();
        tiledMap.dispose();
    }
}
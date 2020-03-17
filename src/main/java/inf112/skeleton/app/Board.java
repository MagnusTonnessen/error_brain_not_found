package inf112.skeleton.app;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Vector2;
import inf112.skeleton.app.enums.Direction;
import inf112.skeleton.app.enums.TileID;
import inf112.skeleton.app.objects.Flag;

import java.util.ArrayList;

public class Board extends BoardLayers {

    private ArrayList<Player> players;

    public Board(String mapPath, int numberOfPlayers) {
        super(mapPath);

        this.players = new ArrayList<>();

        laserLayer.setVisible(false);

        addPlayersToStartPositions(numberOfPlayers);
    }

    private TiledMapTile getRobotTile(Player player) {
        TiledMapTileSet tileSet = this.tiledMap.getTileSets().getTileSet("robots");
        switch (player.getDirection()) {
            case SOUTH:
                return tileSet.getTile(140);
            case NORTH:
                return tileSet.getTile(141);
            case EAST:
                return tileSet.getTile(142);
            case WEST:
                return tileSet.getTile(143);
            default:
                return null;
        }
    }

    /**
     * Make new player and add player to game and board
     *
     * @param x            coordinate
     * @param y            coordinate
     * @param playerNumber of player
     */
    public void addPlayer(int x, int y, int playerNumber) {
        Player player = new Player(new Vector2(x, y), playerNumber);
        addPlayer(player);
    }

    /**
     * Add a player to the player layer in coordinate (x, y) and
     * add that player to the list of players
     *
     * @param player to add to game and board
     */
    public void addPlayer(Player player) {
        if (outsideBoard(player)) {
            respawn(player);
        }
        TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
        cell.setTile(getRobotTile(player));
        playerLayer.setCell((int) player.getPosition().x, (int) player.getPosition().y, cell);
        //TODO: Implement equals method for player
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    /**
     * Check all cells on map for start positions with {@link TileID} and add a new player to that
     * position based on number of players
     *
     * @param numPlayers number of robots playing, between 1-8
     */
    public void addPlayersToStartPositions(int numPlayers) {
        for (int x = 0; x < groundLayer.getWidth(); x++) {
            for (int y = 0; y < groundLayer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = groundLayer.getCell(x, y);
                int ID = cell.getTile().getId();
                //TODO: Check that numPlayers > 0
                if (ID == TileID.START_POS1.getId()) {
                    addPlayer(x, y, 1);
                } else if (ID == TileID.START_POS2.getId() && numPlayers > 1) {
                    addPlayer(x, y, 2);
                } else if (ID == TileID.START_POS3.getId() && numPlayers > 2) {
                    addPlayer(x, y, 3);
                } else if (ID == TileID.START_POS4.getId() && numPlayers > 3) {
                    addPlayer(x, y, 4);
                } else if (ID == TileID.START_POS5.getId() && numPlayers > 4) {
                    addPlayer(x, y, 5);
                } else if (ID == TileID.START_POS6.getId() && numPlayers > 5) {
                    addPlayer(x, y, 6);
                } else if (ID == TileID.START_POS7.getId() && numPlayers > 6) {
                    addPlayer(x, y, 7);
                } else if (ID == TileID.START_POS8.getId() && numPlayers > 7) {
                    addPlayer(x, y, 8);
                }
            }
        }
    }

    /**
     * Checks if player moves on to a hole
     * @param player that is checked
     * @return true if the player moves onto a hole
     */
    private boolean onHole(Player player) {
        Vector2 position = player.getPosition();
        for (Vector2 vector : holes){
            if (vector.equals(position)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if player is outside of board
     *
     * @param player to check
     */
    public boolean outsideBoard(Player player) {
        return player.getPosition().x < 0 ||
                player.getPosition().x >= this.boardWidth ||
                player.getPosition().y < 0 ||
                player.getPosition().y >= this.boardHeight ||
                onHole(player);
    }

    /**
     * Places a player in backup position
     *
     * @param player to respawn
     */
    private void respawn(Player player) {
        player.setPosition(new Vector2(player.getBackupPosition().x, player.getBackupPosition().y));
        player.setDirection(player.getBackupDirection());
    }

    /**
     * @return player that should be moved with arrows
     */
    public Player getPlayer1() {
        for (Player player : players) {
            if (player.getPlayerNr() == 1) {
                return player;
            }
        }
        return players.get(0);
    }

    /**
     * @param position  to go from
     * @param direction to go in
     * @return true if there is no wall blocking the way
     */
    public boolean canGo(Vector2 position, Direction direction) {
        TiledMapTileLayer.Cell cell = wallLayer.getCell((int) position.x, (int) position.y);
        TiledMapTileLayer.Cell northCell = wallLayer.getCell((int) position.x, (int) position.y + 1);
        TiledMapTileLayer.Cell southCell = wallLayer.getCell((int) position.x, (int) position.y - 1);
        TiledMapTileLayer.Cell eastCell = wallLayer.getCell((int) position.x + 1, (int) position.y);
        TiledMapTileLayer.Cell westCell = wallLayer.getCell((int) position.x - 1, (int) position.y);

        switch (direction) {
            case NORTH:
                if (hasNorthWall(cell) || hasSouthWall(northCell)) {
                    return false;
                }
                break;
            case SOUTH:
                if (hasSouthWall(cell) || hasNorthWall(southCell)) {
                    return false;
                }
                break;
            case EAST:
                if (hasEastWall(cell) || hasWestWall(eastCell)) {
                    return false;
                }
                break;
            case WEST:
                if (hasWestWall(cell) || hasEastWall(westCell)) {
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * @param cell to check for wall
     * @return true if cell has a wall on west side
     */
    public boolean hasWestWall(TiledMapTileLayer.Cell cell) {
        if (cell != null) {
            int tileID = cell.getTile().getId();
            return tileID == TileID.WEST_WALL.getId() ||
                    tileID == TileID.NORTHWEST_WALL.getId() ||
                    tileID == TileID.SOUTHWEST_WALL.getId() ||
                    tileID == TileID.WEST_LASER_WALL.getId();
        }
        return false;
    }

    /**
     * @param cell to check for wall
     * @return true if cell has a wall on east side
     */
    public boolean hasEastWall(TiledMapTileLayer.Cell cell) {
        if (cell != null) {
            int tileID = cell.getTile().getId();
            return tileID == TileID.EAST_WALL.getId() ||
                    tileID == TileID.NORTHEAST_WALL.getId() ||
                    tileID == TileID.SOUTHEAST_WALL.getId() ||
                    tileID == TileID.EAST_LASER_WALL.getId();
        }
        return false;
    }

    /**
     * @param cell to check for wall
     * @return true if cell has a wall on south side
     */
    public boolean hasSouthWall(TiledMapTileLayer.Cell cell) {
        if (cell != null) {
            int tileID = cell.getTile().getId();
            return tileID == TileID.SOUTH_WALL.getId() ||
                    tileID == TileID.SOUTHWEST_WALL.getId() ||
                    tileID == TileID.SOUTHEAST_WALL.getId() ||
                    tileID == TileID.SOUTH_LASER_WALL.getId();
        }
        return false;
    }

    /**
     * @param cell to check for wall
     * @return true if cell has a wall on north side
     */
    public boolean hasNorthWall(TiledMapTileLayer.Cell cell) {
        if (cell != null) {
            int tileID = cell.getTile().getId();
            return tileID == TileID.NORTH_WALL.getId() ||
                    tileID == TileID.NORTHWEST_WALL.getId() ||
                    tileID == TileID.NORTHEAST_WALL.getId() ||
                    tileID == TileID.NORTH_LASER_WALL.getId();
        }
        return false;
    }


    /**
     * Add player to the board, so the direction is correct
     * @param player
     */
    public void rotatePlayer(Player player) {
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
    public void movePlayer(Player player) {
        Vector2 position = player.getPosition();
        Direction direction = player.getDirection();

        if (!canGo(position, direction)) {
            addPlayer(player);
            return;
        }
        if (shouldPush(player)) {
            Player enemyPlayer = getPlayer(getNeighbourPosition(player.getPosition(), direction));
            if (canPush(enemyPlayer, direction)) {
                pushPlayer(enemyPlayer, direction);
            } else {
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

        if (hasFlag(player.getPosition())) {
            pickUpFlag(player);
        }
    }

    /**
     * @param position  to go from
     * @param direction to go in
     * @return neighbour position in direction from position
     */
    private Vector2 getNeighbourPosition(Vector2 position, Direction direction) {
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

    private boolean hasPlayer(Vector2 position) {
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
     * Check if the moving player should try to push another player
     *
     * @param player trying to move
     * @return true if player should push another player to move
     */
    private boolean shouldPush(Player player) {
        return hasPlayer(getNeighbourPosition(player.getPosition(), player.getDirection()));
    }

    /**
     * @param player    that should be pushed
     * @param direction to push in
     * @return true if it is possible to push all enemies in a row
     */
    private boolean canPush(Player player, Direction direction) {
        if (hasPlayer(getNeighbourPosition(player.getPosition(), direction))) {
            return canPush(getPlayer(getNeighbourPosition(player.getPosition(), direction)), direction);
        }
        return canGo(player.getPosition(), direction);
    }

    /**
     * Push all players in a row
     * Remove player from board
     * Update player position according to direction
     * Add player to cell that corresponds to player position
     *
     * @param player    that should be pushed
     * @param direction to push player in
     */
    private void pushPlayer(Player player, Direction direction) {
        if (hasPlayer(getNeighbourPosition(player.getPosition(), direction))) {
            pushPlayer(getPlayer(getNeighbourPosition(player.getPosition(), direction)), direction);
        }
        removePlayerFromBoard(player);
        switch (direction) {
            case SOUTH:
                player.setPosition(new Vector2(player.getPosition().x, player.getPosition().y - 1));
                break;
            case WEST:
                player.setPosition(new Vector2(player.getPosition().x - 1, player.getPosition().y));
                break;
            case EAST:
                player.setPosition(new Vector2(player.getPosition().x + 1, player.getPosition().y));
                break;
            case NORTH:
                player.setPosition(new Vector2(player.getPosition().x, player.getPosition().y + 1));
                break;
            default:
                break;
        }
        addPlayer(player);
    }

    /**
     * Remove player from board
     *
     * @param player to remove from board
     */
    public void removePlayerFromBoard(Player player) {
        playerLayer.setCell((int) player.getPosition().x, (int) player.getPosition().y, null);
    }

    public boolean hasFlag(Vector2 position) {
        return flagLayer.getCell((int) position.x, (int) position.y) != null;
    }

    public Flag getFlag(Vector2 position) {
        for (Flag flag : flags) {
            if (flag.getPosition().equals(position)) {
                return flag;
            }
        }
        return null;
    }

    public void pickUpFlag(Player player) {
        Flag flag = getFlag(player.getPosition());
        player.pickUpFlag(getFlag(player.getPosition()), flag.getFlagnr());
    }

    public ArrayList<Flag> getFlags(){
        return flags;
    }

    /**
     * @return list of all players
     */
    public ArrayList<Player> getPlayers() {
        return players;
    }

    /**
     * @return {@link TiledMapTileLayer} of player layer
     */
    public TiledMapTileLayer getPlayerLayer() {
        return playerLayer;
    }

    /**
     * @return {@link TiledMapTileLayer} of flag layer
     */
    public TiledMapTileLayer getFlagLayer() {
        return flagLayer;
    }

    /**
     * @return {@link TiledMapTileLayer} of laser layer
     */
    public TiledMapTileLayer getLaserLayer() {
        return laserLayer;
    }

    /**
     * @return {@link TiledMapTileLayer} of wall layer
     */
    public TiledMapTileLayer getWallLayer() {
        return wallLayer;
    }

    /**
     * @return {@link TiledMapTileLayer} of ground layer
     */
    public TiledMapTileLayer getGroundLayer() {
        return groundLayer;
    }

    /**
     * @return width of the board
     */
    public int getWidth() {
        return boardWidth;
    }

    /**
     * @return height of the board
     */
    public int getHeight() {
        return boardHeight;
    }

    @Override
    public TiledMap getMap() {
        return tiledMap;
    }
}
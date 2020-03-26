package inf112.skeleton.app.objects;

import com.badlogic.gdx.math.Vector2;
import inf112.skeleton.app.RallyGame;
import inf112.skeleton.app.enums.Direction;

public class Laser {
    private Vector2 startPosition;
    private Direction direction;

    public Laser(int x, int y, Direction direction) {
        this.startPosition = new Vector2(x, y);
        this.direction = direction;
    }

    public void fire(RallyGame game) {
        fire(game, startPosition);
    }

    public void fire(RallyGame game, Vector2 position) {
        game.getBoard().addLaser(position, direction);
        if (game.getBoard().hasPlayer(position)) {

        } else if (game.getBoard().canGo(position, this.direction)) {
            fire(game, game.getBoard().getNeighbourPosition(position, direction));
        }
    }
}

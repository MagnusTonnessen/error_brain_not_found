package inf112.skeleton.app.screens.gamescreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import inf112.skeleton.app.RallyGame;
import inf112.skeleton.app.screens.standardscreen.StandardScreen;

public class GameScreen extends StandardScreen {

    private final TiledMapRenderer mapRenderer;
    private final GameScreenActors actors;

    public GameScreen(final RallyGame game) {
        super(game);

        actors = new GameScreenActors(game, stage);
        if (game.haveReceivedDeck()) {
            actors.initializeProgramCardButtons();
        }
        actors.initializeConfirmButton();
        actors.initializeLifeTokens();
        actors.initializeDamageTokens();

        float tilePx = 300f;
        float unitScale = ((float) Gdx.graphics.getHeight() / game.getBoard().getHeight()) / tilePx;

        this.mapRenderer = new OrthogonalTiledMapRenderer(game.getBoard().getMap(), unitScale, batch);
        this.mapRenderer.setView(camera);
    }

    @Override
    public void render(float v) {
        if (game.haveReceivedDeck()) {
            actors.initializeProgramCardButtons();
        }
        actors.updateButtons();
        super.render(v);
        mapRenderer.render();
    }

    public void initializeCardButtons() {
        actors.initializeProgramCardButtons();
    }
}

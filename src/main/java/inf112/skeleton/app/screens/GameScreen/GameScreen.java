package inf112.skeleton.app.screens.GameScreen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import inf112.skeleton.app.RallyGame;
import inf112.skeleton.app.screens.StandardScreen.StandardScreen;

public class GameScreen extends StandardScreen {

    private TiledMapRenderer mapRenderer;

    public GameScreen(final RallyGame game) {
        super(game);

        GameScreenActors actors = new GameScreenActors(game, stage);
        actors.initializeProgramCardButtons();
        actors.initializeConfirmButton();
        actors.initializeLifeTokens();
        actors.initializeDamageTokens();

        float tileHeight = 300f;
        float prefTileHeight = (float) Gdx.graphics.getHeight() / game.getBoard().boardHeight;
        float unitScale = prefTileHeight / tileHeight;

        this.mapRenderer = new OrthogonalTiledMapRenderer(game.getBoard().getMap(), unitScale, batch);
        this.mapRenderer.setView(camera);
    }

    @Override
    public void render(float v) {
        super.render(v);
        game.checkForWinner();
        mapRenderer.render();
    }
}

package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo
import com.ribbontek.spaceinvaders.util.SpriteSheet

/**
 * The Scores Screen displays the top 5 best scoring results for the user
 *
 * The scores are retrieved from the game database
 */
class ScoresScreen(
    override val game: SpaceInvadersGameDemo,
    override val movingBackground: MovingBackground
) : SimpleScreen() {

    companion object {
        private const val TITLE_FONT_SIZE: Int = 150
        private const val TITLE_TEXT: String = "SCORES"
    }

    private lateinit var spaceshipLogo: Texture
    private lateinit var titleFont: BitmapFont
    private lateinit var titleLayout: GlyphLayout
    private lateinit var uiSprites: SpriteSheet
    private lateinit var stage: Stage
    private lateinit var glassySkin: Skin

    override fun show() {
        createCamera()
        createFonts()
        createBackground()
        createSpaceshipLogo()
        createSprites()
        createStage()
    }

    override fun render(delta: Float) {
        game.batch.begin()
        renderCamera()
        renderMovingBackground(delta)
        renderLogo()
        game.batch.draw(
            uiSprites.getSpriteRegion("glassPanel_cornerBL.png"),
            (Gdx.app.graphics.width - titleLayout.width) / 2f - 100f,
            Gdx.app.graphics.height - titleLayout.height - 275f,
            titleLayout.width + 200f,
            titleLayout.height * 2f
        )
        titleFont.draw(
            game.batch,
            TITLE_TEXT,
            (Gdx.app.graphics.width - titleLayout.width) / 2f,
            Gdx.app.graphics.height - 250f
        )
        stage.act(delta)
        stage.draw()
        game.batch.end()
    }

    override fun dispose() {
        disposeAll()
    }

    private fun createStage() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        glassySkin = Skin(Gdx.files.internal("glassy-ui/glassy-ui.json"))
        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)
        game.getScores().takeIf { it.isNotEmpty() }?.values?.sortedByDescending { it.cloudsHit }?.take(5)?.let {
            root.add(Label("Top 5 Scores", glassySkin, "big")).pad(25f).height(150f)
            root.row()
            root.add(it.createScoresTable())
            root.row()
        } ?: run {
            root.add(Label("Start playing & see your scores here!", glassySkin, "big")).pad(25f)
            root.row()
        }
        root.row()

        val buttonTable = Table().apply {
            defaults().pad(25.0f)
            add(createExitButton()).width(250f).height(100f)
            row()
        }
        root.add(buttonTable).expandX()
        root.row()
        addDisposable(glassySkin, stage)
    }

    private fun createSprites() {
        uiSprites = SpriteSheet("ui_sprite.xml")
        addDisposable(uiSprites)
    }

    private fun createSpaceshipLogo() {
        spaceshipLogo = Texture(Gdx.files.internal("spaceship/megaport_red.png"))
        addDisposable(spaceshipLogo)
    }

    private fun renderLogo() {
        game.batch.draw(
            spaceshipLogo,
            (Gdx.app.graphics.width - 250f) / 2f,
            250f,
            250f,
            250f
        )
    }

    private fun createFonts() {
        val fontFile = Gdx.files.internal("data/space_age.ttf")
        val generator = FreeTypeFontGenerator(fontFile)
        titleFont = generator.generateFont(
            FreeTypeFontGenerator.FreeTypeFontParameter().apply { size = TITLE_FONT_SIZE }
        )
        titleFont.data.setScale(1.2f, 1.2f)
        titleLayout = GlyphLayout(titleFont, TITLE_TEXT)
        generator.dispose()
        addDisposable(titleFont)
    }

    private fun List<GameState>.createScoresTable(): Table {
        return Table().apply {
            defaults().pad(25.0f)
            add(Label("Score", glassySkin, "big"))
            add(Label("Time", glassySkin, "big"))
            add(Label("Level", glassySkin, "big"))
            row()
            this@createScoresTable.forEach { score ->
                add(Label("${score.cloudsHit}", glassySkin, "big"))
                add(Label(score.readableDurationString(), glassySkin, "big"))
                add(Label("${score.levelReached}", glassySkin, "big"))
                row()
            }
        }
    }

    private fun createExitButton(): TextButton {
        return TextButton("Back", glassySkin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    Gdx.app.log(LOG_TAG, "$text Button Pressed")
                    game.screen = MainMenuScreen(game, movingBackground)
                    dispose()
                }
            })
        }
    }
}

package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo
import com.ribbontek.spaceinvaders.util.SpriteSheet

/**
 * This Main Screen is the entry point of the app & displays navigational options for the game
 */
class MainMenuScreen(
    override val game: SpaceInvadersGameDemo,
    override val movingBackground: MovingBackground = MovingBackground(speed = 5),
) : SimpleScreen() {

    companion object {
        private const val TITLE_SIZE: Int = 100
        private const val TITLE_TEXT: String = "Welcome to \nSpace Invaders"
    }

    private lateinit var titleFont: BitmapFont
    private lateinit var titleLayout: GlyphLayout
    private lateinit var spaceshipLogo: Texture
    private lateinit var uiSprites: SpriteSheet
    private lateinit var stage: Stage
    private lateinit var skin: Skin

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
            Gdx.app.graphics.height - 500f,
            titleLayout.width + 200f,
            titleLayout.height * 1.9f
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

    private fun createSprites() {
        uiSprites = SpriteSheet("ui_sprite.xml")
        addDisposable(uiSprites)
    }

    override fun dispose() {
        disposeAll()
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
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = TITLE_SIZE
        titleFont = generator.generateFont(parameter)
        titleFont.data.setScale(1.2f, 1.2f)
        titleLayout = GlyphLayout(titleFont, TITLE_TEXT)
        generator.dispose()
        addDisposable(titleFont)
    }

    private fun createStage() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        skin = Skin(Gdx.files.internal("glassy-ui/glassy-ui.json"))
        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)

        val table = createMenuButtonTable(
            createMenuTextButton("Play") {
                game.screen = SpaceInvadersGameScreen(game, movingBackground)
                dispose()
            },
            createMenuTextButton("Scores") {
                game.screen = ScoresScreen(game, movingBackground)
                dispose()
            },
            createMenuTextButton("Options") {
                game.screen = OptionsScreen(game, movingBackground)
                dispose()
            },
            createMenuTextButton("Quit") {
                Gdx.app.exit()
            }
        )
        root.add(table).expandX()
        addDisposable(skin, stage)
    }

    private fun createMenuButtonTable(vararg buttons: TextButton): Table {
        return Table().apply {
            defaults().pad(25.0f)
            buttons.forEach {
                add(it).width(500f).height(200f)
                row()
            }
        }
    }

    private fun createMenuTextButton(text: String, action: () -> Any): TextButton {
        return TextButton(text, skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    Gdx.app.log(LOG_TAG, "$text Button Pressed")
                    action()
                }
            })
            label.setFontScale(1.8f)
        }
    }
}

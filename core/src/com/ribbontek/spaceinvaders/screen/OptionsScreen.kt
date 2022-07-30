package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo
import com.ribbontek.spaceinvaders.db.Option
import com.ribbontek.spaceinvaders.util.SpriteSheet

/**
 * The Options Screen is a way to change the game's settings
 *
 * The options are retrieved & stored from the game database
 */
class OptionsScreen(
    override val game: SpaceInvadersGameDemo,
    override val movingBackground: MovingBackground
) : SimpleScreen() {

    companion object {
        private const val TITLE_FONT_SIZE: Int = 150
        private const val TITLE_TEXT: String = "OPTIONS"
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
        game.getOptions().takeIf { it.isNotEmpty() }
            ?.let {
                root.add(it.createOptionsTable())
                root.row()
            } ?: run {
            root.add(Label("You have no options here!", glassySkin, "big")).pad(25f)
            root.row()
        }
        root.row()

        val buttonTable = Table().apply {
            defaults().pad(25.0f)
            add(createExitButton())
            row()
        }
        root.add(buttonTable).expandX()
        root.row()
        addDisposable(glassySkin, stage)
    }

    private fun Map<Option, Boolean>.createOptionsTable(): Table {
        return Table().apply {
            defaults().pad(25.0f)
            this@createOptionsTable.forEach { (key, value) ->
                Gdx.app.log(LOG_TAG, "$key ->> $value")
                add(Label("$key", glassySkin, "big"))
                add(
                    CheckBox("", glassySkin, "radio").apply {
                        isChecked = value
                        imageCell.height(60f)
                        imageCell.width(60f)
                        image.setScaling(Scaling.fill)
                        addListener(object : ClickListener() {
                            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                super.clicked(event, x, y)
                                Gdx.app.log(LOG_TAG, "$text Radio Button Pressed")
                                game.updateOption(key, isChecked)
                            }
                        })
                    }
                )
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
}

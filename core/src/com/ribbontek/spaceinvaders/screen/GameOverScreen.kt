package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.TimeUtils
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo
import com.ribbontek.spaceinvaders.util.GifDecoder

/**
 * The Game Over screen displays the final score & party animation
 */
class GameOverScreen(
    override val game: SpaceInvadersGameDemo,
    override val movingBackground: MovingBackground,
    private val gameState: GameState
) : SimpleScreen() {

    init {
        game.updateGame(gameState)
    }

    companion object {
        private const val FONT_SIZE: Int = 50
        private const val GAME_OVER_FONT_SIZE: Int = 150
        private const val GAME_OVER: String = "Game Over"
        private const val CLICK_OUT: String = "Click anywhere"
        private const val TO_EXIT: String = "to exit"
        private val lastActionTime: Long = TimeUtils.nanoTime()
    }

    private lateinit var gameoverLayout: GlyphLayout
    private lateinit var fontLayout: GlyphLayout
    private lateinit var gameoverFont: BitmapFont
    private lateinit var font: BitmapFont
    private lateinit var megaportPartyAnimation: Animation<TextureRegion>
    private lateinit var circlePartyAnimation: Animation<TextureRegion>

    private var elapsed: Float = 0f

    override fun show() {
        createCamera()
        movingBackground.speed = 5
        megaportPartyAnimation = GifDecoder.loadGIFAnimation(
            Animation.PlayMode.LOOP,
            Gdx.files.internal("spaceship/megaport_party_min.gif").read()
        )
        circlePartyAnimation = GifDecoder.loadGIFAnimation(
            Animation.PlayMode.LOOP,
            Gdx.files.internal("spaceship/circle_party.gif").read()
        )
        createFonts()
        createBackground()
    }

    override fun render(delta: Float) {
        elapsed += delta
        game.batch.begin()
        renderCamera()
        renderMovingBackground(delta)
        renderLogo()
        gameoverFont.draw(
            game.batch, GAME_OVER,
            (Gdx.app.graphics.width - gameoverLayout.width) / 2f,
            Gdx.app.graphics.height / 2f,
        )
        val text = "Score ${gameState.cloudsHit}"
        fontLayout = fontLayout(text)
        font.draw(
            game.batch, text,
            (Gdx.app.graphics.width - fontLayout.width) / 2f,
            Gdx.app.graphics.height / 2f - gameoverLayout.height * 1.6f
        )
        fontLayout = fontLayout(CLICK_OUT)
        font.draw(
            game.batch, CLICK_OUT,
            (Gdx.app.graphics.width - fontLayout.width) / 2f,
            Gdx.app.graphics.height / 2f - gameoverLayout.height * 5f
        )
        fontLayout = fontLayout(TO_EXIT)
        font.draw(
            game.batch, TO_EXIT,
            (Gdx.app.graphics.width - fontLayout.width) / 2f,
            Gdx.app.graphics.height / 2f - gameoverLayout.height * 5.6f
        )
        game.batch.end()
        if (TimeUtils.nanoTime() - lastActionTime > 2_000_000_000L && Gdx.input.isTouched) {
            game.screen = MainMenuScreen(game, movingBackground)
            dispose()
        }
    }

    override fun dispose() {
        disposeAll()
    }

    private fun renderLogo() {
        game.batch.draw(
            megaportPartyAnimation.getKeyFrame(elapsed),
            (Gdx.app.graphics.width - 250f) / 2f,
            250f,
            250f,
            250f
        )
        game.batch.draw(
            circlePartyAnimation.getKeyFrame(elapsed),
            ((Gdx.app.graphics.width - 250f) / 2f) - 75,
            250f - 75,
            250f + 150,
            250f + 150
        )
    }

    private fun createFonts() {
        val fontFile = Gdx.files.internal("data/space_age.ttf")
        val generator = FreeTypeFontGenerator(fontFile)
        font = generator.generateFont(
            FreeTypeFontGenerator.FreeTypeFontParameter().apply { size = FONT_SIZE; }
        )
        font.data.setScale(1.2f, 1.2f)
        fontLayout = fontLayout()

        gameoverFont = generator.generateFont(
            FreeTypeFontGenerator.FreeTypeFontParameter().apply { size = GAME_OVER_FONT_SIZE }
        )
        gameoverFont.data.setScale(1.2f, 1.2f)
        gameoverLayout = GlyphLayout(gameoverFont, GAME_OVER, Color.WHITE, 0f, Align.center, false)

        generator.dispose()
        addDisposable(font, gameoverFont)
    }

    private fun fontLayout(text: String = "DEFAULT"): GlyphLayout {
        return GlyphLayout(font, text, Color.WHITE, 0f, Align.center, false)
    }
}

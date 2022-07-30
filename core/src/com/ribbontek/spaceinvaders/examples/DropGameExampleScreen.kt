package com.ribbontek.spaceinvaders.examples

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.TimeUtils
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo

class DropGameExampleScreen(
    private val game: SpaceInvadersGameDemo
) : Screen {
    companion object {
        private const val ASSETS: String = "drop-game"
    }
    // load the images for the droplet & bucket, 64x64 pixels each
    private var dropImage: Texture = Texture(Gdx.files.internal("$ASSETS/droplet.png"))
    private var bucketImage: Texture = Texture(Gdx.files.internal("$ASSETS/bucket.png"))
    private var dropSound: Sound = Gdx.audio.newSound(Gdx.files.internal("$ASSETS/drop.mp3"))
    private var rainMusic: Music = Gdx.audio.newMusic(Gdx.files.internal("$ASSETS/rain.mp3"))

    // The camera ensures we can render using our target resolution of 800x480
    //    pixels no matter what the screen resolution is.
    private lateinit var camera: OrthographicCamera
    private lateinit var bucket: Rectangle
    private lateinit var touchPos: Vector3
    private lateinit var raindrops: Array<Rectangle> // gdx, not Kotlin Array
    private var lastDropTime: Long = 0L
    private var dropsGathered: Int = 0

    private fun spawnRaindrop() {
        val raindrop = Rectangle()
        raindrop.x = MathUtils.random(0f, 1080f - 64f)
        raindrop.y = 1980f
        raindrop.width = 64f
        raindrop.height = 64f
        raindrops.add(raindrop)
        lastDropTime = TimeUtils.nanoTime()
    }

    // initializer block
    override fun show() {

        // load the drop sound effect and the rain background music
        rainMusic.isLooping = true

        // create the camera and the SpriteBatch
        camera = OrthographicCamera()
        camera.setToOrtho(false, 1980f, 1080f)

        // create a Rectangle to logically represent the bucket
        bucket = Rectangle()
        bucket.x = 1080f / 2f - 64f / 2f // center the bucket horizontally
        bucket.y = 20f // bottom left bucket corner is 20px above
        //    bottom screen edge
        bucket.width = 64f
        bucket.height = 64f

        // create the touchPos to store mouse click position
        touchPos = Vector3()

        // create the raindrops array and spawn the first raindrop
        raindrops = Array<Rectangle>()
        spawnRaindrop()

        // start the playback of the background music when the screen is shown
        rainMusic.play()
    }

    override fun render(delta: Float) {
        // clear the screen with a dark blue color. The arguments to clear
        //    are the RGB and alpha component in the range [0,1] of the color to
        //    be used to clear the screen.
        ScreenUtils.clear(0f, 0f, 0.2f, 1f)

        // generally good practice to update the camera's matrices once per frame
        camera.update()

        // tell the SpriteBatch to render in the coordinate system specified by the camera.
        game.batch.projectionMatrix = camera.combined

        // begin a new batch and draw the bucket and all drops
        game.batch.begin()
//        game.font.draw(game.batch, "Drops Collected: $dropsGathered", 0f, 480f)
        game.batch.draw(
            bucketImage, bucket.x, bucket.y,
            bucket.width, bucket.height
        )
        for (raindrop in raindrops) {
            game.batch.draw(dropImage, raindrop.x, raindrop.y)
        }
        game.batch.end()

        // process user input
        if (Gdx.input.isTouched) {
            touchPos.set(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat(),
                0f
            )
            camera.unproject(touchPos)
            bucket.x = touchPos.x - 64f / 2f
        }
        if (Gdx.input.isKeyPressed(Keys.LEFT)) {
            // getDeltaTime returns the time passed between the last and the current
            //    frame in seconds
            bucket.x -= 200 * Gdx.graphics.deltaTime
        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
            bucket.x += 200 * Gdx.graphics.deltaTime
        }

        // make sure the bucket stays within the screen bounds
        if (bucket.x < 0f)
            bucket.x = 0f
        if (bucket.x > 1080f - 64f)
            bucket.x = 1080f - 64f

        // check if we need to create a new raindrop
        if (TimeUtils.nanoTime() - lastDropTime > 1_000_000_000L)
            spawnRaindrop()

        // move the raindrops, remove any that are beneath the bottom edge of the
        //    screen or that hit the bucket.  In the latter case, play back a sound
        //    effect also
        val iter = raindrops.iterator()
        while (iter.hasNext()) {
            val raindrop = iter.next()
            raindrop.y -= 200 * Gdx.graphics.deltaTime
            if (raindrop.y + 64 < 0)
                iter.remove()

            if (raindrop.overlaps(bucket)) {
                dropsGathered++
                dropSound.play()
                iter.remove()
            }
        }
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        dropImage.dispose()
        bucketImage.dispose()
        dropSound.dispose()
        rainMusic.dispose()
    }
}

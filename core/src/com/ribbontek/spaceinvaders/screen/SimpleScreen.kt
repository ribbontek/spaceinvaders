package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo
import kotlin.math.abs

/**
 * A simple screen that overrides most of the Screen functions
 * to provide a cleaner interface using the most common functions - show, render & dispose
 *
 * Pause & Resume could be used in future for exiting the app mid-game
 */
abstract class SimpleScreen : Screen {
    protected val LOG_TAG: String = this::class.java.simpleName
    private val disposables: MutableList<Disposable> = mutableListOf()
    protected abstract val game: SpaceInvadersGameDemo
    protected abstract val movingBackground: MovingBackground
    private lateinit var camera: OrthographicCamera
    private lateinit var background: Texture

    protected fun addDisposable(vararg disposable: Disposable) {
        disposable.forEach { disposables.add(it) }
    }

    protected fun disposeAll() {
        disposables.forEach { it.dispose() }
    }

    protected fun renderMovingBackground(delta: Float) {
        game.batch.draw(
            background,
            0f,
            movingBackground.firstYMovement,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        game.batch.draw(
            background,
            0f,
            movingBackground.secondYMovement,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat()
        )
        movingBackground.firstYMovement -= movingBackground.speed * delta
        if (abs(movingBackground.firstYMovement) >= Gdx.graphics.height.toFloat()) {
            movingBackground.firstYMovement = Gdx.graphics.height.toFloat()
        }
        movingBackground.secondYMovement -= movingBackground.speed * delta
        if (abs(movingBackground.secondYMovement) >= Gdx.graphics.height.toFloat()) {
            movingBackground.secondYMovement = Gdx.graphics.height.toFloat()
        }
    }

    protected fun createBackground() {
        background = Texture(Gdx.files.internal("blue_nebula_bg.png"))
        if (movingBackground.secondYMovement == 0f) movingBackground.secondYMovement = Gdx.graphics.height.toFloat()
        addDisposable(background)
    }

    protected fun createCamera() {
        camera = OrthographicCamera(1440f, 2560f)
        camera.setToOrtho(false)
    }

    protected fun renderCamera() {
        camera.update()
        game.batch.projectionMatrix = camera.combined
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }
}

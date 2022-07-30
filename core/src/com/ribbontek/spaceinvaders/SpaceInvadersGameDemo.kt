package com.ribbontek.spaceinvaders

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.ribbontek.spaceinvaders.db.DEFAULT_OPTIONS
import com.ribbontek.spaceinvaders.db.GameDatabase
import com.ribbontek.spaceinvaders.db.Option
import com.ribbontek.spaceinvaders.db.SpaceInvadersGameData
import com.ribbontek.spaceinvaders.screen.GameState
import com.ribbontek.spaceinvaders.screen.MainMenuScreen
import java.util.UUID

class SpaceInvadersGameDemo(
    private val fileDir: String
) : Game() {
    private val LOG_TAG = this::class.java.simpleName
    lateinit var batch: SpriteBatch
    private val gameDB: GameDatabase by lazy { GameDatabase(fileDir) }
    private var game: SpaceInvadersGameData? = null

    override fun create() {
        Gdx.app.log(LOG_TAG, ">>> Starting app")
        Gdx.app.log(LOG_TAG, "fileDir: $fileDir")
        batch = SpriteBatch()
        game = gameDB.read()
        Gdx.app.log(LOG_TAG, "Found data $game")
        this.setScreen(MainMenuScreen(this))
    }

    fun getScores(): Map<UUID, GameState> {
        return game?.games ?: emptyMap()
    }

    fun getOptions(): Map<Option, Boolean> {
        return game?.options ?: DEFAULT_OPTIONS
    }

    fun updateOption(option: Option, state: Boolean) {
        game?.updateOption(option, state)?.run { gameDB.write(this) }
    }

    fun updateGame(gameState: GameState) {
        game?.addNewGame(gameState)?.run { gameDB.write(this) }
    }

    override fun dispose() {
        this.getScreen().dispose()
        batch.dispose()
    }
}

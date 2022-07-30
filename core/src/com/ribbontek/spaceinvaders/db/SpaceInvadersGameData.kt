package com.ribbontek.spaceinvaders.db

import com.ribbontek.spaceinvaders.screen.GameState
import java.util.UUID

/**
 * The Game Data model for the DB
 */
class SpaceInvadersGameData {
    val games: MutableMap<UUID, GameState> = mutableMapOf()
    val options: MutableMap<Option, Boolean> = mutableMapOf()

    fun addNewGame(gameState: GameState): SpaceInvadersGameData {
        games[UUID.randomUUID()] = gameState
        return this
    }

    fun updateOption(option: Option, state: Boolean): SpaceInvadersGameData {
        options[option] = state
        return this
    }
}

enum class Option {
    MUSIC, SFX, DEBUG
}

val DEFAULT_OPTIONS = mapOf(
    Option.MUSIC to false,
    Option.SFX to false,
    Option.DEBUG to false
)

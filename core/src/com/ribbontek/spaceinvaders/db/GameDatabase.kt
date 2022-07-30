package com.ribbontek.spaceinvaders.db

import com.badlogic.gdx.Gdx
import com.ribbontek.spaceinvaders.screen.testData
import com.ribbontek.spaceinvaders.util.fromJson
import com.ribbontek.spaceinvaders.util.toJson
import java.io.File

/**
 * The Game Database is using a quick n easy file-based system implementation
 * due to restraints from using an Android native SQLite implementation
 *
 * In addition, retrofit would be a "nice-to-have" to access data from an outside service
 */
class GameDatabase(
    private val fileDir: String
) {
    private val LOG_TAG = this::class.java.simpleName
    private val DB_DEBUG = false

    @Volatile
    private var INSTANCE: File? = null

    init {
        createInstance()
    }

    private fun createInstance(): File {
        synchronized(this@GameDatabase) {
            var instance: File? = INSTANCE
            if (instance == null) {
                instance = File("$fileDir/game.json")
                if (!instance.exists()) {
                    instance.createNewFile()
                    instance.writeText(SpaceInvadersGameData().toJson())
                }
                if (DB_DEBUG) instance.testData()
                INSTANCE = instance
            }
            return instance
        }
    }

    fun write(data: SpaceInvadersGameData) {
        synchronized(this@GameDatabase) {
            Gdx.app.log(LOG_TAG, "write called")
            INSTANCE?.writeText(data.toJson())
        }
    }

    fun read(): SpaceInvadersGameData? {
        synchronized(this@GameDatabase) {
            Gdx.app.log(LOG_TAG, "read called")
            return INSTANCE?.readText()?.takeIf { it.isNotEmpty() }
                ?.fromJson(SpaceInvadersGameData::class.java)
        }
    }
}

package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.math.Rectangle
import com.ribbontek.spaceinvaders.db.DEFAULT_OPTIONS
import com.ribbontek.spaceinvaders.db.SpaceInvadersGameData
import com.ribbontek.spaceinvaders.util.toJson
import java.io.File
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Returns the nth position of a geometric sequence
 *
 * Game Example: 5,10,20,40,80,160,320,640,1280,2560
 * (Reaching level 11 will be very tedious lol)
 */
fun nthOfGeometricPosition(
    starting: Int,
    recurring: Int,
    nthPosition: Int
): Long {
    // using formula to find the Nth = term TN = a1 * r(N-1)
    return starting * recurring.toDouble().pow((nthPosition - 1).toDouble()).toLong()
}

data class MovingBackground(
    var speed: Int = 0,
    var firstYMovement: Float = 0f,
    var secondYMovement: Float = 0f
)

enum class GameBonusType {
    HEALTH, SHIELD
}

inline fun <reified T : Enum<*>> randomEnum(): T =
    T::class.java.enumConstants[Random.nextInt(0, T::class.java.enumConstants.size)]

data class GameBonus(
    var type: GameBonusType = randomEnum()
) {
    fun provision() {
        type = randomEnum()
    }
}

data class GameLevel(
    var level: Int = 1,
    var cloudSpawnTime: Long = 1_000_000_000L,
    var cloudTravelSpeed: Long = 200,
    var levelUpFlag: Boolean = true,
    val gameBonus: GameBonus = GameBonus()
) {
    fun levelUp() {
        level += 1
        cloudSpawnTime -= 75_000_000
        cloudTravelSpeed += 50
        levelUpFlag = false
        gameBonus.provision()
    }
}

data class GameState(
    val startTimeMilli: Long = System.currentTimeMillis(),
    var endTimeMilli: Long = startTimeMilli,
    var cloudsHit: Long = 0L,
    var levelReached: Int = 0,
) {
    fun endGame(cloudsHit: Long, levelReached: Int): GameState {
        this.endTimeMilli = System.currentTimeMillis()
        this.cloudsHit = cloudsHit
        this.levelReached = levelReached
        return this
    }
}

fun GameState.readableDurationString(): String {
    return (endTimeMilli - startTimeMilli).toDuration(DurationUnit.MILLISECONDS)
        .toComponents { hours, minutes, seconds, _ ->
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
}

private val explosionSeq: List<String> by lazy {
    listOf(
        "explosion_1_1.png",
        "explosion_1_2.png",
        "explosion_1_3.png",
        "explosion_1_4.png",
        "explosion_2_1.png",
        "explosion_2_2.png",
        "explosion_2_3.png",
        "explosion_2_4.png",
        "explosion_3_1.png",
        "explosion_3_2.png",
        "explosion_3_3.png",
        "explosion_3_4.png",
        "explosion_4_1.png",
        "explosion_4_2.png",
        "explosion_4_3.png",
        "explosion_4_4.png"
    )
}

data class ExplosionContainer(
    val rec: Rectangle,
    var explosionSeqPos: Int = 0,
    var reversed: Boolean = false
) {
    fun spriteName(): String {
        return explosionSeq[(if (reversed) explosionSeqPos-- else explosionSeqPos++)].also {
            if (explosionSeqPos == explosionSeq.size - 1) reversed = true
        }
    }
}

private val boosterSeq: List<String> by lazy {
    listOf(
        "booster_1.png",
        "booster_2.png",
        "booster_3.png",
        "booster_4.png"
    )
}
enum class BoosterPosition(val distance: Float) {
    LEFT(-75f), CENTER(0f), RIGHT(+75f)
}

data class BoosterContainer(
    val rec: Rectangle,
    val position: BoosterPosition,
    var boosterSeqPos: Int = 0,
    var reversed: Boolean = false
) {
    fun spriteName(): String {
        return boosterSeq[(if (reversed) boosterSeqPos-- else boosterSeqPos++)].also {
            when (boosterSeqPos) {
                boosterSeq.size - 1 -> reversed = true
                0 -> reversed = false
            }
        }
    }
}

enum class Direction {
    LEFT, RIGHT
}

data class BonusContainer(
    val rec: Rectangle,
    val type: GameBonusType,
    var direction: Direction = randomEnum()
)

fun File.testData() {
    writeText(
        SpaceInvadersGameData().apply {
            listOf(
                GameState(cloudsHit = 100, endTimeMilli = System.currentTimeMillis() + 1_000_000),
                GameState(cloudsHit = 125, endTimeMilli = System.currentTimeMillis() + 2_000_000),
                GameState(cloudsHit = 150, endTimeMilli = System.currentTimeMillis() + 3_000_000),
                GameState(cloudsHit = 250, endTimeMilli = System.currentTimeMillis() + 4_000_000),
                GameState(cloudsHit = 350, endTimeMilli = System.currentTimeMillis() + 5_000_000),
                GameState(cloudsHit = 450, endTimeMilli = System.currentTimeMillis() + 6_000_000)
            ).forEach {
                addNewGame(it)
            }
            DEFAULT_OPTIONS.forEach { updateOption(it.key, it.value) }
        }.toJson()
    )
}

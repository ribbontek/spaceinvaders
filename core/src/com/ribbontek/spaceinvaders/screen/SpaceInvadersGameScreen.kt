package com.ribbontek.spaceinvaders.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.ribbontek.spaceinvaders.SpaceInvadersGameDemo
import com.ribbontek.spaceinvaders.db.Option
import com.ribbontek.spaceinvaders.util.GifDecoder
import com.ribbontek.spaceinvaders.util.SpriteSheet
import kotlin.math.abs

/**
 * This Space Invaders Game Screen most of the game logic,
 * including drawing screen textures, object movement & interactions,
 * & a game state & leveling system
 *
 * It also includes a visual means of debugging the rectangles / circles for object interactions
 */
class SpaceInvadersGameScreen(
    override val game: SpaceInvadersGameDemo,
    override val movingBackground: MovingBackground
) : SimpleScreen() {

    companion object {
        private const val ASSETS: String = "spaceship"
        private const val FONT_SIZE: Int = 50
        private const val HEALTH: String = "Health"
        private const val SHIELD: String = "Shield"
    }

    private val DEBUG: Boolean = game.getOptions()[Option.DEBUG] ?: false

    private lateinit var spaceshipTexture: Texture
    private lateinit var shieldTexture: Texture
    private lateinit var cloudTexture: Texture
    private lateinit var bulletTexture: Texture
    private lateinit var explosionSprites: SpriteSheet
    private lateinit var boosterSprites: SpriteSheet
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var spaceshipRec: Rectangle
    private lateinit var spaceshipShield: Circle
    private lateinit var touchPos: Vector3
    private lateinit var healthLayout: GlyphLayout
    private lateinit var font: BitmapFont
    private lateinit var megaportPartyAnimation: Animation<TextureRegion>
    private lateinit var circlePartyAnimation: Animation<TextureRegion>

    private val gameState: GameState = GameState()
    private val gameLevel: GameLevel = GameLevel()

    private val clouds: MutableList<Rectangle> = mutableListOf()
    private val bullets: MutableList<Rectangle> = mutableListOf()
    private val boosters: MutableList<BoosterContainer> = mutableListOf()
    private val explosions: MutableList<ExplosionContainer> = mutableListOf()
    private val bonuses: MutableList<BonusContainer> = mutableListOf()

    private var elapsed: Float = 0f
    private var cloudsHit: Long = 0L
    private var health: Int = 3
    private var shield: Int = 0
    private var lastCloudTime: Long = 0L
    private var lastBulletTime: Long = 0L

    private var touchedInitFlag: Boolean = false
    private var cloudsHitFlag: Boolean = false
    private var shieldBonusFlag: Boolean = false
    private var provisionBonusFlag: Boolean = false

    override fun show() {
        createCamera()
        spaceshipTexture = Texture(Gdx.files.internal("$ASSETS/megaport_red.png"))
        shieldTexture = Texture(Gdx.files.internal("$ASSETS/circle_red.png"))
        cloudTexture = Texture(Gdx.files.internal("$ASSETS/cloud.png"))
        bulletTexture = Texture(Gdx.files.internal("$ASSETS/bullet.png"))
        explosionSprites = SpriteSheet("$ASSETS/explosion.xml")
        boosterSprites = SpriteSheet("$ASSETS/booster.xml")
        megaportPartyAnimation = GifDecoder.loadGIFAnimation(
            Animation.PlayMode.LOOP,
            Gdx.files.internal("spaceship/megaport_party_min.gif").read()
        )
        circlePartyAnimation = GifDecoder.loadGIFAnimation(
            Animation.PlayMode.LOOP,
            Gdx.files.internal("spaceship/circle_party.gif").read()
        )
        movingBackground.speed = 50
        // create the touchPos to store mouse click position
        touchPos = Vector3((Gdx.graphics.width - 250f) / 2f, 250f, 0f)

        spawnSpaceship()
        lastCloudTime = TimeUtils.nanoTime()
        lastBulletTime = TimeUtils.nanoTime()
        spawnBoosters()
        createBackground()
        createFonts()
        addDisposable(explosionSprites, boosterSprites)

        if (DEBUG) {
            shapeRenderer = ShapeRenderer()
        }
    }

    override fun render(delta: Float) {
        if (health <= 0 && explosions.isEmpty()) {
            game.screen = GameOverScreen(
                game,
                movingBackground,
                gameState.endGame(cloudsHit, gameLevel.level)
            )
            dispose()
        }
        game.batch.begin()
        elapsed += delta
        renderCamera()
        renderMovingBackground(delta)
        renderScoreBoard()
        if (health > 0) {
            game.batch.draw(
                spaceshipTexture,
                spaceshipRec.x,
                spaceshipRec.y,
                spaceshipRec.width,
                spaceshipRec.height
            )
            if (shieldBonusFlag) {
                game.batch.draw(
                    circlePartyAnimation.getKeyFrame(elapsed),
                    spaceshipRec.x - 75,
                    spaceshipRec.y - 75,
                    spaceshipRec.width + 150,
                    spaceshipRec.height + 150
                )
            }
            boosters.forEach {
                game.batch.draw(
                    boosterSprites.getSpriteRegion(it.spriteName()),
                    it.rec.x, it.rec.y, it.rec.width, it.rec.height
                )
            }
        }
        clouds.forEach { game.batch.draw(cloudTexture, it.x, it.y, it.width, it.height) }
        bullets.forEach { game.batch.draw(bulletTexture, it.x, it.y, it.width, it.height) }
        bonuses.forEach {
            game.batch.draw(
                when (it.type) {
                    GameBonusType.SHIELD -> circlePartyAnimation.getKeyFrame(elapsed)
                    GameBonusType.HEALTH -> megaportPartyAnimation.getKeyFrame(elapsed)
                },
                it.rec.x, it.rec.y, it.rec.width, it.rec.height
            )
        }
        explosions.forEach {
            game.batch.draw(
                explosionSprites.getSpriteRegion(it.spriteName()),
                it.rec.x - it.rec.width / 2,
                it.rec.y - it.rec.height / 2,
                it.rec.width, it.rec.height
            )
        }
        game.batch.end()
        // process user input
        if (Gdx.input.isTouched) {
            touchPos.set(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat(),
                0f
            )
            touchedInitFlag = true
        }

        calculateSpaceshipMovements(delta)
        calculateCloudMovements(delta)
        calculateBulletMovements(delta)
        calculateBonusMovements(delta)

        explosions.removeAll { it.explosionSeqPos == 0 && it.reversed }
        boosters.recalculatePositions()
        recalculateSpaceshipShieldPosition()
        levelingSystem()

        debug()
    }

    override fun dispose() {
        disposeAll()
    }

    private fun debug() {
        if (DEBUG) {
            shapeRenderRectangles(
                spaceshipRec,
//                spaceshipShield.toRectangle(),
                *clouds.toTypedArray(),
                *bullets.toTypedArray(),
                *explosions.map { it.rec }.toTypedArray(),
                *boosters.map { it.rec }.toTypedArray(),
                *bonuses.map { it.rec }.toTypedArray()
            )
            shapeRenderCircles(
                spaceshipShield
            )
            health = 10
        }
    }

    private fun renderScoreBoard() {
        font.draw(game.batch, "Score $cloudsHit", 75f, Gdx.app.graphics.height - 100f)
        font.draw(game.batch, "Level ${gameLevel.level}", 75f, Gdx.app.graphics.height - 175f)
        font.draw(game.batch, HEALTH, 75f, Gdx.app.graphics.height - 250f)
        repeat(health) {
            game.batch.draw(
                spaceshipTexture,
                (healthLayout.width + 120) + (it * FONT_SIZE),
                Gdx.app.graphics.height - 285f,
                FONT_SIZE.toFloat(),
                FONT_SIZE.toFloat()
            )
        }
        font.draw(game.batch, SHIELD, 75f, Gdx.app.graphics.height - 325f)
        repeat(shield) {
            game.batch.draw(
                shieldTexture,
                (healthLayout.width + 125) + (it * FONT_SIZE),
                Gdx.app.graphics.height - 355f,
                FONT_SIZE.toFloat() - 15,
                FONT_SIZE.toFloat() - 15
            )
        }
    }

    private fun calculateSpaceshipMovements(delta: Float) {
        if (touchedInitFlag) {
            val currentUserInput = touchPos.x - (spaceshipRec.width / 2)
            if (spaceshipRec.x != currentUserInput && abs(spaceshipRec.x - currentUserInput) > (spaceshipRec.width / 10)) {
                when {
                    spaceshipRec.x > currentUserInput -> spaceshipRec.x -= 1200 * delta
                    spaceshipRec.x < currentUserInput -> spaceshipRec.x += 1200 * delta
                }
            } else spaceshipRec.x = currentUserInput
        }
        // make sure the bucket stays within the screen bounds
        if (spaceshipRec.x < 0f) spaceshipRec.x = 0f
        if (spaceshipRec.x > Gdx.graphics.width - spaceshipRec.width)
            spaceshipRec.x = Gdx.graphics.width - spaceshipRec.width
    }

    private fun calculateCloudMovements(delta: Float) {
        clouds.forEach { it.y -= gameLevel.cloudTravelSpeed * delta }
        clouds.removeAll { cloud ->
            cloud.y + gameLevel.cloudTravelSpeed < 0 || (
                cloud.overlaps(spaceshipRec) ||
                    (shieldBonusFlag && Intersector.overlaps(spaceshipShield, cloud))
                )
                .intersectCloudAndSpaceshipOrShield(cloud)
        }
        if (TimeUtils.nanoTime() - lastCloudTime > gameLevel.cloudSpawnTime)
            spawnCloud()
    }

    private fun calculateBulletMovements(delta: Float) {
        bullets.forEach { it.y += 600 * delta }
        bullets.removeAll { bullet ->
            bullet.y + 41f > Gdx.graphics.height || clouds.any { it.overlaps(bullet) }
                .intersectBulletAndClouds(bullet)
        }
        if (TimeUtils.nanoTime() - lastBulletTime > 500_000_000L)
            spawnBullet()
    }

    private fun calculateBonusMovements(delta: Float) {
        bonuses.forEach {
            it.rec.y -= 200 * delta
            when (it.direction) {
                Direction.LEFT -> it.rec.x -= 250 * delta
                Direction.RIGHT -> it.rec.x += 250 * delta
            }
            if (it.rec.x > Gdx.graphics.width - it.rec.width) it.direction = Direction.LEFT
            if (it.rec.x < 0f) it.direction = Direction.RIGHT
        }
        bonuses.removeAll { bonus ->
            bonus.rec.y + 200 < 0 || (
                bonus.rec.overlaps(spaceshipRec) ||
                    (shieldBonusFlag && Intersector.overlaps(spaceshipShield, bonus.rec))
                )
                .intersectBonusAndSpaceshipOrShield(bonus)
        }
    }

    private fun Boolean.intersectCloudAndSpaceshipOrShield(cloud: Rectangle) = also {
        if (it) {
            with(Rectangle()) {
                when {
                    shieldBonusFlag && Intersector.intersectRectangles(
                        cloud,
                        spaceshipShield.toRectangle(),
                        this
                    ) -> {
                        spawnExplosion(this.apply { width = 250f; height = 250f })
                        if (shield > 0) shield--
                        shieldBonusFlag = shield > 0
                    }
                    Intersector.intersectRectangles(cloud, spaceshipRec, this) -> {
                        spawnExplosion(this.apply { width = 250f; height = 250f })
                        if (health > 0) health--
                    }
                }
            }
        }
    }

    private fun Boolean.intersectBonusAndSpaceshipOrShield(bonus: BonusContainer) = also {
        if (it) {
            with(Rectangle()) {
                if (Intersector.intersectRectangles(bonus.rec, spaceshipRec, this) ||
                    Intersector.intersectRectangles(bonus.rec, spaceshipShield.toRectangle(), this)
                ) {
                    when (bonus.type) {
                        GameBonusType.HEALTH -> health++
                        GameBonusType.SHIELD -> {
                            shield++
                            shieldBonusFlag = shield > 0
                        }
                    }
                }
            }
        }
    }

    private fun Boolean.intersectBulletAndClouds(bullet: Rectangle) = also {
        if (it) {
            clouds.forEach {
                with(Rectangle()) {
                    if (Intersector.intersectRectangles(it, bullet, this)) {
                        spawnExplosion(this.apply { width = 128f; height = 128f })
                    }
                }
            }
            cloudsHit++
            cloudsHitFlag = true
            clouds.removeAll { cloud -> cloud.overlaps(bullet) }
        }
    }

    private fun levelingSystem() {
        if (cloudsHitFlag) {
            nthOfGeometricPosition(5, 2, gameLevel.level).run {
                when {
                    cloudsHit >= this && gameLevel.levelUpFlag -> {
                        gameLevel.levelUp()
                        spawnBonus()
                        Gdx.app.log(LOG_TAG, "LEVEL UP TO ${gameLevel.level}!")
                    }
                    cloudsHit != this && !gameLevel.levelUpFlag -> {
                        gameLevel.levelUpFlag = true
                    }
                }
                if (cloudsHit != 0L && cloudsHit % 50L == 0L && provisionBonusFlag) {
                    spawnBonus()
                    provisionBonusFlag = false
                    Gdx.app.log(LOG_TAG, "BONUS TIME!")
                } else if (cloudsHit != 0L && cloudsHit % 50L != 0L && !provisionBonusFlag) {
                    provisionBonusFlag = true
                }
            }
            cloudsHitFlag = false
        }
    }

    private fun createFonts() {
        val fontFile = Gdx.files.internal("data/space_age.ttf")
        val generator = FreeTypeFontGenerator(fontFile)
        val fontParam = FreeTypeFontGenerator.FreeTypeFontParameter()
        fontParam.size = FONT_SIZE
        font = generator.generateFont(fontParam)
        font.data.setScale(1.2f, 1.2f)
        healthLayout = GlyphLayout(font, HEALTH)
        generator.dispose()
        addDisposable(font)
    }

    private fun shapeRenderRectangles(vararg recs: Rectangle) {
        with(shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Filled)
            color = Color.RED
            recs.forEach { rect(it.x, it.y, it.width, it.height) }
            end()
        }
    }

    private fun shapeRenderCircles(vararg circs: Circle) {
        with(shapeRenderer) {
            begin(ShapeRenderer.ShapeType.Filled)
            color = Color.RED
            circs.forEach { circle(it.x, it.y, it.radius) }
            end()
        }
    }

    private fun spawnCloud() {
        val cloud = Rectangle().apply {
            x = MathUtils.random(0f, Gdx.graphics.width - 250f)
            y = Gdx.graphics.height.toFloat()
            width = 250f
            height = 250f
        }
        clouds.add(cloud)
        lastCloudTime = TimeUtils.nanoTime()
    }

    private fun spawnBullet() {
        val bullet = Rectangle().apply {
            x = spaceshipRec.x + (spaceshipRec.width / 2) - (21f / 2)
            y = spaceshipRec.y + spaceshipRec.height
            width = 21f
            height = 41f
        }
        bullets.add(bullet)
        lastBulletTime = TimeUtils.nanoTime()
    }

    private fun spawnBonus() {
        val bonus = Rectangle().apply {
            x = MathUtils.random(0f, Gdx.graphics.width - 250f)
            y = Gdx.graphics.height.toFloat()
            width = 125f
            height = 125f
        }
        bonuses.add(BonusContainer(bonus, gameLevel.gameBonus.type))
    }

    private fun List<BoosterContainer>.recalculatePositions(boosterWidth: Float = 60f) {
        forEach {
            it.rec.x = spaceshipRec.x +
                (spaceshipRec.width / 2) - (boosterWidth / 2) +
                it.position.distance
        }
    }

    private fun recalculateSpaceshipShieldPosition() {
        spaceshipShield.x = spaceshipRec.x + 125
    }

    private fun Circle.toRectangle(): Rectangle {
        val cir = this
        return Rectangle().apply {
            x = cir.x - cir.radius
            y = cir.y - cir.radius
            height = cir.radius * 2
            width = cir.radius * 2
        }
    }

    private fun spawnSpaceship() {
        spaceshipRec = Rectangle().apply {
            x = (Gdx.graphics.width - 250f) / 2f
            y = 250f
            width = 250f
            height = 250f
        }
        spaceshipShield = Circle().apply {
            x = spaceshipRec.x + 125
            y = spaceshipRec.y + 125
            radius = (spaceshipRec.width + 150) / 2
        }
    }

    private fun spawnBoosters() {
        boosters.add(createBoosterContainer(boosterPosition = BoosterPosition.CENTER))
        boosters.add(createBoosterContainer(boosterPosition = BoosterPosition.RIGHT))
        boosters.add(createBoosterContainer(boosterPosition = BoosterPosition.LEFT))
    }

    private fun createBoosterContainer(
        boosterWidth: Float = 60f,
        boosterHeight: Float = 100f,
        boosterPosition: BoosterPosition
    ): BoosterContainer {
        return Rectangle().apply {
            x = spaceshipRec.x +
                (spaceshipRec.width / 2) - (boosterWidth / 2) + boosterPosition.distance
            y = spaceshipRec.y - boosterHeight
            width = boosterWidth
            height = boosterHeight
        }.let {
            BoosterContainer(it, boosterPosition)
        }
    }

    private fun spawnExplosion(rectangle: Rectangle) {
        explosions.add(ExplosionContainer(rectangle))
    }
}

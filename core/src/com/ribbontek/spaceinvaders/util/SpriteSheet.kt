package com.ribbontek.spaceinvaders.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.XmlReader

/**
 * The Sprite Sheet converts an xml referenced sprite sheet into a hashmap, referenced by name & TextureRegion
 *
 * Not officially part of the libGDX library & borrowed from:
 * https://gist.github.com/stevensona/ac1b6a14fd8670f730a2
 *
 * Code converted to Kotlin
 */
class SpriteSheet(filename: String) : Disposable {
    private lateinit var sprites: Map<String, TextureRegion>
    private lateinit var texture: Texture
    fun getSpriteRegion(name: String): TextureRegion? {
        return sprites[name]
    }

    init {
        try {
            val xmlReader = XmlReader()
            val root: XmlReader.Element = xmlReader.parse(Gdx.files.internal(filename))
            texture = Texture(Gdx.files.internal(root.getAttribute("imagePath")))
            sprites = root.getChildrenByName("SubTexture").associate {
                it.getAttribute("name") to TextureRegion(
                    texture,
                    it.getAttribute("x").toInt(),
                    it.getAttribute("y").toInt(),
                    it.getAttribute("width").toInt(),
                    it.getAttribute("height").toInt()
                )
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    override fun dispose() {
        texture.dispose()
    }
}

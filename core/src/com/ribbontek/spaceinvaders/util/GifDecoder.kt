package com.ribbontek.spaceinvaders.util

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import java.io.InputStream
import java.util.Vector

/**
 * The Gif Decoder converts gifs into texture animations. This provides an alternative to using sprites
 *
 * Not officially part of the libGDX library. Borrowed from here:
 * https://gamedev.stackexchange.com/questions/136659/is-it-possible-to-use-animated-gif-images-in-lbgdx
 *
 * Use this to turn a gif into an animated button example here:
 * https://gamedev.stackexchange.com/questions/136664/how-to-add-event-listener-to-gifdecoder-in-libgdx
 *
 * Code converted to Kotlin
 */
class GifDecoder {
    protected var `in`: InputStream? = null
    protected var status = 0
    protected var width = 0 // full image width
    protected var height = 0 // full image height
    protected var gctFlag = false // global color table used
    protected var gctSize = 0 // size of global color table

    /**
     * Gets the "Netscape" iteration count, if any. A count of 0 means repeat indefinitely.
     *
     * @return iteration count if one was specified, else 1.
     */
    var loopCount = 1 // iterations; 0 = repeat forever
        protected set
    protected var gct: IntArray? = null // global color table
    protected var lct: IntArray? = null // local color table
    protected var act: IntArray? = null // active color table
    protected var bgIndex = 0 // background color index

    protected var bgColor = 0 // background color
    protected var lastBgColor = 0 // previous bg color
    protected var pixelAspect = 0 // pixel aspect ratio
    protected var lctFlag = false // local color table flag
    protected var interlace = false // interlace flag
    protected var lctSize = 0 // local color table size

    protected var ix = 0
    protected var iy = 0
    protected var iw = 0
    protected var ih = 0 // current image rectangle
    protected var lrx = 0
    protected var lry = 0
    protected var lrw = 0
    protected var lrh = 0
    protected var image: DixieMap? = null // current frame
    protected var lastPixmap: DixieMap? = null // previous frame

    protected var block = ByteArray(256) // current data block
    protected var blockSize = 0 // block size last graphic control extension info
    protected var dispose = 0 // 0=no action; 1=leave in place; 2=restore to bg; 3=restore to prev
    protected var lastDispose = 0
    protected var transparency = false // use transparent color
    protected var delay = 0 // delay in milliseconds
    protected var transIndex = 0 // transparent color index

    // LZW decoder working arrays
    protected var prefix: ShortArray? = null
    protected var suffix: ByteArray? = null
    protected var pixelStack: ByteArray? = null
    protected var pixels: ByteArray? = null
    protected var frames: Vector<GifFrame>? = null // frames read from current file

    /**
     * Gets the number of frames read from file.
     *
     * @return frame count
     */
    var frameCount = 0
        protected set

    class DixieMap : Pixmap {
        internal constructor(w: Int, h: Int, f: Format?) : super(w, h, f) {}
        internal constructor(data: IntArray, w: Int, h: Int, f: Format?) : super(w, h, f) {
            var x: Int
            var y: Int
            y = 0
            while (y < h) {
                x = 0
                while (x < w) {
                    val pxl_ARGB8888 = data[x + y * w]
                    val pxl_RGBA8888 =
                        pxl_ARGB8888 shr 24 and 0x000000ff or (pxl_ARGB8888 shl 8 and -0x100)
                    // convert ARGB8888 > RGBA8888
                    drawPixel(x, y, pxl_RGBA8888)
                    x++
                }
                y++
            }
        }

        fun getPixels(
            pixels: IntArray,
            offset: Int,
            stride: Int,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ) {
            var modOffset = offset
            val bb = getPixels()
            var l: Int
            var k: Int = y
            while (k < y + height) {
                var modOffset2 = modOffset
                l = x
                while (l < x + width) {
                    val pxl = bb.getInt(4 * (l + k * width))

                    // convert RGBA8888 > ARGB8888
                    pixels[modOffset2++] = pxl shr 8 and 0x00ffffff or (pxl shl 24 and -0x1000000)
                    l++
                }
                modOffset += stride
                k++
            }
        }
    }

    class GifFrame(var image: DixieMap, var delay: Int)

    /**
     * Gets display duration for specified frame.
     *
     * @param n
     * int index of frame
     * @return delay in milliseconds
     */
    fun getDelay(n: Int): Int {
        delay = -1
        if (n in 0 until frameCount) {
            delay = frames!!.elementAt(n).delay
        }
        return delay
    }

    /**
     * Gets the first (or only) image read.
     *
     * @return BufferedPixmap containing first frame, or null if none.
     */
    val pixmap: Pixmap?
        get() = getFrame(0)

    /**
     * Creates new frame image from current data (and previous frames as specified by their disposition codes).
     */
    protected fun setPixels() {
        // expose destination image's pixels as int array
        val dest = IntArray(width * height)
        // fill in starting image contents based on last image's dispose code
        if (lastDispose > 0) {
            if (lastDispose == 3) {
                // use image before last
                val n = frameCount - 2
                lastPixmap = if (n > 0) {
                    getFrame(n - 1)
                } else {
                    null
                }
            }
            if (lastPixmap != null) {
                lastPixmap!!.getPixels(dest, 0, width, 0, 0, width, height)
                // copy pixels
                if (lastDispose == 2) {
                    // fill last image rect area with background color
                    var c = 0
                    if (!transparency) {
                        c = lastBgColor
                    }
                    for (i in 0 until lrh) {
                        val n1 = (lry + i) * width + lrx
                        val n2 = n1 + lrw
                        for (k in n1 until n2) {
                            dest[k] = c
                        }
                    }
                }
            }
        }
        // copy each source line to the appropriate place in the destination
        var pass = 1
        var inc = 8
        var iline = 0
        for (i in 0 until ih) {
            var line = i
            if (interlace) {
                if (iline >= ih) {
                    pass++
                    when (pass) {
                        2 -> iline = 4
                        3 -> {
                            iline = 2
                            inc = 4
                        }
                        4 -> {
                            iline = 1
                            inc = 2
                        }
                        else -> {}
                    }
                }
                line = iline
                iline += inc
            }
            line += iy
            if (line < height) {
                val k = line * width
                var dx = k + ix // start of line in dest
                var dlim = dx + iw // end of dest line
                if (k + width < dlim) {
                    dlim = k + width // past dest edge
                }
                var sx = i * iw // start of line in source
                while (dx < dlim) {
                    // map color and insert in destination
                    val index = pixels!![sx++].toInt() and 0xff
                    val c = act!![index]
                    if (c != 0) {
                        dest[dx] = c
                    }
                    dx++
                }
            }
        }
        image = DixieMap(dest, width, height, Pixmap.Format.RGBA8888)
        // Pixmap.createPixmap(dest, width, height, Config.ARGB_4444);
    }

    /**
     * Gets the image contents of frame n.
     *
     * @return BufferedPixmap representation of frame, or null if n is invalid.
     */
    fun getFrame(n: Int): DixieMap? {
        var modN = n
        if (frameCount <= 0) return null
        modN %= frameCount
        return (frames!!.elementAt(modN) as GifFrame).image
    }

    /**
     * Reads GIF image from stream
     *
     * @param is
     * containing GIF file.
     * @return read status code (0 = no errors)
     */
    fun read(`is`: InputStream?): Int {
        init()
        if (`is` != null) {
            `in` = `is`
            readHeader()
            if (!err()) {
                readContents()
                if (frameCount < 0) {
                    status = STATUS_FORMAT_ERROR
                }
            }
        } else {
            status = STATUS_OPEN_ERROR
        }
        try {
            `is`!!.close()
        } catch (e: Exception) {
        }
        return status
    }

    /**
     * Decodes LZW image data into pixel array. Adapted from John Cristy's BitmapMagick.
     */
    protected fun decodeBitmapData() {
        val nullCode = -1
        val npix = iw * ih
        var available: Int
        val clear: Int
        var codeMask: Int
        var codeSize: Int
        var inCode: Int
        var oldCode: Int
        var bits: Int
        var code: Int
        var count: Int
        var datum: Int
        var first: Int
        var top: Int
        var bi: Int
        if (pixels == null || pixels!!.size < npix) {
            pixels = ByteArray(npix) // allocate new pixel array
        }
        if (prefix == null) {
            prefix = ShortArray(MAX_STACK_SIZE)
        }
        if (suffix == null) {
            suffix = ByteArray(MAX_STACK_SIZE)
        }
        if (pixelStack == null) {
            pixelStack = ByteArray(MAX_STACK_SIZE + 1)
        }
        // Initialize GIF data stream decoder.
        val dataSize: Int = read()
        clear = 1 shl dataSize
        val endOfInformation: Int = clear + 1
        available = clear + 2
        oldCode = nullCode
        codeSize = dataSize + 1
        codeMask = (1 shl codeSize) - 1
        code = 0
        while (code < clear) {
            prefix!![code] = 0 // XXX ArrayIndexOutOfBoundsException
            suffix!![code] = code.toByte()
            code++
        }
        // Decode GIF pixel stream.
        bi = 0
        var pi: Int = bi
        top = pi
        first = top
        count = first
        bits = count
        datum = bits
        var index = 0
        while (index < npix) {
            if (top == 0) {
                if (bits < codeSize) {
                    // Load bytes until there are enough bits for a code.
                    if (count == 0) {
                        // Read a new data block.
                        count = readBlock()
                        if (count <= 0) {
                            break
                        }
                        bi = 0
                    }
                    datum += block[bi].toInt() and 0xff shl bits
                    bits += 8
                    bi++
                    count--
                    continue
                }
                // Get the next code.
                code = datum and codeMask
                datum = datum shr codeSize
                bits -= codeSize
                // Interpret the code
                if (code > available || code == endOfInformation) {
                    break
                }
                if (code == clear) {
                    // Reset decoder.
                    codeSize = dataSize + 1
                    codeMask = (1 shl codeSize) - 1
                    available = clear + 2
                    oldCode = nullCode
                    continue
                }
                if (oldCode == nullCode) {
                    pixelStack!![top++] = suffix!![code]
                    oldCode = code
                    first = code
                    continue
                }
                inCode = code
                if (code == available) {
                    pixelStack!![top++] = first.toByte()
                    code = oldCode
                }
                while (code > clear) {
                    pixelStack!![top++] = suffix!![code]
                    code = prefix!![code].toInt()
                }
                first = suffix!![code].toInt() and 0xff
                // Add a new string to the string table,
                if (available >= MAX_STACK_SIZE) {
                    break
                }
                pixelStack!![top++] = first.toByte()
                prefix!![available] = oldCode.toShort()
                suffix!![available] = first.toByte()
                available++
                if (available and codeMask == 0 && available < MAX_STACK_SIZE) {
                    codeSize++
                    codeMask += available
                }
                oldCode = inCode
            }
            // Pop a pixel off the pixel stack.
            top--
            pixels!![pi++] = pixelStack!![top]
            index++
        }
        index = pi
        while (index < npix) {
            pixels!![index] = 0 // clear missing pixels
            index++
        }
    }

    /**
     * Returns true if an error was encountered during reading/decoding
     */
    protected fun err(): Boolean {
        return status != STATUS_OK
    }

    /**
     * Initializes or re-initializes reader
     */
    protected fun init() {
        status = STATUS_OK
        frameCount = 0
        frames = Vector()
        gct = null
        lct = null
    }

    /**
     * Reads a single byte from the input stream.
     */
    protected fun read(): Int {
        var curByte = 0
        try {
            curByte = `in`!!.read()
        } catch (e: Exception) {
            status = STATUS_FORMAT_ERROR
        }
        return curByte
    }

    /**
     * Reads next variable length block from input.
     *
     * @return number of bytes stored in "buffer"
     */
    protected fun readBlock(): Int {
        blockSize = read()
        var n = 0
        if (blockSize > 0) {
            try {
                var count: Int
                while (n < blockSize) {
                    count = `in`!!.read(block, n, blockSize - n)
                    if (count == -1) {
                        break
                    }
                    n += count
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (n < blockSize) {
                status = STATUS_FORMAT_ERROR
            }
        }
        return n
    }

    /**
     * Reads color table as 256 RGB integer values
     *
     * @param ncolors
     * int number of colors to read
     * @return int array containing 256 colors (packed ARGB with full alpha)
     */
    protected fun readColorTable(ncolors: Int): IntArray? {
        val nbytes = 3 * ncolors
        var tab: IntArray? = null
        val c = ByteArray(nbytes)
        var n = 0
        try {
            n = `in`!!.read(c)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (n < nbytes) {
            status = STATUS_FORMAT_ERROR
        } else {
            tab = IntArray(256) // max size to avoid bounds checks
            var i = 0
            var j = 0
            while (i < ncolors) {
                val r = c[j++].toInt() and 0xff
                val g = c[j++].toInt() and 0xff
                val b = c[j++].toInt() and 0xff
                tab[i++] = -0x1000000 or (r shl 16) or (g shl 8) or b
            }
        }
        return tab
    }

    /**
     * Main file parser. Reads GIF content blocks.
     */
    protected fun readContents() {
        // read GIF file content blocks
        var done = false
        while (!(done || err())) {
            var code = read()
            when (code) {
                0x2C -> readBitmap()
                0x21 -> {
                    code = read()
                    when (code) {
                        0xf9 -> readGraphicControlExt()
                        0xff -> {
                            readBlock()
                            var app = ""
                            var i = 0
                            while (i < 11) {
                                app += block[i].toInt().toChar()
                                i++
                            }
                            if (app == "NETSCAPE2.0") {
                                readNetscapeExt()
                            } else {
                                skip() // don't care
                            }
                        }
                        0xfe -> skip()
                        0x01 -> skip()
                        else -> skip()
                    }
                }
                0x3b -> done = true
                0x00 -> status = STATUS_FORMAT_ERROR
                else -> status = STATUS_FORMAT_ERROR
            }
        }
    }

    /**
     * Reads Graphics Control Extension values
     */
    protected fun readGraphicControlExt() {
        read() // block size
        val packed = read() // packed fields
        dispose = packed and 0x1c shr 2 // disposal method
        if (dispose == 0) {
            dispose = 1 // elect to keep old image if discretionary
        }
        transparency = packed and 1 != 0
        delay = readShort() * 10 // delay in milliseconds
        transIndex = read() // transparent color index
        read() // block terminator
    }

    /**
     * Reads GIF file header information.
     */
    protected fun readHeader() {
        var id = ""
        for (i in 0..5) {
            id += read().toChar()
        }
        if (!id.startsWith("GIF")) {
            status = STATUS_FORMAT_ERROR
            return
        }
        readLSD()
        if (gctFlag && !err()) {
            gct = readColorTable(gctSize)
            bgColor = gct!![bgIndex]
        }
    }

    /**
     * Reads next frame image
     */
    protected fun readBitmap() {
        ix = readShort() // (sub)image position & size
        iy = readShort()
        iw = readShort()
        ih = readShort()
        val packed = read()
        lctFlag = packed and 0x80 != 0 // 1 - local color table flag interlace
        lctSize = Math.pow(2.0, ((packed and 0x07) + 1).toDouble()).toInt()
        // 3 - sort flag
        // 4-5 - reserved lctSize = 2 << (packed & 7); // 6-8 - local color
        // table size
        interlace = packed and 0x40 != 0
        if (lctFlag) {
            lct = readColorTable(lctSize) // read table
            act = lct // make local table active
        } else {
            act = gct // make global table active
            if (bgIndex == transIndex) {
                bgColor = 0
            }
        }
        var save = 0
        if (transparency) {
            save = act!![transIndex]
            act!![transIndex] = 0 // set transparent color if specified
        }
        if (act == null) {
            status = STATUS_FORMAT_ERROR // no color table defined
        }
        if (err()) {
            return
        }
        decodeBitmapData() // decode pixel data
        skip()
        if (err()) {
            return
        }
        frameCount++
        // create new image to receive frame data
        image = DixieMap(width, height, Pixmap.Format.RGBA8888)
        setPixels() // transfer pixel data to image
        frames!!.addElement(GifFrame(image!!, delay)) // add image to frame
        // list
        if (transparency) {
            act!![transIndex] = save
        }
        resetFrame()
    }

    /**
     * Reads Logical Screen Descriptor
     */
    protected fun readLSD() {
        // logical screen size
        width = readShort()
        height = readShort()
        // packed fields
        val packed = read()
        gctFlag = packed and 0x80 != 0 // 1 : global color table flag
        // 2-4 : color resolution
        // 5 : gct sort flag
        gctSize = 2 shl (packed and 7) // 6-8 : gct size
        bgIndex = read() // background color index
        pixelAspect = read() // pixel aspect ratio
    }

    /**
     * Reads Netscape extenstion to obtain iteration count
     */
    protected fun readNetscapeExt() {
        do {
            readBlock()
            if (block[0] == 1.toByte()) {
                // loop count sub-block
                val b1 = block[1].toInt() and 0xff
                val b2 = block[2].toInt() and 0xff
                loopCount = b2 shl 8 or b1
            }
        } while (blockSize > 0 && !err())
    }

    /**
     * Reads next 16-bit value, LSB first
     */
    protected fun readShort(): Int {
        // read 16-bit value, LSB first
        return read() or (read() shl 8)
    }

    /**
     * Resets frame state for reading next image.
     */
    protected fun resetFrame() {
        lastDispose = dispose
        lrx = ix
        lry = iy
        lrw = iw
        lrh = ih
        lastPixmap = image
        lastBgColor = bgColor
        dispose = 0
        transparency = false
        delay = 0
        lct = null
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    protected fun skip() {
        do {
            readBlock()
        } while (blockSize > 0 && !err())
    }

    fun getAnimation(playMode: PlayMode?): Animation<TextureRegion> {
        val nrFrames = frameCount
        var frame: Pixmap? = getFrame(0)
        val width = frame!!.width
        val height = frame.height
        var vzones = Math.sqrt(nrFrames.toDouble()).toInt()
        val hzones = vzones
        while (vzones * hzones < nrFrames) vzones++
        var v: Int
        val target = Pixmap(width * hzones, height * vzones, Pixmap.Format.RGBA8888)
        var h = 0
        while (h < hzones) {
            v = 0
            while (v < vzones) {
                val frameID = v + h * vzones
                if (frameID < nrFrames) {
                    frame = getFrame(frameID)
                    target.drawPixmap(frame, h * width, v * height)
                }
                v++
            }
            h++
        }
        val texture = Texture(target)
        val texReg =
            Array<TextureRegion>()
        h = 0
        while (h < hzones) {
            v = 0
            while (v < vzones) {
                val frameID = v + h * vzones
                if (frameID < nrFrames) {
                    val tr =
                        TextureRegion(texture, h * width, v * height, width, height)
                    texReg.add(tr)
                }
                v++
            }
            h++
        }
        var frameDuration = getDelay(0).toFloat()
        frameDuration /= 1000f // convert milliseconds into seconds
        return Animation(frameDuration, texReg, playMode)
    }

    companion object {
        /**
         * File read status: No errors.
         */
        const val STATUS_OK = 0

        /**
         * File read status: Error decoding file (may be partially decoded)
         */
        const val STATUS_FORMAT_ERROR = 1

        /**
         * File read status: Unable to open source.
         */
        const val STATUS_OPEN_ERROR = 2

        /** max decoder pixel stack size  */
        protected const val MAX_STACK_SIZE = 4096
        fun loadGIFAnimation(playMode: PlayMode?, `is`: InputStream?): Animation<TextureRegion> {
            val gdec = GifDecoder()
            gdec.read(`is`)
            return gdec.getAnimation(playMode)
        }
    }
}

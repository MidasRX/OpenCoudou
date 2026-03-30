package li.cil.oc.client.os.libs

/**
 * Picture/Image handling library for SkibidiOS2.
 * Compatible with SkibidiLuaOS Image.lua format.
 */
data class Picture(
    val width: Int,
    val height: Int,
    val pixels: Array<IntArray>,  // Color data
    val chars: Array<CharArray>   // Character data
) {
    
    companion object {
        /**
         * Create an empty picture.
         */
        fun create(width: Int, height: Int, bg: Int = 0x000000, char: Char = ' '): Picture {
            val pixels = Array(height) { IntArray(width) { bg } }
            val chars = Array(height) { CharArray(width) { char } }
            return Picture(width, height, pixels, chars)
        }
        
        /**
         * Load picture from .pic file format.
         */
        fun load(data: ByteArray): Picture? {
            // TODO: Implement .pic file format parsing
            return null
        }
        
        /**
         * Load from raw ARGB data.
         */
        fun fromRGB(width: Int, height: Int, data: IntArray): Picture {
            val pixels = Array(height) { y ->
                IntArray(width) { x -> data[y * width + x] }
            }
            val chars = Array(height) { CharArray(width) { ' ' } }
            return Picture(width, height, pixels, chars)
        }
    }
    
    /**
     * Get width of the picture.
     */
    fun getWidth() = width
    
    /**
     * Get height of the picture.
     */
    fun getHeight() = height
    
    /**
     * Get pixel color at position.
     */
    fun getPixel(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0
        return pixels[y][x]
    }
    
    /**
     * Set pixel color at position.
     */
    fun setPixel(x: Int, y: Int, color: Int) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        pixels[y][x] = color
    }
    
    /**
     * Get character at position.
     */
    fun getChar(x: Int, y: Int): Char {
        if (x < 0 || x >= width || y < 0 || y >= height) return ' '
        return chars[y][x]
    }
    
    /**
     * Set character at position.
     */
    fun setChar(x: Int, y: Int, char: Char) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        chars[y][x] = char
    }
    
    /**
     * Fill region with color.
     */
    fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int, char: Char = ' ') {
        for (py in y until (y + h).coerceAtMost(height)) {
            for (px in x until (x + w).coerceAtMost(width)) {
                if (px >= 0 && py >= 0) {
                    pixels[py][px] = color
                    chars[py][px] = char
                }
            }
        }
    }
    
    /**
     * Clone the picture.
     */
    fun clone(): Picture {
        val newPixels = Array(height) { y -> pixels[y].copyOf() }
        val newChars = Array(height) { y -> chars[y].copyOf() }
        return Picture(width, height, newPixels, newChars)
    }
    
    /**
     * Save picture to .pic file format.
     */
    fun save(): ByteArray {
        // TODO: Implement .pic file format serialization
        return ByteArray(0)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Picture) return false
        if (width != other.width || height != other.height) return false
        return pixels.contentDeepEquals(other.pixels) && chars.contentDeepEquals(other.chars)
    }
    
    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentDeepHashCode()
        result = 31 * result + chars.contentDeepHashCode()
        return result
    }
}

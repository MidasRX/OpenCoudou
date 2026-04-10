package li.cil.oc.server.component

/**
 * Keyboard component that sends key events to connected computers.
 */
class KeyboardComponent : AbstractComponent("keyboard") {
    
    init {
        // Keyboard methods
        registerMethod("getKeys", true, "getKeys():table -- Get pressed keys") { _ ->
            // Would track currently pressed keys
            arrayOf(emptyArray<Int>())
        }
    }
    
    /**
     * Generate key_down signal.
     */
    fun keyDown(char: Int, code: Int): Array<Any?> {
        return arrayOf("key_down", address, char.toChar(), code)
    }
    
    /**
     * Generate key_up signal.
     */
    fun keyUp(char: Int, code: Int): Array<Any?> {
        return arrayOf("key_up", address, char.toChar(), code)
    }
    
    /**
     * Generate clipboard paste signal.
     */
    fun clipboard(text: String): Array<Any?> {
        return arrayOf("clipboard", address, text)
    }
}

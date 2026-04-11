package li.cil.oc.util

import org.lwjgl.glfw.GLFW

/**
 * Keyboard key mapping from GLFW (LWJGL3) to OpenComputers/LWJGL2 keycodes.
 * Original OpenComputers uses LWJGL2 key codes in Lua programs.
 * This mapping allows NeoForge 1.21+ (which uses LWJGL3/GLFW) to send
 * compatible key codes to Lua programs.
 */
object KeyboardKeys {
    
    // OpenComputers/LWJGL2 key codes (what Lua programs expect)
    object LwjglKeys {
        const val KEY_NONE = 0
        const val KEY_ESCAPE = 1
        const val KEY_1 = 2
        const val KEY_2 = 3
        const val KEY_3 = 4
        const val KEY_4 = 5
        const val KEY_5 = 6
        const val KEY_6 = 7
        const val KEY_7 = 8
        const val KEY_8 = 9
        const val KEY_9 = 10
        const val KEY_0 = 11
        const val KEY_MINUS = 12
        const val KEY_EQUALS = 13
        const val KEY_BACK = 14  // Backspace
        const val KEY_TAB = 15
        const val KEY_Q = 16
        const val KEY_W = 17
        const val KEY_E = 18
        const val KEY_R = 19
        const val KEY_T = 20
        const val KEY_Y = 21
        const val KEY_U = 22
        const val KEY_I = 23
        const val KEY_O = 24
        const val KEY_P = 25
        const val KEY_LBRACKET = 26
        const val KEY_RBRACKET = 27
        const val KEY_RETURN = 28  // Enter
        const val KEY_LCONTROL = 29
        const val KEY_A = 30
        const val KEY_S = 31
        const val KEY_D = 32
        const val KEY_F = 33
        const val KEY_G = 34
        const val KEY_H = 35
        const val KEY_J = 36
        const val KEY_K = 37
        const val KEY_L = 38
        const val KEY_SEMICOLON = 39
        const val KEY_APOSTROPHE = 40
        const val KEY_GRAVE = 41  // `
        const val KEY_LSHIFT = 42
        const val KEY_BACKSLASH = 43
        const val KEY_Z = 44
        const val KEY_X = 45
        const val KEY_C = 46
        const val KEY_V = 47
        const val KEY_B = 48
        const val KEY_N = 49
        const val KEY_M = 50
        const val KEY_COMMA = 51
        const val KEY_PERIOD = 52
        const val KEY_SLASH = 53
        const val KEY_RSHIFT = 54
        const val KEY_MULTIPLY = 55  // Numpad *
        const val KEY_LMENU = 56  // Left Alt
        const val KEY_SPACE = 57
        const val KEY_CAPITAL = 58  // Caps Lock
        const val KEY_F1 = 59
        const val KEY_F2 = 60
        const val KEY_F3 = 61
        const val KEY_F4 = 62
        const val KEY_F5 = 63
        const val KEY_F6 = 64
        const val KEY_F7 = 65
        const val KEY_F8 = 66
        const val KEY_F9 = 67
        const val KEY_F10 = 68
        const val KEY_NUMLOCK = 69
        const val KEY_SCROLL = 70  // Scroll Lock
        const val KEY_NUMPAD7 = 71
        const val KEY_NUMPAD8 = 72
        const val KEY_NUMPAD9 = 73
        const val KEY_SUBTRACT = 74  // Numpad -
        const val KEY_NUMPAD4 = 75
        const val KEY_NUMPAD5 = 76
        const val KEY_NUMPAD6 = 77
        const val KEY_ADD = 78  // Numpad +
        const val KEY_NUMPAD1 = 79
        const val KEY_NUMPAD2 = 80
        const val KEY_NUMPAD3 = 81
        const val KEY_NUMPAD0 = 82
        const val KEY_DECIMAL = 83  // Numpad .
        const val KEY_F11 = 87
        const val KEY_F12 = 88
        const val KEY_F13 = 100
        const val KEY_F14 = 101
        const val KEY_F15 = 102
        const val KEY_NUMPADEQUALS = 141
        const val KEY_NUMPADENTER = 156
        const val KEY_RCONTROL = 157
        const val KEY_DIVIDE = 181  // Numpad /
        const val KEY_RMENU = 184  // Right Alt
        const val KEY_PAUSE = 197
        const val KEY_HOME = 199
        const val KEY_UP = 200
        const val KEY_PRIOR = 201  // Page Up
        const val KEY_LEFT = 203
        const val KEY_RIGHT = 205
        const val KEY_END = 207
        const val KEY_DOWN = 208
        const val KEY_NEXT = 209  // Page Down
        const val KEY_INSERT = 210
        const val KEY_DELETE = 211
        const val KEY_LMETA = 219  // Left Windows/Super
        const val KEY_RMETA = 220  // Right Windows/Super
    }
    
    // GLFW to LWJGL2 mapping
    private val glfwToLwjgl = mapOf(
        GLFW.GLFW_KEY_ESCAPE to LwjglKeys.KEY_ESCAPE,
        GLFW.GLFW_KEY_1 to LwjglKeys.KEY_1,
        GLFW.GLFW_KEY_2 to LwjglKeys.KEY_2,
        GLFW.GLFW_KEY_3 to LwjglKeys.KEY_3,
        GLFW.GLFW_KEY_4 to LwjglKeys.KEY_4,
        GLFW.GLFW_KEY_5 to LwjglKeys.KEY_5,
        GLFW.GLFW_KEY_6 to LwjglKeys.KEY_6,
        GLFW.GLFW_KEY_7 to LwjglKeys.KEY_7,
        GLFW.GLFW_KEY_8 to LwjglKeys.KEY_8,
        GLFW.GLFW_KEY_9 to LwjglKeys.KEY_9,
        GLFW.GLFW_KEY_0 to LwjglKeys.KEY_0,
        GLFW.GLFW_KEY_MINUS to LwjglKeys.KEY_MINUS,
        GLFW.GLFW_KEY_EQUAL to LwjglKeys.KEY_EQUALS,
        GLFW.GLFW_KEY_BACKSPACE to LwjglKeys.KEY_BACK,
        GLFW.GLFW_KEY_TAB to LwjglKeys.KEY_TAB,
        GLFW.GLFW_KEY_Q to LwjglKeys.KEY_Q,
        GLFW.GLFW_KEY_W to LwjglKeys.KEY_W,
        GLFW.GLFW_KEY_E to LwjglKeys.KEY_E,
        GLFW.GLFW_KEY_R to LwjglKeys.KEY_R,
        GLFW.GLFW_KEY_T to LwjglKeys.KEY_T,
        GLFW.GLFW_KEY_Y to LwjglKeys.KEY_Y,
        GLFW.GLFW_KEY_U to LwjglKeys.KEY_U,
        GLFW.GLFW_KEY_I to LwjglKeys.KEY_I,
        GLFW.GLFW_KEY_O to LwjglKeys.KEY_O,
        GLFW.GLFW_KEY_P to LwjglKeys.KEY_P,
        GLFW.GLFW_KEY_LEFT_BRACKET to LwjglKeys.KEY_LBRACKET,
        GLFW.GLFW_KEY_RIGHT_BRACKET to LwjglKeys.KEY_RBRACKET,
        GLFW.GLFW_KEY_ENTER to LwjglKeys.KEY_RETURN,
        GLFW.GLFW_KEY_LEFT_CONTROL to LwjglKeys.KEY_LCONTROL,
        GLFW.GLFW_KEY_A to LwjglKeys.KEY_A,
        GLFW.GLFW_KEY_S to LwjglKeys.KEY_S,
        GLFW.GLFW_KEY_D to LwjglKeys.KEY_D,
        GLFW.GLFW_KEY_F to LwjglKeys.KEY_F,
        GLFW.GLFW_KEY_G to LwjglKeys.KEY_G,
        GLFW.GLFW_KEY_H to LwjglKeys.KEY_H,
        GLFW.GLFW_KEY_J to LwjglKeys.KEY_J,
        GLFW.GLFW_KEY_K to LwjglKeys.KEY_K,
        GLFW.GLFW_KEY_L to LwjglKeys.KEY_L,
        GLFW.GLFW_KEY_SEMICOLON to LwjglKeys.KEY_SEMICOLON,
        GLFW.GLFW_KEY_APOSTROPHE to LwjglKeys.KEY_APOSTROPHE,
        GLFW.GLFW_KEY_GRAVE_ACCENT to LwjglKeys.KEY_GRAVE,
        GLFW.GLFW_KEY_LEFT_SHIFT to LwjglKeys.KEY_LSHIFT,
        GLFW.GLFW_KEY_BACKSLASH to LwjglKeys.KEY_BACKSLASH,
        GLFW.GLFW_KEY_Z to LwjglKeys.KEY_Z,
        GLFW.GLFW_KEY_X to LwjglKeys.KEY_X,
        GLFW.GLFW_KEY_C to LwjglKeys.KEY_C,
        GLFW.GLFW_KEY_V to LwjglKeys.KEY_V,
        GLFW.GLFW_KEY_B to LwjglKeys.KEY_B,
        GLFW.GLFW_KEY_N to LwjglKeys.KEY_N,
        GLFW.GLFW_KEY_M to LwjglKeys.KEY_M,
        GLFW.GLFW_KEY_COMMA to LwjglKeys.KEY_COMMA,
        GLFW.GLFW_KEY_PERIOD to LwjglKeys.KEY_PERIOD,
        GLFW.GLFW_KEY_SLASH to LwjglKeys.KEY_SLASH,
        GLFW.GLFW_KEY_RIGHT_SHIFT to LwjglKeys.KEY_RSHIFT,
        GLFW.GLFW_KEY_KP_MULTIPLY to LwjglKeys.KEY_MULTIPLY,
        GLFW.GLFW_KEY_LEFT_ALT to LwjglKeys.KEY_LMENU,
        GLFW.GLFW_KEY_SPACE to LwjglKeys.KEY_SPACE,
        GLFW.GLFW_KEY_CAPS_LOCK to LwjglKeys.KEY_CAPITAL,
        GLFW.GLFW_KEY_F1 to LwjglKeys.KEY_F1,
        GLFW.GLFW_KEY_F2 to LwjglKeys.KEY_F2,
        GLFW.GLFW_KEY_F3 to LwjglKeys.KEY_F3,
        GLFW.GLFW_KEY_F4 to LwjglKeys.KEY_F4,
        GLFW.GLFW_KEY_F5 to LwjglKeys.KEY_F5,
        GLFW.GLFW_KEY_F6 to LwjglKeys.KEY_F6,
        GLFW.GLFW_KEY_F7 to LwjglKeys.KEY_F7,
        GLFW.GLFW_KEY_F8 to LwjglKeys.KEY_F8,
        GLFW.GLFW_KEY_F9 to LwjglKeys.KEY_F9,
        GLFW.GLFW_KEY_F10 to LwjglKeys.KEY_F10,
        GLFW.GLFW_KEY_NUM_LOCK to LwjglKeys.KEY_NUMLOCK,
        GLFW.GLFW_KEY_SCROLL_LOCK to LwjglKeys.KEY_SCROLL,
        GLFW.GLFW_KEY_KP_7 to LwjglKeys.KEY_NUMPAD7,
        GLFW.GLFW_KEY_KP_8 to LwjglKeys.KEY_NUMPAD8,
        GLFW.GLFW_KEY_KP_9 to LwjglKeys.KEY_NUMPAD9,
        GLFW.GLFW_KEY_KP_SUBTRACT to LwjglKeys.KEY_SUBTRACT,
        GLFW.GLFW_KEY_KP_4 to LwjglKeys.KEY_NUMPAD4,
        GLFW.GLFW_KEY_KP_5 to LwjglKeys.KEY_NUMPAD5,
        GLFW.GLFW_KEY_KP_6 to LwjglKeys.KEY_NUMPAD6,
        GLFW.GLFW_KEY_KP_ADD to LwjglKeys.KEY_ADD,
        GLFW.GLFW_KEY_KP_1 to LwjglKeys.KEY_NUMPAD1,
        GLFW.GLFW_KEY_KP_2 to LwjglKeys.KEY_NUMPAD2,
        GLFW.GLFW_KEY_KP_3 to LwjglKeys.KEY_NUMPAD3,
        GLFW.GLFW_KEY_KP_0 to LwjglKeys.KEY_NUMPAD0,
        GLFW.GLFW_KEY_KP_DECIMAL to LwjglKeys.KEY_DECIMAL,
        GLFW.GLFW_KEY_F11 to LwjglKeys.KEY_F11,
        GLFW.GLFW_KEY_F12 to LwjglKeys.KEY_F12,
        GLFW.GLFW_KEY_F13 to LwjglKeys.KEY_F13,
        GLFW.GLFW_KEY_F14 to LwjglKeys.KEY_F14,
        GLFW.GLFW_KEY_F15 to LwjglKeys.KEY_F15,
        GLFW.GLFW_KEY_KP_EQUAL to LwjglKeys.KEY_NUMPADEQUALS,
        GLFW.GLFW_KEY_KP_ENTER to LwjglKeys.KEY_NUMPADENTER,
        GLFW.GLFW_KEY_RIGHT_CONTROL to LwjglKeys.KEY_RCONTROL,
        GLFW.GLFW_KEY_KP_DIVIDE to LwjglKeys.KEY_DIVIDE,
        GLFW.GLFW_KEY_RIGHT_ALT to LwjglKeys.KEY_RMENU,
        GLFW.GLFW_KEY_PAUSE to LwjglKeys.KEY_PAUSE,
        GLFW.GLFW_KEY_HOME to LwjglKeys.KEY_HOME,
        GLFW.GLFW_KEY_UP to LwjglKeys.KEY_UP,
        GLFW.GLFW_KEY_PAGE_UP to LwjglKeys.KEY_PRIOR,
        GLFW.GLFW_KEY_LEFT to LwjglKeys.KEY_LEFT,
        GLFW.GLFW_KEY_RIGHT to LwjglKeys.KEY_RIGHT,
        GLFW.GLFW_KEY_END to LwjglKeys.KEY_END,
        GLFW.GLFW_KEY_DOWN to LwjglKeys.KEY_DOWN,
        GLFW.GLFW_KEY_PAGE_DOWN to LwjglKeys.KEY_NEXT,
        GLFW.GLFW_KEY_INSERT to LwjglKeys.KEY_INSERT,
        GLFW.GLFW_KEY_DELETE to LwjglKeys.KEY_DELETE,
        GLFW.GLFW_KEY_LEFT_SUPER to LwjglKeys.KEY_LMETA,
        GLFW.GLFW_KEY_RIGHT_SUPER to LwjglKeys.KEY_RMETA
    )
    
    /**
     * Convert a GLFW key code to LWJGL2/OpenComputers key code.
     */
    fun glfwToOC(glfwKey: Int): Int {
        return glfwToLwjgl[glfwKey] ?: glfwKey
    }
    
    /**
     * Get the name of a key for display purposes.
     */
    fun getName(lwjglKey: Int): String {
        return when (lwjglKey) {
            LwjglKeys.KEY_ESCAPE -> "escape"
            LwjglKeys.KEY_BACK -> "back"
            LwjglKeys.KEY_TAB -> "tab"
            LwjglKeys.KEY_RETURN -> "enter"
            LwjglKeys.KEY_LCONTROL, LwjglKeys.KEY_RCONTROL -> "control"
            LwjglKeys.KEY_LSHIFT, LwjglKeys.KEY_RSHIFT -> "shift"
            LwjglKeys.KEY_LMENU, LwjglKeys.KEY_RMENU -> "menu"
            LwjglKeys.KEY_SPACE -> "space"
            LwjglKeys.KEY_CAPITAL -> "capital"
            LwjglKeys.KEY_NUMLOCK -> "numlock"
            LwjglKeys.KEY_SCROLL -> "scroll"
            LwjglKeys.KEY_UP -> "up"
            LwjglKeys.KEY_DOWN -> "down"
            LwjglKeys.KEY_LEFT -> "left"
            LwjglKeys.KEY_RIGHT -> "right"
            LwjglKeys.KEY_HOME -> "home"
            LwjglKeys.KEY_END -> "end"
            LwjglKeys.KEY_PRIOR -> "pageUp"
            LwjglKeys.KEY_NEXT -> "pageDown"
            LwjglKeys.KEY_INSERT -> "insert"
            LwjglKeys.KEY_DELETE -> "delete"
            in LwjglKeys.KEY_F1..LwjglKeys.KEY_F12 -> "f${lwjglKey - LwjglKeys.KEY_F1 + 1}"
            else -> "key$lwjglKey"
        }
    }
}

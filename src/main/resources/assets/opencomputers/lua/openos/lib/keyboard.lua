-- Keyboard API
-- Provides keyboard key code constants and utilities.

local keyboard = {}

-- Key codes (based on LWJGL key constants)
keyboard.keys = {
  -- Function keys
  ["f1"] = 0x3B,
  ["f2"] = 0x3C,
  ["f3"] = 0x3D,
  ["f4"] = 0x3E,
  ["f5"] = 0x3F,
  ["f6"] = 0x40,
  ["f7"] = 0x41,
  ["f8"] = 0x42,
  ["f9"] = 0x43,
  ["f10"] = 0x44,
  ["f11"] = 0x57,
  ["f12"] = 0x58,
  
  -- Numbers (top row)
  ["1"] = 0x02,
  ["2"] = 0x03,
  ["3"] = 0x04,
  ["4"] = 0x05,
  ["5"] = 0x06,
  ["6"] = 0x07,
  ["7"] = 0x08,
  ["8"] = 0x09,
  ["9"] = 0x0A,
  ["0"] = 0x0B,
  
  -- Letters
  ["a"] = 0x1E,
  ["b"] = 0x30,
  ["c"] = 0x2E,
  ["d"] = 0x20,
  ["e"] = 0x12,
  ["f"] = 0x21,
  ["g"] = 0x22,
  ["h"] = 0x23,
  ["i"] = 0x17,
  ["j"] = 0x24,
  ["k"] = 0x25,
  ["l"] = 0x26,
  ["m"] = 0x32,
  ["n"] = 0x31,
  ["o"] = 0x18,
  ["p"] = 0x19,
  ["q"] = 0x10,
  ["r"] = 0x13,
  ["s"] = 0x1F,
  ["t"] = 0x14,
  ["u"] = 0x16,
  ["v"] = 0x2F,
  ["w"] = 0x11,
  ["x"] = 0x2D,
  ["y"] = 0x15,
  ["z"] = 0x2C,
  
  -- Special keys
  ["back"] = 0x0E,
  ["backspace"] = 0x0E,
  ["tab"] = 0x0F,
  ["enter"] = 0x1C,
  ["return"] = 0x1C,
  ["lshift"] = 0x2A,
  ["rshift"] = 0x36,
  ["lcontrol"] = 0x1D,
  ["rcontrol"] = 0x9D,
  ["lmenu"] = 0x38,   -- Left Alt
  ["lalt"] = 0x38,
  ["rmenu"] = 0xB8,   -- Right Alt
  ["ralt"] = 0xB8,
  ["space"] = 0x39,
  ["capital"] = 0x3A, -- Caps Lock
  ["capslock"] = 0x3A,
  ["escape"] = 0x01,
  
  -- Navigation
  ["up"] = 0xC8,
  ["down"] = 0xD0,
  ["left"] = 0xCB,
  ["right"] = 0xCD,
  ["home"] = 0xC7,
  ["end"] = 0xCF,
  ["pageup"] = 0xC9,
  ["prior"] = 0xC9,
  ["pagedown"] = 0xD1,
  ["next"] = 0xD1,
  ["insert"] = 0xD2,
  ["delete"] = 0xD3,
  
  -- Punctuation
  ["minus"] = 0x0C,
  ["equals"] = 0x0D,
  ["lbracket"] = 0x1A,
  ["rbracket"] = 0x1B,
  ["semicolon"] = 0x27,
  ["apostrophe"] = 0x28,
  ["grave"] = 0x29,   -- Backtick
  ["backslash"] = 0x2B,
  ["comma"] = 0x33,
  ["period"] = 0x34,
  ["slash"] = 0x35,
  
  -- Numpad
  ["numpad0"] = 0x52,
  ["numpad1"] = 0x4F,
  ["numpad2"] = 0x50,
  ["numpad3"] = 0x51,
  ["numpad4"] = 0x4B,
  ["numpad5"] = 0x4C,
  ["numpad6"] = 0x4D,
  ["numpad7"] = 0x47,
  ["numpad8"] = 0x48,
  ["numpad9"] = 0x49,
  ["numlock"] = 0x45,
  ["scroll"] = 0x46,  -- Scroll Lock
  ["numpadmul"] = 0x37,
  ["numpadsub"] = 0x4A,
  ["numpadadd"] = 0x4E,
  ["numpaddecimal"] = 0x53,
  ["numpadenter"] = 0x9C,
  ["numpaddiv"] = 0xB5,
}

-- Reverse mapping: code -> name
keyboard.names = {}
for name, code in pairs(keyboard.keys) do
  keyboard.names[code] = keyboard.names[code] or name
end

-- Check if a key is down
function keyboard.isKeyDown(keyCode)
  checkArg(1, keyCode, "number")
  local component = require("component")
  local kb = component.getPrimary("keyboard")
  if kb then
    return kb.isKeyDown(keyCode)
  end
  return false
end

-- Check if Shift is held
function keyboard.isShiftDown()
  return keyboard.isKeyDown(keyboard.keys.lshift) or 
         keyboard.isKeyDown(keyboard.keys.rshift)
end

-- Check if Control is held
function keyboard.isControlDown()
  return keyboard.isKeyDown(keyboard.keys.lcontrol) or 
         keyboard.isKeyDown(keyboard.keys.rcontrol)
end

-- Check if Alt is held
function keyboard.isAltDown()
  return keyboard.isKeyDown(keyboard.keys.lalt) or 
         keyboard.isKeyDown(keyboard.keys.ralt)
end

-- Check if a character is printable
function keyboard.isCharacterPrintable(char)
  if type(char) == "number" then
    return char >= 32 and char < 127
  elseif type(char) == "string" then
    local code = char:byte()
    return code and code >= 32 and code < 127
  end
  return false
end

-- Get key code by name
function keyboard.keyCode(name)
  checkArg(1, name, "string")
  return keyboard.keys[name:lower()]
end

-- Get key name by code
function keyboard.keyName(code)
  checkArg(1, code, "number")
  return keyboard.names[code]
end

return keyboard

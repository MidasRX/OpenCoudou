-- Unicode String Library
-- Provides unicode-aware string operations.

local unicode = {}

-- Check argument type helper
local function checkArg(n, have, ...)
  local want = {...}
  local haveType = type(have)
  for i = 1, #want do
    if haveType == want[i] then return end
  end
  error(string.format("bad argument #%d (%s expected, got %s)", 
    n, table.concat(want, " or "), haveType), 2)
end

-- Get length of a unicode string
function unicode.len(str)
  checkArg(1, str, "string", "nil")
  if not str then return 0 end
  
  local len = 0
  local i = 1
  while i <= #str do
    local byte = str:byte(i)
    if byte < 0x80 then
      i = i + 1
    elseif byte < 0xC0 then
      -- Continuation byte, invalid start
      i = i + 1
    elseif byte < 0xE0 then
      i = i + 2
    elseif byte < 0xF0 then
      i = i + 3
    else
      i = i + 4
    end
    len = len + 1
  end
  return len
end

-- Get width of a unicode string (for display)
function unicode.wlen(str)
  checkArg(1, str, "string", "nil")
  if not str then return 0 end
  
  local width = 0
  local i = 1
  while i <= #str do
    local byte = str:byte(i)
    local codepoint = 0
    local size = 1
    
    if byte < 0x80 then
      codepoint = byte
      size = 1
    elseif byte < 0xE0 then
      codepoint = (byte % 0x20) * 0x40 + (str:byte(i + 1) or 0) % 0x40
      size = 2
    elseif byte < 0xF0 then
      codepoint = (byte % 0x10) * 0x1000 + 
                  ((str:byte(i + 1) or 0) % 0x40) * 0x40 +
                  (str:byte(i + 2) or 0) % 0x40
      size = 3
    else
      codepoint = (byte % 0x08) * 0x40000 +
                  ((str:byte(i + 1) or 0) % 0x40) * 0x1000 +
                  ((str:byte(i + 2) or 0) % 0x40) * 0x40 +
                  (str:byte(i + 3) or 0) % 0x40
      size = 4
    end
    
    -- East Asian Wide characters
    if codepoint >= 0x1100 and (
       (codepoint <= 0x115F) or  -- Hangul Jamo
       (codepoint >= 0x2E80 and codepoint <= 0x9FFF) or  -- CJK
       (codepoint >= 0xAC00 and codepoint <= 0xD7A3) or  -- Hangul Syllables
       (codepoint >= 0xF900 and codepoint <= 0xFAFF) or  -- CJK Compat
       (codepoint >= 0xFE10 and codepoint <= 0xFE1F) or  -- Vertical forms
       (codepoint >= 0xFE30 and codepoint <= 0xFE6F) or  -- CJK Compat forms
       (codepoint >= 0xFF00 and codepoint <= 0xFF60) or  -- Fullwidth
       (codepoint >= 0xFFE0 and codepoint <= 0xFFE6) or  -- Fullwidth
       (codepoint >= 0x20000 and codepoint <= 0x2FFFF)   -- CJK Ext
    ) then
      width = width + 2
    else
      width = width + 1
    end
    
    i = i + size
  end
  return width
end

-- Get character width
function unicode.charWidth(char)
  checkArg(1, char, "string")
  return unicode.wlen(unicode.sub(char, 1, 1))
end

-- Check if wide character
function unicode.isWide(char)
  return unicode.charWidth(char) > 1
end

-- Unicode-aware substring
function unicode.sub(str, i, j)
  checkArg(1, str, "string", "nil")
  checkArg(2, i, "number")
  checkArg(3, j, "number", "nil")
  
  if not str or str == "" then return "" end
  
  local len = unicode.len(str)
  j = j or len
  
  -- Handle negative indices
  if i < 0 then i = len + i + 1 end
  if j < 0 then j = len + j + 1 end
  
  -- Clamp to valid range
  if i < 1 then i = 1 end
  if j > len then j = len end
  if i > j then return "" end
  
  local result = {}
  local pos = 1
  local charIndex = 0
  
  while pos <= #str do
    local byte = str:byte(pos)
    local size = 1
    
    if byte < 0x80 then
      size = 1
    elseif byte < 0xE0 then
      size = 2
    elseif byte < 0xF0 then
      size = 3
    else
      size = 4
    end
    
    charIndex = charIndex + 1
    
    if charIndex >= i and charIndex <= j then
      table.insert(result, str:sub(pos, pos + size - 1))
    end
    
    if charIndex > j then break end
    pos = pos + size
  end
  
  return table.concat(result)
end

-- Unicode-aware character extraction
function unicode.char(...)
  local args = {...}
  local result = {}
  
  for _, codepoint in ipairs(args) do
    if codepoint < 0x80 then
      table.insert(result, string.char(codepoint))
    elseif codepoint < 0x800 then
      table.insert(result, string.char(
        0xC0 + math.floor(codepoint / 0x40),
        0x80 + (codepoint % 0x40)
      ))
    elseif codepoint < 0x10000 then
      table.insert(result, string.char(
        0xE0 + math.floor(codepoint / 0x1000),
        0x80 + math.floor(codepoint / 0x40) % 0x40,
        0x80 + (codepoint % 0x40)
      ))
    else
      table.insert(result, string.char(
        0xF0 + math.floor(codepoint / 0x40000),
        0x80 + math.floor(codepoint / 0x1000) % 0x40,
        0x80 + math.floor(codepoint / 0x40) % 0x40,
        0x80 + (codepoint % 0x40)
      ))
    end
  end
  
  return table.concat(result)
end

-- Get codepoint(s) from string
function unicode.codepoint(str, i, j)
  checkArg(1, str, "string")
  i = i or 1
  j = j or i
  
  local result = {}
  local extracted = unicode.sub(str, i, j)
  local pos = 1
  
  while pos <= #extracted do
    local byte = extracted:byte(pos)
    local codepoint = 0
    local size = 1
    
    if byte < 0x80 then
      codepoint = byte
      size = 1
    elseif byte < 0xE0 then
      codepoint = (byte % 0x20) * 0x40 + (extracted:byte(pos + 1) or 0) % 0x40
      size = 2
    elseif byte < 0xF0 then
      codepoint = (byte % 0x10) * 0x1000 + 
                  ((extracted:byte(pos + 1) or 0) % 0x40) * 0x40 +
                  (extracted:byte(pos + 2) or 0) % 0x40
      size = 3
    else
      codepoint = (byte % 0x08) * 0x40000 +
                  ((extracted:byte(pos + 1) or 0) % 0x40) * 0x1000 +
                  ((extracted:byte(pos + 2) or 0) % 0x40) * 0x40 +
                  (extracted:byte(pos + 3) or 0) % 0x40
      size = 4
    end
    
    table.insert(result, codepoint)
    pos = pos + size
  end
  
  return table.unpack(result)
end

-- Upper case conversion
function unicode.upper(str)
  checkArg(1, str, "string")
  -- Basic ASCII uppercase
  return str:upper()
end

-- Lower case conversion  
function unicode.lower(str)
  checkArg(1, str, "string")
  -- Basic ASCII lowercase
  return str:lower()
end

-- Reverse string
function unicode.reverse(str)
  checkArg(1, str, "string")
  local result = {}
  local pos = 1
  
  while pos <= #str do
    local byte = str:byte(pos)
    local size = 1
    
    if byte < 0x80 then
      size = 1
    elseif byte < 0xE0 then
      size = 2
    elseif byte < 0xF0 then
      size = 3
    else
      size = 4
    end
    
    table.insert(result, 1, str:sub(pos, pos + size - 1))
    pos = pos + size
  end
  
  return table.concat(result)
end

-- Get width-truncated string
function unicode.wtrunc(str, maxWidth)
  checkArg(1, str, "string")
  checkArg(2, maxWidth, "number")
  
  local width = 0
  local result = {}
  local pos = 1
  
  while pos <= #str do
    local byte = str:byte(pos)
    local size = 1
    
    if byte < 0x80 then
      size = 1
    elseif byte < 0xE0 then
      size = 2
    elseif byte < 0xF0 then
      size = 3
    else
      size = 4
    end
    
    local char = str:sub(pos, pos + size - 1)
    local charWidth = unicode.wlen(char)
    
    if width + charWidth > maxWidth then
      break
    end
    
    width = width + charWidth
    table.insert(result, char)
    pos = pos + size
  end
  
  return table.concat(result)
end

return unicode

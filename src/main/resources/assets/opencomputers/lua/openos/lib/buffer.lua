-- Buffer Library
-- Provides buffered I/O streams for file handling.

local buffer = {}
buffer.__index = buffer

-- Create a new buffer
function buffer.new(mode, stream)
  local self = setmetatable({}, buffer)
  
  self.mode = {}
  for m in mode:gmatch(".") do
    self.mode[m] = true
  end
  
  self.stream = stream
  self.readBuffer = ""
  self.readBufferOffset = 0
  self.writeBuffer = ""
  self.bufferSize = 2048
  self.closed = false
  
  return self
end

-- Check if buffer is open
function buffer:isClosed()
  return self.closed
end

-- Close the buffer
function buffer:close()
  if self.closed then
    return nil, "already closed"
  end
  
  -- Flush write buffer
  if self.mode.w or self.mode.a then
    self:flush()
  end
  
  -- Close underlying stream
  if self.stream.close then
    self.stream:close()
  end
  
  self.closed = true
  return true
end

-- Flush the write buffer
function buffer:flush()
  if self.closed then
    return nil, "closed"
  end
  
  if #self.writeBuffer > 0 then
    local result, err = self.stream:write(self.writeBuffer)
    if not result then
      return nil, err
    end
    self.writeBuffer = ""
  end
  
  return true
end

-- Read from buffer
function buffer:read(...)
  if self.closed then
    return nil, "closed"
  end
  
  if not self.mode.r then
    return nil, "not opened for reading"
  end
  
  local args = {...}
  if #args == 0 then
    args = {"*l"}
  end
  
  local results = {}
  
  for _, arg in ipairs(args) do
    local result
    
    if type(arg) == "number" then
      result = self:readBytes(arg)
    elseif arg == "*l" then
      result = self:readLine(false)
    elseif arg == "*L" then
      result = self:readLine(true)
    elseif arg == "*a" then
      result = self:readAll()
    elseif arg == "*n" then
      result = self:readNumber()
    else
      error("bad argument (invalid option)", 2)
    end
    
    table.insert(results, result)
  end
  
  return table.unpack(results)
end

-- Read specific number of bytes
function buffer:readBytes(n)
  if n <= 0 then return "" end
  
  -- Fill buffer if needed
  while #self.readBuffer - self.readBufferOffset < n do
    local data = self.stream:read(self.bufferSize)
    if not data or #data == 0 then
      break
    end
    self.readBuffer = self.readBuffer:sub(self.readBufferOffset + 1) .. data
    self.readBufferOffset = 0
  end
  
  local available = #self.readBuffer - self.readBufferOffset
  if available == 0 then
    return nil
  end
  
  local count = math.min(n, available)
  local result = self.readBuffer:sub(self.readBufferOffset + 1, self.readBufferOffset + count)
  self.readBufferOffset = self.readBufferOffset + count
  
  return result
end

-- Read a line
function buffer:readLine(keepNewline)
  local result = {}
  
  while true do
    -- Look for newline in buffer
    local bufferContent = self.readBuffer:sub(self.readBufferOffset + 1)
    local nlPos = bufferContent:find("\n")
    
    if nlPos then
      local line = bufferContent:sub(1, keepNewline and nlPos or (nlPos - 1))
      self.readBufferOffset = self.readBufferOffset + nlPos
      table.insert(result, line)
      break
    else
      -- Add all buffered content and read more
      if #bufferContent > 0 then
        table.insert(result, bufferContent)
      end
      
      local data = self.stream:read(self.bufferSize)
      if not data or #data == 0 then
        break
      end
      
      self.readBuffer = data
      self.readBufferOffset = 0
    end
  end
  
  local line = table.concat(result)
  return #line > 0 and line or nil
end

-- Read all remaining content
function buffer:readAll()
  local result = {self.readBuffer:sub(self.readBufferOffset + 1)}
  self.readBuffer = ""
  self.readBufferOffset = 0
  
  while true do
    local data = self.stream:read(self.bufferSize)
    if not data or #data == 0 then
      break
    end
    table.insert(result, data)
  end
  
  local content = table.concat(result)
  return #content > 0 and content or nil
end

-- Read a number
function buffer:readNumber()
  local str = ""
  local hasDigit = false
  local hasDecimal = false
  
  -- Skip whitespace
  while true do
    local char = self:readBytes(1)
    if not char then return nil end
    
    if not char:match("%s") then
      str = char
      break
    end
  end
  
  -- Read number
  while true do
    local char = self:readBytes(1)
    if not char then break end
    
    if char:match("[0-9]") then
      str = str .. char
      hasDigit = true
    elseif char == "." and not hasDecimal then
      str = str .. char
      hasDecimal = true
    elseif char == "-" and #str == 0 then
      str = str .. char
    else
      -- Unread this character
      self.readBuffer = char .. self.readBuffer:sub(self.readBufferOffset + 1)
      self.readBufferOffset = 0
      break
    end
  end
  
  return hasDigit and tonumber(str) or nil
end

-- Write to buffer
function buffer:write(...)
  if self.closed then
    return nil, "closed"
  end
  
  if not (self.mode.w or self.mode.a) then
    return nil, "not opened for writing"
  end
  
  local args = {...}
  for _, arg in ipairs(args) do
    self.writeBuffer = self.writeBuffer .. tostring(arg)
    
    -- Flush if buffer is full or contains newline
    if #self.writeBuffer >= self.bufferSize or 
       (self.writeBuffer:find("\n") and not self.mode.b) then
      local result, err = self:flush()
      if not result then
        return nil, err
      end
    end
  end
  
  return true
end

-- Seek in buffer
function buffer:seek(whence, offset)
  if self.closed then
    return nil, "closed"
  end
  
  -- Flush before seeking
  if self.mode.w or self.mode.a then
    self:flush()
  end
  
  -- Clear read buffer on seek
  self.readBuffer = ""
  self.readBufferOffset = 0
  
  if self.stream.seek then
    return self.stream:seek(whence, offset)
  end
  
  return nil, "seek not supported"
end

-- Set buffering mode
function buffer:setvbuf(mode, size)
  if mode == "no" then
    self.bufferSize = 0
  elseif mode == "full" then
    self.bufferSize = size or 2048
  elseif mode == "line" then
    self.bufferSize = size or 2048
    self.mode.b = nil  -- Line buffering
  end
  return true
end

-- Get underlying stream
function buffer:getStream()
  return self.stream
end

-- Metatable for file handle behavior
function buffer:__gc()
  self:close()
end

return buffer

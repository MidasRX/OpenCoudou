-- Pipe Library
-- Provides inter-process communication pipes.

local buffer = require("buffer")

local pipe = {}

-- Create a new pipe
function pipe.create()
  local p = {
    readBuffer = "",
    closed = false,
    writers = 0,
    readers = 0
  }
  
  -- Create a stream-like object for the pipe
  local stream = {}
  
  function stream:read(n)
    if p.closed and #p.readBuffer == 0 then
      return nil
    end
    
    -- If no data available and pipe is open, wait
    while #p.readBuffer == 0 and not p.closed do
      coroutine.yield()
    end
    
    if #p.readBuffer == 0 then
      return nil
    end
    
    n = n or #p.readBuffer
    local data = p.readBuffer:sub(1, n)
    p.readBuffer = p.readBuffer:sub(n + 1)
    return data
  end
  
  function stream:write(data)
    if p.closed then
      return nil, "broken pipe"
    end
    
    p.readBuffer = p.readBuffer .. data
    return true
  end
  
  function stream:close()
    p.closed = true
    return true
  end
  
  function stream:seek()
    return nil, "cannot seek on pipe"
  end
  
  p.stream = stream
  
  return p
end

-- Create read end of pipe
function pipe.popen(command, mode)
  mode = mode or "r"
  
  local p = pipe.create()
  local readMode = mode:find("r")
  local writeMode = mode:find("w")
  
  -- Create appropriate buffer
  local buf
  if readMode then
    buf = buffer.new("r", p.stream)
    p.readers = p.readers + 1
  elseif writeMode then
    buf = buffer.new("w", p.stream)
    p.writers = p.writers + 1
  end
  
  -- If command provided, execute it
  if command then
    local shell = require("shell")
    -- Run command and connect to pipe
    -- This would need proper process handling
  end
  
  return buf
end

-- Create bidirectional pipe pair
function pipe.createPair()
  local p1 = pipe.create()
  local p2 = pipe.create()
  
  -- Connect p1 write to p2 read and vice versa
  local write1, read1 = p1.stream.write, p1.stream.read
  local write2, read2 = p2.stream.write, p2.stream.read
  
  -- Redirect
  p1.stream.read = read2
  p2.stream.read = read1
  
  local buf1 = buffer.new("rw", p1.stream)
  local buf2 = buffer.new("rw", p2.stream)
  
  return buf1, buf2
end

-- Create a null pipe (discards all data)
function pipe.null()
  local stream = {
    read = function() return nil end,
    write = function() return true end,
    close = function() return true end,
    seek = function() return nil, "cannot seek" end
  }
  
  return buffer.new("rw", stream)
end

-- Tee pipe - writes to multiple destinations
function pipe.tee(...)
  local outputs = {...}
  
  local stream = {
    read = function()
      return nil, "tee is write-only"
    end,
    
    write = function(self, data)
      for _, output in ipairs(outputs) do
        if output.write then
          output:write(data)
        end
      end
      return true
    end,
    
    close = function()
      for _, output in ipairs(outputs) do
        if output.close then
          output:close()
        end
      end
      return true
    end,
    
    seek = function()
      return nil, "cannot seek on tee"
    end
  }
  
  return buffer.new("w", stream)
end

-- Create buffered pipe with specified buffer size
function pipe.buffered(size)
  size = size or 4096
  
  local p = pipe.create()
  local buf = buffer.new("rw", p.stream)
  buf.bufferSize = size
  
  return buf
end

return pipe

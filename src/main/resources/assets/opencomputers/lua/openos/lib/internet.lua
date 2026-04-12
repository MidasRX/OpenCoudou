-- Internet API
-- Provides HTTP/S and TCP/IP networking capabilities.

local internet = {}

local component = require("component")

-- Check if internet card is available
function internet.isAvailable()
  return component.isAvailable("internet")
end

-- Check if HTTP is enabled
function internet.isHttpEnabled()
  if not internet.isAvailable() then return false end
  local inet = component.internet
  return inet and inet.isHttpEnabled and inet.isHttpEnabled()
end

-- Check if TCP is enabled
function internet.isTcpEnabled()
  if not internet.isAvailable() then return false end
  local inet = component.internet
  return inet and inet.isTcpEnabled and inet.isTcpEnabled()
end

-- Make an HTTP request
function internet.request(url, data, headers, method)
  checkArg(1, url, "string")
  checkArg(2, data, "string", "table", "nil")
  checkArg(3, headers, "table", "nil")
  checkArg(4, method, "string", "nil")
  
  if not internet.isAvailable() then
    return nil, "no internet card"
  end
  
  local inet = component.internet
  
  -- Convert data table to POST string
  if type(data) == "table" then
    local parts = {}
    for k, v in pairs(data) do
      table.insert(parts, tostring(k) .. "=" .. tostring(v))
    end
    data = table.concat(parts, "&")
  end
  
  -- Default method
  method = method or (data and "POST" or "GET")
  
  local response, err = inet.request(url, data, headers, method)
  
  if not response then
    return nil, err or "request failed"
  end
  
  -- Return a response handle
  local handle = {}
  
  function handle:read(n)
    if type(response) == "string" then
      return response
    elseif type(response) == "function" then
      return response(n or math.huge)
    elseif type(response) == "table" and response.read then
      return response:read(n)
    end
    return nil
  end
  
  function handle:readAll()
    local chunks = {}
    while true do
      local chunk = self:read(4096)
      if not chunk then break end
      table.insert(chunks, chunk)
    end
    return table.concat(chunks)
  end
  
  function handle:close()
    if type(response) == "table" and response.close then
      response:close()
    end
  end
  
  function handle:response()
    if type(response) == "table" then
      return response.code, response.message, response.headers
    end
    return 200, "OK", {}
  end
  
  return handle
end

-- Open a TCP connection
function internet.open(address, port)
  checkArg(1, address, "string")
  checkArg(2, port, "number", "nil")
  
  if not internet.isAvailable() then
    return nil, "no internet card"
  end
  
  -- Parse address:port format
  if not port then
    local host, p = address:match("^(.+):(%d+)$")
    if host and p then
      address = host
      port = tonumber(p)
    else
      return nil, "no port specified"
    end
  end
  
  local inet = component.internet
  local socket, err = inet.connect(address, port)
  
  if not socket then
    return nil, err or "connection failed"
  end
  
  -- Create socket handle
  local handle = {
    socket = socket,
    address = address,
    port = port,
    closed = false
  }
  
  function handle:read(n)
    if self.closed then return nil, "closed" end
    
    n = n or math.huge
    local result, err = self.socket.read(n)
    
    if not result and err then
      return nil, err
    end
    
    return result
  end
  
  function handle:write(data)
    if self.closed then return nil, "closed" end
    
    local result, err = self.socket.write(data)
    
    if not result then
      return nil, err or "write failed"
    end
    
    return result
  end
  
  function handle:close()
    if self.closed then return end
    
    self.closed = true
    if self.socket.close then
      self.socket.close()
    end
  end
  
  function handle:finishConnect()
    if self.socket.finishConnect then
      return self.socket.finishConnect()
    end
    return true
  end
  
  function handle:id()
    if self.socket.id then
      return self.socket.id()
    end
    return nil
  end
  
  return handle
end

-- Convenience function for GET request
function internet.get(url, headers)
  local response, err = internet.request(url, nil, headers, "GET")
  if not response then
    return nil, err
  end
  
  local content = response:readAll()
  response:close()
  return content
end

-- Convenience function for POST request
function internet.post(url, data, headers)
  local response, err = internet.request(url, data, headers, "POST")
  if not response then
    return nil, err
  end
  
  local content = response:readAll()
  response:close()
  return content
end

-- Parse URL
function internet.parseUrl(url)
  local result = {}
  
  -- Protocol
  result.protocol = url:match("^(%w+)://") or "http"
  url = url:gsub("^%w+://", "")
  
  -- User:password
  local userInfo = url:match("^([^@]+)@")
  if userInfo then
    result.user, result.password = userInfo:match("^([^:]+):?(.*)$")
    url = url:gsub("^[^@]+@", "")
  end
  
  -- Host and port
  local hostPort = url:match("^([^/]+)")
  if hostPort then
    result.host, result.port = hostPort:match("^([^:]+):?(%d*)$")
    result.port = tonumber(result.port)
    url = url:gsub("^[^/]+", "")
  end
  
  -- Path
  result.path = url:match("^([^?#]*)") or "/"
  url = url:gsub("^[^?#]*", "")
  
  -- Query
  result.query = url:match("^%?([^#]*)")
  url = url:gsub("^%?[^#]*", "")
  
  -- Fragment
  result.fragment = url:match("^#(.*)$")
  
  return result
end

-- URL encode
function internet.encode(str)
  str = str:gsub("([^%w%-%.%_%~])", function(c)
    return string.format("%%%02X", string.byte(c))
  end)
  return str
end

-- URL decode
function internet.decode(str)
  str = str:gsub("%%(%x%x)", function(hex)
    return string.char(tonumber(hex, 16))
  end)
  return str
end

return internet

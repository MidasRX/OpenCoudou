-- Component API
-- Provides access to computer components.

local component = {}

-- Cache for component proxies
local proxies = {}

-- List all components of a specific type
function component.list(filter, exact)
  checkArg(1, filter, "string", "nil")
  checkArg(2, exact, "boolean", "nil")
  
  local result = {}
  for address, ctype in component_native.list() do
    if not filter or 
       (exact and ctype == filter) or 
       (not exact and ctype:find(filter, 1, true)) then
      result[address] = ctype
    end
  end
  return result
end

-- Get the type of a component
function component.type(address)
  checkArg(1, address, "string")
  return component_native.type(address)
end

-- Get documentation for a method
function component.doc(address, method)
  checkArg(1, address, "string")
  checkArg(2, method, "string")
  return component_native.doc(address, method)
end

-- Get a slot for a component
function component.slot(address)
  checkArg(1, address, "string")
  return component_native.slot(address)
end

-- Get list of methods for a component
function component.methods(address)
  checkArg(1, address, "string")
  return component_native.methods(address)
end

-- Invoke a method on a component
function component.invoke(address, method, ...)
  checkArg(1, address, "string")
  checkArg(2, method, "string")
  return component_native.invoke(address, method, ...)
end

-- Get the primary component of a specific type
function component.getPrimary(ctype)
  checkArg(1, ctype, "string")
  
  local address = component_native.getPrimary(ctype)
  if address then
    return component.proxy(address)
  end
  return nil, "no primary component of type '" .. ctype .. "'"
end

-- Set the primary component of a specific type
function component.setPrimary(ctype, address)
  checkArg(1, ctype, "string")
  checkArg(2, address, "string", "nil")
  return component_native.setPrimary(ctype, address)
end

-- Is a component available?
function component.isAvailable(ctype)
  checkArg(1, ctype, "string")
  return component.getPrimary(ctype) ~= nil
end

-- Get a proxy for a component
function component.proxy(address)
  checkArg(1, address, "string")
  
  -- Return cached proxy if available
  if proxies[address] then
    return proxies[address]
  end
  
  local ctype = component.type(address)
  if not ctype then
    return nil, "no such component"
  end
  
  local proxy = {address = address, type = ctype}
  local methods = component.methods(address)
  
  if methods then
    for method, direct in pairs(methods) do
      if direct then
        proxy[method] = function(...)
          return component.invoke(address, method, ...)
        end
      else
        proxy[method] = function(...)
          return component.invoke(address, method, ...)
        end
      end
    end
  end
  
  -- Cache the proxy
  proxies[address] = proxy
  return proxy
end

-- Get first component of a type
function component.get(address, ctype)
  checkArg(1, address, "string")
  checkArg(2, ctype, "string", "nil")
  
  -- Try exact match first
  if component.type(address) then
    return address
  end
  
  -- Then try partial match
  for addr, t in component.list(ctype or "") do
    if addr:find(address, 1, true) then
      return addr
    end
  end
  
  return nil, "no such component"
end

-- Iterate over components
function component.pairs(ctype)
  local iter = component.list(ctype)
  return function()
    local address = next(iter)
    if address then
      return address, component.proxy(address)
    end
  end
end

-- Create a filtered component iterator
function component.filter(ctype)
  return function()
    for address in component.list(ctype) do
      coroutine.yield(address, component.proxy(address))
    end
  end
end

-- Metatable for lazy loading
setmetatable(component, {
  __index = function(_, key)
    -- Try to find primary component of that type
    local primary, err = component.getPrimary(key)
    if primary then
      return primary
    end
    return nil
  end
})

return component

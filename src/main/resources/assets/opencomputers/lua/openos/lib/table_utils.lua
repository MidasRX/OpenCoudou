-- Table Utilities Library
-- Extended table operations for OpenComputers.

local tableUtil = {}

-- Deep copy a table
function tableUtil.copy(t, seen)
  if type(t) ~= "table" then return t end
  
  seen = seen or {}
  if seen[t] then return seen[t] end
  
  local copy = {}
  seen[t] = copy
  
  for k, v in pairs(t) do
    copy[tableUtil.copy(k, seen)] = tableUtil.copy(v, seen)
  end
  
  return setmetatable(copy, getmetatable(t))
end

-- Shallow copy a table
function tableUtil.shallowCopy(t)
  if type(t) ~= "table" then return t end
  
  local copy = {}
  for k, v in pairs(t) do
    copy[k] = v
  end
  
  return setmetatable(copy, getmetatable(t))
end

-- Merge tables (later tables override earlier)
function tableUtil.merge(...)
  local result = {}
  
  for _, t in ipairs({...}) do
    if type(t) == "table" then
      for k, v in pairs(t) do
        result[k] = v
      end
    end
  end
  
  return result
end

-- Deep merge tables
function tableUtil.deepMerge(...)
  local result = {}
  
  for _, t in ipairs({...}) do
    if type(t) == "table" then
      for k, v in pairs(t) do
        if type(v) == "table" and type(result[k]) == "table" then
          result[k] = tableUtil.deepMerge(result[k], v)
        else
          result[k] = tableUtil.copy(v)
        end
      end
    end
  end
  
  return result
end

-- Get table keys
function tableUtil.keys(t)
  local keys = {}
  for k in pairs(t) do
    table.insert(keys, k)
  end
  return keys
end

-- Get table values
function tableUtil.values(t)
  local values = {}
  for _, v in pairs(t) do
    table.insert(values, v)
  end
  return values
end

-- Check if table contains value
function tableUtil.contains(t, value)
  for _, v in pairs(t) do
    if v == value then
      return true
    end
  end
  return false
end

-- Find key for value
function tableUtil.find(t, value)
  for k, v in pairs(t) do
    if v == value then
      return k
    end
  end
  return nil
end

-- Count table elements
function tableUtil.count(t)
  local count = 0
  for _ in pairs(t) do
    count = count + 1
  end
  return count
end

-- Check if table is empty
function tableUtil.isEmpty(t)
  return next(t) == nil
end

-- Check if table is array-like
function tableUtil.isArray(t)
  if type(t) ~= "table" then return false end
  
  local count = 0
  for k in pairs(t) do
    count = count + 1
    if type(k) ~= "number" or k ~= math.floor(k) or k < 1 then
      return false
    end
  end
  
  return count == 0 or t[count] ~= nil
end

-- Filter table by predicate
function tableUtil.filter(t, predicate)
  local result = {}
  
  if tableUtil.isArray(t) then
    for i, v in ipairs(t) do
      if predicate(v, i) then
        table.insert(result, v)
      end
    end
  else
    for k, v in pairs(t) do
      if predicate(v, k) then
        result[k] = v
      end
    end
  end
  
  return result
end

-- Map table values
function tableUtil.map(t, func)
  local result = {}
  
  if tableUtil.isArray(t) then
    for i, v in ipairs(t) do
      result[i] = func(v, i)
    end
  else
    for k, v in pairs(t) do
      result[k] = func(v, k)
    end
  end
  
  return result
end

-- Reduce table to single value
function tableUtil.reduce(t, func, initial)
  local result = initial
  local started = initial ~= nil
  
  for k, v in pairs(t) do
    if not started then
      result = v
      started = true
    else
      result = func(result, v, k)
    end
  end
  
  return result
end

-- Flatten nested arrays
function tableUtil.flatten(t, depth)
  depth = depth or 1
  local result = {}
  
  local function flattenHelper(arr, currentDepth)
    for _, v in ipairs(arr) do
      if type(v) == "table" and currentDepth > 0 then
        flattenHelper(v, currentDepth - 1)
      else
        table.insert(result, v)
      end
    end
  end
  
  flattenHelper(t, depth)
  return result
end

-- Reverse array
function tableUtil.reverse(t)
  local result = {}
  for i = #t, 1, -1 do
    table.insert(result, t[i])
  end
  return result
end

-- Slice array
function tableUtil.slice(t, start, stop)
  local result = {}
  start = start or 1
  stop = stop or #t
  
  if start < 0 then start = #t + start + 1 end
  if stop < 0 then stop = #t + stop + 1 end
  
  for i = start, stop do
    if t[i] ~= nil then
      table.insert(result, t[i])
    end
  end
  
  return result
end

-- Join array elements into string
function tableUtil.join(t, separator)
  separator = separator or ""
  return table.concat(t, separator)
end

-- Zip multiple arrays
function tableUtil.zip(...)
  local arrays = {...}
  local result = {}
  local maxLen = 0
  
  for _, arr in ipairs(arrays) do
    maxLen = math.max(maxLen, #arr)
  end
  
  for i = 1, maxLen do
    local tuple = {}
    for _, arr in ipairs(arrays) do
      table.insert(tuple, arr[i])
    end
    table.insert(result, tuple)
  end
  
  return result
end

-- Compare two tables for equality
function tableUtil.equals(a, b, deep)
  if type(a) ~= "table" or type(b) ~= "table" then
    return a == b
  end
  
  local checked = {}
  
  for k, v in pairs(a) do
    checked[k] = true
    local bv = b[k]
    
    if deep and type(v) == "table" and type(bv) == "table" then
      if not tableUtil.equals(v, bv, true) then
        return false
      end
    elseif v ~= bv then
      return false
    end
  end
  
  for k in pairs(b) do
    if not checked[k] then
      return false
    end
  end
  
  return true
end

-- Pretty print table
function tableUtil.tostring(t, indent, seen)
  indent = indent or 0
  seen = seen or {}
  
  if type(t) ~= "table" then
    if type(t) == "string" then
      return string.format("%q", t)
    end
    return tostring(t)
  end
  
  if seen[t] then
    return "<circular>"
  end
  seen[t] = true
  
  local parts = {}
  local prefix = string.rep("  ", indent)
  local prefix2 = string.rep("  ", indent + 1)
  
  table.insert(parts, "{\n")
  
  for k, v in pairs(t) do
    local keyStr
    if type(k) == "string" and k:match("^[%a_][%w_]*$") then
      keyStr = k
    else
      keyStr = "[" .. tableUtil.tostring(k, indent + 1, seen) .. "]"
    end
    
    table.insert(parts, prefix2 .. keyStr .. " = " .. 
                 tableUtil.tostring(v, indent + 1, seen) .. ",\n")
  end
  
  table.insert(parts, prefix .. "}")
  
  return table.concat(parts)
end

return tableUtil

-- bin/tr.lua - Translate or delete characters

local shell = require("shell")
local args, opts = shell.parse(...)

local delete   = opts["d"]
local squeeze  = opts["s"]

if #args < 1 or (not delete and #args < 2) then
  io.write("Usage: tr [-ds] <set1> [set2]\n")
  io.write("  -d   Delete characters in set1\n")
  io.write("  -s   Squeeze repeated characters in set2\n")
  os.exit(1)
end

-- Expand ranges like a-z, 0-9
local function expandSet(s)
  local result = {}
  local i = 1
  while i <= #s do
    if i + 2 <= #s and s:sub(i+1, i+1) == "-" then
      local from = s:byte(i)
      local to   = s:byte(i+2)
      if from <= to then
        for c = from, to do
          table.insert(result, string.char(c))
        end
      else
        table.insert(result, s:sub(i, i))
        table.insert(result, "-")
        table.insert(result, s:sub(i+2, i+2))
      end
      i = i + 3
    else
      table.insert(result, s:sub(i, i))
      i = i + 1
    end
  end
  return result
end

local set1 = expandSet(args[1] or "")
local set2 = delete and {} or expandSet(args[2] or "")

-- Build translation map
local transMap = {}
if not delete then
  for i, c in ipairs(set1) do
    local replacement = set2[i] or set2[#set2] or c
    transMap[c] = replacement
  end
end

-- Build delete set
local delSet = {}
if delete then
  for _, c in ipairs(set1) do
    delSet[c] = true
  end
end

for line in io.stdin:lines() do
  local out = {}
  local prev = nil
  for i = 1, #line do
    local c = line:sub(i, i)
    if delete then
      if not delSet[c] then table.insert(out, c) end
    else
      local t = transMap[c] or c
      if squeeze and set2 then
        -- Only squeeze characters in set2
        local inSet2 = false
        for _, sc in ipairs(set2) do
          if sc == t then inSet2 = true; break end
        end
        if inSet2 and t == prev then
          -- skip (squeeze)
        else
          table.insert(out, t)
        end
      else
        table.insert(out, t)
      end
      prev = t
    end
  end
  io.write(table.concat(out) .. "\n")
end

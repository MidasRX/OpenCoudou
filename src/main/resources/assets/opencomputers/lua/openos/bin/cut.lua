-- bin/cut.lua - Cut fields from each line

local shell = require("shell")
local args, opts = shell.parse(...)

-- Options: -d delimiter (default tab), -f fields (e.g. 1,2,3 or 1-3), -c chars
local delim  = opts["d"] or "\t"
local fields = opts["f"]
local chars  = opts["c"]

if not fields and not chars then
  io.write("Usage: cut -f <fields> [-d delim] [file]\n")
  io.write("       cut -c <chars> [file]\n")
  io.write("Examples:\n")
  io.write("  cut -d: -f1,3   /etc/passwd\n")
  io.write("  cut -c1-5       file.txt\n")
  os.exit(1)
end

-- Parse field/char spec: "1", "1,3,5", "1-3", "1-3,5"
local function parseSpec(spec)
  local ranges = {}
  for part in spec:gmatch("[^,]+") do
    local a, b = part:match("^(%d+)-(%d+)$")
    if a then
      table.insert(ranges, {tonumber(a), tonumber(b)})
    else
      local n = tonumber(part:match("^(%d+)$"))
      if n then table.insert(ranges, {n, n}) end
    end
  end
  return ranges
end

local function inRanges(ranges, n)
  for _, r in ipairs(ranges) do
    if n >= r[1] and n <= r[2] then return true end
  end
  return false
end

local function processLine(line)
  if chars then
    local ranges = parseSpec(chars)
    local out = {}
    for i = 1, #line do
      if inRanges(ranges, i) then
        table.insert(out, line:sub(i, i))
      end
    end
    return table.concat(out)
  elseif fields then
    local ranges = parseSpec(fields)
    -- Handle escape sequences in delimiter
    local d = delim:gsub("\\t", "\t"):gsub("\\n", "\n")
    local parts = {}
    local idx = 0
    for part in (line .. d):gmatch("(.-)" .. d:gsub("([%.%+%-%^%$%(%)%[%]{}%*%?%%])", "%%%1")) do
      idx = idx + 1
      if inRanges(ranges, idx) then
        table.insert(parts, part)
      end
    end
    return table.concat(parts, d)
  end
  return line
end

local function processStream(stream)
  for line in stream:lines() do
    io.write(processLine(line) .. "\n")
  end
end

if #args == 0 then
  processStream(io.stdin)
else
  for _, file in ipairs(args) do
    local path = require("shell").resolve(file)
    local f, err = io.open(path, "r")
    if not f then
      io.stderr:write("cut: " .. file .. ": " .. (err or "cannot open") .. "\n")
    else
      processStream(f)
      f:close()
    end
  end
end

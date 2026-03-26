-- bin/uniq.lua - Report or omit repeated lines

local shell = require("shell")
local args, opts = shell.parse(...)

local count    = opts["c"]  -- prefix count
local dupOnly  = opts["d"]  -- only print duplicated lines
local uniqOnly = opts["u"]  -- only print unique lines (not repeated)

local function process(stream)
  local prev  = nil
  local cnt   = 0

  local function flush()
    if prev == nil then return end
    local isDup   = cnt > 1
    local isUniq  = cnt == 1
    if dupOnly  and not isDup   then return end
    if uniqOnly and not isUniq  then return end
    if count then
      io.write(string.format("%7d %s\n", cnt, prev))
    else
      io.write(prev .. "\n")
    end
  end

  for line in stream:lines() do
    if line == prev then
      cnt = cnt + 1
    else
      flush()
      prev = line
      cnt  = 1
    end
  end
  flush()
end

if #args == 0 then
  process(io.stdin)
else
  local path = shell.resolve(args[1])
  local f, err = io.open(path, "r")
  if not f then
    io.stderr:write("uniq: " .. args[1] .. ": " .. (err or "cannot open") .. "\n")
    os.exit(1)
  end
  process(f)
  f:close()
end

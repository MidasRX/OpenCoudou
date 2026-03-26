-- bin/xxd.lua - Hex dump (like xxd)

local shell = require("shell")
local args, opts = shell.parse(...)

local cols   = tonumber(opts["c"]) or 16
local revert = opts["r"]  -- reverse: hex back to binary

local function hexDump(stream, outStream)
  local offset = 0
  while true do
    local chunk = stream:read(cols)
    if not chunk or #chunk == 0 then break end

    -- Offset
    io.write(string.format("%08x: ", offset))

    -- Hex bytes (in groups of 2)
    for i = 1, cols do
      if i <= #chunk then
        io.write(string.format("%02x", chunk:byte(i)))
      else
        io.write("  ")
      end
      if i % 2 == 0 then io.write(" ") end
    end

    io.write(" ")

    -- ASCII
    for i = 1, #chunk do
      local c = chunk:sub(i, i)
      if c:match("[%g ]") then io.write(c) else io.write(".") end
    end
    io.write("\n")

    offset = offset + #chunk
  end
end

local function hexRevert(stream)
  for line in stream:lines() do
    -- Strip offset and trailing ASCII: "00000000: 4865 6c6c ..."
    local hexPart = line:match("^%x+:%s+(.-)%s+|") or line:match("^%x+:%s+(.+)$")
    if hexPart then
      hexPart = hexPart:gsub("%s", "")
      for i = 1, #hexPart, 2 do
        local byte = tonumber(hexPart:sub(i, i+1), 16)
        if byte then io.write(string.char(byte)) end
      end
    end
  end
end

if #args == 0 then
  if revert then hexRevert(io.stdin) else hexDump(io.stdin) end
else
  for _, file in ipairs(args) do
    local path = shell.resolve(file)
    local mode = revert and "r" or "rb"
    local f, err = io.open(path, mode)
    if not f then
      io.stderr:write("xxd: " .. file .. ": " .. (err or "cannot open") .. "\n")
    else
      if revert then hexRevert(f) else hexDump(f) end
      f:close()
    end
  end
end

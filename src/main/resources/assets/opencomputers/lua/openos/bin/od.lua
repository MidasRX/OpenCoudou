-- bin/od.lua - Octal dump / hex dump

local shell = require("shell")
local args, opts = shell.parse(...)

local format = opts["t"] or (opts["x"] and "x") or (opts["c"] and "c") or "o"
local width  = tonumber(opts["w"]) or 16  -- bytes per line

local function processStream(stream)
  local offset = 0
  while true do
    local chunk = stream:read(width)
    if not chunk or #chunk == 0 then break end

    -- Print offset
    io.write(string.format("%08o  ", offset))

    -- Print bytes
    for i = 1, #chunk do
      local b = chunk:byte(i)
      if format == "x" then
        io.write(string.format("%02x ", b))
      elseif format == "c" then
        local c = chunk:sub(i, i)
        if c:match("[%g ]") then
          io.write(string.format("  %s ", c))
        else
          io.write(string.format(" %02x ", b))
        end
      else  -- octal
        io.write(string.format("%03o ", b))
      end
    end

    -- Pad if last chunk
    if #chunk < width then
      local padding = width - #chunk
      local padStr = format == "x" and "   " or (format == "c" and "    " or "    ")
      io.write(string.rep(padStr, padding))
    end

    -- ASCII representation
    io.write(" |")
    for i = 1, #chunk do
      local c = chunk:sub(i, i)
      if c:match("[%g ]") then io.write(c) else io.write(".") end
    end
    io.write("|\n")

    offset = offset + #chunk
  end
  io.write(string.format("%08o\n", offset))
end

if #args == 0 then
  processStream(io.stdin)
else
  for _, file in ipairs(args) do
    local path = shell.resolve(file)
    local f, err = io.open(path, "rb")
    if not f then
      io.stderr:write("od: " .. file .. ": " .. (err or "cannot open") .. "\n")
    else
      processStream(f)
      f:close()
    end
  end
end

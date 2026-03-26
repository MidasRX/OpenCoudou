-- bin/head.lua - Output first N lines of a file

local shell = require("shell")
local args, opts = shell.parse(...)

local n = tonumber(opts["n"] or opts[1]) or 10

local function printHead(stream, filename, showName)
  if showName then io.write("==> " .. filename .. " <==\n") end
  local count = 0
  for line in stream:lines() do
    io.write(line .. "\n")
    count = count + 1
    if count >= n then break end
  end
end

-- Remove numeric option from args if it was positional
local files = {}
for _, a in ipairs(args) do
  if not tonumber(a) then table.insert(files, a) end
end

if #files == 0 then
  printHead(io.stdin, "stdin", false)
else
  for i, file in ipairs(files) do
    local path = shell.resolve(file)
    local f, err = io.open(path, "r")
    if not f then
      io.stderr:write("head: " .. file .. ": " .. (err or "cannot open") .. "\n")
    else
      printHead(f, file, #files > 1)
      f:close()
      if i < #files then io.write("\n") end
    end
  end
end

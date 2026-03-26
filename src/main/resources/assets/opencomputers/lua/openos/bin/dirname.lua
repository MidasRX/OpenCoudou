-- bin/dirname.lua - Strip last component from path

local shell = require("shell")
local args  = shell.parse(...)

if #args == 0 then
  io.write("Usage: dirname <path>\n")
  os.exit(1)
end

local path = args[1]

-- Strip trailing slashes
path = path:gsub("[/\\]+$", "")

-- Get directory part
local dir = path:match("(.*)[/\\][^/\\]+$")
if not dir or dir == "" then
  dir = "."
end

io.write(dir .. "\n")

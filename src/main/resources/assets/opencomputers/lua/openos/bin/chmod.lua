-- bin/chmod.lua - Change file permissions (OC stub)
-- OC filesystem doesn't have UNIX permissions; this is a no-op stub
-- that prints a warning but doesn't fail scripts that use it.

local shell = require("shell")
local args  = shell.parse(...)

if #args < 2 then
  io.write("Usage: chmod <mode> <file...>\n")
  io.write("Note: OpenOS does not support file permissions.\n")
  os.exit(0)
end

-- Just silently succeed (for script compatibility)
os.exit(0)

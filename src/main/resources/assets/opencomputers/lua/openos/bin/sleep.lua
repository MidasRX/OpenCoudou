-- bin/sleep.lua - Pause execution for N seconds

local shell = require("shell")
local args  = shell.parse(...)

if #args == 0 then
  io.write("Usage: sleep <seconds>\n")
  os.exit(1)
end

local seconds = tonumber(args[1])
if not seconds or seconds < 0 then
  io.stderr:write("sleep: invalid time interval '" .. tostring(args[1]) .. "'\n")
  os.exit(1)
end

os.sleep(seconds)

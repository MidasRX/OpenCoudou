-- bin/yes.lua - Output string repeatedly until killed

local shell = require("shell")
local args  = shell.parse(...)

local text = #args > 0 and table.concat(args, " ") or "y"

local ok, event = pcall(require, "event")

while true do
  io.write(text .. "\n")
  if ok then
    local e = event.pull(0, "interrupted")
    if e then break end
  end
end

-- bin/seq.lua - Print a sequence of numbers

local shell = require("shell")
local args, opts = shell.parse(...)

local sep = opts["s"] or "\n"

local first, incr, last

if #args == 1 then
  first, incr, last = 1, 1, tonumber(args[1])
elseif #args == 2 then
  first, incr, last = tonumber(args[1]), 1, tonumber(args[2])
elseif #args == 3 then
  first, incr, last = tonumber(args[1]), tonumber(args[2]), tonumber(args[3])
else
  io.write("Usage: seq [first [incr]] last\n")
  os.exit(1)
end

if not first or not incr or not last then
  io.stderr:write("seq: invalid argument\n")
  os.exit(1)
end

if incr == 0 then
  io.stderr:write("seq: increment must be non-zero\n")
  os.exit(1)
end

local nums = {}
local i = first
while (incr > 0 and i <= last) or (incr < 0 and i >= last) do
  table.insert(nums, tostring(i))
  i = i + incr
end

io.write(table.concat(nums, sep))
if #nums > 0 then io.write("\n") end

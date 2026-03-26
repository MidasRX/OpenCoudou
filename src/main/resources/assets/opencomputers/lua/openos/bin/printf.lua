-- bin/printf.lua - Formatted output (like Unix printf)

local shell = require("shell")
local args  = shell.parse(...)

if #args == 0 then
  io.write("Usage: printf <format> [args...]\n")
  os.exit(1)
end

local fmt = args[1]
-- Escape sequences
fmt = fmt:gsub("\\n", "\n")
         :gsub("\\t", "\t")
         :gsub("\\r", "\r")
         :gsub("\\\\", "\\")
         :gsub("\\\"", "\"")

-- Build format args
local fmtArgs = {}
for i = 2, #args do
  local v = args[i]
  -- Try number
  local n = tonumber(v)
  if n then
    table.insert(fmtArgs, n)
  else
    table.insert(fmtArgs, v)
  end
end

local ok, result = pcall(string.format, fmt, table.unpack(fmtArgs))
if ok then
  io.write(result)
else
  io.stderr:write("printf: " .. result .. "\n")
  os.exit(1)
end

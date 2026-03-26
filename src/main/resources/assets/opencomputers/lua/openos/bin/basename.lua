-- bin/basename.lua - Strip directory and optional suffix from filename

local shell = require("shell")
local args  = shell.parse(...)

if #args == 0 then
  io.write("Usage: basename <path> [suffix]\n")
  os.exit(1)
end

local path   = args[1]
local suffix = args[2]

-- Strip trailing slashes
path = path:gsub("[/\\]+$", "")

-- Get last component
local base = path:match("[^/\\]+$") or path

-- Strip suffix if provided
if suffix and suffix ~= "" then
  -- Escape suffix for pattern matching
  local esc = suffix:gsub("([%.%+%-%^%$%(%)%[%]{}%*%?%%])", "%%%1")
  base = base:gsub(esc .. "$", "")
end

io.write(base .. "\n")

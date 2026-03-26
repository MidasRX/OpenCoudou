-- bin/touch.lua - Create empty file or update timestamp

local shell = require("shell")
local fs    = require("filesystem")
local args  = shell.parse(...)

if #args == 0 then
  io.write("Usage: touch <file...>\n")
  os.exit(1)
end

for _, file in ipairs(args) do
  local path = shell.resolve(file)
  if not fs.exists(path) then
    local f, err = io.open(path, "w")
    if not f then
      io.stderr:write("touch: " .. file .. ": " .. (err or "cannot create") .. "\n")
    else
      f:close()
    end
  end
  -- If it already exists, nothing to do (OC has no utime)
end

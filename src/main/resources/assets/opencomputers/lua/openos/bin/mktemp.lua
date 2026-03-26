-- bin/mktemp.lua - Create a temporary file or directory

local shell = require("shell")
local fs    = require("filesystem")
local args, opts = shell.parse(...)

local isDir    = opts["d"]
local template = args[1] or "tmp.XXXXXX"

-- Replace the last 6 X's with random chars
local function makeTemp(tmpl)
  local chars = "abcdefghijklmnopqrstuvwxyz0123456789"
  local result = tmpl:gsub("XXXXXX$", function()
    local s = ""
    for _ = 1, 6 do
      local idx = math.random(1, #chars)
      s = s .. chars:sub(idx, idx)
    end
    return s
  end)
  return result
end

-- Try up to 10 times to find a non-existing name
local tmpDir = "/tmp"
local path

for _ = 1, 10 do
  local name = makeTemp(template)
  path = tmpDir .. "/" .. name
  if not fs.exists(path) then break end
  path = nil
end

if not path then
  io.stderr:write("mktemp: could not create temp file\n")
  os.exit(1)
end

if isDir then
  local ok, err = fs.makeDirectory(path)
  if not ok then
    io.stderr:write("mktemp: " .. (err or "cannot create directory") .. "\n")
    os.exit(1)
  end
else
  local f, err = io.open(path, "w")
  if not f then
    io.stderr:write("mktemp: " .. (err or "cannot create file") .. "\n")
    os.exit(1)
  end
  f:close()
end

io.write(path .. "\n")

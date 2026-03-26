-- bin/ln.lua - Create symbolic or hard links
-- Note: OC filesystem supports symlinks via filesystem.link()

local shell = require("shell")
local fs    = require("filesystem")
local args, opts = shell.parse(...)

local symbolic = opts["s"]
local force    = opts["f"]

if #args < 2 then
  io.write("Usage: ln [-sf] <target> <linkname>\n")
  io.write("  -s   Create symbolic link\n")
  io.write("  -f   Remove existing destination\n")
  os.exit(1)
end

local target   = args[1]
local linkname = shell.resolve(args[2])

if force and fs.exists(linkname) then
  local ok, err = fs.remove(linkname)
  if not ok then
    io.stderr:write("ln: cannot remove '" .. args[2] .. "': " .. (err or "") .. "\n")
    os.exit(1)
  end
end

if symbolic then
  -- OC filesystem.link creates a symlink
  local ok, err = fs.link(shell.resolve(target), linkname)
  if not ok then
    io.stderr:write("ln: cannot create symlink: " .. (err or "not supported") .. "\n")
    os.exit(1)
  end
else
  io.stderr:write("ln: hard links not supported in OC filesystem\n")
  io.stderr:write("    Use -s for symbolic links\n")
  os.exit(1)
end

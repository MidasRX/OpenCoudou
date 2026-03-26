-- bin/xargs.lua - Build and execute commands from stdin

local shell = require("shell")
local args  = shell.parse(...)

-- Usage: xargs [cmd] [initial-args]
-- Reads arguments from stdin and appends to cmd

if #args == 0 then
  io.write("Usage: xargs <command> [args...]\n")
  io.write("Reads arguments from stdin and appends to command.\n")
  os.exit(1)
end

local cmd = args[1]
local baseArgs = {}
for i = 2, #args do
  table.insert(baseArgs, args[i])
end

-- Collect stdin words
local tokens = {}
for line in io.stdin:lines() do
  for word in line:gmatch("%S+") do
    table.insert(tokens, word)
  end
end

if #tokens == 0 then
  os.exit(0)
end

-- Build full argument list
local allArgs = {}
for _, a in ipairs(baseArgs) do table.insert(allArgs, a) end
for _, t in ipairs(tokens) do table.insert(allArgs, t) end

-- Execute
local fullCmd = cmd
for _, a in ipairs(allArgs) do
  -- Quote args that contain spaces
  if a:find("%s") then
    fullCmd = fullCmd .. ' "' .. a:gsub('"', '\\"') .. '"'
  else
    fullCmd = fullCmd .. " " .. a
  end
end

local ok, err = shell.execute(fullCmd)
if not ok then
  io.stderr:write("xargs: " .. (err or "command failed") .. "\n")
  os.exit(1)
end

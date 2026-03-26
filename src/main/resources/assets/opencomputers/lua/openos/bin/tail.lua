-- bin/tail.lua - Output last N lines of a file

local shell = require("shell")
local args, opts = shell.parse(...)

local n = tonumber(opts["n"]) or 10
local follow = opts["f"]  -- -f: follow (re-read)

local function readAllLines(stream)
  local lines = {}
  for line in stream:lines() do
    table.insert(lines, line)
  end
  return lines
end

local function printTail(lines, filename, showName)
  if showName then io.write("==> " .. filename .. " <==\n") end
  local start = math.max(1, #lines - n + 1)
  for i = start, #lines do
    io.write(lines[i] .. "\n")
  end
end

local files = {}
for _, a in ipairs(args) do
  if not tonumber(a) then table.insert(files, a) end
end

if #files == 0 then
  local lines = readAllLines(io.stdin)
  printTail(lines, "stdin", false)
else
  for i, file in ipairs(files) do
    local path = shell.resolve(file)
    local f, err = io.open(path, "r")
    if not f then
      io.stderr:write("tail: " .. file .. ": " .. (err or "cannot open") .. "\n")
    else
      local lines = readAllLines(f)
      f:close()
      printTail(lines, file, #files > 1)
      if i < #files then io.write("\n") end
    end
  end

  -- Follow mode: keep re-reading last file
  if follow and #files > 0 then
    local path = shell.resolve(files[#files])
    local lastSize = require("filesystem").size(path) or 0
    io.write("(following " .. files[#files] .. ", Ctrl+C to stop)\n")
    local ok, event = pcall(require, "event")
    while true do
      os.sleep(1)
      local curSize = require("filesystem").size(path) or 0
      if curSize > lastSize then
        local f2 = io.open(path, "r")
        if f2 then
          f2:seek("set", lastSize)
          for line in f2:lines() do
            io.write(line .. "\n")
          end
          f2:close()
        end
        lastSize = curSize
      end
      -- Check for interrupt
      if ok then
        local e = event.pull(0, "interrupted")
        if e then break end
      end
    end
  end
end

-- bin/test.lua - Evaluate conditional expressions
-- Also callable as [ condition ]

local shell = require("shell")
local args  = shell.parse(...)

-- Strip leading [ and trailing ] if called as [ ... ]
if args[1] == "[" then table.remove(args, 1) end
if args[#args] == "]" then table.remove(args, #args) end

local fs = require("filesystem")

local function usage()
  io.write("Usage: test <expression>\n")
  io.write("File tests: -e -f -d -r -w -s\n")
  io.write("String tests: -z -n str1 = str2, str1 != str2\n")
  io.write("Number tests: n1 -eq -ne -lt -le -gt -ge n2\n")
  os.exit(2)
end

if #args == 0 then os.exit(1) end  -- false with no args

local function eval()
  -- Unary tests
  if #args == 2 then
    local op, val = args[1], args[2]
    if op == "-e" then return fs.exists(shell.resolve(val)) end
    if op == "-f" then local p = shell.resolve(val); return fs.exists(p) and not fs.isDirectory(p) end
    if op == "-d" then return fs.isDirectory(shell.resolve(val)) end
    if op == "-r" then return fs.exists(shell.resolve(val)) end  -- simplified
    if op == "-w" then return fs.exists(shell.resolve(val)) end  -- simplified
    if op == "-s" then local sz = fs.size(shell.resolve(val)); return sz ~= nil and sz > 0 end
    if op == "-z" then return val == "" end
    if op == "-n" then return val ~= "" end
    return false
  end

  -- Binary tests
  if #args == 3 then
    local a, op, b = args[1], args[2], args[3]
    -- String
    if op == "="  or op == "==" then return a == b end
    if op == "!=" then return a ~= b end
    if op == "<"  then return a < b end
    if op == ">"  then return a > b end
    -- Numeric
    local na, nb = tonumber(a), tonumber(b)
    if op == "-eq" then return na and nb and na == nb or false end
    if op == "-ne" then return na and nb and na ~= nb or false end
    if op == "-lt" then return na and nb and na <  nb or false end
    if op == "-le" then return na and nb and na <= nb or false end
    if op == "-gt" then return na and nb and na >  nb or false end
    if op == "-ge" then return na and nb and na >= nb or false end
    return false
  end

  -- Single string: true if non-empty
  if #args == 1 then return args[1] ~= "" end

  -- Logical: a -a b  / a -o b
  if #args == 5 and args[3] == "-a" then
    -- simplified: just test first and last pairs
    return args[2] ~= "" and args[4] ~= ""
  end

  return false
end

local result = eval()
os.exit(result and 0 or 1)

-- Transforms Library
-- Provides geometric transformations for robots and drones.

local transforms = {}

-- Direction constants (matches Minecraft directions)
transforms.sides = {
  bottom = 0,
  down = 0,
  top = 1,
  up = 1,
  back = 2,
  north = 2,
  front = 3,
  south = 3,
  right = 4,
  west = 4,
  left = 5,
  east = 5,
  
  [0] = "bottom",
  [1] = "top",
  [2] = "back",
  [3] = "front",
  [4] = "right",
  [5] = "left"
}

-- Vector operations
transforms.vec3 = {}
transforms.vec3.__index = transforms.vec3

function transforms.vec3.new(x, y, z)
  return setmetatable({x = x or 0, y = y or 0, z = z or 0}, transforms.vec3)
end

function transforms.vec3:__add(other)
  return transforms.vec3.new(self.x + other.x, self.y + other.y, self.z + other.z)
end

function transforms.vec3:__sub(other)
  return transforms.vec3.new(self.x - other.x, self.y - other.y, self.z - other.z)
end

function transforms.vec3:__mul(scalar)
  if type(scalar) == "number" then
    return transforms.vec3.new(self.x * scalar, self.y * scalar, self.z * scalar)
  elseif type(scalar) == "table" then
    -- Dot product
    return self.x * scalar.x + self.y * scalar.y + self.z * scalar.z
  end
end

function transforms.vec3:__unm()
  return transforms.vec3.new(-self.x, -self.y, -self.z)
end

function transforms.vec3:__eq(other)
  return self.x == other.x and self.y == other.y and self.z == other.z
end

function transforms.vec3:__tostring()
  return string.format("(%d, %d, %d)", self.x, self.y, self.z)
end

function transforms.vec3:length()
  return math.sqrt(self.x^2 + self.y^2 + self.z^2)
end

function transforms.vec3:lengthSquared()
  return self.x^2 + self.y^2 + self.z^2
end

function transforms.vec3:normalize()
  local len = self:length()
  if len > 0 then
    return transforms.vec3.new(self.x / len, self.y / len, self.z / len)
  end
  return transforms.vec3.new(0, 0, 0)
end

function transforms.vec3:cross(other)
  return transforms.vec3.new(
    self.y * other.z - self.z * other.y,
    self.z * other.x - self.x * other.z,
    self.x * other.y - self.y * other.x
  )
end

function transforms.vec3:dot(other)
  return self.x * other.x + self.y * other.y + self.z * other.z
end

function transforms.vec3:floor()
  return transforms.vec3.new(math.floor(self.x), math.floor(self.y), math.floor(self.z))
end

function transforms.vec3:ceil()
  return transforms.vec3.new(math.ceil(self.x), math.ceil(self.y), math.ceil(self.z))
end

function transforms.vec3:round()
  return transforms.vec3.new(
    math.floor(self.x + 0.5),
    math.floor(self.y + 0.5),
    math.floor(self.z + 0.5)
  )
end

function transforms.vec3:abs()
  return transforms.vec3.new(math.abs(self.x), math.abs(self.y), math.abs(self.z))
end

function transforms.vec3:min(other)
  return transforms.vec3.new(
    math.min(self.x, other.x),
    math.min(self.y, other.y),
    math.min(self.z, other.z)
  )
end

function transforms.vec3:max(other)
  return transforms.vec3.new(
    math.max(self.x, other.x),
    math.max(self.y, other.y),
    math.max(self.z, other.z)
  )
end

function transforms.vec3:distance(other)
  return (self - other):length()
end

function transforms.vec3:manhattan(other)
  local d = (self - other):abs()
  return d.x + d.y + d.z
end

function transforms.vec3:unpack()
  return self.x, self.y, self.z
end

-- Direction vectors
transforms.directions = {
  [0] = transforms.vec3.new(0, -1, 0),  -- down
  [1] = transforms.vec3.new(0, 1, 0),   -- up
  [2] = transforms.vec3.new(0, 0, -1),  -- north/back
  [3] = transforms.vec3.new(0, 0, 1),   -- south/front
  [4] = transforms.vec3.new(-1, 0, 0),  -- west/right
  [5] = transforms.vec3.new(1, 0, 0),   -- east/left
}

-- Get direction vector from side
function transforms.sideToDirection(side)
  return transforms.directions[side] or transforms.vec3.new(0, 0, 0)
end

-- Get opposite side
function transforms.opposite(side)
  if side == 0 then return 1
  elseif side == 1 then return 0
  elseif side == 2 then return 3
  elseif side == 3 then return 2
  elseif side == 4 then return 5
  elseif side == 5 then return 4
  end
  return side
end

-- Rotation around Y axis (left/right)
function transforms.rotateY(x, z, times)
  times = times or 1
  times = times % 4
  
  for _ = 1, times do
    x, z = -z, x
  end
  
  return x, z
end

-- Create rotation matrix for Y axis
function transforms.rotationMatrixY(angle)
  local cos = math.cos(angle)
  local sin = math.sin(angle)
  
  return {
    {cos, 0, sin},
    {0, 1, 0},
    {-sin, 0, cos}
  }
end

-- Apply transformation matrix to vector
function transforms.transform(vec, matrix)
  local x = matrix[1][1] * vec.x + matrix[1][2] * vec.y + matrix[1][3] * vec.z
  local y = matrix[2][1] * vec.x + matrix[2][2] * vec.y + matrix[2][3] * vec.z
  local z = matrix[3][1] * vec.x + matrix[3][2] * vec.y + matrix[3][3] * vec.z
  
  return transforms.vec3.new(x, y, z)
end

-- Convert facing direction to heading (0-3, 0=south, 1=west, 2=north, 3=east)
function transforms.facingToHeading(facing)
  if facing == 3 then return 0      -- south
  elseif facing == 4 then return 1  -- west
  elseif facing == 2 then return 2  -- north
  elseif facing == 5 then return 3  -- east
  end
  return 0
end

-- Convert heading to facing direction
function transforms.headingToFacing(heading)
  heading = heading % 4
  if heading == 0 then return 3     -- south
  elseif heading == 1 then return 4 -- west
  elseif heading == 2 then return 2 -- north
  elseif heading == 3 then return 5 -- east
  end
  return 3
end

-- Calculate turn direction (1=right, -1=left)
function transforms.turnToFace(currentHeading, targetHeading)
  local diff = (targetHeading - currentHeading) % 4
  if diff == 0 then
    return 0  -- No turn needed
  elseif diff == 1 then
    return 1  -- Turn right once
  elseif diff == 2 then
    return 2  -- Turn around (either direction)
  else
    return -1 -- Turn left once
  end
end

-- Linear interpolation
function transforms.lerp(a, b, t)
  return a + (b - a) * t
end

-- Clamp value
function transforms.clamp(value, min, max)
  return math.max(min, math.min(max, value))
end

-- Map value from one range to another
function transforms.map(value, inMin, inMax, outMin, outMax)
  return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin)
end

return transforms

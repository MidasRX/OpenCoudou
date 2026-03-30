package li.cil.oc.client.os.apps.games

import li.cil.oc.client.os.apps.*
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.*
import li.cil.oc.client.os.libs.Screen
import li.cil.oc.client.os.libs.Color
import kotlin.math.*
import kotlin.random.Random

/**
 * Shooting game app for SkibidiOS2.
 * Simple space shooter game.
 */
class ShootingApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "shooting",
            name = "Shooting",
            icon = "🎮",
            category = AppCategory.GAMES,
            description = "Space shooter game",
            version = "1.0",
            author = "System"
        ) { ShootingApp(it) }
    }
    
    // Game state
    private var gameRunning = false
    private var score = 0
    private var lives = 3
    private var level = 1
    
    // Player
    private var playerX = 40
    private var playerY = 22
    private val playerWidth = 3
    
    // Bullets
    private data class Bullet(var x: Int, var y: Int, var active: Boolean = true)
    private val bullets = mutableListOf<Bullet>()
    
    // Enemies
    private data class Enemy(var x: Int, var y: Int, var type: Int, var active: Boolean = true)
    private val enemies = mutableListOf<Enemy>()
    
    // Game timing
    private var tickCount = 0
    private var enemySpawnRate = 30
    
    override fun onCreate() {
        createWindow("Space Shooter", 5, 2, 80, 26)
        startGame()
    }
    
    override fun onStart() {}
    override fun onResume() { gameRunning = true }
    override fun onPause() { gameRunning = false }
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        if (!gameRunning || lives <= 0) return
        
        tickCount++
        
        // Update bullets
        bullets.forEach { bullet ->
            if (bullet.active) {
                bullet.y--
                if (bullet.y < 1) bullet.active = false
            }
        }
        bullets.removeAll { !it.active }
        
        // Update enemies
        enemies.forEach { enemy ->
            if (enemy.active) {
                if (tickCount % 3 == 0) enemy.y++
                if (enemy.y > 24) {
                    enemy.active = false
                    lives--
                }
            }
        }
        enemies.removeAll { !it.active }
        
        // Spawn enemies
        if (tickCount % enemySpawnRate == 0) {
            spawnEnemy()
        }
        
        // Check collisions
        checkCollisions()
        
        // Render
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        if (lives <= 0) {
            if (char == ' ' || char == '\n') {
                startGame()
            }
            return
        }
        
        when (char) {
            'a', 'A' -> playerX = maxOf(2, playerX - 2)
            'd', 'D' -> playerX = minOf(76, playerX + 2)
            ' ' -> shoot()
            'p', 'P' -> gameRunning = !gameRunning
        }
        
        when (keyCode) {
            0x105 -> playerX = maxOf(2, playerX - 2) // Left arrow
            0x106 -> playerX = minOf(76, playerX + 2) // Right arrow
        }
    }
    
    private fun startGame() {
        score = 0
        lives = 3
        level = 1
        playerX = 40
        bullets.clear()
        enemies.clear()
        tickCount = 0
        enemySpawnRate = 30
        gameRunning = true
    }
    
    private fun shoot() {
        if (bullets.size < 5) {
            bullets.add(Bullet(playerX, playerY - 1))
        }
    }
    
    private fun spawnEnemy() {
        val x = Random.nextInt(5, 75)
        val type = Random.nextInt(3)
        enemies.add(Enemy(x, 1, type))
    }
    
    private fun checkCollisions() {
        bullets.forEach { bullet ->
            enemies.forEach { enemy ->
                if (bullet.active && enemy.active) {
                    if (abs(bullet.x - enemy.x) < 2 && abs(bullet.y - enemy.y) < 2) {
                        bullet.active = false
                        enemy.active = false
                        score += (enemy.type + 1) * 10
                        
                        // Level up
                        if (score > level * 200) {
                            level++
                            enemySpawnRate = maxOf(10, enemySpawnRate - 5)
                        }
                    }
                }
            }
        }
    }
    
    private fun render() {
        val w = window ?: return
        
        // Clear game area
        Screen.setBackground(0x000020)
        Screen.fill(1, 1, 78, 24, ' ')
        
        // Draw stars background
        Screen.setForeground(0x444444)
        for (i in 0 until 20) {
            val sx = ((tickCount + i * 17) % 78) + 1
            val sy = ((tickCount / 2 + i * 7) % 22) + 1
            Screen.set(sx, sy, ".")
        }
        
        // Draw player
        Screen.setForeground(0x00FF00)
        Screen.set(playerX - 1, playerY, "/█\\")
        Screen.set(playerX, playerY - 1, "▲")
        
        // Draw bullets
        Screen.setForeground(0xFFFF00)
        bullets.forEach { bullet ->
            if (bullet.active) Screen.set(bullet.x, bullet.y, "│")
        }
        
        // Draw enemies
        enemies.forEach { enemy ->
            if (enemy.active) {
                Screen.setForeground(when (enemy.type) {
                    0 -> 0xFF0000
                    1 -> 0xFF00FF
                    else -> 0xFFAA00
                })
                Screen.set(enemy.x, enemy.y, when (enemy.type) {
                    0 -> "◆"
                    1 -> "▼"
                    else -> "●"
                })
            }
        }
        
        // Draw HUD
        Screen.setBackground(0x000000)
        Screen.setForeground(0xFFFFFF)
        Screen.fill(1, 25, 78, 1, ' ')
        Screen.set(2, 25, "Score: $score")
        Screen.set(20, 25, "Lives: ${"❤".repeat(lives)}")
        Screen.set(40, 25, "Level: $level")
        Screen.set(55, 25, if (gameRunning) "P=Pause" else "PAUSED")
        
        // Game over
        if (lives <= 0) {
            Screen.setBackground(0x880000)
            Screen.setForeground(0xFFFFFF)
            Screen.fill(25, 10, 30, 5, ' ')
            Screen.set(32, 11, "GAME OVER")
            Screen.set(29, 12, "Final Score: $score")
            Screen.set(27, 13, "Press SPACE to restart")
        }
    }
}

/**
 * RayWalk - 3D raycasting demo.
 */
class RayWalkApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "raywalk",
            name = "RayWalk",
            icon = "🚶",
            category = AppCategory.GAMES,
            description = "3D raycasting demo",
            version = "1.0",
            author = "System"
        ) { RayWalkApp(it) }
    }
    
    // Player position and direction
    private var posX = 3.0
    private var posY = 3.0
    private var dirX = 1.0
    private var dirY = 0.0
    private var planeX = 0.0
    private var planeY = 0.66
    
    // Map
    private val mapWidth = 16
    private val mapHeight = 16
    private val worldMap = arrayOf(
        "################",
        "#..............#",
        "#..............#",
        "#....####......#",
        "#....#..#......#",
        "#....#..#......#",
        "#....####......#",
        "#..............#",
        "#..............#",
        "#......####....#",
        "#......#..#....#",
        "#......#..#....#",
        "#......####....#",
        "#..............#",
        "#..............#",
        "################"
    )
    
    private val screenWidth = 76
    private val screenHeight = 22
    
    override fun onCreate() {
        createWindow("RayWalk 3D", 2, 1, 80, 26)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        val moveSpeed = 0.2
        val rotSpeed = 0.1
        
        when (char.lowercaseChar()) {
            'w' -> {
                val newX = posX + dirX * moveSpeed
                val newY = posY + dirY * moveSpeed
                if (worldMap[newY.toInt()][newX.toInt()] == '.') {
                    posX = newX
                    posY = newY
                }
            }
            's' -> {
                val newX = posX - dirX * moveSpeed
                val newY = posY - dirY * moveSpeed
                if (worldMap[newY.toInt()][newX.toInt()] == '.') {
                    posX = newX
                    posY = newY
                }
            }
            'a' -> {
                val oldDirX = dirX
                dirX = dirX * cos(rotSpeed) - dirY * sin(rotSpeed)
                dirY = oldDirX * sin(rotSpeed) + dirY * cos(rotSpeed)
                val oldPlaneX = planeX
                planeX = planeX * cos(rotSpeed) - planeY * sin(rotSpeed)
                planeY = oldPlaneX * sin(rotSpeed) + planeY * cos(rotSpeed)
            }
            'd' -> {
                val oldDirX = dirX
                dirX = dirX * cos(-rotSpeed) - dirY * sin(-rotSpeed)
                dirY = oldDirX * sin(-rotSpeed) + dirY * cos(-rotSpeed)
                val oldPlaneX = planeX
                planeX = planeX * cos(-rotSpeed) - planeY * sin(-rotSpeed)
                planeY = oldPlaneX * sin(-rotSpeed) + planeY * cos(-rotSpeed)
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x000000)
        Screen.fill(1, 1, screenWidth, screenHeight, ' ')
        
        // Draw ceiling
        Screen.setBackground(0x444488)
        Screen.fill(1, 1, screenWidth, screenHeight / 2, ' ')
        
        // Draw floor
        Screen.setBackground(0x448844)
        Screen.fill(1, screenHeight / 2 + 1, screenWidth, screenHeight / 2, ' ')
        
        // Raycasting
        for (x in 0 until screenWidth) {
            val cameraX = 2 * x / screenWidth.toDouble() - 1
            val rayDirX = dirX + planeX * cameraX
            val rayDirY = dirY + planeY * cameraX
            
            var mapX = posX.toInt()
            var mapY = posY.toInt()
            
            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)
            
            val stepX: Int
            val stepY: Int
            var sideDistX: Double
            var sideDistY: Double
            
            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (posX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0 - posX) * deltaDistX
            }
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (posY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0 - posY) * deltaDistY
            }
            
            var hit = false
            var side = 0
            
            while (!hit) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }
                if (mapX in 0 until mapWidth && mapY in 0 until mapHeight) {
                    if (worldMap[mapY][mapX] == '#') hit = true
                } else {
                    hit = true
                }
            }
            
            val perpWallDist = if (side == 0) {
                (mapX - posX + (1 - stepX) / 2) / rayDirX
            } else {
                (mapY - posY + (1 - stepY) / 2) / rayDirY
            }
            
            val lineHeight = (screenHeight / perpWallDist).toInt()
            var drawStart = -lineHeight / 2 + screenHeight / 2
            if (drawStart < 0) drawStart = 0
            var drawEnd = lineHeight / 2 + screenHeight / 2
            if (drawEnd >= screenHeight) drawEnd = screenHeight - 1
            
            // Wall color based on distance and side
            val intensity = (255 / (1 + perpWallDist * 0.3)).toInt().coerceIn(50, 255)
            val color = if (side == 0) {
                Color.fromRGB(intensity, intensity / 2, intensity / 4)
            } else {
                Color.fromRGB(intensity * 3 / 4, intensity / 3, intensity / 5)
            }
            
            Screen.setBackground(color)
            for (y in drawStart..drawEnd) {
                Screen.set(x + 1, y + 1, " ")
            }
        }
        
        // Draw minimap
        Screen.setBackground(0x000000)
        Screen.setForeground(0xFFFFFF)
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val mapX = (posX - 4 + x).toInt()
                val mapY = (posY - 4 + y).toInt()
                if (mapX in 0 until mapWidth && mapY in 0 until mapHeight) {
                    val char = if (mapX == posX.toInt() && mapY == posY.toInt()) "@"
                              else if (worldMap[mapY][mapX] == '#') "█"
                              else "·"
                    Screen.set(70 + x, 2 + y, char)
                }
            }
        }
        
        // Controls
        Screen.setBackground(0x000000)
        Screen.setForeground(0xAAAAAA)
        Screen.set(2, 24, "WASD: Move/Turn | ESC: Quit")
    }
}

/**
 * Spinner - Spinning animation demo.
 */
class SpinnerApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "spinner",
            name = "Spinner",
            icon = "🌀",
            category = AppCategory.GAMES,
            description = "Spinning animation demo",
            version = "1.0",
            author = "System"
        ) { SpinnerApp(it) }
    }
    
    private var angle = 0.0
    private var speed = 0.05
    private var size = 8
    private var colorOffset = 0
    
    override fun onCreate() {
        createWindow("Spinner", 20, 5, 40, 20)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        angle += speed
        colorOffset++
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char) {
            '+', '=' -> speed = minOf(0.3, speed + 0.01)
            '-', '_' -> speed = maxOf(0.01, speed - 0.01)
            '[' -> size = maxOf(3, size - 1)
            ']' -> size = minOf(15, size + 1)
        }
    }
    
    private fun render() {
        val w = window ?: return
        val centerX = 18
        val centerY = 8
        
        Screen.setBackground(0x000000)
        Screen.fill(1, 1, 38, 17, ' ')
        
        // Draw spinner
        for (i in 0 until 8) {
            val a = angle + i * PI / 4
            val x = (centerX + cos(a) * size).toInt()
            val y = (centerY + sin(a) * size / 2).toInt()
            
            val hue = ((colorOffset + i * 32) % 256) / 256.0
            val color = Color.fromHSV(hue, 1.0, 1.0)
            
            Screen.setForeground(color)
            Screen.set(x, y, "●")
            
            // Trail
            for (t in 1..3) {
                val ta = a - t * 0.1
                val tx = (centerX + cos(ta) * size).toInt()
                val ty = (centerY + sin(ta) * size / 2).toInt()
                Screen.setForeground(Color.fromHSV(hue, 1.0, 1.0 - t * 0.2))
                Screen.set(tx, ty, "○")
            }
        }
        
        // Info
        Screen.setForeground(0xAAAAAA)
        Screen.set(2, 16, "Speed: %.2f  Size: %d".format(speed, size))
        Screen.set(2, 17, "+/- Speed  [/] Size")
    }
}

/**
 * Christmas Tree animation.
 */
class ChristmasTreeApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "christmas_tree",
            name = "Christmas Tree",
            icon = "🎄",
            category = AppCategory.GAMES,
            description = "Festive Christmas tree animation",
            version = "1.0",
            author = "System"
        ) { ChristmasTreeApp(it) }
    }
    
    private var frame = 0
    private val ornamentColors = listOf(0xFF0000, 0xFFFF00, 0x00FF00, 0x0000FF, 0xFF00FF, 0x00FFFF)
    
    override fun onCreate() {
        createWindow("Christmas Tree", 25, 3, 30, 22)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        frame++
        render()
    }
    
    private fun render() {
        Screen.setBackground(0x000033)
        Screen.fill(1, 1, 28, 20, ' ')
        
        val centerX = 14
        
        // Star
        val starColor = if (frame % 10 < 5) 0xFFFF00 else 0xFFAA00
        Screen.setForeground(starColor)
        Screen.set(centerX, 2, "★")
        
        // Tree layers
        Screen.setForeground(0x00AA00)
        val treeLines = listOf(
            "▲",
            "███",
            "█████",
            "███████",
            "█████████",
            "███████████",
            "█████████████",
            "███████████████",
            "█████████████████"
        )
        
        for ((i, line) in treeLines.withIndex()) {
            val x = centerX - line.length / 2
            Screen.set(x, 3 + i, line)
            
            // Ornaments
            if (i > 0 && i < treeLines.size - 1) {
                val ornamentX1 = x + 1 + (frame + i) % (line.length - 2)
                val ornamentX2 = x + line.length - 2 - (frame + i * 2) % (line.length - 2)
                
                Screen.setForeground(ornamentColors[(frame / 5 + i) % ornamentColors.size])
                Screen.set(ornamentX1, 3 + i, "●")
                Screen.setForeground(ornamentColors[(frame / 5 + i + 3) % ornamentColors.size])
                Screen.set(ornamentX2, 3 + i, "●")
                
                Screen.setForeground(0x00AA00)
            }
        }
        
        // Trunk
        Screen.setForeground(0x8B4513)
        Screen.set(centerX - 1, 12, "███")
        Screen.set(centerX - 1, 13, "███")
        
        // Snow
        Screen.setForeground(0xFFFFFF)
        for (i in 0 until 10) {
            val snowX = (frame / 2 + i * 7) % 28
            val snowY = 14 + (frame / 3 + i * 3) % 6
            Screen.set(snowX + 1, snowY, "*")
        }
        
        // Ground snow
        Screen.setForeground(0xFFFFFF)
        Screen.fill(1, 19, 28, 2, '░')
        
        // Message
        if (frame % 40 < 20) {
            Screen.setForeground(0xFF0000)
            Screen.set(5, 18, "Merry Christmas!")
        } else {
            Screen.setForeground(0x00FF00)
            Screen.set(6, 18, "Happy Holidays!")
        }
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {}
}

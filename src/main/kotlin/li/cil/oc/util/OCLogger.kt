package li.cil.oc.util

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * OpenComputers logging system.
 * Logs to both SLF4J (game log) and a dedicated file in .minecraft/logs/opencomputers.log
 */
object OCLogger {
    private val logger = LoggerFactory.getLogger("OpenComputers")
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    
    private var logFile: File? = null
    private var fileWriter: PrintWriter? = null
    private var logLevel = LogLevel.DEBUG
    
    enum class LogLevel(val priority: Int) {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3)
    }
    
    fun init() {
        try {
            // Create log file in .minecraft/logs/
            val gameDir = System.getProperty("user.dir")
            val logsDir = File(gameDir, "logs")
            if (!logsDir.exists()) logsDir.mkdirs()
            
            logFile = File(logsDir, "opencomputers.log")
            fileWriter = PrintWriter(FileWriter(logFile, true), true)
            
            info("=".repeat(60))
            info("OpenComputers logging initialized")
            info("Log file: ${logFile?.absolutePath}")
            info("=".repeat(60))
        } catch (e: Exception) {
            logger.error("Failed to initialize OC log file", e)
        }
    }
    
    fun setLevel(level: LogLevel) {
        logLevel = level
        info("Log level set to $level")
    }
    
    private fun log(level: LogLevel, message: String, throwable: Throwable? = null) {
        if (level.priority < logLevel.priority) return
        
        val timestamp = LocalDateTime.now().format(timeFormatter)
        val formatted = "[$timestamp] [${level.name}] $message"
        
        // Log to SLF4J (game console)
        when (level) {
            LogLevel.DEBUG -> logger.debug(message)
            LogLevel.INFO -> logger.info(message)
            LogLevel.WARN -> logger.warn(message)
            LogLevel.ERROR -> if (throwable != null) logger.error(message, throwable) else logger.error(message)
        }
        
        // Log to file
        fileWriter?.println(formatted)
        throwable?.let { fileWriter?.println(it.stackTraceToString()) }
    }
    
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String) = log(LogLevel.INFO, message)
    fun warn(message: String) = log(LogLevel.WARN, message)
    fun error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)
    
    // Computer-specific logging
    fun computer(action: String, details: String = "") {
        val msg = "[COMPUTER] $action${if (details.isNotEmpty()) ": $details" else ""}"
        info(msg)
    }
    
    fun component(action: String, componentType: String, address: String = "") {
        val msg = "[COMPONENT] $action - $componentType${if (address.isNotEmpty()) " ($address)" else ""}"
        debug(msg)
    }
    
    fun network(action: String, details: String = "") {
        val msg = "[NETWORK] $action${if (details.isNotEmpty()) ": $details" else ""}"
        debug(msg)
    }
    
    fun boot(stage: String, details: String = "") {
        val msg = "[BOOT] $stage${if (details.isNotEmpty()) ": $details" else ""}"
        info(msg)
    }
    
    fun shutdown() {
        info("OpenComputers logging shutdown")
        fileWriter?.close()
        fileWriter = null
    }
}

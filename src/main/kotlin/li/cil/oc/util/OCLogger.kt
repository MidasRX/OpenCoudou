package li.cil.oc.util

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * OpenComputers logging system.
 *
 * - Creates a new timestamped log file per game session (e.g. opencomputers-2026-04-16_00-03-14.log)
 * - Default level is INFO — DEBUG messages are suppressed unless explicitly enabled
 * - GPU invoke/result spam is handled at the call site (removed from SimpleLuaArchitecture)
 */
object OCLogger {
    private val logger = LoggerFactory.getLogger("OpenComputers")
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

    private var logFile: File? = null
    private var fileWriter: PrintWriter? = null

    // Default to INFO — change to DEBUG if you need verbose output
    private var logLevel = LogLevel.INFO

    enum class LogLevel(val priority: Int) {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3)
    }

    fun init() {
        try {
            // Close any previous session's writer
            fileWriter?.close()
            fileWriter = null

            val gameDir = System.getProperty("user.dir")
            val logsDir = File(gameDir, "logs")
            if (!logsDir.exists()) logsDir.mkdirs()

            // New file per session — never appends to an old file
            val sessionStamp = LocalDateTime.now().format(fileNameFormatter)
            logFile = File(logsDir, "opencomputers-$sessionStamp.log")
            fileWriter = PrintWriter(FileWriter(logFile!!, false), true)

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

        when (level) {
            LogLevel.DEBUG -> logger.debug(message)
            LogLevel.INFO  -> logger.info(message)
            LogLevel.WARN  -> logger.warn(message)
            LogLevel.ERROR -> if (throwable != null) logger.error(message, throwable) else logger.error(message)
        }

        fileWriter?.println(formatted)
        throwable?.let { fileWriter?.println(it.stackTraceToString()) }
    }

    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String)  = log(LogLevel.INFO,  message)
    fun warn(message: String)  = log(LogLevel.WARN,  message)
    fun error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)

    fun computer(action: String, details: String = "") {
        info("[COMPUTER] $action${if (details.isNotEmpty()) ": $details" else ""}")
    }

    fun component(action: String, componentType: String, address: String = "") {
        debug("[COMPONENT] $action - $componentType${if (address.isNotEmpty()) " ($address)" else ""}")
    }

    fun network(action: String, details: String = "") {
        debug("[NETWORK] $action${if (details.isNotEmpty()) ": $details" else ""}")
    }

    fun boot(stage: String, details: String = "") {
        info("[BOOT] $stage${if (details.isNotEmpty()) ": $details" else ""}")
    }

    fun shutdown() {
        info("OpenComputers logging shutdown")
        fileWriter?.close()
        fileWriter = null
    }
}

package li.cil.oc.server.component

import li.cil.oc.util.OCLogger

/**
 * EEPROM component that stores BIOS code.
 * This is what boots the computer.
 */
class EEPROMComponent(
    private val tier: Int = 1,
    private var code: String = DEFAULT_BIOS,
    private var data: ByteArray = ByteArray(256),
    private var label: String = "EEPROM"
) : AbstractComponent("eeprom") {
    
    companion object {
        // Default BIOS that initializes GPU and displays boot message
        val DEFAULT_BIOS = """
            -- OpenComputers BIOS
            computer.beep("Starting BIOS")
            
            local gpuAddr = component.list("gpu")()
            computer.beep("GPU: " .. tostring(gpuAddr))
            
            local gpu = component.proxy(gpuAddr)
            computer.beep("GPU proxy created")
            
            local screen = component.list("screen")()
            computer.beep("Screen: " .. tostring(screen))
            
            if gpu and screen then
                local ok, err = gpu.bind(screen)
                computer.beep("Bind result: " .. tostring(ok) .. " " .. tostring(err))
                
                local w, h = gpu.getResolution()
                computer.beep("Resolution: " .. tostring(w) .. "x" .. tostring(h))
                
                gpu.fill(1, 1, w, h, " ")
                gpu.setForeground(0x00FF00)
                gpu.set(1, 1, "OpenComputers v3.0.0")
                gpu.setForeground(0xFFFFFF)
                gpu.set(1, 2, "")
                gpu.set(1, 3, "No bootable medium found.")
                gpu.set(1, 4, "Press any key to shutdown.")
                computer.beep("Boot message displayed")
            else
                computer.beep("ERROR: No GPU or screen found")
            end
            
            while true do
                local signal = computer.pullSignal(1)
                if signal == "key_down" then
                    computer.shutdown()
                end
            end
        """.trimIndent()
        
        // Max sizes per tier
        val CODE_SIZES = intArrayOf(4096, 4096, 4096) // All tiers same for EEPROM
        val DATA_SIZES = intArrayOf(256, 256, 256)
    }
    
    init {
        registerMethod("get", true, "Get the BIOS code") { _ ->
            arrayOf(code)
        }
        
        registerMethod("set", false, "Set the BIOS code") { args ->
            val newCode = args.getOrNull(0) as? String ?: ""
            val maxSize = CODE_SIZES.getOrElse(tier - 1) { 4096 }
            if (newCode.length > maxSize) {
                throw IllegalArgumentException("Code too large (max $maxSize bytes)")
            }
            code = newCode
            OCLogger.component("EEPROM", "Code updated (${code.length} bytes)")
            arrayOf(true)
        }
        
        registerMethod("getSize", true, "Get max code size") { _ ->
            arrayOf(CODE_SIZES.getOrElse(tier - 1) { 4096 })
        }
        
        registerMethod("getDataSize", true, "Get max data size") { _ ->
            arrayOf(DATA_SIZES.getOrElse(tier - 1) { 256 })
        }
        
        registerMethod("getData", true, "Get the data section") { _ ->
            arrayOf(String(data))
        }
        
        registerMethod("setData", false, "Set the data section") { args ->
            val newData = (args.getOrNull(0) as? String ?: "").toByteArray()
            val maxSize = DATA_SIZES.getOrElse(tier - 1) { 256 }
            if (newData.size > maxSize) {
                throw IllegalArgumentException("Data too large (max $maxSize bytes)")
            }
            data = newData.copyOf(maxSize)
            arrayOf(true)
        }
        
        registerMethod("getLabel", true, "Get the label") { _ ->
            arrayOf(label)
        }
        
        registerMethod("setLabel", false, "Set the label") { args ->
            label = (args.getOrNull(0) as? String ?: "").take(24)
            arrayOf(label)
        }
        
        registerMethod("getChecksum", true, "Get code checksum") { _ ->
            // Simple checksum
            var sum = 0
            code.forEach { sum = (sum * 31 + it.code) and 0xFFFFFFFF.toInt() }
            arrayOf(String.format("%08x", sum))
        }
        
        registerMethod("makeReadonly", false, "Make EEPROM read-only") { args ->
            val checksum = args.getOrNull(0) as? String ?: ""
            // Simplified - would actually lock the EEPROM
            OCLogger.component("EEPROM", "Made read-only with checksum $checksum")
            arrayOf(true)
        }
    }
    
    fun getCode(): String = code
    fun setCode(newCode: String) { code = newCode }
}

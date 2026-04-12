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
        // Default BIOS that initializes GPU and tries to boot from filesystem.
        // Matches original OC boot sequence: init GPU, find boot device, load /init.lua
        val DEFAULT_BIOS = """
            -- OpenComputers BIOS v3.0
            local function tryLoadFrom(address)
              local handle, reason = component.invoke(address, "open", "/init.lua")
              if not handle then return nil, reason end
              local buffer = ""
              repeat
                local data, reason = component.invoke(address, "read", handle, math.huge)
                if not data and reason then
                  return nil, reason
                end
                buffer = buffer .. (data or "")
              until not data
              component.invoke(address, "close", handle)
              return load(buffer, "=init")
            end
            
            local function boot()
              -- Initialize GPU
              local screen = component.list("screen")()
              local gpu = component.list("gpu")()
              if gpu and screen then
                component.invoke(gpu, "bind", screen)
              end
              
              -- Try boot address from EEPROM data first  
              local eeprom = component.list("eeprom")()
              local bootAddr = eeprom and component.invoke(eeprom, "getData")
              if bootAddr and #bootAddr > 0 then
                local init, reason = tryLoadFrom(bootAddr)
                if init then
                  computer.setBootAddress(bootAddr)
                  return init
                end
              end
              
              -- Try all filesystems
              for address in component.list("filesystem") do
                local init, reason = tryLoadFrom(address)
                if init then
                  computer.setBootAddress(address)
                  if eeprom then
                    component.invoke(eeprom, "setData", address)
                  end
                  return init
                end
              end
              
              return nil, "no bootable medium found"
            end
            
            -- Attempt boot
            local init, reason = boot()
            
            if not init then
              -- No bootable medium - show message
              local gpu = component.list("gpu")()
              if gpu then
                local w, h = component.invoke(gpu, "getResolution")
                component.invoke(gpu, "setBackground", 0x000000)
                component.invoke(gpu, "setForeground", 0xFFFFFF)
                component.invoke(gpu, "fill", 1, 1, w, h, " ")
                component.invoke(gpu, "setForeground", 0x00FF00)
                component.invoke(gpu, "set", 1, 1, "OpenComputers v3.0.0")
                component.invoke(gpu, "setForeground", 0xFFFFFF)
                component.invoke(gpu, "set", 1, 3, reason or "Unknown error")
                component.invoke(gpu, "set", 1, 4, "Press any key to shutdown.")
              end
              while true do
                local signal = computer.pullSignal(1)
                if signal == "key_down" then
                  computer.shutdown()
                end
              end
            else
              -- Boot!
              computer.beep(1000, 0.2)
              init()
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
            arrayOf(String(data).trimEnd('\u0000'))
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

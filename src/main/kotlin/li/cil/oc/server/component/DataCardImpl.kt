package li.cil.oc.server.component

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Data card component - provides hashing, encryption, and data utilities.
 * Tier 1: CRC32, deflate/inflate
 * Tier 2: MD5, SHA256, Base64 encode/decode
 * Tier 3: Random, AES encrypt/decrypt
 */
class DataCardComponent(
    val tier: Int = 1
) : AbstractComponent("data") {
    
    private val random = SecureRandom()
    
    init {
        // Tier 1 methods
        registerMethod("crc32", true, "crc32(data:string):string -- Compute CRC32 checksum") { args ->
            val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
            val crc = CRC32()
            crc.update(data)
            arrayOf(crc.value.toString(16))
        }
        
        registerMethod("deflate", true, "deflate(data:string):string -- Compress data using DEFLATE") { args ->
            val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
            val deflater = Deflater()
            deflater.setInput(data)
            deflater.finish()
            
            val output = ByteArray(data.size * 2 + 100)
            val length = deflater.deflate(output)
            deflater.end()
            
            arrayOf(Base64.getEncoder().encodeToString(output.copyOf(length)))
        }
        
        registerMethod("inflate", true, "inflate(data:string):string -- Decompress DEFLATE data") { args ->
            val base64 = args.getOrNull(0)?.toString() ?: ""
            try {
                val data = Base64.getDecoder().decode(base64)
                val inflater = Inflater()
                inflater.setInput(data)
                
                val output = ByteArray(data.size * 10)
                val length = inflater.inflate(output)
                inflater.end()
                
                arrayOf(String(output, 0, length))
            } catch (e: Exception) {
                arrayOf(null, "inflate failed: ${e.message}")
            }
        }
        
        // Tier 2+ methods
        if (tier >= 2) {
            registerMethod("md5", true, "md5(data:string):string -- Compute MD5 hash") { args ->
                val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
                val md = MessageDigest.getInstance("MD5")
                val hash = md.digest(data)
                arrayOf(hash.joinToString("") { "%02x".format(it) })
            }
            
            registerMethod("sha256", true, "sha256(data:string):string -- Compute SHA-256 hash") { args ->
                val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
                val md = MessageDigest.getInstance("SHA-256")
                val hash = md.digest(data)
                arrayOf(hash.joinToString("") { "%02x".format(it) })
            }
            
            registerMethod("encode64", true, "encode64(data:string):string -- Encode to Base64") { args ->
                val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
                arrayOf(Base64.getEncoder().encodeToString(data))
            }
            
            registerMethod("decode64", true, "decode64(data:string):string -- Decode from Base64") { args ->
                val base64 = args.getOrNull(0)?.toString() ?: ""
                try {
                    arrayOf(String(Base64.getDecoder().decode(base64)))
                } catch (e: Exception) {
                    arrayOf(null, "invalid base64")
                }
            }
        }
        
        // Tier 3 methods
        if (tier >= 3) {
            registerMethod("random", true, "random(count:number):string -- Generate random bytes") { args ->
                val count = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, 1024) ?: 16
                val bytes = ByteArray(count)
                random.nextBytes(bytes)
                arrayOf(Base64.getEncoder().encodeToString(bytes))
            }
            
            registerMethod("encrypt", true, "encrypt(data:string, key:string, iv:string):string -- AES encrypt") { args ->
                try {
                    val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
                    val keyStr = args.getOrNull(1)?.toString() ?: ""
                    val ivStr = args.getOrNull(2)?.toString() ?: ""
                    
                    val keyBytes = Base64.getDecoder().decode(keyStr).copyOf(16)
                    val ivBytes = Base64.getDecoder().decode(ivStr).copyOf(16)
                    
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
                    val encrypted = cipher.doFinal(data)
                    
                    arrayOf(Base64.getEncoder().encodeToString(encrypted))
                } catch (e: Exception) {
                    arrayOf(null, "encryption failed: ${e.message}")
                }
            }
            
            registerMethod("decrypt", true, "decrypt(data:string, key:string, iv:string):string -- AES decrypt") { args ->
                try {
                    val base64 = args.getOrNull(0)?.toString() ?: ""
                    val keyStr = args.getOrNull(1)?.toString() ?: ""
                    val ivStr = args.getOrNull(2)?.toString() ?: ""
                    
                    val data = Base64.getDecoder().decode(base64)
                    val keyBytes = Base64.getDecoder().decode(keyStr).copyOf(16)
                    val ivBytes = Base64.getDecoder().decode(ivStr).copyOf(16)
                    
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
                    val decrypted = cipher.doFinal(data)
                    
                    arrayOf(String(decrypted))
                } catch (e: Exception) {
                    arrayOf(null, "decryption failed: ${e.message}")
                }
            }
            
            registerMethod("generateKeyPair", true, "generateKeyPair():table -- Generate AES key and IV") { _ ->
                val key = ByteArray(16)
                val iv = ByteArray(16)
                random.nextBytes(key)
                random.nextBytes(iv)
                
                arrayOf(mapOf(
                    "key" to Base64.getEncoder().encodeToString(key),
                    "iv" to Base64.getEncoder().encodeToString(iv)
                ))
            }
        }
    }
}

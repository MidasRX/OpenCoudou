package li.cil.oc.server.component

import li.cil.oc.util.OCLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Internet card component for HTTP requests and TCP connections.
 * Allows computers to access the internet (with configurable restrictions).
 */
class InternetCardComponent : AbstractComponent("internet") {
    
    companion object {
        // Thread pool for async HTTP requests
        private val executor = Executors.newCachedThreadPool { r ->
            Thread(r, "OC-Internet").also { it.isDaemon = true }
        }
        
        // Maximum response size (1MB)
        const val MAX_RESPONSE_SIZE = 1024 * 1024
        
        // Request timeout
        const val TIMEOUT_MS = 5000
        
        // Allowed URL schemes
        val ALLOWED_SCHEMES = setOf("http", "https")
    }
    
    // Active HTTP handles
    private val httpHandles = ConcurrentHashMap<Int, HttpHandle>()
    private var nextHandle = 1
    
    // TCP connections (simplified - would need more work for full implementation)
    private val tcpConnections = ConcurrentHashMap<Int, TcpConnection>()
    private var nextTcpHandle = 1
    
    init {
        registerMethod("isHttpEnabled", true, "isHttpEnabled():boolean -- Is HTTP enabled") { _ ->
            arrayOf(true) // Could be config-controlled
        }
        
        registerMethod("isTcpEnabled", true, "isTcpEnabled():boolean -- Is TCP enabled") { _ ->
            arrayOf(true) // Could be config-controlled
        }
        
        registerMethod("request", false, "request(url:string[,data:string[,headers:table[,method:string]]]):userdata -- Start HTTP request") { args ->
            val url = args.getOrNull(0)?.toString() 
                ?: return@registerMethod arrayOf(null, "no URL provided")
            val data = args.getOrNull(1)?.toString()
            val headers = (args.getOrNull(2) as? Map<*, *>)?.mapKeys { it.key.toString() }
                ?.mapValues { it.value.toString() } ?: emptyMap()
            val method = args.getOrNull(3)?.toString()?.uppercase() ?: if (data != null) "POST" else "GET"
            
            // Validate URL
            val uri = try {
                URI.create(url)
            } catch (e: Exception) {
                return@registerMethod arrayOf(null, "invalid URL: ${e.message}")
            }
            
            if (uri.scheme?.lowercase() !in ALLOWED_SCHEMES) {
                return@registerMethod arrayOf(null, "unsupported protocol: ${uri.scheme}")
            }
            
            // Create handle
            val handle = nextHandle++
            val httpHandle = HttpHandle(handle, url, method, headers, data)
            httpHandles[handle] = httpHandle
            
            // Start async request
            httpHandle.start()
            
            OCLogger.component("INTERNET", "request", "$method $url -> handle $handle")
            arrayOf(handle)
        }
        
        registerMethod("read", true, "read(handle:number[,count:number]):string -- Read from HTTP response") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(null, "no handle")
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: Int.MAX_VALUE
            
            val httpHandle = httpHandles[handle] 
                ?: return@registerMethod arrayOf(null, "invalid handle")
            
            httpHandle.read(count)
        }
        
        registerMethod("response", true, "response(handle:number):number,string,table -- Get response info") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(null, "no handle")
            
            val httpHandle = httpHandles[handle] 
                ?: return@registerMethod arrayOf(null, "invalid handle")
            
            httpHandle.getResponse()
        }
        
        registerMethod("close", false, "close(handle:number):boolean -- Close HTTP handle") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(false)
            
            val httpHandle = httpHandles.remove(handle)
            httpHandle?.close()
            arrayOf(httpHandle != null)
        }
        
        registerMethod("connect", false, "connect(address:string,port:number):userdata -- Open TCP connection") { args ->
            val address = args.getOrNull(0)?.toString() 
                ?: return@registerMethod arrayOf(null, "no address")
            val port = (args.getOrNull(1) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(null, "no port")
            
            if (port < 1 || port > 65535) {
                return@registerMethod arrayOf(null, "port out of range")
            }
            
            // Create TCP connection (simplified)
            val handle = nextTcpHandle++
            val tcp = TcpConnection(handle, address, port)
            tcpConnections[handle] = tcp
            
            OCLogger.component("INTERNET", "connect", "TCP to $address:$port -> handle $handle")
            arrayOf(handle)
        }
        
        registerMethod("finishConnect", true, "finishConnect(handle:number):boolean -- Check if TCP connected") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(false)
            
            val tcp = tcpConnections[handle] ?: return@registerMethod arrayOf(false)
            arrayOf(tcp.isConnected)
        }
        
        registerMethod("write", false, "write(handle:number,data:string):number -- Write to TCP connection") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(0, "no handle")
            val data = args.getOrNull(1)?.toString() 
                ?: return@registerMethod arrayOf(0, "no data")
            
            val tcp = tcpConnections[handle] ?: return@registerMethod arrayOf(0, "invalid handle")
            val written = tcp.write(data)
            arrayOf(written)
        }
    }
    
    /**
     * HTTP request handle.
     */
    private inner class HttpHandle(
        val id: Int,
        val url: String,
        val method: String,
        val headers: Map<String, String>,
        val postData: String?
    ) {
        var future: CompletableFuture<Void?>? = null
        var responseCode: Int = 0
        var responseMessage: String = ""
        var responseHeaders: Map<String, String> = emptyMap()
        var responseBody: StringBuilder = StringBuilder()
        var error: String? = null
        var finished = false
        var readPosition = 0
        
        fun start() {
            future = CompletableFuture.runAsync({
                try {
                    val uri = URI.create(url)
                    val conn = uri.toURL().openConnection() as HttpURLConnection
                    conn.requestMethod = method
                    conn.connectTimeout = TIMEOUT_MS
                    conn.readTimeout = TIMEOUT_MS
                    conn.instanceFollowRedirects = true
                    
                    // Set headers
                    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                    
                    // Send POST data
                    if (postData != null) {
                        conn.doOutput = true
                        conn.outputStream.use { it.write(postData.toByteArray()) }
                    }
                    
                    // Read response
                    responseCode = conn.responseCode
                    responseMessage = conn.responseMessage ?: ""
                    responseHeaders = conn.headerFields
                        .filterKeys { it != null }
                        .mapValues { it.value.joinToString(", ") }
                    
                    val inputStream = if (responseCode >= 400) conn.errorStream else conn.inputStream
                    inputStream?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).use { reader ->
                            var bytesRead = 0
                            val buffer = CharArray(8192)
                            var len: Int
                            while (reader.read(buffer).also { len = it } != -1) {
                                if (bytesRead + len > MAX_RESPONSE_SIZE) {
                                    responseBody.append(buffer, 0, MAX_RESPONSE_SIZE - bytesRead)
                                    break
                                }
                                responseBody.append(buffer, 0, len)
                                bytesRead += len
                            }
                        }
                    }
                    
                    conn.disconnect()
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                    OCLogger.error("HTTP request failed: $url", e)
                } finally {
                    finished = true
                }
            }, executor)
        }
        
        fun read(count: Int): Array<Any?> {
            if (!finished) {
                return arrayOf("") // Not ready yet
            }
            
            if (error != null) {
                return arrayOf(null, error)
            }
            
            val available = responseBody.length - readPosition
            if (available <= 0) {
                return arrayOf(null) // EOF
            }
            
            val toRead = minOf(count, available)
            val data = responseBody.substring(readPosition, readPosition + toRead)
            readPosition += toRead
            return arrayOf(data)
        }
        
        fun getResponse(): Array<Any?> {
            if (!finished) {
                return arrayOf(null, "not finished")
            }
            
            if (error != null) {
                return arrayOf(null, error)
            }
            
            return arrayOf(responseCode, responseMessage, responseHeaders)
        }
        
        fun close() {
            future?.cancel(true)
        }
    }
    
    /**
     * TCP connection handle (simplified).
     */
    private inner class TcpConnection(
        val id: Int,
        val address: String,
        val port: Int
    ) {
        var isConnected = false
        var error: String? = null
        // Would need actual socket implementation
        
        fun write(data: String): Int {
            if (!isConnected) return 0
            // Would write to socket
            return data.length
        }
        
        fun read(count: Int): String? {
            if (!isConnected) return null
            // Would read from socket
            return null
        }
        
        fun close() {
            isConnected = false
        }
    }
    
    /**
     * Clean up all connections.
     */
    fun dispose() {
        httpHandles.values.forEach { it.close() }
        httpHandles.clear()
        tcpConnections.values.forEach { it.close() }
        tcpConnections.clear()
    }
}

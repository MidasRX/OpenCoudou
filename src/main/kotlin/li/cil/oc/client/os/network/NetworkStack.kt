package li.cil.oc.client.os.network

import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * High-performance network stack for SkibidiOS2.
 * Features:
 * - HTTP/HTTPS requests
 * - WebSocket support
 * - Connection pooling
 * - Async operations with coroutines
 */

data class HttpResponse(
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
    val url: String
) {
    val isSuccess: Boolean get() = statusCode in 200..299
    val isRedirect: Boolean get() = statusCode in 300..399
    val isClientError: Boolean get() = statusCode in 400..499
    val isServerError: Boolean get() = statusCode in 500..599
    
    fun bodyAsString(charset: Charset = Charsets.UTF_8): String = String(body, charset)
    
    fun header(name: String): String? = headers[name.lowercase()]?.firstOrNull()
    
    val contentType: String? get() = header("content-type")
    val contentLength: Int? get() = header("content-length")?.toIntOrNull()
}

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val timeout: Int = 30000,
    val followRedirects: Boolean = true,
    val maxRedirects: Int = 5
)

sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Failure<T>(val error: String, val exception: Exception? = null) : NetworkResult<T>()
    data class InProgress<T>(val progress: Float) : NetworkResult<T>()
}

interface WebSocketListener {
    fun onOpen()
    fun onMessage(text: String)
    fun onBinaryMessage(data: ByteArray)
    fun onClose(code: Int, reason: String)
    fun onError(error: String)
}

class NetworkStack {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeSockets = ConcurrentHashMap<Int, WebSocketConnection>()
    private var nextSocketId = 1
    
    private val userAgent = "SkibidiOS/2.0 (OpenComputers)"
    private val connectionTimeout = 10000
    private val readTimeout = 30000
    
    // ==================== HTTP Operations ====================
    
    fun request(request: HttpRequest, callback: (NetworkResult<HttpResponse>) -> Unit) {
        scope.launch {
            val result = executeRequest(request)
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }
    
    suspend fun requestSync(request: HttpRequest): NetworkResult<HttpResponse> {
        return withContext(Dispatchers.IO) {
            executeRequest(request)
        }
    }
    
    private fun executeRequest(request: HttpRequest, redirectCount: Int = 0): NetworkResult<HttpResponse> {
        try {
            val url = URL(request.url)
            val connection = url.openConnection() as HttpURLConnection
            
            // Configure connection
            connection.requestMethod = request.method
            connection.connectTimeout = connectionTimeout
            connection.readTimeout = request.timeout
            connection.instanceFollowRedirects = false // Handle manually
            
            // Set headers
            connection.setRequestProperty("User-Agent", userAgent)
            request.headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            
            // Send body if present
            if (request.body != null && request.method in listOf("POST", "PUT", "PATCH")) {
                connection.doOutput = true
                connection.outputStream.use { it.write(request.body) }
            }
            
            // Get response
            val statusCode = connection.responseCode
            val statusMessage = connection.responseMessage ?: ""
            
            // Handle redirects
            if (statusCode in 300..399 && request.followRedirects && redirectCount < request.maxRedirects) {
                val location = connection.getHeaderField("Location")
                if (location != null) {
                    val redirectUrl = if (location.startsWith("http")) location 
                                     else URL(url, location).toString()
                    return executeRequest(request.copy(url = redirectUrl), redirectCount + 1)
                }
            }
            
            // Read body
            val body = try {
                (if (statusCode < 400) connection.inputStream else connection.errorStream)
                    ?.use { it.readBytes() } ?: ByteArray(0)
            } catch (e: Exception) {
                ByteArray(0)
            }
            
            // Collect headers
            val headers = connection.headerFields
                .filter { it.key != null }
                .mapKeys { it.key.lowercase() }
            
            connection.disconnect()
            
            return NetworkResult.Success(HttpResponse(
                statusCode = statusCode,
                statusMessage = statusMessage,
                headers = headers,
                body = body,
                url = request.url
            ))
            
        } catch (e: SocketTimeoutException) {
            return NetworkResult.Failure("Connection timed out", e)
        } catch (e: UnknownHostException) {
            return NetworkResult.Failure("Unknown host: ${e.message}", e)
        } catch (e: IOException) {
            return NetworkResult.Failure("Network error: ${e.message}", e)
        } catch (e: Exception) {
            return NetworkResult.Failure("Error: ${e.message}", e)
        }
    }
    
    // Convenience methods
    fun get(url: String, headers: Map<String, String> = emptyMap(), callback: (NetworkResult<HttpResponse>) -> Unit) {
        request(HttpRequest("GET", url, headers), callback)
    }
    
    fun post(url: String, body: String, contentType: String = "application/json", callback: (NetworkResult<HttpResponse>) -> Unit) {
        request(HttpRequest(
            method = "POST",
            url = url,
            headers = mapOf("Content-Type" to contentType),
            body = body.toByteArray()
        ), callback)
    }
    
    suspend fun getSync(url: String, headers: Map<String, String> = emptyMap()): NetworkResult<HttpResponse> {
        return requestSync(HttpRequest("GET", url, headers))
    }
    
    suspend fun postSync(url: String, body: String, contentType: String = "application/json"): NetworkResult<HttpResponse> {
        return requestSync(HttpRequest(
            method = "POST",
            url = url,
            headers = mapOf("Content-Type" to contentType),
            body = body.toByteArray()
        ))
    }
    
    // ==================== WebSocket Operations ====================
    
    fun openWebSocket(url: String, listener: WebSocketListener): Int {
        val id = nextSocketId++
        val connection = WebSocketConnection(id, url, listener)
        activeSockets[id] = connection
        
        scope.launch {
            connection.connect()
        }
        
        return id
    }
    
    fun sendWebSocket(socketId: Int, message: String): Boolean {
        val socket = activeSockets[socketId] ?: return false
        return socket.send(message)
    }
    
    fun sendWebSocketBinary(socketId: Int, data: ByteArray): Boolean {
        val socket = activeSockets[socketId] ?: return false
        return socket.sendBinary(data)
    }
    
    fun closeWebSocket(socketId: Int, code: Int = 1000, reason: String = "Normal closure") {
        activeSockets.remove(socketId)?.close(code, reason)
    }
    
    // ==================== DNS Operations ====================
    
    fun resolve(hostname: String): String? {
        return try {
            InetAddress.getByName(hostname).hostAddress
        } catch (e: Exception) {
            null
        }
    }
    
    fun resolveAll(hostname: String): List<String> {
        return try {
            InetAddress.getAllByName(hostname).map { it.hostAddress }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun reverseLookup(ip: String): String? {
        return try {
            InetAddress.getByName(ip).canonicalHostName
        } catch (e: Exception) {
            null
        }
    }
    
    // ==================== Utility ====================
    
    fun isOnline(): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun parseUrl(url: String): UrlComponents? {
        return try {
            val parsed = URL(url)
            UrlComponents(
                protocol = parsed.protocol,
                host = parsed.host,
                port = if (parsed.port == -1) parsed.defaultPort else parsed.port,
                path = parsed.path.ifEmpty { "/" },
                query = parsed.query,
                fragment = parsed.ref
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun encodeUrl(text: String): String = URLEncoder.encode(text, "UTF-8")
    
    fun decodeUrl(text: String): String = URLDecoder.decode(text, "UTF-8")
    
    // ==================== Cleanup ====================
    
    fun shutdown() {
        activeSockets.values.forEach { it.close(1001, "Shutdown") }
        activeSockets.clear()
        scope.cancel()
    }
}

data class UrlComponents(
    val protocol: String,
    val host: String,
    val port: Int,
    val path: String,
    val query: String?,
    val fragment: String?
) {
    override fun toString(): String {
        val portStr = if (port == 80 || port == 443) "" else ":$port"
        val queryStr = query?.let { "?$it" } ?: ""
        val fragStr = fragment?.let { "#$it" } ?: ""
        return "$protocol://$host$portStr$path$queryStr$fragStr"
    }
}

class WebSocketConnection(
    private val id: Int,
    private val url: String,
    private val listener: WebSocketListener
) {
    private var socket: Socket? = null
    private var output: OutputStream? = null
    private var input: InputStream? = null
    private var connected = false
    private val scope = CoroutineScope(Dispatchers.IO)
    
    suspend fun connect() {
        try {
            val uri = URL(url.replace("ws://", "http://").replace("wss://", "https://"))
            val isSecure = url.startsWith("wss://")
            val port = if (uri.port == -1) (if (isSecure) 443 else 80) else uri.port
            
            socket = if (isSecure) {
                SSLSocketFactory.getDefault().createSocket(uri.host, port)
            } else {
                Socket(uri.host, port)
            }
            
            output = socket!!.getOutputStream()
            input = socket!!.getInputStream()
            
            // Send handshake
            val key = java.util.Base64.getEncoder().encodeToString(ByteArray(16).also { java.util.Random().nextBytes(it) })
            val path = uri.path.ifEmpty { "/" }
            val handshake = """
                GET $path HTTP/1.1
                Host: ${uri.host}
                Upgrade: websocket
                Connection: Upgrade
                Sec-WebSocket-Key: $key
                Sec-WebSocket-Version: 13
                
            """.trimIndent().replace("\n", "\r\n") + "\r\n"
            
            output!!.write(handshake.toByteArray())
            output!!.flush()
            
            // Read response
            val reader = BufferedReader(InputStreamReader(input!!))
            val responseLine = reader.readLine()
            
            if (responseLine?.contains("101") == true) {
                // Skip headers until empty line
                while (reader.readLine()?.isNotEmpty() == true) { }
                
                connected = true
                listener.onOpen()
                
                // Start reading frames
                readFrames()
            } else {
                listener.onError("Handshake failed: $responseLine")
            }
            
        } catch (e: Exception) {
            listener.onError("Connection failed: ${e.message}")
        }
    }
    
    private suspend fun readFrames() {
        try {
            while (connected && socket?.isConnected == true) {
                val byte1 = input!!.read()
                if (byte1 == -1) break
                
                val fin = (byte1 and 0x80) != 0
                val opcode = byte1 and 0x0F
                
                val byte2 = input!!.read()
                val masked = (byte2 and 0x80) != 0
                var length = (byte2 and 0x7F).toLong()
                
                if (length == 126L) {
                    length = ((input!!.read() shl 8) or input!!.read()).toLong()
                } else if (length == 127L) {
                    length = 0
                    for (i in 0 until 8) {
                        length = (length shl 8) or input!!.read().toLong()
                    }
                }
                
                val mask = if (masked) ByteArray(4).also { input!!.read(it) } else null
                val payload = ByteArray(length.toInt())
                input!!.read(payload)
                
                if (mask != null) {
                    for (i in payload.indices) {
                        payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
                    }
                }
                
                when (opcode) {
                    1 -> listener.onMessage(String(payload)) // Text
                    2 -> listener.onBinaryMessage(payload) // Binary
                    8 -> { // Close
                        val code = if (payload.size >= 2) {
                            ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
                        } else 1000
                        val reason = if (payload.size > 2) String(payload.copyOfRange(2, payload.size)) else ""
                        listener.onClose(code, reason)
                        connected = false
                    }
                    9 -> sendPong(payload) // Ping
                    10 -> { } // Pong - ignore
                }
            }
        } catch (e: Exception) {
            if (connected) {
                listener.onError("Read error: ${e.message}")
            }
        }
    }
    
    fun send(message: String): Boolean {
        return try {
            sendFrame(1, message.toByteArray())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun sendBinary(data: ByteArray): Boolean {
        return try {
            sendFrame(2, data)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun sendPong(payload: ByteArray) {
        sendFrame(10, payload)
    }
    
    private fun sendFrame(opcode: Int, payload: ByteArray) {
        val out = output ?: return
        
        // Generate mask
        val mask = ByteArray(4).also { java.util.Random().nextBytes(it) }
        val masked = ByteArray(payload.size)
        for (i in payload.indices) {
            masked[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
        }
        
        synchronized(out) {
            out.write(0x80 or opcode) // FIN + opcode
            
            when {
                payload.size < 126 -> out.write(0x80 or payload.size) // Mask bit + length
                payload.size < 65536 -> {
                    out.write(0x80 or 126)
                    out.write((payload.size shr 8) and 0xFF)
                    out.write(payload.size and 0xFF)
                }
                else -> {
                    out.write(0x80 or 127)
                    for (i in 7 downTo 0) {
                        out.write((payload.size shr (8 * i)) and 0xFF)
                    }
                }
            }
            
            out.write(mask)
            out.write(masked)
            out.flush()
        }
    }
    
    fun close(code: Int, reason: String) {
        connected = false
        try {
            val payload = ByteArray(2 + reason.length)
            payload[0] = ((code shr 8) and 0xFF).toByte()
            payload[1] = (code and 0xFF).toByte()
            reason.toByteArray().copyInto(payload, 2)
            sendFrame(8, payload)
        } catch (e: Exception) { }
        
        try { socket?.close() } catch (e: Exception) { }
        scope.cancel()
    }
}

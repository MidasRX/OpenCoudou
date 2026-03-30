package li.cil.oc.client.os.apps.network

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.apps.Application
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.AppCategory
import li.cil.oc.client.os.libs.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * IRC Client - Connect to IRC networks and chat.
 */

private val IRC_INFO = AppInfo(
    id = "irc",
    name = "IRC Client",
    icon = "💬",
    category = AppCategory.NETWORK,
    description = "Connect to IRC networks and chat"
) { IRCApp(it) }

class IRCApp(os: KotlinOS) : Application(os, IRC_INFO) {
    
    // Connection
    private var server = "irc.esper.net"
    private var port = 6667
    private var nickname = "OCUser${(Math.random() * 1000).toInt()}"
    private var channel = "#opencomputers"
    private var isConnected = false
    private var isConnecting = false
    
    // Chat
    private val messages = mutableListOf<ChatMessage>()
    private val channels = mutableListOf<String>()
    private var currentChannel = ""
    private var inputBuffer = ""
    private var scrollOffset = 0
    
    // UI
    private var showServerList = false
    private var showChannelList = false
    private var showSettings = false
    private var inputFocused = true
    
    // Users
    private val channelUsers = mutableMapOf<String, MutableList<String>>()
    
    data class ChatMessage(
        val time: String,
        val nick: String,
        val message: String,
        val type: MessageType = MessageType.NORMAL,
        val channel: String = ""
    )
    
    enum class MessageType {
        NORMAL, SYSTEM, ACTION, JOIN, PART, NOTICE, ERROR
    }
    
    // Preset servers
    private val serverPresets = listOf(
        "irc.esper.net" to 6667,
        "irc.freenode.net" to 6667,
        "irc.rizon.net" to 6667,
        "irc.quakenet.org" to 6667
    )
    
    override fun onCreate() {
        createWindow("IRC Client", 2, 1, 95, 30)
    }
    
    override fun onStart() {
        addSystemMessage("Welcome to IRC Client!")
        addSystemMessage("Type /connect to connect to $server")
        addSystemMessage("Type /help for commands")
    }
    
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() { disconnect() }
    override fun onDestroy() { disconnect() }
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                when {
                    showServerList -> showServerList = false
                    showChannelList -> showChannelList = false
                    showSettings -> showSettings = false
                    else -> close()
                }
            }
            Keyboard.KEY_ENTER -> {
                if (inputBuffer.isNotEmpty()) {
                    processInput(inputBuffer)
                    inputBuffer = ""
                }
            }
            Keyboard.KEY_BACK -> {
                if (inputBuffer.isNotEmpty()) {
                    inputBuffer = inputBuffer.dropLast(1)
                }
            }
            Keyboard.KEY_UP -> scrollUp()
            Keyboard.KEY_DOWN -> scrollDown()
            Keyboard.KEY_PAGE_UP -> scrollUp(10)
            Keyboard.KEY_PAGE_DOWN -> scrollDown(10)
            Keyboard.KEY_TAB -> {
                // Tab completion for nicks
                if (inputBuffer.isNotEmpty()) {
                    tabComplete()
                }
            }
            else -> {
                if (char.code >= 32) {
                    inputBuffer += char
                }
            }
        }
    }
    
    private fun processInput(input: String) {
        if (input.startsWith("/")) {
            processCommand(input.substring(1))
        } else if (isConnected && currentChannel.isNotEmpty()) {
            sendMessage(currentChannel, input)
        }
    }
    
    private fun processCommand(command: String) {
        val parts = command.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val args = parts.getOrNull(1) ?: ""
        
        when (cmd) {
            "connect" -> {
                if (args.isNotEmpty()) {
                    val serverParts = args.split(":")
                    server = serverParts[0]
                    port = serverParts.getOrNull(1)?.toIntOrNull() ?: 6667
                }
                connect()
            }
            "disconnect", "quit" -> disconnect()
            "join" -> if (args.isNotEmpty()) joinChannel(args)
            "part", "leave" -> if (args.isNotEmpty()) partChannel(args) else if (currentChannel.isNotEmpty()) partChannel(currentChannel)
            "nick" -> if (args.isNotEmpty()) changeNick(args)
            "msg", "privmsg" -> {
                val msgParts = args.split(" ", limit = 2)
                if (msgParts.size >= 2) {
                    sendMessage(msgParts[0], msgParts[1])
                }
            }
            "me" -> if (args.isNotEmpty() && currentChannel.isNotEmpty()) sendAction(currentChannel, args)
            "server" -> showServerList = true
            "channels" -> showChannelList = true
            "settings" -> showSettings = true
            "clear" -> messages.clear()
            "help" -> showHelp()
            else -> addSystemMessage("Unknown command: $cmd")
        }
    }
    
    private fun connect() {
        if (isConnected || isConnecting) return
        
        isConnecting = true
        addSystemMessage("Connecting to $server:$port...")
        
        // Simulate connection
        CoroutineScope(Dispatchers.Default).launch {
            delay(1000)
            isConnected = true
            isConnecting = false
            addSystemMessage("Connected to $server")
            addSystemMessage("Nickname: $nickname")
            
            // Auto-join default channel
            if (channel.isNotEmpty()) {
                joinChannel(channel)
            }
        }
    }
    
    private fun disconnect() {
        if (!isConnected) return
        
        addSystemMessage("Disconnecting...")
        isConnected = false
        channels.clear()
        channelUsers.clear()
        currentChannel = ""
    }
    
    private fun joinChannel(channel: String) {
        val ch = if (channel.startsWith("#")) channel else "#$channel"
        
        if (!isConnected) {
            addSystemMessage("Not connected")
            return
        }
        
        addSystemMessage("Joining $ch...")
        
        // Simulate join
        channels.add(ch)
        currentChannel = ch
        channelUsers[ch] = mutableListOf("@ChanServ", "@Operator", "+Voice", "User1", "User2", nickname)
        
        addMessage(ChatMessage(
            time = currentTime(),
            nick = "*",
            message = "You have joined $ch",
            type = MessageType.JOIN,
            channel = ch
        ))
        
        // Simulate some chat
        addMessage(ChatMessage(currentTime(), "User1", "Hello!", channel = ch))
        addMessage(ChatMessage(currentTime(), "User2", "Welcome $nickname!", channel = ch))
    }
    
    private fun partChannel(channel: String) {
        if (channel in channels) {
            channels.remove(channel)
            channelUsers.remove(channel)
            addSystemMessage("Left $channel")
            
            if (currentChannel == channel) {
                currentChannel = channels.firstOrNull() ?: ""
            }
        }
    }
    
    private fun changeNick(newNick: String) {
        val oldNick = nickname
        nickname = newNick
        addSystemMessage("You are now known as $newNick")
        
        // Update user lists
        for ((_, users) in channelUsers) {
            val idx = users.indexOf(oldNick)
            if (idx >= 0) users[idx] = newNick
        }
    }
    
    private fun sendMessage(target: String, message: String) {
        addMessage(ChatMessage(
            time = currentTime(),
            nick = nickname,
            message = message,
            channel = target
        ))
    }
    
    private fun sendAction(target: String, action: String) {
        addMessage(ChatMessage(
            time = currentTime(),
            nick = nickname,
            message = action,
            type = MessageType.ACTION,
            channel = target
        ))
    }
    
    private fun addSystemMessage(message: String) {
        addMessage(ChatMessage(
            time = currentTime(),
            nick = "*",
            message = message,
            type = MessageType.SYSTEM
        ))
    }
    
    private fun addMessage(message: ChatMessage) {
        messages.add(message)
        // Auto-scroll to bottom
        val w = window ?: return
        val visibleLines = w.height - 6
        if (messages.size > visibleLines) {
            scrollOffset = messages.size - visibleLines
        }
    }
    
    private fun scrollUp(lines: Int = 1) {
        scrollOffset = maxOf(0, scrollOffset - lines)
    }
    
    private fun scrollDown(lines: Int = 1) {
        val w = window ?: return
        val visibleLines = w.height - 6
        scrollOffset = minOf(maxOf(0, messages.size - visibleLines), scrollOffset + lines)
    }
    
    private fun tabComplete() {
        val users = channelUsers[currentChannel] ?: return
        val lastWord = inputBuffer.split(" ").lastOrNull() ?: return
        
        val matches = users.filter { 
            it.trimStart('@', '+', '%').lowercase().startsWith(lastWord.lowercase()) 
        }
        
        if (matches.size == 1) {
            val completion = matches[0].trimStart('@', '+', '%')
            inputBuffer = inputBuffer.dropLast(lastWord.length) + completion + ": "
        }
    }
    
    private fun showHelp() {
        addSystemMessage("--- IRC Commands ---")
        addSystemMessage("/connect [server:port] - Connect to server")
        addSystemMessage("/disconnect - Disconnect from server")
        addSystemMessage("/join #channel - Join a channel")
        addSystemMessage("/part [channel] - Leave a channel")
        addSystemMessage("/nick name - Change nickname")
        addSystemMessage("/msg user message - Send private message")
        addSystemMessage("/me action - Send action")
        addSystemMessage("/clear - Clear messages")
    }
    
    private fun currentTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    private fun render() {
        val w = window ?: return
        
        Screen.setBackground(0x1E1E1E)
        Screen.fill(w.x, w.y, w.width, w.height, ' ')
        
        // Title bar
        Screen.setBackground(0x3C3C3C)
        Screen.fill(w.x, w.y, w.width, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 2, w.y, "💬 IRC - ${if (isConnected) server else "Not Connected"}")
        
        // Channel tabs
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + 1, w.width, 1, ' ')
        
        var tabX = w.x + 2
        for (ch in channels) {
            val isActive = ch == currentChannel
            Screen.setBackground(if (isActive) 0x1E1E1E else 0x2D2D2D)
            Screen.setForeground(if (isActive) 0xFFFFFF else 0x888888)
            Screen.set(tabX, w.y + 1, " $ch ")
            tabX += ch.length + 3
        }
        
        // User list (right side)
        val userListWidth = 20
        val chatWidth = w.width - userListWidth - 1
        
        Screen.setBackground(0x252526)
        Screen.fill(w.x + chatWidth, w.y + 2, userListWidth, w.height - 5, ' ')
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + chatWidth + 1, w.y + 2, "Users")
        
        val users = channelUsers[currentChannel] ?: emptyList()
        for ((i, user) in users.take(w.height - 8).withIndex()) {
            val color = when {
                user.startsWith("@") -> 0xFF5555  // Op
                user.startsWith("+") -> 0x55FF55  // Voice
                user.startsWith("%") -> 0xFFAA00  // Half-op
                else -> 0xAAAAAA
            }
            Screen.setForeground(color)
            Screen.set(w.x + chatWidth + 1, w.y + 4 + i, user.take(userListWidth - 2))
        }
        
        // Chat area
        Screen.setBackground(0x1E1E1E)
        val chatY = w.y + 2
        val chatH = w.height - 5
        
        val channelMessages = messages.filter { it.channel.isEmpty() || it.channel == currentChannel }
        val visibleMessages = channelMessages.drop(scrollOffset).take(chatH)
        
        for ((i, msg) in visibleMessages.withIndex()) {
            val y = chatY + i
            
            // Time
            Screen.setForeground(0x666666)
            Screen.set(w.x + 1, y, "[${msg.time}]")
            
            // Nick
            val nickColor = when (msg.type) {
                MessageType.SYSTEM -> 0x888888
                MessageType.JOIN -> 0x55FF55
                MessageType.PART -> 0xFF5555
                MessageType.ACTION -> 0xFFAA00
                MessageType.NOTICE -> 0x55FFFF
                MessageType.ERROR -> 0xFF0000
                else -> if (msg.nick == nickname) 0x55FF55 else 0x3399FF
            }
            Screen.setForeground(nickColor)
            
            val prefix = when (msg.type) {
                MessageType.ACTION -> "* ${msg.nick}"
                MessageType.SYSTEM, MessageType.JOIN, MessageType.PART -> "***"
                else -> "<${msg.nick}>"
            }
            Screen.set(w.x + 9, y, prefix)
            
            // Message
            Screen.setForeground(when (msg.type) {
                MessageType.ACTION -> 0xFFAA00
                MessageType.SYSTEM, MessageType.JOIN, MessageType.PART -> 0x888888
                else -> 0xCCCCCC
            })
            val msgX = w.x + 9 + prefix.length + 1
            Screen.set(msgX, y, msg.message.take(chatWidth - prefix.length - 12))
        }
        
        // Input
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + w.height - 2, chatWidth, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 1, w.y + w.height - 2, ">" + inputBuffer + "█")
        
        // Status bar
        Screen.setBackground(0x007ACC)
        Screen.fill(w.x, w.y + w.height - 1, w.width, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        val status = if (isConnected) {
            "${channels.size} channels | ${users.size} users in $currentChannel"
        } else {
            "Disconnected - /connect to connect"
        }
        Screen.set(w.x + 2, w.y + w.height - 1, status)
        
        // Overlays
        if (showServerList) renderServerList(w)
    }
    
    private fun renderServerList(w: li.cil.oc.client.os.gui.Window) {
        val listX = w.x + 20
        val listY = w.y + 5
        val listW = 40
        val listH = 12
        
        Screen.setBackground(0x252526)
        Screen.fill(listX, listY, listW, listH, ' ')
        Screen.setForeground(0x3C3C3C)
        Screen.drawBorder(listX, listY, listW, listH)
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(listX + 2, listY, " Server List ")
        
        for ((i, preset) in serverPresets.withIndex()) {
            Screen.setForeground(0xAAAAAA)
            Screen.set(listX + 2, listY + 2 + i, "${i + 1}. ${preset.first}:${preset.second}")
        }
        
        Screen.setForeground(0x888888)
        Screen.set(listX + 2, listY + listH - 2, "Esc to close")
    }
}

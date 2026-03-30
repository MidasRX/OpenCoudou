package li.cil.oc.client.os.browser

import li.cil.oc.client.os.apps.AppCategory
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.Application
import li.cil.oc.client.os.core.FrameBuffer
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.*
import li.cil.oc.client.os.network.HttpResponse
import li.cil.oc.client.os.network.NetworkResult
import kotlinx.coroutines.*

/**
 * Web Browser for SkibidiOS2.
 * Features:
 * - HTTP/HTTPS support
 * - HTML rendering (simplified)
 * - Tab support
 * - Bookmarks
 * - History
 */

private val BROWSER_INFO = AppInfo(
    id = "web_browser",
    name = "Browser",
    icon = "🌐",
    category = AppCategory.NETWORK,
    description = "Browse the internet"
) { os -> WebBrowser(os) }

class WebBrowser(os: KotlinOS) : Application(os, BROWSER_INFO) {
    
    private val tabs = mutableListOf<BrowserTab>()
    private var activeTabIndex = 0
    private val bookmarks = mutableListOf<Bookmark>()
    private val history = mutableListOf<HistoryEntry>()
    
    private lateinit var urlBar: TextField
    private lateinit var contentArea: TextArea
    private lateinit var statusLabel: Label
    private lateinit var tabBar: Label
    
    data class BrowserTab(
        var url: String = "about:blank",
        var title: String = "New Tab",
        var content: List<String> = listOf(""),
        var loading: Boolean = false,
        var scrollOffset: Int = 0
    )
    
    data class Bookmark(val title: String, val url: String)
    data class HistoryEntry(val title: String, val url: String, val timestamp: Long)
    
    override fun onCreate() {
        val w = createWindow("Browser", 2, 1, 100, 32)
        
        // Navigation buttons
        val backBtn = Button(1, 0, "◀").also { it.onClick = { goBack() } }
        val forwardBtn = Button(5, 0, "▶").also { it.onClick = { goForward() } }
        val refreshBtn = Button(9, 0, "🔄").also { it.onClick = { refresh() } }
        val homeBtn = Button(14, 0, "🏠").also { it.onClick = { navigate("about:home") } }
        
        // URL bar
        urlBar = TextField(20, 0, 70, "about:home").also {
            it.onSubmit = { url -> navigate(url) }
        }
        
        // Tab buttons
        val newTabBtn = Button(92, 0, "+").also { it.onClick = { newTab() } }
        
        // Tab bar
        tabBar = Label(1, 1, "", FrameBuffer.TEXT)
        
        // Content area
        contentArea = TextArea(1, 3, 96, 25, mutableListOf("")).also {
            it.editable = false
        }
        
        // Status bar
        statusLabel = Label(1, 29, "Ready")
        
        w.addComponent(backBtn)
        w.addComponent(forwardBtn)
        w.addComponent(refreshBtn)
        w.addComponent(homeBtn)
        w.addComponent(urlBar)
        w.addComponent(newTabBtn)
        w.addComponent(tabBar)
        w.addComponent(contentArea)
        w.addComponent(statusLabel)
        
        // Initialize with one tab
        newTab()
        navigate("about:home")
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        updateTabBar()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when {
            char == 't' && keyCode == 84 -> newTab() // Ctrl+T
            char == 'w' && keyCode == 87 -> closeTab(activeTabIndex) // Ctrl+W
            char == 'l' && keyCode == 76 -> { urlBar.focused = true } // Ctrl+L
            char == 'r' && keyCode == 82 -> refresh() // Ctrl+R
        }
    }
    
    private fun updateTabBar() {
        val maxTabWidth = 15
        tabBar.text = tabs.mapIndexed { index, tab ->
            val title = tab.title.take(maxTabWidth - 3)
            val indicator = if (tab.loading) "⟳" else if (index == activeTabIndex) "●" else "○"
            "$indicator $title"
        }.joinToString(" | ")
    }
    
    fun newTab() {
        tabs.add(BrowserTab())
        activeTabIndex = tabs.size - 1
        urlBar.text = "about:blank"
        contentArea.lines = mutableListOf("")
    }
    
    fun closeTab(index: Int) {
        if (tabs.size <= 1) return
        tabs.removeAt(index)
        if (activeTabIndex >= tabs.size) {
            activeTabIndex = tabs.size - 1
        }
        val tab = tabs[activeTabIndex]
        urlBar.text = tab.url
        contentArea.lines = tab.content.toMutableList()
    }
    
    fun switchTab(index: Int) {
        if (index in tabs.indices) {
            // Save current tab state
            tabs[activeTabIndex].content = contentArea.lines.toList()
            tabs[activeTabIndex].scrollOffset = contentArea.scrollOffset
            
            // Switch
            activeTabIndex = index
            val tab = tabs[activeTabIndex]
            urlBar.text = tab.url
            contentArea.lines = tab.content.toMutableList()
            contentArea.scrollOffset = tab.scrollOffset
        }
    }
    
    fun navigate(url: String) {
        var targetUrl = url.trim()
        
        // Handle special URLs
        when {
            targetUrl == "about:blank" -> {
                setContent(listOf(""))
                return
            }
            targetUrl == "about:home" -> {
                showHomePage()
                return
            }
            targetUrl == "about:bookmarks" -> {
                showBookmarks()
                return
            }
            targetUrl == "about:history" -> {
                showHistory()
                return
            }
            !targetUrl.contains("://") -> {
                // Add protocol if missing
                targetUrl = if (targetUrl.contains(".")) "https://$targetUrl"
                           else "https://www.google.com/search?q=${os.network.encodeUrl(targetUrl)}"
            }
        }
        
        val tab = tabs[activeTabIndex]
        tab.url = targetUrl
        tab.loading = true
        tab.title = "Loading..."
        urlBar.text = targetUrl
        statusLabel.text = "Loading $targetUrl..."
        
        // Fetch the page
        os.network.get(targetUrl) { result ->
            tab.loading = false
            
            when (result) {
                is NetworkResult.Success -> {
                    val response = result.data
                    val html = response.bodyAsString()
                    val rendered = renderHtml(html)
                    
                    tab.title = extractTitle(html)
                    tab.content = rendered
                    
                    if (activeTabIndex == tabs.indexOf(tab)) {
                        contentArea.lines = rendered.toMutableList()
                        contentArea.scrollOffset = 0
                    }
                    
                    // Add to history
                    history.add(HistoryEntry(tab.title, targetUrl, System.currentTimeMillis()))
                    
                    statusLabel.text = "Done"
                }
                is NetworkResult.Failure -> {
                    tab.title = "Error"
                    val errorContent = listOf(
                        "═══════════════════════════════════════",
                        "           ⚠ Error Loading Page",
                        "═══════════════════════════════════════",
                        "",
                        "URL: $targetUrl",
                        "",
                        "Error: ${result.error}",
                        "",
                        "Please check:",
                        "  • The URL is correct",
                        "  • You have internet access",
                        "  • The server is available"
                    )
                    tab.content = errorContent
                    
                    if (activeTabIndex == tabs.indexOf(tab)) {
                        contentArea.lines = errorContent.toMutableList()
                    }
                    
                    statusLabel.text = "Error: ${result.error}"
                }
                else -> {}
            }
        }
    }
    
    private fun renderHtml(html: String): List<String> {
        val lines = mutableListOf<String>()
        val width = 94
        
        // Very simple HTML renderer
        var text = html
            // Remove scripts and styles
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            // Convert common tags
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>|</div>|</li>|</tr>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<h[1-6][^>]*>", RegexOption.IGNORE_CASE), "\n═══ ")
            .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), " ═══\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "  • ")
            .replace(Regex("<hr[^>]*>", RegexOption.IGNORE_CASE), "\n${"─".repeat(width)}\n")
            .replace(Regex("<a[^>]*href=\"([^\"]*)\"[^>]*>([^<]*)</a>", RegexOption.IGNORE_CASE)) { match ->
                "[${match.groupValues[2]}](${match.groupValues[1]})"
            }
            .replace(Regex("<img[^>]*alt=\"([^\"]*)\"[^>]*>", RegexOption.IGNORE_CASE)) { match ->
                "[Image: ${match.groupValues[1]}]"
            }
            .replace(Regex("<img[^>]*>", RegexOption.IGNORE_CASE), "[Image]")
            // Remove remaining tags
            .replace(Regex("<[^>]+>"), "")
            // Decode entities
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: ""
            }
            // Clean up whitespace
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n\\s*\n\\s*\n+"), "\n\n")
            .trim()
        
        // Word wrap
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                lines.add("")
                continue
            }
            
            var line = ""
            for (word in paragraph.split(" ")) {
                if (line.length + word.length + 1 > width) {
                    lines.add(line)
                    line = word
                } else {
                    line = if (line.isEmpty()) word else "$line $word"
                }
            }
            if (line.isNotEmpty()) {
                lines.add(line)
            }
        }
        
        return lines
    }
    
    private fun extractTitle(html: String): String {
        val match = Regex("<title[^>]*>([^<]*)</title>", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.get(1)?.trim()?.take(30) ?: "Untitled"
    }
    
    private fun setContent(lines: List<String>) {
        tabs[activeTabIndex].content = lines
        contentArea.lines = lines.toMutableList()
        contentArea.scrollOffset = 0
    }
    
    private fun showHomePage() {
        val tab = tabs[activeTabIndex]
        tab.url = "about:home"
        tab.title = "Home"
        urlBar.text = "about:home"
        
        val content = listOf(
            "",
            "    ╔═══════════════════════════════════════════════════════════════╗",
            "    ║                                                               ║",
            "    ║           🌐  Welcome to SkibidiOS Browser  🌐               ║",
            "    ║                                                               ║",
            "    ╚═══════════════════════════════════════════════════════════════╝",
            "",
            "    Quick Links:",
            "    ─────────────",
            "    • [Google](https://www.google.com)",
            "    • [Wikipedia](https://www.wikipedia.org)",
            "    • [GitHub](https://www.github.com)",
            "    • [OpenComputers Wiki](https://ocdoc.cil.li)",
            "",
            "    Internal Pages:",
            "    ───────────────",
            "    • about:bookmarks - View your bookmarks",
            "    • about:history - View browsing history",
            "",
            "    Search:",
            "    ────────",
            "    Type in the address bar and press Enter to search Google",
            "",
            "    Keyboard Shortcuts:",
            "    ───────────────────",
            "    Ctrl+T - New tab",
            "    Ctrl+W - Close tab",
            "    Ctrl+L - Focus address bar",
            "    Ctrl+R - Refresh page"
        )
        
        setContent(content)
        statusLabel.text = "Home"
    }
    
    private fun showBookmarks() {
        val tab = tabs[activeTabIndex]
        tab.url = "about:bookmarks"
        tab.title = "Bookmarks"
        urlBar.text = "about:bookmarks"
        
        val content = mutableListOf(
            "",
            "    ╔═══════════════════════════════════════╗",
            "    ║           📚  Bookmarks               ║",
            "    ╚═══════════════════════════════════════╝",
            ""
        )
        
        if (bookmarks.isEmpty()) {
            content.add("    No bookmarks yet.")
            content.add("    Use Ctrl+D to bookmark a page.")
        } else {
            for (bookmark in bookmarks) {
                content.add("    • [${bookmark.title}](${bookmark.url})")
            }
        }
        
        setContent(content)
        statusLabel.text = "Bookmarks"
    }
    
    private fun showHistory() {
        val tab = tabs[activeTabIndex]
        tab.url = "about:history"
        tab.title = "History"
        urlBar.text = "about:history"
        
        val content = mutableListOf(
            "",
            "    ╔═══════════════════════════════════════╗",
            "    ║           📜  History                 ║",
            "    ╚═══════════════════════════════════════╝",
            ""
        )
        
        if (history.isEmpty()) {
            content.add("    No history yet.")
        } else {
            for (entry in history.takeLast(50).reversed()) {
                val time = java.text.SimpleDateFormat("MM/dd HH:mm").format(java.util.Date(entry.timestamp))
                content.add("    $time - [${entry.title}](${entry.url})")
            }
        }
        
        setContent(content)
        statusLabel.text = "History"
    }
    
    private fun goBack() {
        // Simple history navigation would require tracking per-tab history
        statusLabel.text = "Back not implemented yet"
    }
    
    private fun goForward() {
        statusLabel.text = "Forward not implemented yet"
    }
    
    private fun refresh() {
        val currentUrl = tabs[activeTabIndex].url
        if (!currentUrl.startsWith("about:")) {
            navigate(currentUrl)
        }
    }
    
    fun addBookmark(title: String, url: String) {
        bookmarks.add(Bookmark(title, url))
    }
}

/**
 * Simple HTML Document Object Model for more advanced rendering.
 */

sealed class HtmlNode {
    data class Element(
        val tag: String,
        val attributes: Map<String, String> = emptyMap(),
        val children: MutableList<HtmlNode> = mutableListOf()
    ) : HtmlNode()
    
    data class Text(val content: String) : HtmlNode()
}

class HtmlParser {
    fun parse(html: String): HtmlNode.Element {
        val root = HtmlNode.Element("html")
        // Simplified parser - would need full implementation for complex HTML
        parseInto(html, root)
        return root
    }
    
    private fun parseInto(html: String, parent: HtmlNode.Element) {
        // This is a stub - full HTML parsing is complex
        // For now, we use regex-based rendering above
        parent.children.add(HtmlNode.Text(html))
    }
}

package com.example.data.model

enum class SearchEngine(
    val title: String,
    val searchUrl: String,
    val iconDomain: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=", "google.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "duckduckgo.com"),
    BING("Bing", "https://www.bing.com/search?q=", "bing.com"),
    BAIDU("Baidu", "https://www.baidu.com/s?wd=", "baidu.com"),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=", "ecosia.org")
}

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val encryptedDns: EncryptedDnsProvider = EncryptedDnsProvider.SYSTEM,
    val adBlockEnabled: Boolean = true,
    val noImageMode: Boolean = false,
    val desktopMode: Boolean = false,
    val nightMode: Boolean = false,
    val javaScriptEnabled: Boolean = true,
    val clearCacheOnExit: Boolean = false,
    val blockedAdsCount: Int = 0
)

data class DownloadedItem(
    val id: Long,
    val filename: String,
    val url: String,
    val totalBytes: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)

package com.example.data.model

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "",
    val isIncognito: Boolean = false,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val faviconText: String = "",
    val blockedAdsCount: Int = 0
) {
    val isHome: Boolean
        get() = url.isBlank() || url == "about:blank"
}

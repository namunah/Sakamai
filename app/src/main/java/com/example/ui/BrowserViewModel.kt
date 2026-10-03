package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BrowserDatabase
import com.example.data.model.Bookmark
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab
import com.example.data.model.DownloadedItem
import com.example.data.model.EncryptedDnsProvider
import com.example.data.model.HistoryItem
import com.example.data.model.SearchEngine
import com.example.data.model.SpeedDialItem
import com.example.data.repository.BrowserRepository
import com.example.util.DnsOverHttpsResolver
import com.example.util.UrlUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab(id = "tab_1")),
    val activeTabId: String = "tab_1",
    val settings: BrowserSettings = BrowserSettings(),
    val showTabsOverview: Boolean = false,
    val showMenuSheet: Boolean = false,
    val showBookmarksHistoryDialog: Boolean = false,
    val bookmarksHistoryInitialTab: Int = 0, // 0 for Bookmarks, 1 for History
    val showSettingsDialog: Boolean = false,
    val showFindInPage: Boolean = false,
    val findQuery: String = "",
    val findActiveMatch: Int = 0,
    val findTotalMatches: Int = 0,
    val showViewSourceDialog: Boolean = false,
    val pageSourceCode: String = "",
    val pageSourceTitle: String = "",
    val showAddSpeedDialDialog: Boolean = false,
    val showDownloadsDialog: Boolean = false,
    val downloads: List<DownloadedItem> = emptyList(),
    val isCurrentUrlBookmarked: Boolean = false,
    val isTestingDns: Boolean = false,
    val dnsTestResult: String? = null
) {
    val activeTab: BrowserTab
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTab()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    init {
        val db = BrowserDatabase.getInstance(application)
        repository = BrowserRepository(db.browserDao())

        viewModelScope.launch {
            repository.initDefaultSpeedDialsIfEmpty()
        }
    }

    val bookmarks: StateFlow<List<Bookmark>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryItem>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speedDialItems: StateFlow<List<SpeedDialItem>> = repository.speedDialItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Web Navigation Actions
    fun loadInput(input: String) {
        val targetUrl = UrlUtils.formatInputToUrl(input, _uiState.value.settings.searchEngine)
        if (targetUrl.isNotBlank()) {
            loadUrl(targetUrl)
        }
    }

    fun loadUrl(url: String) {
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(url = url, isLoading = true, progress = 10)
                } else tab
            }
            state.copy(tabs = updatedTabs, showTabsOverview = false)
        }
        checkBookmarkStatus(url)
    }

    fun goHome() {
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == activeId) {
                    tab.copy(
                        url = "",
                        title = if (tab.isIncognito) "Private Tab" else "New Tab",
                        isLoading = false,
                        progress = 0,
                        canGoBack = false,
                        canGoForward = false
                    )
                } else tab
            }
            state.copy(tabs = updatedTabs, showTabsOverview = false, showFindInPage = false)
        }
        _uiState.update { it.copy(isCurrentUrlBookmarked = false) }
    }

    fun updateTabProgress(tabId: String, progress: Int) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        progress = progress,
                        isLoading = progress in 1..99
                    )
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun onPageStarted(tabId: String, url: String) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(url = url, isLoading = true, progress = 15)
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
        checkBookmarkStatus(url)
    }

    fun onPageFinished(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean
    ) {
        val finalTitle = title.ifBlank { UrlUtils.extractDomain(url) }
        val faviconChar = UrlUtils.extractFaviconLetter(finalTitle, url)

        val activeTab = _uiState.value.tabs.find { it.id == tabId }
        val isIncognito = activeTab?.isIncognito ?: false

        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(
                        url = url,
                        title = finalTitle,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        isLoading = false,
                        progress = 100,
                        faviconText = faviconChar
                    )
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }

        checkBookmarkStatus(url)

        // Record history only if NOT in incognito mode and valid web url
        if (!isIncognito && url.isNotBlank() && !url.startsWith("about:") && !url.startsWith("data:")) {
            viewModelScope.launch {
                repository.addHistory(finalTitle, url)
            }
        }
    }

    fun incrementBlockedAds(tabId: String) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(blockedAdsCount = tab.blockedAdsCount + 1)
                } else tab
            }
            val totalBlocked = state.settings.blockedAdsCount + 1
            state.copy(
                tabs = updatedTabs,
                settings = state.settings.copy(blockedAdsCount = totalBlocked)
            )
        }
    }

    private fun checkBookmarkStatus(url: String) {
        if (url.isBlank() || url.startsWith("about:")) {
            _uiState.update { it.copy(isCurrentUrlBookmarked = false) }
            return
        }
        viewModelScope.launch {
            repository.isBookmarked(url).collect { bookmarked ->
                _uiState.update { it.copy(isCurrentUrlBookmarked = bookmarked) }
            }
        }
    }

    // Tab Management
    fun addNewTab(isIncognito: Boolean = false, url: String = "") {
        val newId = "tab_" + System.currentTimeMillis()
        val newTab = BrowserTab(
            id = newId,
            title = if (isIncognito) "Private Tab" else "New Tab",
            url = url,
            isIncognito = isIncognito
        )
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabId = newId,
                showTabsOverview = false,
                showFindInPage = false
            )
        }
        if (url.isNotBlank()) {
            checkBookmarkStatus(url)
        } else {
            _uiState.update { it.copy(isCurrentUrlBookmarked = false) }
        }
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            val remaining = state.tabs.filter { it.id != tabId }
            if (remaining.isEmpty()) {
                val fresh = BrowserTab(id = "tab_" + System.currentTimeMillis())
                state.copy(tabs = listOf(fresh), activeTabId = fresh.id)
            } else {
                val newActiveId = if (state.activeTabId == tabId) {
                    val currentIndex = state.tabs.indexOfFirst { it.id == tabId }
                    val nextIndex = (currentIndex - 1).coerceAtLeast(0)
                    remaining.getOrNull(nextIndex)?.id ?: remaining.first().id
                } else {
                    state.activeTabId
                }
                state.copy(tabs = remaining, activeTabId = newActiveId)
            }
        }
        val activeUrl = _uiState.value.activeTab.url
        checkBookmarkStatus(activeUrl)
    }

    fun closeAllTabs() {
        val fresh = BrowserTab(id = "tab_" + System.currentTimeMillis())
        _uiState.update { state ->
            state.copy(
                tabs = listOf(fresh),
                activeTabId = fresh.id,
                showTabsOverview = false,
                showFindInPage = false
            )
        }
    }

    fun selectTab(tabId: String) {
        _uiState.update { state ->
            state.copy(activeTabId = tabId, showTabsOverview = false, showFindInPage = false)
        }
        val activeUrl = _uiState.value.tabs.find { it.id == tabId }?.url ?: ""
        checkBookmarkStatus(activeUrl)
    }

    // Settings Toggles
    fun toggleAdBlock() {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(adBlockEnabled = !state.settings.adBlockEnabled))
        }
    }

    fun toggleNoImageMode() {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(noImageMode = !state.settings.noImageMode))
        }
    }

    fun toggleDesktopMode() {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(desktopMode = !state.settings.desktopMode))
        }
    }

    fun toggleNightMode() {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(nightMode = !state.settings.nightMode))
        }
    }

    fun toggleJavaScript() {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(javaScriptEnabled = !state.settings.javaScriptEnabled))
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(searchEngine = engine))
        }
    }

    fun setEncryptedDns(provider: EncryptedDnsProvider) {
        DnsOverHttpsResolver.clearCache()
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(encryptedDns = provider),
                dnsTestResult = null
            )
        }
    }

    fun testEncryptedDns(domain: String = "wikipedia.org") {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingDns = true, dnsTestResult = null) }
            val currentProvider = _uiState.value.settings.encryptedDns
            val (success, message) = DnsOverHttpsResolver.testProvider(currentProvider, domain)
            _uiState.update {
                it.copy(
                    isTestingDns = false,
                    dnsTestResult = if (success) "✓ $message" else "✗ $message"
                )
            }
        }
    }

    // Bookmarks and History
    fun toggleBookmarkCurrentPage() {
        val activeTab = _uiState.value.activeTab
        if (activeTab.url.isBlank() || activeTab.url.startsWith("about:")) return

        viewModelScope.launch {
            if (_uiState.value.isCurrentUrlBookmarked) {
                repository.removeBookmarkByUrl(activeTab.url)
                _uiState.update { it.copy(isCurrentUrlBookmarked = false) }
            } else {
                repository.addBookmark(activeTab.title, activeTab.url)
                _uiState.update { it.copy(isCurrentUrlBookmarked = true) }
            }
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            val formatted = UrlUtils.formatInputToUrl(url, _uiState.value.settings.searchEngine)
            repository.addBookmark(title.ifBlank { UrlUtils.extractDomain(formatted) }, formatted)
            val currentUrl = _uiState.value.activeTab.url
            checkBookmarkStatus(currentUrl)
        }
    }

    fun removeBookmarkById(id: Long) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
            val currentUrl = _uiState.value.activeTab.url
            checkBookmarkStatus(currentUrl)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    // Speed Dial
    fun addSpeedDial(title: String, url: String) {
        viewModelScope.launch {
            val formatted = UrlUtils.formatInputToUrl(url, _uiState.value.settings.searchEngine)
            repository.addSpeedDial(title.ifBlank { UrlUtils.extractDomain(formatted) }, formatted)
            _uiState.update { it.copy(showAddSpeedDialDialog = false) }
        }
    }

    fun removeSpeedDial(id: Long) {
        viewModelScope.launch {
            repository.removeSpeedDial(id)
        }
    }

    // Find In Page
    fun setFindInPageVisible(visible: Boolean) {
        _uiState.update { it.copy(showFindInPage = visible, findQuery = if (!visible) "" else it.findQuery) }
    }

    fun updateFindQuery(query: String) {
        _uiState.update { it.copy(findQuery = query) }
    }

    fun updateFindMatches(active: Int, total: Int) {
        _uiState.update { it.copy(findActiveMatch = active, findTotalMatches = total) }
    }

    // View Source
    fun showPageSource(title: String, html: String) {
        _uiState.update {
            it.copy(
                showViewSourceDialog = true,
                pageSourceTitle = title,
                pageSourceCode = html
            )
        }
    }

    fun dismissPageSource() {
        _uiState.update { it.copy(showViewSourceDialog = false, pageSourceCode = "") }
    }

    // Downloads
    fun addDownload(filename: String, url: String, totalBytes: Long) {
        val item = DownloadedItem(
            id = System.currentTimeMillis(),
            filename = filename,
            url = url,
            totalBytes = totalBytes
        )
        _uiState.update { it.copy(downloads = listOf(item) + it.downloads) }
    }

    // UI Dialogs
    fun setTabsOverviewVisible(visible: Boolean) = _uiState.update { it.copy(showTabsOverview = visible) }
    fun setMenuSheetVisible(visible: Boolean) = _uiState.update { it.copy(showMenuSheet = visible) }
    fun setBookmarksHistoryVisible(visible: Boolean, initialTab: Int = 0) =
        _uiState.update { it.copy(showBookmarksHistoryDialog = visible, bookmarksHistoryInitialTab = initialTab) }
    fun setSettingsVisible(visible: Boolean) = _uiState.update { it.copy(showSettingsDialog = visible) }
    fun setAddSpeedDialVisible(visible: Boolean) = _uiState.update { it.copy(showAddSpeedDialDialog = visible) }
    fun setDownloadsVisible(visible: Boolean) = _uiState.update { it.copy(showDownloadsDialog = visible) }
}

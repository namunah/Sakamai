package com.example

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BrowserViewModel
import com.example.ui.components.AddSpeedDialDialog
import com.example.ui.components.BookmarksHistoryDialog
import com.example.ui.components.BrowserAddressBar
import com.example.ui.components.BrowserBottomBar
import com.example.ui.components.BrowserWebView
import com.example.ui.components.DownloadsDialog
import com.example.ui.components.FindInPageBar
import com.example.ui.components.HomeSpeedDialView
import com.example.ui.components.MenuBottomSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TabsOverviewSheet
import com.example.ui.components.ViewSourceDialog
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
            val history by viewModel.history.collectAsStateWithLifecycle()
            val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()

            MyApplicationTheme(isIncognito = uiState.activeTab.isIncognito) {
                BrowserAppScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    bookmarks = bookmarks,
                    history = history,
                    speedDialItems = speedDialItems,
                    onActivityFinish = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data?.toString()
        if (!data.isNullOrBlank()) {
            viewModel.addNewTab(url = data)
        }
    }
}

@Composable
fun BrowserAppScreen(
    viewModel: BrowserViewModel,
    uiState: com.example.ui.BrowserUiState,
    bookmarks: List<com.example.data.model.Bookmark>,
    history: List<com.example.data.model.HistoryItem>,
    speedDialItems: List<com.example.data.model.SpeedDialItem>,
    onActivityFinish: () -> Unit
) {
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var requestViewSource by remember { mutableStateOf(false) }

    val activeTab = uiState.activeTab

    // Intelligent Back Button Handling
    BackHandler {
        when {
            uiState.showTabsOverview -> viewModel.setTabsOverviewVisible(false)
            uiState.showMenuSheet -> viewModel.setMenuSheetVisible(false)
            uiState.showBookmarksHistoryDialog -> viewModel.setBookmarksHistoryVisible(false)
            uiState.showSettingsDialog -> viewModel.setSettingsVisible(false)
            uiState.showViewSourceDialog -> viewModel.dismissPageSource()
            uiState.showDownloadsDialog -> viewModel.setDownloadsVisible(false)
            uiState.showAddSpeedDialDialog -> viewModel.setAddSpeedDialVisible(false)
            uiState.showFindInPage -> viewModel.setFindInPageVisible(false)
            activeWebView != null && activeWebView?.canGoBack() == true -> {
                activeWebView?.goBack()
            }
            !activeTab.isHome -> viewModel.goHome()
            uiState.tabs.size > 1 -> viewModel.closeTab(activeTab.id)
            else -> onActivityFinish()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BrowserBottomBar(
                activeTab = activeTab,
                tabCount = uiState.tabs.size,
                onBack = { activeWebView?.goBack() },
                onForward = { activeWebView?.goForward() },
                onHome = { viewModel.goHome() },
                onTabsClick = { viewModel.setTabsOverviewVisible(true) },
                onMenuClick = { viewModel.setMenuSheetVisible(true) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Address / URL bar with status bar insets
                BrowserAddressBar(
                    tab = activeTab,
                    isBookmarked = uiState.isCurrentUrlBookmarked,
                    onLoadUrl = { viewModel.loadInput(it) },
                    onReload = { activeWebView?.reload() },
                    onStop = { activeWebView?.stopLoading() },
                    onToggleBookmark = { viewModel.toggleBookmarkCurrentPage() },
                    modifier = Modifier.statusBarsPadding()
                )

                // Find In Page Toolbar
                AnimatedVisibility(
                    visible = uiState.showFindInPage,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    FindInPageBar(
                        query = uiState.findQuery,
                        activeMatch = uiState.findActiveMatch,
                        totalMatches = uiState.findTotalMatches,
                        onQueryChange = { viewModel.updateFindQuery(it) },
                        onPreviousMatch = { activeWebView?.findNext(false) },
                        onNextMatch = { activeWebView?.findNext(true) },
                        onClose = { viewModel.setFindInPageVisible(false) }
                    )
                }

                // Main Web/Home Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab.isHome) {
                        HomeSpeedDialView(
                            settings = uiState.settings,
                            speedDialItems = speedDialItems,
                            isIncognito = activeTab.isIncognito,
                            onSearch = { viewModel.loadInput(it) },
                            onSelectEngine = { viewModel.setSearchEngine(it) },
                            onSpeedDialClick = { viewModel.loadUrl(it) },
                            onAddSpeedDialClick = { viewModel.setAddSpeedDialVisible(true) },
                            onRemoveSpeedDial = { viewModel.removeSpeedDial(it) }
                        )
                    } else {
                        BrowserWebView(
                            tab = activeTab,
                            browserSettings = uiState.settings,
                            findQuery = uiState.findQuery,
                            onPageStarted = { url ->
                                viewModel.onPageStarted(activeTab.id, url)
                            },
                            onPageFinished = { url, title, canGoBack, canGoForward ->
                                viewModel.onPageFinished(activeTab.id, url, title, canGoBack, canGoForward)
                            },
                            onProgressChanged = { progress ->
                                viewModel.updateTabProgress(activeTab.id, progress)
                            },
                            onBlockedAd = {
                                viewModel.incrementBlockedAds(activeTab.id)
                            },
                            onFindMatchesChanged = { active, total ->
                                viewModel.updateFindMatches(active, total)
                            },
                            onDownloadStarted = { filename, url, totalBytes ->
                                viewModel.addDownload(filename, url, totalBytes)
                            },
                            onViewSourceReady = { title, html ->
                                viewModel.showPageSource(title, html)
                            },
                            requestViewSource = requestViewSource,
                            onViewSourceRequestedHandled = { requestViewSource = false },
                            webViewRef = { webView -> activeWebView = webView }
                        )
                    }
                }
            }

            // Tabs Switcher Sheet
            AnimatedVisibility(
                visible = uiState.showTabsOverview,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TabsOverviewSheet(
                    tabs = uiState.tabs,
                    activeTabId = uiState.activeTabId,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onNewTab = { isIncognito -> viewModel.addNewTab(isIncognito = isIncognito) },
                    onCloseAll = { viewModel.closeAllTabs() },
                    onDismiss = { viewModel.setTabsOverviewVisible(false) }
                )
            }

            // Quick Menu Sheet
            if (uiState.showMenuSheet) {
                MenuBottomSheet(
                    activeTab = activeTab,
                    settings = uiState.settings,
                    isBookmarked = uiState.isCurrentUrlBookmarked,
                    onDismiss = { viewModel.setMenuSheetVisible(false) },
                    onOpenBookmarks = { viewModel.setBookmarksHistoryVisible(true, initialTab = 0) },
                    onOpenHistory = { viewModel.setBookmarksHistoryVisible(true, initialTab = 1) },
                    onOpenDownloads = { viewModel.setDownloadsVisible(true) },
                    onToggleBookmark = { viewModel.toggleBookmarkCurrentPage() },
                    onToggleNightMode = { viewModel.toggleNightMode() },
                    onToggleNoImageMode = { viewModel.toggleNoImageMode() },
                    onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                    onToggleAdBlock = { viewModel.toggleAdBlock() },
                    onFindInPage = { viewModel.setFindInPageVisible(true) },
                    onViewSource = { requestViewSource = true },
                    onOpenSettings = { viewModel.setSettingsVisible(true) }
                )
            }

            // Bookmarks and History Dialog
            if (uiState.showBookmarksHistoryDialog) {
                BookmarksHistoryDialog(
                    initialTab = uiState.bookmarksHistoryInitialTab,
                    bookmarks = bookmarks,
                    history = history,
                    onOpenUrl = { viewModel.loadUrl(it) },
                    onDeleteBookmark = { viewModel.removeBookmarkById(it) },
                    onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                    onClearAllHistory = { viewModel.clearAllHistory() },
                    onAddBookmark = { title, url -> viewModel.addBookmark(title, url) },
                    onDismiss = { viewModel.setBookmarksHistoryVisible(false) }
                )
            }

            // Settings Dialog
            if (uiState.showSettingsDialog) {
                SettingsDialog(
                    settings = uiState.settings,
                    onSelectEngine = { viewModel.setSearchEngine(it) },
                    onSelectEncryptedDns = { viewModel.setEncryptedDns(it) },
                    onTestEncryptedDns = { viewModel.testEncryptedDns() },
                    isTestingDns = uiState.isTestingDns,
                    dnsTestResult = uiState.dnsTestResult,
                    onToggleJavaScript = { viewModel.toggleJavaScript() },
                    onToggleNoImageMode = { viewModel.toggleNoImageMode() },
                    onToggleAdBlock = { viewModel.toggleAdBlock() },
                    onClearAllData = {
                        activeWebView?.clearCache(true)
                        activeWebView?.clearHistory()
                    },
                    onDismiss = { viewModel.setSettingsVisible(false) }
                )
            }

            // View Source Dialog
            if (uiState.showViewSourceDialog) {
                ViewSourceDialog(
                    title = uiState.pageSourceTitle,
                    htmlCode = uiState.pageSourceCode,
                    onDismiss = { viewModel.dismissPageSource() }
                )
            }

            // Downloads Dialog
            if (uiState.showDownloadsDialog) {
                DownloadsDialog(
                    downloads = uiState.downloads,
                    onDismiss = { viewModel.setDownloadsVisible(false) }
                )
            }

            // Add Speed Dial Shortcut Dialog
            if (uiState.showAddSpeedDialDialog) {
                AddSpeedDialDialog(
                    onDismiss = { viewModel.setAddSpeedDialVisible(false) },
                    onAdd = { title, url -> viewModel.addSpeedDial(title, url) }
                )
            }
        }
    }
}

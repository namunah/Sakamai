package com.example.ui.components

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab
import com.example.data.model.EncryptedDnsProvider
import com.example.util.AdBlocker
import com.example.util.DnsOverHttpsResolver

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: BrowserTab,
    browserSettings: BrowserSettings,
    findQuery: String,
    onPageStarted: (String) -> Unit,
    onPageFinished: (url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onBlockedAd: () -> Unit,
    onFindMatchesChanged: (active: Int, total: Int) -> Unit,
    onDownloadStarted: (filename: String, url: String, totalBytes: Long) -> Unit,
    onViewSourceReady: (title: String, html: String) -> Unit,
    requestViewSource: Boolean,
    onViewSourceRequestedHandled: () -> Unit,
    webViewRef: (WebView?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember(tab.id) {
        WebView(context).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // Apply reactive settings
    LaunchedEffect(browserSettings.noImageMode, webView) {
        webView.settings.loadsImagesAutomatically = !browserSettings.noImageMode
        webView.settings.blockNetworkImage = browserSettings.noImageMode
    }

    LaunchedEffect(browserSettings.javaScriptEnabled, webView) {
        webView.settings.javaScriptEnabled = browserSettings.javaScriptEnabled
    }

    LaunchedEffect(browserSettings.desktopMode, webView) {
        if (browserSettings.desktopMode) {
            webView.settings.userAgentString = DESKTOP_USER_AGENT
        } else {
            webView.settings.userAgentString = null // Reset to default
        }
        if (tab.url.isNotBlank() && !tab.isHome) {
            webView.reload()
        }
    }

    LaunchedEffect(browserSettings.nightMode, webView) {
        if (browserSettings.nightMode) {
            webView.evaluateJavascript(AdBlocker.NIGHT_MODE_JS, null)
        } else {
            webView.evaluateJavascript(AdBlocker.REMOVE_NIGHT_MODE_JS, null)
        }
    }

    // Handle Find in Page
    LaunchedEffect(findQuery, webView) {
        if (findQuery.isNotBlank()) {
            webView.setFindListener { activeIndex, numberOfMatches, _ ->
                onFindMatchesChanged(if (numberOfMatches > 0) activeIndex + 1 else 0, numberOfMatches)
            }
            webView.findAllAsync(findQuery)
        } else {
            webView.clearMatches()
            onFindMatchesChanged(0, 0)
        }
    }

    // Handle View Source request
    LaunchedEffect(requestViewSource) {
        if (requestViewSource) {
            webView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();"
            ) { htmlJson ->
                val cleanHtml = if (htmlJson != null && htmlJson.startsWith("\"") && htmlJson.endsWith("\"")) {
                    // Unescape JSON string
                    htmlJson.substring(1, htmlJson.length - 1)
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                } else {
                    htmlJson ?: "<!-- No source available -->"
                }
                onViewSourceReady(tab.title.ifBlank { "Source Code" }, cleanHtml)
                onViewSourceRequestedHandled()
            }
        }
    }

    // Load URL when it changes
    LaunchedEffect(tab.url) {
        if (tab.url.isNotBlank() && tab.url != webView.url && !tab.isHome) {
            webView.loadUrl(tab.url)
        }
    }

    // Setup clients
    LaunchedEffect(webView, browserSettings.adBlockEnabled, browserSettings.encryptedDns, tab.id) {
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            try {
                val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimetype)
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("Downloading $filename...")
                    setTitle(filename)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                }
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(context, "Download started: $filename", Toast.LENGTH_SHORT).show()
                onDownloadStarted(filename, url, contentLength)
            } catch (e: Exception) {
                Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false

                if (scheme == "http" || scheme == "https") {
                    return false
                }

                // Handle external apps (tel:, mailto:, sms:, market:, etc.)
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                    true
                } catch (_: Exception) {
                    true
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val requestUrl = request?.url?.toString() ?: return null

                // 1. Block ads and trackers
                if (browserSettings.adBlockEnabled && AdBlocker.isAd(requestUrl)) {
                    view?.post { onBlockedAd() }
                    return AdBlocker.createEmptyResource()
                }

                // 2. Route via Encrypted DNS (DoH) resolver if enabled
                if (browserSettings.encryptedDns != EncryptedDnsProvider.SYSTEM &&
                    request.method.equals("GET", ignoreCase = true) &&
                    (requestUrl.startsWith("http://") || requestUrl.startsWith("https://"))
                ) {
                    try {
                        val client = DnsOverHttpsResolver.getClient(browserSettings.encryptedDns)
                        val okBuilder = okhttp3.Request.Builder().url(requestUrl)

                        // Forward request headers
                        request.requestHeaders?.forEach { (headerName, headerValue) ->
                            if (!headerName.equals("accept-encoding", ignoreCase = true)) {
                                okBuilder.addHeader(headerName, headerValue)
                            }
                        }

                        // Attach relevant cookies
                        val cookie = CookieManager.getInstance().getCookie(requestUrl)
                        if (!cookie.isNullOrBlank()) {
                            okBuilder.addHeader("Cookie", cookie)
                        }

                        val response = client.newCall(okBuilder.build()).execute()
                        if (response.isSuccessful && response.body != null) {
                            val setCookieHeaders = response.headers("Set-Cookie")
                            if (setCookieHeaders.isNotEmpty()) {
                                setCookieHeaders.forEach { sc ->
                                    CookieManager.getInstance().setCookie(requestUrl, sc)
                                }
                            }

                            val contentType = response.header("content-type") ?: "text/html"
                            val mimeType = contentType.substringBefore(";").trim()
                            val encoding = if (contentType.contains("charset=")) {
                                contentType.substringAfter("charset=").substringBefore(";").trim()
                            } else {
                                "utf-8"
                            }

                            val responseHeaders = mutableMapOf<String, String>()
                            for (i in 0 until response.headers.size) {
                                responseHeaders[response.headers.name(i)] = response.headers.value(i)
                            }

                            return WebResourceResponse(
                                mimeType,
                                encoding,
                                response.code,
                                response.message.ifBlank { "OK" },
                                responseHeaders,
                                response.body!!.byteStream()
                            )
                        }
                    } catch (_: Exception) {
                        // Gracefully fall back to standard webview request loading
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onPageStarted(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val finalUrl = url ?: ""

                // Inject cosmetic ad blocking
                if (browserSettings.adBlockEnabled) {
                    view?.evaluateJavascript(AdBlocker.COSMETIC_AD_BLOCK_JS, null)
                }

                // Inject night mode
                if (browserSettings.nightMode) {
                    view?.evaluateJavascript(AdBlocker.NIGHT_MODE_JS, null)
                }

                onPageFinished(
                    finalUrl,
                    view?.title ?: "",
                    view?.canGoBack() ?: false,
                    view?.canGoForward() ?: false
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                val url = view?.url ?: ""
                onPageFinished(
                    url,
                    title ?: "",
                    view?.canGoBack() ?: false,
                    view?.canGoForward() ?: false
                )
            }
        }
    }

    DisposableEffect(webView) {
        webViewRef(webView)
        onDispose {
            webViewRef(null)
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}

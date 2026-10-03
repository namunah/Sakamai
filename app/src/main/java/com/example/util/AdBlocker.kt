package com.example.util

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {

    private val AD_HOSTS = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "adnxs.com",
        "adroll.com",
        "adsystem.com",
        "amazon-adsystem.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "popads.net",
        "propellerads.com",
        "scorecardresearch.com",
        "quantserve.com",
        "advertising.com",
        "bidswitch.net",
        "rubiconproject.com",
        "casalemedia.com",
        "moatads.com",
        "smartadserver.com",
        "adsafeprotected.com",
        "openx.net",
        "pubmatic.com",
        "yieldmo.com"
    )

    fun isAd(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val host = try {
            Uri.parse(url).host?.lowercase() ?: return false
        } catch (_: Exception) {
            return false
        }

        for (adHost in AD_HOSTS) {
            if (host == adHost || host.endsWith(".$adHost")) {
                return true
            }
        }

        // Generic ad script/path heuristics
        val path = url.lowercase()
        if (path.contains("/pagead/") ||
            path.contains("/ads.js") ||
            path.contains("/ad_server/") ||
            path.contains("advertisement") ||
            path.contains("doubleclick")
        ) {
            return true
        }

        return false
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    const val COSMETIC_AD_BLOCK_JS = """
        (function() {
            var css = '.adsbygoogle, [id*="google_ads"], [class*="sponsored"], [id*="banner-ad"], [class*="ad-banner"], [class*="advertisement"], .taboola, .outbrain, #carbonads, div[class*="-ad-"], div[id*="-ad-"] { display: none !important; height: 0 !important; visibility: hidden !important; }';
            var head = document.head || document.getElementsByTagName('head')[0];
            if (head) {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                head.appendChild(style);
            }
        })();
    """

    const val NIGHT_MODE_JS = """
        (function() {
            var id = 'via_night_mode_style';
            var existing = document.getElementById(id);
            if (!existing) {
                var style = document.createElement('style');
                style.id = id;
                style.type = 'text/css';
                style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #121212 !important; } img, video, canvas, iframe, svg { filter: invert(100%) hue-rotate(180deg) !important; }';
                document.documentElement.appendChild(style);
            }
        })();
    """

    const val REMOVE_NIGHT_MODE_JS = """
        (function() {
            var existing = document.getElementById('via_night_mode_style');
            if (existing && existing.parentNode) {
                existing.parentNode.removeChild(existing);
            }
        })();
    """
}

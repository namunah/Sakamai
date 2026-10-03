package com.example.util

import android.net.Uri
import com.example.data.model.SearchEngine
import java.net.URLEncoder

object UrlUtils {

    fun formatInputToUrl(input: String, searchEngine: SearchEngine): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)
        ) {
            return trimmed
        }

        // Check if it's a domain name (contains dot and no spaces, e.g. "google.com" or "localhost:3000")
        val isDomain = !trimmed.contains(" ") && (
            trimmed.contains(".") ||
            trimmed.startsWith("localhost", ignoreCase = true) ||
            trimmed.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(:\d+)?$"""))
        )

        return if (isDomain) {
            "https://$trimmed"
        } else {
            val encodedQuery = try {
                URLEncoder.encode(trimmed, "UTF-8")
            } catch (_: Exception) {
                trimmed
            }
            "${searchEngine.searchUrl}$encodedQuery"
        }
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host ?: return url
            if (host.startsWith("www.")) host.substring(4) else host
        } catch (_: Exception) {
            url
        }
    }

    fun extractFaviconLetter(title: String, url: String): String {
        val domain = extractDomain(url)
        val text = domain.ifBlank { title }
        return text.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "W"
    }
}

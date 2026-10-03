package com.example.util

import com.example.data.model.EncryptedDnsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DnsOverHttpsResolver {

    private data class CachedDns(val addresses: List<InetAddress>, val expiresAt: Long)

    private val cache = ConcurrentHashMap<String, CachedDns>()

    // Lightweight bootstrap OkHttpClient using default DNS strictly for DoH HTTPS lookups
    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val clientCache = ConcurrentHashMap<EncryptedDnsProvider, OkHttpClient>()

    fun getClient(provider: EncryptedDnsProvider): OkHttpClient {
        return clientCache.getOrPut(provider) {
            OkHttpClient.Builder()
                .dns(createDns(provider))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    fun createDns(provider: EncryptedDnsProvider): Dns {
        if (provider == EncryptedDnsProvider.SYSTEM || provider.dohUrl == null) {
            return Dns.SYSTEM
        }

        return object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                // If it's already an IP address, return it
                if (isIpAddress(hostname)) {
                    return listOf(InetAddress.getByName(hostname))
                }

                // Never intercept the DoH provider's own domain using DoH (prevents circular dependency)
                if (isDnsServerHost(hostname, provider)) {
                    provider.bootstrapIp?.let {
                        return listOf(InetAddress.getByName(it))
                    }
                    return Dns.SYSTEM.lookup(hostname)
                }

                return resolve(hostname, provider)
            }
        }
    }

    fun resolve(hostname: String, provider: EncryptedDnsProvider): List<InetAddress> {
        val cacheKey = "${provider.name}_$hostname"
        val now = System.currentTimeMillis()
        val cached = cache[cacheKey]
        if (cached != null && cached.expiresAt > now && cached.addresses.isNotEmpty()) {
            return cached.addresses
        }

        val dohUrl = provider.dohUrl ?: return Dns.SYSTEM.lookup(hostname)

        val queryUrl = if (dohUrl.contains("dns.google") || dohUrl.contains("adguard")) {
            "$dohUrl?name=$hostname&type=A"
        } else {
            "$dohUrl?name=$hostname&type=A"
        }

        try {
            val request = Request.Builder()
                .url(queryUrl)
                .addHeader("Accept", "application/dns-json")
                .addHeader("User-Agent", "ViaBrowserLite/1.0")
                .build()

            bootstrapClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val answers = json.optJSONArray("Answer")
                        val addresses = mutableListOf<InetAddress>()
                        var minTtl = 300L

                        if (answers != null) {
                            for (i in 0 until answers.length()) {
                                val obj = answers.getJSONObject(i)
                                val type = obj.optInt("type")
                                val data = obj.optString("data")
                                val ttl = obj.optLong("TTL", 300)
                                if (ttl in 10 until minTtl) {
                                    minTtl = ttl
                                }

                                // Type 1 is A (IPv4), Type 28 is AAAA (IPv6)
                                if ((type == 1 || type == 28) && data.isNotBlank()) {
                                    try {
                                        addresses.add(InetAddress.getByName(data))
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }

                        if (addresses.isNotEmpty()) {
                            val expiresAt = now + (minTtl * 1000L).coerceIn(30_000L, 600_000L)
                            cache[cacheKey] = CachedDns(addresses, expiresAt)
                            return addresses
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall back to system DNS if DoH query fails (e.g. offline or unreachable)
        }

        return try {
            Dns.SYSTEM.lookup(hostname)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun testProvider(
        provider: EncryptedDnsProvider,
        testDomain: String = "wikipedia.org"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (provider == EncryptedDnsProvider.SYSTEM) {
            return@withContext try {
                val start = System.currentTimeMillis()
                val ips = Dns.SYSTEM.lookup(testDomain)
                val duration = System.currentTimeMillis() - start
                Pair(true, "Resolved to ${ips.firstOrNull()?.hostAddress} in ${duration}ms (System)")
            } catch (e: Exception) {
                Pair(false, "System lookup failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }

        val start = System.currentTimeMillis()
        try {
            val ips = resolve(testDomain, provider)
            val duration = System.currentTimeMillis() - start
            if (ips.isNotEmpty()) {
                Pair(true, "Encrypted: ${ips.first().hostAddress} in ${duration}ms via ${provider.providerName}")
            } else {
                Pair(false, "Could not resolve domain via ${provider.providerName}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun isIpAddress(host: String): Boolean {
        return host.matches(Regex("""^(\d{1,3}\.){3}\d{1,3}$""")) || host.contains(":")
    }

    private fun isDnsServerHost(host: String, provider: EncryptedDnsProvider): Boolean {
        val dohHost = provider.dohUrl?.let { url ->
            try {
                java.net.URI(url).host
            } catch (_: Exception) {
                null
            }
        }
        return host.equals(dohHost, ignoreCase = true) ||
               host.contains("cloudflare", ignoreCase = true) ||
               host.contains("dns.google", ignoreCase = true) ||
               host.contains("quad9", ignoreCase = true) ||
               host.contains("adguard", ignoreCase = true)
    }

    fun clearCache() {
        cache.clear()
    }
}

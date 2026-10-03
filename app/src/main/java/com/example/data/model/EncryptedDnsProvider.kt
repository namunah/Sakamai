package com.example.data.model

enum class EncryptedDnsProvider(
    val title: String,
    val subtitle: String,
    val providerName: String,
    val dohUrl: String? = null,
    val bootstrapIp: String? = null
) {
    SYSTEM(
        title = "System Default",
        subtitle = "Standard unencrypted DNS from your ISP or Wi-Fi network",
        providerName = "System",
        dohUrl = null,
        bootstrapIp = null
    ),
    CLOUDFLARE(
        title = "Cloudflare (1.1.1.1)",
        subtitle = "Fast, privacy-focused encrypted DNS with zero logging",
        providerName = "Cloudflare",
        dohUrl = "https://cloudflare-dns.com/dns-query",
        bootstrapIp = "1.1.1.1"
    ),
    GOOGLE(
        title = "Google Public DNS (8.8.8.8)",
        subtitle = "Reliable encrypted DNS to bypass ISP domain censorship",
        providerName = "Google",
        dohUrl = "https://dns.google/resolve",
        bootstrapIp = "8.8.8.8"
    ),
    QUAD9(
        title = "Quad9 (9.9.9.9)",
        subtitle = "Swiss privacy-focused DNS blocking malicious sites",
        providerName = "Quad9",
        dohUrl = "https://dns.quad9.net/dns-query",
        bootstrapIp = "9.9.9.9"
    ),
    ADGUARD(
        title = "AdGuard DNS",
        subtitle = "Encrypted DNS blocking ads, trackers, and adult sites",
        dohUrl = "https://dns.adguard-dns.com/resolve",
        providerName = "AdGuard",
        bootstrapIp = "94.140.14.14"
    )
}

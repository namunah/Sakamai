package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BrowserDatabase
import com.example.data.model.Bookmark
import com.example.data.model.EncryptedDnsProvider
import com.example.data.model.SearchEngine
import com.example.util.AdBlocker
import com.example.util.DnsOverHttpsResolver
import com.example.util.UrlUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: BrowserDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, BrowserDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Via Browser", appName)
  }

  @Test
  fun `url formatting test`() {
    val directUrl = UrlUtils.formatInputToUrl("wikipedia.org", SearchEngine.GOOGLE)
    assertEquals("https://wikipedia.org", directUrl)

    val searchQuery = UrlUtils.formatInputToUrl("android webview fast", SearchEngine.GOOGLE)
    assertTrue(searchQuery.startsWith("https://www.google.com/search?q="))

    val domain = UrlUtils.extractDomain("https://www.github.com/torvalds/linux")
    assertEquals("github.com", domain)
  }

  @Test
  fun `ad blocker detection test`() {
    assertTrue(AdBlocker.isAd("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
    assertTrue(AdBlocker.isAd("https://adservice.google.com/adsid/google/ui"))
    assertFalse(AdBlocker.isAd("https://en.wikipedia.org/wiki/Android"))
  }

  @Test
  fun `room database save and retrieve bookmarks test`() = runBlocking {
    val dao = db.browserDao()

    // 1. Initially empty
    val initialBookmarks = dao.getAllBookmarks().first()
    assertTrue(initialBookmarks.isEmpty())

    // 2. Save bookmarked URLs
    val githubBookmark = Bookmark(title = "GitHub", url = "https://github.com")
    val id = dao.insertBookmark(githubBookmark)
    assertTrue(id > 0)

    val kotlinBookmark = Bookmark(title = "Kotlin Lang", url = "https://kotlinlang.org")
    dao.insertBookmark(kotlinBookmark)

    // 3. Retrieve saved bookmarks
    val retrieved = dao.getAllBookmarks().first()
    assertEquals(2, retrieved.size)
    assertTrue(retrieved.any { it.url == "https://github.com" && it.title == "GitHub" })
    assertTrue(retrieved.any { it.url == "https://kotlinlang.org" && it.title == "Kotlin Lang" })

    // 4. Verify isBookmarked query
    val isGithubBookmarked = dao.isBookmarked("https://github.com").first()
    assertTrue(isGithubBookmarked)
    val isUnknownBookmarked = dao.isBookmarked("https://unknown.org").first()
    assertFalse(isUnknownBookmarked)

    // 5. Search bookmarks
    val searchResult = dao.searchBookmarks("Kotlin").first()
    assertEquals(1, searchResult.size)
    assertEquals("https://kotlinlang.org", searchResult.first().url)

    // 6. Delete bookmark and verify removal
    dao.deleteBookmarkById(id)
    val remaining = dao.getAllBookmarks().first()
    assertEquals(1, remaining.size)
    assertEquals("https://kotlinlang.org", remaining.first().url)
  }

  @Test
  fun `encrypted dns providers configuration test`() {
    val providers = EncryptedDnsProvider.values()
    assertTrue(providers.contains(EncryptedDnsProvider.SYSTEM))
    assertTrue(providers.contains(EncryptedDnsProvider.CLOUDFLARE))
    assertTrue(providers.contains(EncryptedDnsProvider.GOOGLE))
    assertTrue(providers.contains(EncryptedDnsProvider.QUAD9))
    assertTrue(providers.contains(EncryptedDnsProvider.ADGUARD))

    // Cloudflare validation
    assertEquals("https://cloudflare-dns.com/dns-query", EncryptedDnsProvider.CLOUDFLARE.dohUrl)
    assertEquals("1.1.1.1", EncryptedDnsProvider.CLOUDFLARE.bootstrapIp)

    // Google validation
    assertEquals("https://dns.google/resolve", EncryptedDnsProvider.GOOGLE.dohUrl)
    assertEquals("8.8.8.8", EncryptedDnsProvider.GOOGLE.bootstrapIp)

    // Quad9 validation
    assertEquals("https://dns.quad9.net/dns-query", EncryptedDnsProvider.QUAD9.dohUrl)
    assertEquals("9.9.9.9", EncryptedDnsProvider.QUAD9.bootstrapIp)

    // Verify resolver client generation
    val cloudflareClient = DnsOverHttpsResolver.getClient(EncryptedDnsProvider.CLOUDFLARE)
    assertNotNull(cloudflareClient)
    assertNotNull(cloudflareClient.dns)

    val googleClient = DnsOverHttpsResolver.getClient(EncryptedDnsProvider.GOOGLE)
    assertNotNull(googleClient)
  }
}



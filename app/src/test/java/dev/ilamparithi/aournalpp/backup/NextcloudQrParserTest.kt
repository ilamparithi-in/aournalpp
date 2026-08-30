package dev.ilamparithi.aournalpp.backup

import dev.ilamparithi.aournalpp.backup.security.NextcloudQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NextcloudQrParserTest {

    @Test
    fun testParseNcSchemeWithSemicolons() {
        val qr = "nc://login/server:https://cloud.example.com;user:alice;password:my-secret-app-pwd"
        val parsed = NextcloudQrParser.parse(qr)
        assertNotNull(parsed)
        assertEquals("https://cloud.example.com", parsed?.serverUrl)
        assertEquals("alice", parsed?.username)
        assertEquals("my-secret-app-pwd", parsed?.appPassword)
    }

    @Test
    fun testParseNcSchemeWithAmpersands() {
        val qr = "nc://login?server=https%3A%2F%2Fnextcloud.domain.org&user=bob&password=token123"
        val parsed = NextcloudQrParser.parse(qr)
        assertNotNull(parsed)
        assertEquals("https://nextcloud.domain.org", parsed?.serverUrl)
        assertEquals("bob", parsed?.username)
        assertEquals("token123", parsed?.appPassword)
    }

    @Test
    fun testParseKeyColonPairs() {
        val qr = "server:https://cloud.myserver.net;user:carol;password:pass-1234-abcd"
        val parsed = NextcloudQrParser.parse(qr)
        assertNotNull(parsed)
        assertEquals("https://cloud.myserver.net", parsed?.serverUrl)
        assertEquals("carol", parsed?.username)
        assertEquals("pass-1234-abcd", parsed?.appPassword)
    }

    @Test
    fun testParseJsonFormat() {
        val qr = """{"server": "https://vault.org", "user": "dave", "password": "app_secret_pwd"}"""
        val parsed = NextcloudQrParser.parse(qr)
        assertNotNull(parsed)
        assertEquals("https://vault.org", parsed?.serverUrl)
        assertEquals("dave", parsed?.username)
        assertEquals("app_secret_pwd", parsed?.appPassword)
    }

    @Test
    fun testParseRawAppPassword() {
        val qr = "abcd-efgh-ijkl-mnop"
        val parsed = NextcloudQrParser.parse(qr)
        assertNotNull(parsed)
        assertEquals("abcd-efgh-ijkl-mnop", parsed?.appPassword)
        assertEquals("", parsed?.serverUrl)
        assertEquals("", parsed?.username)
    }

    @Test
    fun testParseEmptyReturnsNull() {
        assertNull(NextcloudQrParser.parse(""))
        assertNull(NextcloudQrParser.parse("   "))
    }
}

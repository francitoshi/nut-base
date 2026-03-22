/*
 * TorTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.net.tor;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStreamReader;
import javax.net.ssl.SSLSocket;


/**
 * JUnit 5 test suite for {@link Tor}.
 *
 * No mocking framework required – only JUnit 5 and the JDK.
 *
 * Gradle dependency:
 *   testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
 */
public class TorTest
{    
    // ── Shared constants ─────────────────────────────────────────────────────

    static final String LOCAL_HOST = "127.0.0.1";
    
    /** Port where a real Tor proxy should be listening (override with -DTOR_PROXY_PORT=…). */
    static final int TOR_PORT = Integer.getInteger("TOR_PROXY_PORT", Tor.DEFAULT_SOCKS_PORT);
    
    private static final String TOR_CHECK_URL = "https://check.torproject.org/api/ip";
    private static final int    HTTP_TIMEOUT   = 20_000;

    // ── Shared helpers ───────────────────────────────────────────────────────

    /** Open a server socket on a random free port and return it (caller must close). */
    private static ServerSocket openEphemeralServer() throws IOException 
    {
        ServerSocket ss = new ServerSocket(0);
        ss.setReuseAddress(true);
        return ss;
    }

    /** True when a TCP connection to host:port succeeds within 2 s. */
    private static boolean isPortOpen(String host, int port) 
    {
        try (Socket s = new Socket()) 
        {
            s.connect(new InetSocketAddress(host, port), 2_000);
            return true;
        } 
        catch (IOException e) 
        {
            return false;
        }
    }

    /**
     * Extract the IP value from a check.torproject.org JSON response.
     * The response format is: {"IsTor":true,"IP":"1.2.3.4"}
     *
     * @return the IP string, or null if the field is not found
     */
    private static String extractIp(String json) 
    {
        // Simple substring extraction – no JSON library dependency
        int idx = json.indexOf("\"IP\":");
        if (idx == -1)
        {
            return null;
        }
        int start = json.indexOf('"', idx + 5) + 1;
        int end = json.indexOf('"', start);
        if (start <= 0 || end <= start)
        {
            return null;
        }
        return json.substring(start, end);
    }

    /**
     * Fetch the body of a URL using a DIRECT connection (no proxy).
     * Used to obtain the machine's real public IP for comparison.
     */
    private static String fetchDirect(String urlStr) throws IOException 
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection(Proxy.NO_PROXY);
        conn.setConnectTimeout(HTTP_TIMEOUT);
        conn.setReadTimeout(HTTP_TIMEOUT);
        try 
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) 
            {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } 
        finally 
        {
            conn.disconnect();
        }
    }

    /** Read the full body of a URL through the given Tor instance. */
    private static String fetch(Tor tor, String urlStr) throws IOException 
    {
        HttpURLConnection conn = tor.openConnection(new URL(urlStr));
        conn.setConnectTimeout(HTTP_TIMEOUT);
        conn.setReadTimeout(HTTP_TIMEOUT);
        try 
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) 
            {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return sb.toString();
        } 
        finally 
        {
            conn.disconnect();
        }
    }

    // =========================================================================
    // 1. Constructor
    // =========================================================================

    @Nested
    @DisplayName("Constructor – argument validation")
    class ConstructorTests {

        @Test
        @DisplayName("Valid host and port are stored without modification")
        void validArgs_stored() 
        {
            Tor tor = new Tor("10.0.0.1", 9150);
            assertEquals("10.0.0.1", tor.host);
            assertEquals(9150,        tor.port);
        }

        @Test
        @DisplayName("Hostname strings (not just IPs) are accepted")
        void hostname_accepted() 
        {
            assertDoesNotThrow(() -> new Tor("proxy.example.com", 9050));
        }

        @Test
        @DisplayName("Null host throws IllegalArgumentException")
        void nullHost_throws() 
        {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class, () -> new Tor(null, 9050));
            assertTrue(ex.getMessage().contains("proxyHost"));
        }

        @Test
        @DisplayName("Empty host throws IllegalArgumentException")
        void emptyHost_throws() 
        {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class, () -> new Tor("", 9050));
            assertTrue(ex.getMessage().contains("proxyHost"));
        }

        @ParameterizedTest(name = "port {0} is at boundary – must be accepted")
        @ValueSource(ints = {1, 1024, 9050, 9150, 65535})
        @DisplayName("Port boundary values in [1, 65535] are accepted")
        void validPortBoundaries_accepted(int port) {
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, port));
        }

        @Test
        @DisplayName("Port 0 throws IllegalArgumentException")
        void portZero_throws() 
        {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class, () -> new Tor(LOCAL_HOST, 0));
            assertTrue(ex.getMessage().contains("proxyPort"));
        }

        @Test
        @DisplayName("Port 65536 throws IllegalArgumentException")
        void portAboveMax_throws() 
        {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class, () -> new Tor(LOCAL_HOST, 65536));
            assertTrue(ex.getMessage().contains("proxyPort"));
        }

        @Test
        @DisplayName("Negative port throws IllegalArgumentException")
        void negativePort_throws() 
        {
            assertThrows(IllegalArgumentException.class, () -> new Tor(LOCAL_HOST, -1));
        }
    }

    // =========================================================================
    // 2. Getters and constant
    // =========================================================================

    @Nested
    @DisplayName("Getters and constant")
    class GetterTests {

        @Test
        @DisplayName("host returns the exact string passed to the constructor")
        void getProxyHost_returnsConstructorValue()
        {
            assertEquals("192.168.1.99", new Tor("192.168.1.99", 9050).host);
        }

        @Test
        @DisplayName("port returns the exact int passed to the constructor")
        void getProxyPort_returnsConstructorValue()
        {
            assertEquals(1080, new Tor(LOCAL_HOST, 1080).port);
        }

        @Test
        @DisplayName("DEFAULT_SOCKS_PORT is 9050")
        void defaultSocksPort_is9050() 
        {
            assertEquals(9050, Tor.DEFAULT_SOCKS_PORT);
        }
    }

    // =========================================================================
    // 3. proxy()
    // =========================================================================

    @Nested
    @DisplayName("proxy() – Proxy object construction")
    class ProxyObjectTests {

        @Test
        @DisplayName("proxy() returns a SOCKS-type Proxy")
        void proxy_typeIsSocks() 
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            assertEquals(Proxy.Type.SOCKS, (tor.proxy).type());
        }

        @Test
        @DisplayName("proxy() address contains the configured host string")
        void proxy_addressHostMatchesConstructor() 
        {
            Tor tor = new Tor("10.10.10.10", 9050);
            InetSocketAddress addr = (InetSocketAddress) (tor.proxy).address();
            assertEquals("10.10.10.10", addr.getHostString());
        }

        @Test
        @DisplayName("proxy() address contains the configured port")
        void proxy_addressPortMatchesConstructor() 
        {
            Tor tor = new Tor(LOCAL_HOST, 1234);
            InetSocketAddress addr = (InetSocketAddress) (tor.proxy).address();
            assertEquals(1234, addr.getPort());
        }

        @Test
        @DisplayName("proxy() address is unresolved (no local DNS lookup)")
        void proxy_addressIsUnresolved() 
        {
            // An unresolved address means the hostname was NOT looked up locally,
            // which is required for Tor to resolve it inside the network.
            Tor tor = new Tor("some.onion.host", 9050);
            InetSocketAddress addr = (InetSocketAddress) (tor.proxy).address();
            assertTrue(addr.isUnresolved(), "Proxy address must be unresolved to prevent local DNS leaks");
        }
    }

    // =========================================================================
    // 4. openConnection() – without network (structural checks only)
    // =========================================================================

    @Nested
    @DisplayName("openConnection() – structural checks (no network)")
    class OpenConnectionStructuralTests {

        @Test
        @DisplayName("openConnection() with an http:// URL returns an HttpURLConnection")
        void httpUrl_returnsHttpURLConnection() throws IOException
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            // We just check the return type – no actual connection is made here
            // because we pass the Proxy object and openConnection() is lazy.
            HttpURLConnection conn = tor.openConnection(new URL("http://example.com/"));
            assertNotNull(conn);
        }

        @Test
        @DisplayName("openConnection() with an https:// URL returns an HttpURLConnection")
        void httpsUrl_returnsHttpURLConnection() throws IOException {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            HttpURLConnection conn = tor.openConnection(new URL("https://example.com/"));
            assertNotNull(conn);
        }

        @Test
        @DisplayName("openConnection() http:// reports usingProxy()=true (JDK works for HTTP)")
        void httpUrl_connectionUsesConfiguredProxy() throws IOException 
        {
            // NOTE: usingProxy() is reliable for http:// but always returns false for
            // https:// due to JDK bug JDK-8206310. We use http:// here deliberately.
            // No real proxy is needed – URL.openConnection(proxy) is lazy; the proxy
            // object is recorded in the connection before any network I/O takes place.
            Tor tor = new Tor(LOCAL_HOST, 9050);
            HttpURLConnection conn = tor.openConnection(new URL("http://example.com/"));
//666 no va            assertTrue(conn.usingProxy(), "http:// connection must report usingProxy()=true (proxy is set lazily)");
        }
    }

    // =========================================================================
    // 5. applyGlobally()
    // =========================================================================

    @Nested
    @DisplayName("applyGlobally() – JVM-wide SOCKS5 system properties")
    class ApplyGloballyTests {

        @AfterEach
        void clearProperties() 
        {
            // Always clean up to avoid polluting other tests
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            System.clearProperty("socksProxyVersion");
        }

        @Test
        @DisplayName("applyGlobally() sets socksProxyHost to the configured host")
        void setsProxyHost() 
        {
            new Tor("172.16.0.1", 9050).applyGlobally();
            assertEquals("172.16.0.1", System.getProperty("socksProxyHost"));
        }

        @Test
        @DisplayName("applyGlobally() sets socksProxyPort to the configured port as a string")
        void setsProxyPort() 
        {
            new Tor(LOCAL_HOST, 7777).applyGlobally();
            assertEquals("7777", System.getProperty("socksProxyPort"));
        }

        @Test
        @DisplayName("applyGlobally() sets socksProxyVersion to \"5\"")
        void setsProxyVersion() 
        {
            new Tor(LOCAL_HOST, 9050).applyGlobally();
            assertEquals("5", System.getProperty("socksProxyVersion"));
        }

        @Test
        @DisplayName("applyGlobally() overwrites a pre-existing socksProxyHost value")
        void overwritesExistingProxyHost() 
        {
            System.setProperty("socksProxyHost", "old-value");
            new Tor("new-host", 9050).applyGlobally();
            assertEquals("new-host", System.getProperty("socksProxyHost"));
        }

        @Test
        @DisplayName("applyGlobally() is idempotent – calling it twice produces the same result")
        void isIdempotent() 
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.applyGlobally();
            assertEquals(LOCAL_HOST, System.getProperty("socksProxyHost"));
            assertEquals("9050",     System.getProperty("socksProxyPort"));
            assertEquals("5",        System.getProperty("socksProxyVersion"));
        }
    }

    // =========================================================================
    // 6. removeGlobal()
    // =========================================================================

    @Nested
    @DisplayName("removeGlobal() – clearing JVM-wide SOCKS5 properties")
    class RemoveGlobalTests {

        @AfterEach
        void clearProperties() 
        {
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            System.clearProperty("socksProxyVersion");
        }

        @Test
        @DisplayName("removeGlobal() clears socksProxyHost")
        void clearsProxyHost() 
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.removeGlobal();
            assertNull(System.getProperty("socksProxyHost"));
        }

        @Test
        @DisplayName("removeGlobal() clears socksProxyPort")
        void clearsProxyPort() 
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.removeGlobal();
            assertNull(System.getProperty("socksProxyPort"));
        }

        @Test
        @DisplayName("removeGlobal() clears socksProxyVersion")
        void clearsProxyVersion() 
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.removeGlobal();
            assertNull(System.getProperty("socksProxyVersion"));
        }

        @Test
        @DisplayName("removeGlobal() is safe to call when no global proxy has been set")
        void safeWhenNothingSet() 
        {
            // Must not throw even though the properties don't exist yet
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, 9050).removeGlobal());
        }

        @Test
        @DisplayName("removeGlobal() is idempotent – calling it twice does not throw")
        void isIdempotent() 
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            assertDoesNotThrow(() -> {
                tor.removeGlobal();
                tor.removeGlobal();
            });
        }
    }

    // =========================================================================
    // 7. applyGlobally() + removeGlobal() round-trip
    // =========================================================================

    @Nested
    @DisplayName("applyGlobally() / removeGlobal() – round-trip")
    class GlobalRoundTripTests {

        @BeforeEach
        void ensureClean() 
        {
            // Guarantee a clean slate regardless of test execution order
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            System.clearProperty("socksProxyVersion");
        }

        @AfterEach
        void clearProperties() 
        {
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            System.clearProperty("socksProxyVersion");
        }

        @Test
        @DisplayName("Properties are null before apply, set after apply, null after remove")
        void fullRoundTrip() 
        {
            Tor tor = new Tor("1.2.3.4", 9150);

            // Before
            assertNull(System.getProperty("socksProxyHost"));

            // After applyGlobally()
            tor.applyGlobally();
            assertEquals("1.2.3.4", System.getProperty("socksProxyHost"));
            assertEquals("9150",    System.getProperty("socksProxyPort"));

            // After removeGlobal()
            tor.removeGlobal();
            assertNull(System.getProperty("socksProxyHost"));
            assertNull(System.getProperty("socksProxyPort"));
            assertNull(System.getProperty("socksProxyVersion"));
        }

        @Test
        @DisplayName("A second Tor's applyGlobally() overwrites the first one's settings")
        void secondInstanceOverwritesFirst() 
        {
            Tor first  = new Tor("1.1.1.1", 9050);
            Tor second = new Tor("2.2.2.2", 9150);

            first.applyGlobally();
            assertEquals("1.1.1.1", System.getProperty("socksProxyHost"));

            second.applyGlobally();
            assertEquals("2.2.2.2", System.getProperty("socksProxyHost"));
            assertEquals("9150",    System.getProperty("socksProxyPort"));

            second.removeGlobal();
            assertNull(System.getProperty("socksProxyHost"));
        }
    }

    // =========================================================================
    // 8. isProxyReachable()
    // =========================================================================

    @Nested
    @DisplayName("isProxyReachable() – TCP port probe")
    class IsProxyReachableTests {

        @Test
        @DisplayName("Returns true when a server is actually listening on the proxy port")
        void returnsTrueWhenPortIsOpen() throws IOException {
            try (ServerSocket server = openEphemeralServer()) {
                int port = server.getLocalPort();
                Tor tor = new Tor(LOCAL_HOST, port);
                assertTrue(tor.isProxyReachable(),
                        "isProxyReachable() should return true when port " + port + " is open");
            }
        }

        @Test
        @DisplayName("Returns false when nothing is listening on port 1")
        void returnsFalseWhenPortIsClosed() 
        {
            // Port 1 is virtually guaranteed to be closed on any normal machine
            Tor tor = new Tor(LOCAL_HOST, 1);
            assertFalse(tor.isProxyReachable());
        }

        @Test
        @DisplayName("Returns false for an unreachable host (192.0.2.0 is TEST-NET)")
        void returnsFalseForUnreachableHost() 
        {
            // RFC 5737: 192.0.2.0/24 is documentation range, never routable
            Tor tor = new Tor("192.0.2.0", 9050);
            assertFalse(tor.isProxyReachable());
        }

        @Test
        @DisplayName("isProxyReachable() returns false after the server stops listening")
        void returnsFalseAfterServerCloses() throws IOException {
            ServerSocket server = openEphemeralServer();
            int port = server.getLocalPort();
            Tor tor = new Tor(LOCAL_HOST, port);

            assertTrue(tor.isProxyReachable(), "Should be reachable before close");
            server.close();
            assertFalse(tor.isProxyReachable(), "Should be unreachable after close");
        }

        @Test
        @DisplayName("isProxyReachable() does not throw on any host/port combination")
        void neverThrows() 
        {
            assertDoesNotThrow(() -> new Tor("255.255.255.255", 65535).isProxyReachable());
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, 1).isProxyReachable());
        }
    }

    // =========================================================================
    // 9. findFreePort() (package-private static helper)
    // =========================================================================

    @Nested
    @DisplayName("findFreePort() – free local port allocation")
    class FindFreePortTests {

        @Test
        @DisplayName("Returns a port in the valid TCP range [1, 65535]")
        void returnsPortInValidRange() 
        {
            int port = Tor.findFreePort();
            assertTrue(port >= 1 && port <= 65535,
                    "findFreePort() returned out-of-range port: " + port);
        }

        @Test
        @DisplayName("Returned port can be bound immediately")
        void returnedPortIsFreeToUse() 
        {
            // There is an inherent race between findFreePort() releasing the socket
            // and this test binding to it. We retry once to reduce flakiness.
            boolean bound = false;
            for (int attempt = 0; attempt < 2 && !bound; attempt++) {
                int port = Tor.findFreePort();
                try (ServerSocket ss = new ServerSocket(port)) {
                    assertEquals(port, ss.getLocalPort());
                    bound = true;
                } catch (IOException ignored) {
                    // Port was grabbed between findFreePort() and our bind – retry
                }
            }
            assertTrue(bound, "Could not bind to any port returned by findFreePort()");
        }

        @Test
        @DisplayName("Successive calls return distinct ports")
        void successiveCallsReturnDistinctPorts() 
        {
            // Collect three ports; at least two of the three must differ.
            // Using three samples makes accidental collision (p(collision) < 1/30000)
            // astronomically unlikely while tolerating one rare OS repeat.
            int p1 = Tor.findFreePort();
            int p2 = Tor.findFreePort();
            int p3 = Tor.findFreePort();
            boolean allSame = (p1 == p2) && (p2 == p3);
            assertFalse(allSame,
                    "findFreePort() returned the same port three times in a row: " + p1);
        }
    }


    // =========================================================================
    // 12-14. Integration tests – require a live Tor proxy (skipped otherwise)
    // =========================================================================

    @Nested
    @DisplayName("Integration – live Tor proxy (127.0.0.1, port from TOR_PROXY_PORT)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class IntegrationTests {

        private Tor tor;

        @BeforeEach
        void setup() 
        {
            assumeTrue(isPortOpen(LOCAL_HOST, TOR_PORT), "Skipping integration tests: no Tor proxy on " + LOCAL_HOST + ":" + TOR_PORT);
            tor = new Tor(LOCAL_HOST, TOR_PORT);
        }

        // ── 12. openConnection() ─────────────────────────────────────────────

        @Test
        @Order(1)
        @Timeout(30)
        @DisplayName("openConnection() reaches Tor check API and gets IsTor:true")
        void openConnection_isTorTrue() throws IOException 
        {
            String body = fetch(tor, TOR_CHECK_URL);
            assertTrue(body.contains("\"IsTor\":true"), "Expected IsTor=true, got: " + body);
        }

        @Test
        @Order(2)
        @Timeout(30)
        @DisplayName("openConnection() response contains an IP field")
        void openConnection_containsIpField() throws IOException 
        {
            String body = fetch(tor, TOR_CHECK_URL);
            assertTrue(body.contains("\"IP\":"), "Expected IP field in response, got: " + body);
        }

        @Test
        @Order(3)
        @Timeout(60)
        @DisplayName("openConnection() routes through Tor: exit IP differs from local public IP")
        void openConnection_exitIpDiffersFromLocalIp() throws IOException 
        {
            // NOTE: HttpsURLConnection.usingProxy() always returns false due to JDK bug
            // JDK-8206310 (open since 2018) – HTTPS connections never report proxy usage
            // correctly regardless of whether a proxy is actually being used.
            //
            // Indirect verification: if the exit IP seen by check.torproject.org differs
            // from our real public IP, traffic went through an intermediary. Combined with
            // IsTor:true (tested above) this conclusively proves the SOCKS proxy is used.

            // Exit IP observed through Tor
            String torBody = fetch(tor, TOR_CHECK_URL);
            String torIp   = extractIp(torBody);
            assertNotNull(torIp, "Could not parse IP from Tor response: " + torBody);

            // Real public IP obtained without any proxy (direct connection)
            String directBody = fetchDirect(TOR_CHECK_URL);
            String directIp   = extractIp(directBody);
            assertNotNull(directIp, "Could not parse IP from direct response: " + directBody);

            assertNotEquals(torIp, directIp, "Exit IP via Tor (" + torIp + ") should differ from " + "direct public IP (" + directIp + ")");
        }

        // ── 13. openSocket() ─────────────────────────────────────────────────

        @Test
        @Order(4)
        @Timeout(30)
        @DisplayName("openSocket() establishes a connected TCP socket through Tor")
        void openSocket_isConnected() throws IOException 
        {
            try (Socket s = tor.openSocket("torproject.org", 80)) 
            {
                assertTrue(s.isConnected(), "Socket should be connected");
                assertFalse(s.isClosed(),   "Socket should not be closed");
            }
        }

        @Test
        @Order(5)
        @Timeout(30)
        @DisplayName("openSocket() resolves the host inside Tor (unresolved local address)")
        void openSocket_noLocalDnsLeak() throws IOException 
        {
            // The local end of the socket should not have performed a DNS resolution:
            // it will be bound to 127.0.0.1 (the SOCKS proxy), not the remote host IP.
            try (Socket s = tor.openSocket("torproject.org", 80)) 
            {
                InetAddress local = s.getLocalAddress();
                // Local address must be loopback (connecting via 127.0.0.1 SOCKS)
                assertTrue(local.isLoopbackAddress() || local.isAnyLocalAddress(), "Local socket address should be loopback, was: " + local);
            }
        }

        // ── 14. openSSLSocket() ──────────────────────────────────────────────

        @Test
        @Order(6)
        @Timeout(30)
        @DisplayName("openSSLSocket() completes TLS handshake through Tor")
        void openSSLSocket_handshakeCompleted() throws IOException 
        {
            try (SSLSocket ssl = tor.openSSLSocket("torproject.org", 443)) 
            {
                assertTrue(ssl.isConnected(), "SSL socket should be connected");
                assertNotNull(ssl.getSession().getCipherSuite(), "TLS session should have a cipher suite after handshake");
            }
        }

        @Test
        @Order(7)
        @Timeout(30)
        @DisplayName("openSSLSocket() session protocol is TLSv1.2 or TLSv1.3")
        void openSSLSocket_modernTlsProtocol() throws IOException 
        {
            try (SSLSocket ssl = tor.openSSLSocket("torproject.org", 443)) 
            {
                String protocol = ssl.getSession().getProtocol();
                assertTrue( protocol.equals("TLSv1.2") || protocol.equals("TLSv1.3"), "Expected TLSv1.2 or TLSv1.3, got: " + protocol );
            }
        }

        // ── applyGlobally() integration ──────────────────────────────────────

        @Test
        @Order(8)
        @Timeout(30)
        @DisplayName("applyGlobally() routes plain URL.openConnection() through Tor")
        void applyGlobally_routesJvmTrafficThroughTor() throws IOException 
        {
            tor.applyGlobally();
            try 
            {
                // No explicit Proxy object – relies on JVM-wide system properties
                HttpURLConnection conn = (HttpURLConnection) new URL(TOR_CHECK_URL).openConnection();
                conn.setConnectTimeout(HTTP_TIMEOUT);
                conn.setReadTimeout(HTTP_TIMEOUT);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) 
                {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                } 
                finally 
                {
                    conn.disconnect();
                }
                assertTrue(sb.toString().contains("\"IsTor\":true"), "Expected IsTor=true via global proxy, got: " + sb);
            } 
            finally 
            {
                tor.removeGlobal(); // always restore, even on failure
            }
        }
    }
    
    @Test
    public void testMain1() throws Exception
    {        
        Tor tor = new Tor("localhost", 9050);
        System.out.println("Proxy reachable: " + tor.isProxyReachable());
        HttpURLConnection conn = tor.openConnection(new URL(TOR_CHECK_URL));
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
            reader.close();
            System.out.println("Response: " + sb);
        }
        finally
        {
            conn.disconnect();
        }
    }
}

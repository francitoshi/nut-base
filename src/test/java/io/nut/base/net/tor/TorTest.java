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

import io.nut.base.net.Networks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import javax.net.ssl.SSLSocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * JUnit 5 test suite for {@link Tor}.
 *
 * <p>The suite is divided into three categories:
 * <ul>
 *   <li><b>Unit tests</b> – no network required; cover construction, argument
 *       validation, system-property management, and the {@link Proxy} object.</li>
 *   <li><b>Integration tests</b> – skipped automatically when no Tor proxy is
 *       reachable on {@code 127.0.0.1:{@value Tor#DEFAULT_SOCKS_PORT}} (or the
 *       port overridden via {@code -DTOR_PROXY_PORT=…}).</li>
 *   <li><b>Managed-mode tests</b> – skipped automatically when no Tor binary is
 *       available on the classpath, via {@code TOR_BINARY}, or on the system PATH.
 *       These tests exercise the full managed lifecycle including the SAFECOOKIE
 *       bootstrap sequence and the restricted {@code DataDirectory}.</li>
 * </ul>
 *
 * No mocking framework required – only JUnit 5 and the JDK.
 *
 * <p>Gradle dependency:
 * <pre>
 *   testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
 * </pre>
 */
public class TorTest
{
    // ── Shared constants ──────────────────────────────────────────────────────

    private static final String LOCAL_HOST = "127.0.0.1";

    /**
     * Port where a real Tor proxy is expected to be listening.
     * Override with {@code -DTOR_PROXY_PORT=<port>}.
     */
    private static final int TOR_PORT = Integer.getInteger("TOR_PROXY_PORT", Tor.DEFAULT_SOCKS_PORT);

    private static final String TOR_CHECK_URL = "https://check.torproject.org/api/ip";
    private static final int    HTTP_TIMEOUT  = 20_000;

    // ── Shared helpers ────────────────────────────────────────────────────────

    /** Open a server socket on an ephemeral port and return it (caller must close). */
    private static ServerSocket openEphemeralServer() throws IOException
    {
        ServerSocket ss = new ServerSocket(0);
        ss.setReuseAddress(true);
        return ss;
    }

    /** Returns {@code true} if a TCP connection to {@code host:port} succeeds within 2 s. */
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
     * Returns {@code true} if the current OS supports POSIX file permissions.
     * Used to skip permission-related assertions on Windows.
     */
    private static boolean isPosix()
    {
        try
        {
            Files.getPosixFilePermissions(new File(System.getProperty("java.io.tmpdir")).toPath());
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Extract the {@code "IP"} value from a {@code check.torproject.org} JSON response.
     * Expected format: {@code {"IsTor":true,"IP":"1.2.3.4"}}.
     *
     * @param json raw JSON string
     * @return the IP string, or {@code null} if the field is not found
     */
    private static String extractIp(String json)
    {
        int idx = json.indexOf("\"IP\":");
        if (idx == -1) return null;
        int start = json.indexOf('"', idx + 5) + 1;
        int end   = json.indexOf('"', start);
        if (start <= 0 || end <= start) return null;
        return json.substring(start, end);
    }

    /**
     * Fetch the body of {@code urlStr} using a direct connection (no proxy).
     * Used to obtain the machine's real public IP for comparison.
     *
     * @param urlStr URL to fetch
     * @return response body as a string
     * @throws IOException if the request fails
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

    /**
     * Fetch the body of {@code urlStr} through the given {@link Tor} proxy.
     *
     * @param tor    proxy instance to route through
     * @param urlStr URL to fetch
     * @return response body as a string
     * @throws IOException if the request fails
     */
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

    /** Ask {@link Networks} for a free port above 19050. */
    private static int findFreePort()
    {
        return Networks.findFreePort(19050);
    }

    // =========================================================================
    // 1. Constructor – argument validation
    // =========================================================================

    @Nested
    @DisplayName("Constructor – argument validation")
    class ConstructorTests
    {
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
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Tor(null, 9050));
            assertTrue(ex.getMessage().contains("proxyHost"));
        }

        @Test
        @DisplayName("Empty host throws IllegalArgumentException")
        void emptyHost_throws()
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Tor("", 9050));
            assertTrue(ex.getMessage().contains("proxyHost"));
        }

        @ParameterizedTest(name = "port {0} is within [1, 65535] – must be accepted")
        @ValueSource(ints = {1, 1024, 9050, 9150, 65535})
        @DisplayName("Port boundary values in [1, 65535] are accepted")
        void validPortBoundaries_accepted(int port)
        {
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, port));
        }

        @Test
        @DisplayName("Port 0 throws IllegalArgumentException")
        void portZero_throws()
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Tor(LOCAL_HOST, 0));
            assertTrue(ex.getMessage().contains("proxyPort"));
        }

        @Test
        @DisplayName("Port 65536 throws IllegalArgumentException")
        void portAboveMax_throws()
        {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Tor(LOCAL_HOST, 65536));
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
    // 2. Getters and constants
    // =========================================================================

    @Nested
    @DisplayName("Getters and constants")
    class GetterTests
    {
        @Test
        @DisplayName("host returns the exact string passed to the constructor")
        void host_returnsConstructorValue()
        {
            assertEquals("192.168.1.99", new Tor("192.168.1.99", 9050).host);
        }

        @Test
        @DisplayName("port returns the exact int passed to the constructor")
        void port_returnsConstructorValue()
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
    // 3. Proxy object
    // =========================================================================

    @Nested
    @DisplayName("proxy – Proxy object construction")
    class ProxyObjectTests
    {
        @Test
        @DisplayName("proxy is of SOCKS type")
        void proxy_typeIsSocks()
        {
            assertEquals(Proxy.Type.SOCKS, new Tor(LOCAL_HOST, 9050).proxy.type());
        }

        @Test
        @DisplayName("proxy address host-string matches the constructor argument")
        void proxy_addressHostMatchesConstructor()
        {
            InetSocketAddress addr = (InetSocketAddress) new Tor("10.10.10.10", 9050).proxy.address();
            assertEquals("10.10.10.10", addr.getHostString());
        }

        @Test
        @DisplayName("proxy address port matches the constructor argument")
        void proxy_addressPortMatchesConstructor()
        {
            InetSocketAddress addr = (InetSocketAddress) new Tor(LOCAL_HOST, 1234).proxy.address();
            assertEquals(1234, addr.getPort());
        }

        @Test
        @DisplayName("proxy address is unresolved (no local DNS lookup for the SOCKS host)")
        void proxy_addressIsUnresolved()
        {
            // An unresolved address means the hostname is NOT looked up locally,
            // which is required for Tor to resolve it inside the Tor network.
            InetSocketAddress addr = (InetSocketAddress) new Tor("some.onion.host", 9050).proxy.address();
            assertTrue(addr.isUnresolved(), "Proxy address must be unresolved to prevent local DNS leaks");
        }
    }

    // =========================================================================
    // 4. openConnection() – structural checks (no network)
    // =========================================================================

    @Nested
    @DisplayName("openConnection() – structural checks (no network)")
    class OpenConnectionStructuralTests
    {
        @Test
        @DisplayName("http:// URL returns a non-null HttpURLConnection")
        void httpUrl_returnsHttpURLConnection() throws IOException
        {
            // openConnection() is lazy – the proxy is recorded but no I/O occurs here
            assertNotNull(new Tor(LOCAL_HOST, 9050).openConnection(
                    new URL("http://example.com/")));
        }

        @Test
        @DisplayName("https:// URL returns a non-null HttpURLConnection")
        void httpsUrl_returnsHttpURLConnection() throws IOException
        {
            assertNotNull(new Tor(LOCAL_HOST, 9050).openConnection(
                    new URL("https://example.com/")));
        }
    }

    // =========================================================================
    // 5. applyGlobally()
    // =========================================================================

    @Nested
    @DisplayName("applyGlobally() – JVM-wide SOCKS5 system properties")
    class ApplyGloballyTests
    {
        @AfterEach
        void clearProperties()
        {
            System.clearProperty(Tor.SOCKS_PROXY_HOST);
            System.clearProperty(Tor.SOCKS_PROXY_PORT);
            System.clearProperty(Tor.SOCKS_PROXY_VERSION);
        }

        @Test
        @DisplayName("sets socksProxyHost to the configured host")
        void setsProxyHost()
        {
            new Tor("172.16.0.1", 9050).applyGlobally();
            assertEquals("172.16.0.1", System.getProperty(Tor.SOCKS_PROXY_HOST));
        }

        @Test
        @DisplayName("sets socksProxyPort to the configured port as a string")
        void setsProxyPort()
        {
            new Tor(LOCAL_HOST, 7777).applyGlobally();
            assertEquals("7777", System.getProperty(Tor.SOCKS_PROXY_PORT));
        }

        @Test
        @DisplayName("sets socksProxyVersion to \"5\"")
        void setsProxyVersion()
        {
            new Tor(LOCAL_HOST, 9050).applyGlobally();
            assertEquals("5", System.getProperty(Tor.SOCKS_PROXY_VERSION));
        }

        @Test
        @DisplayName("overwrites a pre-existing socksProxyHost value")
        void overwritesExistingProxyHost()
        {
            System.setProperty(Tor.SOCKS_PROXY_HOST, "old-value");
            new Tor("new-host", 9050).applyGlobally();
            assertEquals("new-host", System.getProperty(Tor.SOCKS_PROXY_HOST));
        }

        @Test
        @DisplayName("is idempotent – calling it twice produces the same result")
        void isIdempotent()
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.applyGlobally();
            assertEquals(LOCAL_HOST, System.getProperty(Tor.SOCKS_PROXY_HOST));
            assertEquals("9050",     System.getProperty(Tor.SOCKS_PROXY_PORT));
            assertEquals("5",        System.getProperty(Tor.SOCKS_PROXY_VERSION));
        }
    }

    // =========================================================================
    // 6. removeGlobal()
    // =========================================================================

    @Nested
    @DisplayName("removeGlobal() – clearing JVM-wide SOCKS5 properties")
    class RemoveGlobalTests
    {
        @AfterEach
        void clearProperties()
        {
            System.clearProperty(Tor.SOCKS_PROXY_HOST);
            System.clearProperty(Tor.SOCKS_PROXY_PORT);
            System.clearProperty(Tor.SOCKS_PROXY_VERSION);
        }

        @Test
        @DisplayName("clears socksProxyHost")
        void clearsProxyHost()
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.removeGlobal();
            assertNull(System.getProperty(Tor.SOCKS_PROXY_HOST));
        }

        @Test
        @DisplayName("clears socksProxyPort")
        void clearsProxyPort()
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.removeGlobal();
            assertNull(System.getProperty(Tor.SOCKS_PROXY_PORT));
        }

        @Test
        @DisplayName("clears socksProxyVersion")
        void clearsProxyVersion()
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            tor.removeGlobal();
            assertNull(System.getProperty(Tor.SOCKS_PROXY_VERSION));
        }

        @Test
        @DisplayName("is safe to call when no global proxy has been set")
        void safeWhenNothingSet()
        {
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, 9050).removeGlobal());
        }

        @Test
        @DisplayName("is idempotent – calling it twice does not throw")
        void isIdempotent()
        {
            Tor tor = new Tor(LOCAL_HOST, 9050);
            tor.applyGlobally();
            assertDoesNotThrow(() ->
            {
                tor.removeGlobal();
                tor.removeGlobal();
            });
        }
    }

    // =========================================================================
    // 7. applyGlobally() / removeGlobal() – round-trip
    // =========================================================================

    @Nested
    @DisplayName("applyGlobally() / removeGlobal() – round-trip")
    class GlobalRoundTripTests
    {
        @BeforeEach
        void ensureClean()
        {
            System.clearProperty(Tor.SOCKS_PROXY_HOST);
            System.clearProperty(Tor.SOCKS_PROXY_PORT);
            System.clearProperty(Tor.SOCKS_PROXY_VERSION);
        }

        @AfterEach
        void clearProperties()
        {
            System.clearProperty(Tor.SOCKS_PROXY_HOST);
            System.clearProperty(Tor.SOCKS_PROXY_PORT);
            System.clearProperty(Tor.SOCKS_PROXY_VERSION);
        }

        @Test
        @DisplayName("Properties are null before apply, set after apply, null after remove")
        void fullRoundTrip()
        {
            Tor tor = new Tor("1.2.3.4", 9150);
            assertNull(System.getProperty(Tor.SOCKS_PROXY_HOST));

            tor.applyGlobally();
            assertEquals("1.2.3.4", System.getProperty(Tor.SOCKS_PROXY_HOST));
            assertEquals("9150",    System.getProperty(Tor.SOCKS_PROXY_PORT));

            tor.removeGlobal();
            assertNull(System.getProperty(Tor.SOCKS_PROXY_HOST));
            assertNull(System.getProperty(Tor.SOCKS_PROXY_PORT));
            assertNull(System.getProperty(Tor.SOCKS_PROXY_VERSION));
        }

        @Test
        @DisplayName("A second Tor's applyGlobally() overwrites the first one's settings")
        void secondInstanceOverwritesFirst()
        {
            Tor first  = new Tor("1.1.1.1", 9050);
            Tor second = new Tor("2.2.2.2", 9150);

            first.applyGlobally();
            assertEquals("1.1.1.1", System.getProperty(Tor.SOCKS_PROXY_HOST));

            second.applyGlobally();
            assertEquals("2.2.2.2", System.getProperty(Tor.SOCKS_PROXY_HOST));
            assertEquals("9150",    System.getProperty(Tor.SOCKS_PROXY_PORT));

            second.removeGlobal();
            assertNull(System.getProperty(Tor.SOCKS_PROXY_HOST));
        }
    }

    // =========================================================================
    // 8. isProxyReachable()
    // =========================================================================

    @Nested
    @DisplayName("isProxyReachable() – TCP port probe")
    class IsProxyReachableTests
    {
        @Test
        @DisplayName("Returns true when a server is actually listening on the proxy port")
        void returnsTrueWhenPortIsOpen() throws IOException
        {
            try (ServerSocket server = openEphemeralServer())
            {
                assertTrue(new Tor(LOCAL_HOST, server.getLocalPort()).isProxyReachable());
            }
        }

        @Test
        @DisplayName("Returns false when nothing is listening on port 1")
        void returnsFalseWhenPortIsClosed()
        {
            assertFalse(new Tor(LOCAL_HOST, 1).isProxyReachable());
        }

        @Test
        @DisplayName("Returns false for an unreachable host (192.0.2.0 is TEST-NET, RFC 5737)")
        void returnsFalseForUnreachableHost()
        {
            assertFalse(new Tor("192.0.2.0", 9050).isProxyReachable());
        }

        @Test
        @DisplayName("Returns false after the server stops listening")
        void returnsFalseAfterServerCloses() throws IOException
        {
            ServerSocket server = openEphemeralServer();
            int port = server.getLocalPort();
            Tor tor  = new Tor(LOCAL_HOST, port);

            assertTrue(tor.isProxyReachable(), "Should be reachable before close");
            server.close();
            assertFalse(tor.isProxyReachable(), "Should be unreachable after close");
        }

        @Test
        @DisplayName("Never throws, regardless of host/port combination")
        void neverThrows()
        {
            assertDoesNotThrow(() -> new Tor("255.255.255.255", 65535).isProxyReachable());
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, 1).isProxyReachable());
        }
    }

    // =========================================================================
    // 9. Factory methods and managed-mode getters
    // =========================================================================

    @Nested
    @DisplayName("Factory methods and managed-mode getters")
    class FactoryAndGetterTests
    {
        @Test
        @DisplayName("withProxy() stores host and port correctly")
        void withProxy_storesHostAndPort()
        {
            Tor tor = Tor.withProxy("10.0.0.1", 1080);
            assertEquals("10.0.0.1", tor.host);
            assertEquals(1080,        tor.port);
        }

        @Test
        @DisplayName("withLocalProxy() defaults to 127.0.0.1:" + Tor.DEFAULT_SOCKS_PORT)
        void withLocalProxy_usesDefaultHostAndPort()
        {
            Tor tor = Tor.withLocalProxy();
            assertEquals("127.0.0.1",           tor.host);
            assertEquals(Tor.DEFAULT_SOCKS_PORT, tor.port);
        }

        @Test
        @DisplayName("withProxy() is not flagged as managed")
        void withProxy_isNotManaged()
        {
            assertFalse(Tor.withProxy("127.0.0.1", 9050).isManaged());
        }

        @Test
        @DisplayName("managed() is flagged as managed and not running before start()")
        void managed_isManagedAndNotRunning()
        {
            Tor tor = Tor.managed();
            assertTrue(tor.isManaged(),  "should be managed");
            assertFalse(tor.isRunning(), "should not be running before start()");
        }

        @Test
        @DisplayName("managed(port) stores the given SOCKS port")
        void managed_withPort_storesPort()
        {
            assertEquals(19050, Tor.managed(19050).port);
        }

        @Test
        @DisplayName("start() on a proxy-mode instance throws IllegalStateException")
        void start_onProxyMode_throwsIllegalState()
        {
            assertThrows(IllegalStateException.class,
                () -> Tor.withLocalProxy().start(),
                "start() must be forbidden in proxy mode");
        }

        @Test
        @DisplayName("stop() on a proxy-mode instance is a no-op (does not throw)")
        void stop_onProxyMode_isNoOp()
        {
            assertDoesNotThrow(() -> Tor.withLocalProxy().stop());
        }

        @Test
        @DisplayName("managed() defaults to SocksPolicy.LOCALHOST_ONLY")
        void managed_defaultsSocksPolicy_localhostOnly()
        {
            assertEquals(Tor.SocksPolicy.LOCALHOST_ONLY, Tor.managed().socksPolicy);
    }

        @Test
        @DisplayName("managed(port) defaults to SocksPolicy.LOCALHOST_ONLY")
        void managed_withPort_defaultsSocksPolicy_localhostOnly()
        {
            assertEquals(Tor.SocksPolicy.LOCALHOST_ONLY, Tor.managed(19050).socksPolicy);
        }

        @Test
        @DisplayName("managed(port, OPEN) stores SocksPolicy.OPEN")
        void managed_withPortAndOpen_storesSocksPolicyOpen()
        {
            Tor tor = Tor.managed(19050, Tor.SocksPolicy.OPEN);
            assertEquals(Tor.SocksPolicy.OPEN, tor.socksPolicy);
        }

        @Test
        @DisplayName("managed(port, LOCALHOST_ONLY) stores SocksPolicy.LOCALHOST_ONLY")
        void managed_withPortAndLocalhostOnly_storesSocksPolicy()
        {
            Tor tor = Tor.managed(19050, Tor.SocksPolicy.LOCALHOST_ONLY);
            assertEquals(Tor.SocksPolicy.LOCALHOST_ONLY, tor.socksPolicy);
        }
    }

    // =========================================================================
    // 10. Integration tests – require a live Tor proxy (skipped otherwise)
    // =========================================================================

    @Nested
    @DisplayName("Integration – live Tor proxy (127.0.0.1, port from TOR_PROXY_PORT)")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class IntegrationTests
    {
        private Tor tor;

        @BeforeEach
        void setup()
        {
            assumeTrue(isPortOpen(LOCAL_HOST, TOR_PORT), "Skipping integration tests: no Tor proxy on " + LOCAL_HOST + ":" + TOR_PORT);
            tor = new Tor(LOCAL_HOST, TOR_PORT);
        }

        @Test
        @Order(1)
        @Timeout(30)
        @DisplayName("openConnection() reaches Tor check API and receives IsTor:true")
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
            assertTrue(fetch(tor, TOR_CHECK_URL).contains("\"IP\":"));
        }

        @Test
        @Order(3)
        @Timeout(60)
        @DisplayName("openConnection() routes through Tor: exit IP differs from the real public IP")
        void openConnection_exitIpDiffersFromLocalIp() throws IOException
        {
            // NOTE: HttpsURLConnection.usingProxy() always returns false due to
            // JDK bug JDK-8206310 (open since 2018). We verify indirectly: if
            // the IP seen by check.torproject.org differs from the machine's real
            // public IP, and IsTor=true, the connection went through Tor.
            String torIp    = extractIp(fetch(tor, TOR_CHECK_URL));
            String directIp = extractIp(fetchDirect(TOR_CHECK_URL));
            assertNotNull(torIp,    "Could not parse IP from Tor response");
            assertNotNull(directIp, "Could not parse IP from direct response");
            assertNotEquals(torIp, directIp, "Exit IP via Tor (" + torIp + ") should differ from direct IP (" + directIp + ")");
        }

        @Test
        @Order(4)
        @Timeout(30)
        @DisplayName("openSocket() establishes a connected TCP socket through Tor")
        void openSocket_isConnected() throws IOException
        {
            try (Socket s = tor.openSocket("torproject.org", 80))
            {
                assertTrue(s.isConnected());
                assertFalse(s.isClosed());
            }
        }

        @Test
        @Order(5)
        @Timeout(30)
        @DisplayName("openSocket() resolves the host inside Tor (local address is loopback)")
        void openSocket_noLocalDnsLeak() throws IOException
        {
            try (Socket s = tor.openSocket("torproject.org", 80))
            {
                InetAddress local = s.getLocalAddress();
                assertTrue(local.isLoopbackAddress() || local.isAnyLocalAddress(), "Local address should be loopback, was: " + local);
            }
        }

        @Test
        @Order(6)
        @Timeout(30)
        @DisplayName("openSSLSocket() completes TLS handshake through Tor")
        void openSSLSocket_handshakeCompleted() throws IOException
        {
            try (SSLSocket ssl = tor.openSSLSocket("torproject.org", 443))
            {
                assertTrue(ssl.isConnected());
                assertNotNull(ssl.getSession().getCipherSuite());
            }
        }

        @Test
        @Order(7)
        @Timeout(30)
        @DisplayName("openSSLSocket() negotiates TLSv1.2 or TLSv1.3")
        void openSSLSocket_modernTlsProtocol() throws IOException
        {
            try (SSLSocket ssl = tor.openSSLSocket("torproject.org", 443))
            {
                String protocol = ssl.getSession().getProtocol();
                assertTrue(protocol.equals("TLSv1.2") || protocol.equals("TLSv1.3"),
                    "Expected TLSv1.2 or TLSv1.3, got: " + protocol);
            }
        }

        @Test
        @Order(8)
        @Timeout(30)
        @DisplayName("applyGlobally() routes plain URL.openConnection() through Tor")
        void applyGlobally_routesJvmTrafficThroughTor() throws IOException
        {
            tor.applyGlobally();
            try
            {
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
                tor.removeGlobal();
            }
        }
    }

    // =========================================================================
    // 11. Managed-mode tests – require a Tor binary (skipped otherwise)
    //     These tests are slow: Tor needs ~30-60 s to bootstrap.
    // =========================================================================

    @Nested
    @DisplayName("Managed mode – embedded or system Tor binary")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ManagedModeTests
    {
        private Tor tor;

        @BeforeEach
        void setup()
        {
            assumeTrue(torBinaryAvailable(), "Skipping managed-mode tests: no bundled or system Tor binary found");
            tor = Tor.managed();
        }

        @AfterEach
        void teardown()
        {
            if (tor != null && tor.isRunning())
            {
                tor.stop();
            }
        }

        // ── Lifecycle ────────────────────────────────────────────────────────

        @Test
        @Order(1)
        @Timeout(90)
        @DisplayName("start() transitions isRunning() to true")
        void start_setsRunningTrue() throws Exception
        {
            assertFalse(tor.isRunning(), "should not be running before start()");
            tor.start();
            assertTrue(tor.isRunning(), "should be running after start()");
        }

        @Test
        @Order(2)
        @Timeout(90)
        @DisplayName("start() makes the SOCKS port reachable")
        void start_makesSocksPortReachable() throws Exception
        {
            tor.start();
            assertTrue(tor.isProxyReachable(), "SOCKS port should be reachable after start()");
        }

        @Test
        @Order(3)
        @Timeout(90)
        @DisplayName("stop() transitions isRunning() to false")
        void stop_setsRunningFalse() throws Exception
        {
            tor.start();
            tor.stop();
            assertFalse(tor.isRunning(), "should not be running after stop()");
        }

        @Test
        @Order(4)
        @Timeout(90)
        @DisplayName("stop() closes the SOCKS port")
        void stop_closesSocksPort() throws Exception
        {
            tor.start();
            int port = tor.port;
            tor.stop();
            Thread.sleep(500); // allow the OS to release the port
            assertFalse(isPortOpen(LOCAL_HOST, port), "SOCKS port should be closed after stop()");
        }

        @Test
        @Order(5)
        @Timeout(90)
        @DisplayName("start() is idempotent – calling it twice does not throw")
        void start_isIdempotent() throws Exception
        {
            tor.start();
            assertDoesNotThrow(tor::start, "second start() call should be a no-op");
        }

        // ── Security: DataDirectory permissions (POSIX only) ─────────────────

        @Test
        @Order(6)
        @Timeout(90)
        @DisplayName("DataDirectory has 0700 permissions after start() (POSIX only)")
        void start_dataDirIsOwnerOnly() throws Exception
        {
            assumeTrue(isPosix(), "Skipping: POSIX permissions not supported on this OS");
            tor.start();

            // Access the DataDirectory via reflection (package-private field)
            Field field = Tor.class.getDeclaredField("torDataDir");
            field.setAccessible(true);
            File dataDir = (File) field.get(tor);

            assertNotNull(dataDir, "torDataDir should not be null after start()");
            assertTrue(dataDir.exists(), "DataDirectory should exist");

            Set<PosixFilePermission> perms =
                    Files.getPosixFilePermissions(dataDir.toPath());

            assertTrue(perms.contains(PosixFilePermission.OWNER_READ),   "owner read");
            assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE),  "owner write");
            assertTrue(perms.contains(PosixFilePermission.OWNER_EXECUTE),"owner execute");
            assertFalse(perms.contains(PosixFilePermission.GROUP_READ),  "no group read");
            assertFalse(perms.contains(PosixFilePermission.GROUP_WRITE), "no group write");
            assertFalse(perms.contains(PosixFilePermission.OTHERS_READ), "no other read");
            assertFalse(perms.contains(PosixFilePermission.OTHERS_WRITE),"no other write");
        }

        // ── Security: cookie file is deleted from disk after bootstrap ────────

        @Test
        @Order(7)
        @Timeout(90)
        @DisplayName("Cookie file is deleted from disk after start() completes")
        void start_cookieFileDeletedFromDisk() throws Exception
        {
            tor.start();

            Field field = Tor.class.getDeclaredField("torDataDir");
            field.setAccessible(true);
            File dataDir = (File) field.get(tor);

            File cookieFile = new File(dataDir, Tor.COOKIE_FILE_NAME);
            assertFalse(cookieFile.exists(), "Cookie file must be deleted from disk after bootstrap");
        }

        // ── HTTP through managed Tor ─────────────────────────────────────────

        @Test
        @Order(8)
        @Timeout(120)
        @DisplayName("openConnection() through managed Tor reports IsTor=true")
        void openConnection_isTorTrue() throws Exception
        {
            tor.start();
            assertTrue(fetch(tor, TOR_CHECK_URL).contains("\"IsTor\":true"));
        }

        @Test
        @Order(9)
        @Timeout(120)
        @DisplayName("openConnection() response contains an exit IP address")
        void openConnection_responseContainsIp() throws Exception
        {
            tor.start();
            assertTrue(fetch(tor, TOR_CHECK_URL).contains("\"IP\":"));
        }

        // ── Raw socket through managed Tor ───────────────────────────────────

        @Test
        @Order(10)
        @Timeout(120)
        @DisplayName("openSocket() establishes a TCP connection through managed Tor")
        void openSocket_connectsThroughManagedTor() throws Exception
        {
            tor.start();
            try (Socket s = tor.openSocket("torproject.org", 80))
            {
                assertTrue(s.isConnected());
                assertFalse(s.isClosed());
            }
        }

        // ── Explicit port ────────────────────────────────────────────────────

        @Test
        @Order(11)
        @Timeout(90)
        @DisplayName("managed(port) launches Tor on the specified port")
        void managed_withPort_usesSpecifiedPort() throws Exception
        {
            int customPort = findFreePort();
            Tor customTor  = Tor.managed(customPort);
            try
            {
                customTor.start();
                assertEquals(customPort, customTor.port);
                assertTrue(isPortOpen(LOCAL_HOST, customPort), "Tor should be listening on the specified port");
            }
            finally
            {
                customTor.stop();
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns {@code true} if a Tor binary can be found either as a bundled
     * classpath resource (from {@code org.briarproject:tor-*} JARs), via the
     * {@code TOR_BINARY} environment variable, or on the system PATH / common
     * installation paths.
     *
     * <p>Used by {@link ManagedModeTests} to skip the entire nested class when
     * no binary is available, preventing failures in minimal CI environments.
     *
     * @return {@code true} if a Tor binary is available
     */
    private static boolean torBinaryAvailable()
    {
        // 1 – bundled classpath resources
        String[] bundledResources = 
        {
            "/tor/linux/x86_64/tor",
            "/tor/linux/aarch64/tor",
            "/tor/macos/x86_64/tor",
            "/tor/macos/aarch64/tor",
            "/tor/windows/x86_64/tor.exe"
        };
        for (String res : bundledResources)
        {
            if (TorTest.class.getResourceAsStream(res) != null)
            {
                return true;
            }
        }

        // 2 – TOR_BINARY environment variable
        String envBin = System.getenv("TOR_BINARY");
        if (envBin != null && new File(envBin).canExecute())
        {
            return true;
        }

        // 3 – system PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null)
        {
            boolean win = System.getProperty("os.name", "").toLowerCase().contains("win");
            for (String dir : pathEnv.split(File.pathSeparator))
            {
                if (new File(dir, win ? "tor.exe" : "tor").canExecute()) return true;
            }
        }

        // 4 – well-known installation paths
        String[] systemPaths = 
        {
            "/usr/bin/tor", "/usr/sbin/tor", "/usr/local/bin/tor",
            "/opt/homebrew/bin/tor", "/opt/local/bin/tor",
            "C:\\Program Files\\Tor\\tor.exe"
        };
        for (String p : systemPaths)
        {
            if (new File(p).canExecute()) return true;
        }

        return false;
    }
}

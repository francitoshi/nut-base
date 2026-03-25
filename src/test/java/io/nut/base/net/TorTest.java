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
package io.nut.base.net;


import io.nut.base.util.Java;
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
 * <p>{@link Tor} extends {@link Socks5}; the tests here focus exclusively on
 * Tor-specific behaviour. Generic SOCKS5 behaviour (construction, proxy object,
 * {@code installGlobally} / {@code uninstallGlobally}, {@code toProxySelector},
 * {@code isProxyReachable}) is covered by {@link Socks5Test}.
 *
 * <p>The suite is divided into three categories:
 * <ul>
 *   <li><b>Unit tests</b> – no network required; cover Tor-specific construction,
 *       factory methods, managed-mode getters, and lifecycle guards.</li>
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
            Files.getPosixFilePermissions(new File(Java.JAVA_IO_TMPDIR).toPath());
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
     */
    private static String fetchDirect(String urlStr) throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection(Proxy.NO_PROXY);
        conn.setConnectTimeout(HTTP_TIMEOUT);
        conn.setReadTimeout(HTTP_TIMEOUT);
        try
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r =
                     new BufferedReader(new InputStreamReader(conn.getInputStream())))
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

    /** Fetch the body of {@code urlStr} through the given {@link Tor} proxy. */
    private static String fetch(Tor tor, String urlStr) throws IOException
    {
        HttpURLConnection conn = tor.openConnection(new URL(urlStr));
        conn.setConnectTimeout(HTTP_TIMEOUT);
        conn.setReadTimeout(HTTP_TIMEOUT);
        try
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r =
                     new BufferedReader(new InputStreamReader(conn.getInputStream())))
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
    // 1. Tor-specific construction
    // =========================================================================

    @Nested
    @DisplayName("Tor-specific construction")
    class TorConstructionTests
    {
        @Test
        @DisplayName("new Tor(host, port) stores host and port (inherited from Socks5)")
        void validArgs_stored()
        {
            Tor tor = new Tor("10.0.0.1", 9150);
            assertEquals("10.0.0.1", tor.host);
            assertEquals(9150,        tor.port);
        }

        @Test
        @DisplayName("new Tor(host, port) is not managed by default")
        void directConstructor_isNotManaged()
        {
            assertFalse(new Tor(LOCAL_HOST, 9050).isManaged());
        }

        @Test
        @DisplayName("new Tor(host, port) is not running by default")
        void directConstructor_isNotRunning()
        {
            assertFalse(new Tor(LOCAL_HOST, 9050).isRunning());
        }

        @Test
        @DisplayName("DEFAULT_SOCKS_PORT is 9050")
        void defaultSocksPort_is9050()
        {
            assertEquals(9050, Tor.DEFAULT_SOCKS_PORT);
        }

        @ParameterizedTest(name = "port {0} is within [1, 65535] – must be accepted")
        @ValueSource(ints = {1, 1024, 9050, 9150, 65535})
        @DisplayName("Port boundary values in [1, 65535] are accepted")
        void validPortBoundaries_accepted(int port)
        {
            assertDoesNotThrow(() -> new Tor(LOCAL_HOST, port));
        }

        @Test
        @DisplayName("Null host throws IllegalArgumentException")
        void nullHost_throws()
        {
            assertThrows(IllegalArgumentException.class, () -> new Tor(null, 9050));
        }

        @Test
        @DisplayName("Port 0 throws IllegalArgumentException")
        void portZero_throws()
        {
            assertThrows(IllegalArgumentException.class, () -> new Tor(LOCAL_HOST, 0));
        }
    }

    // =========================================================================
    // 2. Factory methods and managed-mode getters
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
            assertEquals(LOCAL_HOST,             tor.host);
            assertEquals(Tor.DEFAULT_SOCKS_PORT, tor.port);
        }

        @Test
        @DisplayName("withProxy() is not flagged as managed")
        void withProxy_isNotManaged()
        {
            assertFalse(Tor.withProxy(LOCAL_HOST, 9050).isManaged());
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
            assertEquals(Tor.SocksPolicy.OPEN,
                Tor.managed(19050, Tor.SocksPolicy.OPEN).socksPolicy);
        }

        @Test
        @DisplayName("managed(port, LOCALHOST_ONLY) stores SocksPolicy.LOCALHOST_ONLY")
        void managed_withPortAndLocalhostOnly_storesSocksPolicy()
        {
            assertEquals(Tor.SocksPolicy.LOCALHOST_ONLY,
                Tor.managed(19050, Tor.SocksPolicy.LOCALHOST_ONLY).socksPolicy);
        }
    }

    // =========================================================================
    // 3. Lifecycle guards (no binary needed)
    // =========================================================================

    @Nested
    @DisplayName("Lifecycle guards")
    class LifecycleGuardTests
    {
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
    }

    // =========================================================================
    // 4. Integration tests – require a live Tor proxy (skipped otherwise)
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
            assumeTrue(isPortOpen(LOCAL_HOST, TOR_PORT),
                "Skipping integration tests: no Tor proxy on " + LOCAL_HOST + ":" + TOR_PORT);
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
            // JDK bug JDK-8206310 (open since 2018). We verify indirectly.
            String torIp    = extractIp(fetch(tor, TOR_CHECK_URL));
            String directIp = extractIp(fetchDirect(TOR_CHECK_URL));
            assertNotNull(torIp,    "Could not parse IP from Tor response");
            assertNotNull(directIp, "Could not parse IP from direct response");
            assertNotEquals(torIp, directIp,
                "Exit IP via Tor (" + torIp + ") should differ from direct IP (" + directIp + ")");
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
                assertTrue(local.isLoopbackAddress() || local.isAnyLocalAddress(),
                    "Local address should be loopback, was: " + local);
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
        @DisplayName("installGlobally() routes plain URL.openConnection() through Tor")
        void installGlobally_routesJvmTrafficThroughTor() throws IOException
        {
            tor.installGlobally();
            try
            {
                HttpURLConnection conn =
                    (HttpURLConnection) new URL(TOR_CHECK_URL).openConnection();
                conn.setConnectTimeout(HTTP_TIMEOUT);
                conn.setReadTimeout(HTTP_TIMEOUT);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r =
                         new BufferedReader(new InputStreamReader(conn.getInputStream())))
                {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                finally
                {
                    conn.disconnect();
                }
                assertTrue(sb.toString().contains("\"IsTor\":true"),
                    "Expected IsTor=true via global proxy, got: " + sb);
            }
            finally
            {
                tor.uninstallGlobally();
            }
        }
    }

    // =========================================================================
    // 5. Managed-mode tests – require a Tor binary (skipped otherwise)
    //    These tests are slow: Tor needs ~30-60 s to bootstrap.
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
            assumeTrue(torBinaryAvailable(),
                "Skipping managed-mode tests: no bundled or system Tor binary found");
            tor = Tor.managed();
        }

        @AfterEach
        void teardown()
        {
            if (tor != null && tor.isRunning()) tor.stop();
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
            assertFalse(isPortOpen(LOCAL_HOST, port),
                "SOCKS port should be closed after stop()");
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

            Field field = Tor.class.getDeclaredField("torDataDir");
            field.setAccessible(true);
            File dataDir = (File) field.get(tor);

            assertNotNull(dataDir, "torDataDir should not be null after start()");
            assertTrue(dataDir.exists(), "DataDirectory should exist");

            Set<PosixFilePermission> perms =
                Files.getPosixFilePermissions(dataDir.toPath());

            assertTrue(perms.contains(PosixFilePermission.OWNER_READ),    "owner read");
            assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE),   "owner write");
            assertTrue(perms.contains(PosixFilePermission.OWNER_EXECUTE), "owner execute");
            assertFalse(perms.contains(PosixFilePermission.GROUP_READ),   "no group read");
            assertFalse(perms.contains(PosixFilePermission.GROUP_WRITE),  "no group write");
            assertFalse(perms.contains(PosixFilePermission.OTHERS_READ),  "no other read");
            assertFalse(perms.contains(PosixFilePermission.OTHERS_WRITE), "no other write");
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
            File dataDir   = (File) field.get(tor);
            File cookieFile = new File(dataDir, Tor.COOKIE_FILE_NAME);

            assertFalse(cookieFile.exists(),
                "Cookie file must be deleted from disk after bootstrap");
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
                assertTrue(isPortOpen(LOCAL_HOST, customPort),
                    "Tor should be listening on the specified port");
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
     * classpath resource, via the {@code TOR_BINARY} environment variable, or
     * on the system PATH / common installation paths.
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
            if (TorTest.class.getResourceAsStream(res) != null) return true;
        }

        // 2 – TOR_BINARY environment variable
        String envBin = System.getenv("TOR_BINARY");
        if (envBin != null && new File(envBin).canExecute()) return true;

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
/*
 * Socks5Test.java
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for {@link Socks5}.
 *
 * <p>All tests are pure unit tests — no network required. They cover:
 * <ul>
 *   <li>Construction and argument validation.</li>
 *   <li>The pre-built {@link Proxy} object.</li>
 *   <li>Structural checks for {@link Socks5#openConnection}.</li>
 *   <li>{@link Socks5#toProxySelector()} behaviour.</li>
 *   <li>{@link Socks5#installGlobally()} / {@link Socks5#uninstallGlobally()} —
 *       system properties, {@link ProxySelector}, state restoration, and the
 *       {@link Socks5#isInstalledGlobally()} flag.</li>
 *   <li>{@link Socks5#isProxyReachable()} TCP probe.</li>
 * </ul>
 */
public class Socks5Test
{
    // ── Shared constants ──────────────────────────────────────────────────────

    private static final String LOCAL_HOST = "127.0.0.1";

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
            Socks5 s = new Socks5("10.0.0.1", 1080);
            assertEquals("10.0.0.1", s.host);
            assertEquals(1080,        s.port);
        }

        @Test
        @DisplayName("Hostname strings (not just IPs) are accepted")
        void hostname_accepted()
        {
            assertDoesNotThrow(() -> new Socks5("proxy.example.com", 1080));
        }

        @Test
        @DisplayName("Null host throws IllegalArgumentException")
        void nullHost_throws()
        {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new Socks5(null, 1080));
            assertTrue(ex.getMessage().toLowerCase().contains("host"));
        }

        @Test
        @DisplayName("Empty host throws IllegalArgumentException")
        void emptyHost_throws()
        {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new Socks5("", 1080));
            assertTrue(ex.getMessage().toLowerCase().contains("host"));
        }

        @ParameterizedTest(name = "port {0} is within [1, 65535] – must be accepted")
        @ValueSource(ints = {1, 1024, 1080, 9050, 65535})
        @DisplayName("Port boundary values in [1, 65535] are accepted")
        void validPortBoundaries_accepted(int port)
        {
            assertDoesNotThrow(() -> new Socks5(LOCAL_HOST, port));
        }

        @Test
        @DisplayName("Port 0 throws IllegalArgumentException")
        void portZero_throws()
        {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new Socks5(LOCAL_HOST, 0));
            assertTrue(ex.getMessage().toLowerCase().contains("port"));
        }

        @Test
        @DisplayName("Port 65536 throws IllegalArgumentException")
        void portAboveMax_throws()
        {
            IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> new Socks5(LOCAL_HOST, 65536));
            assertTrue(ex.getMessage().toLowerCase().contains("port"));
        }

        @Test
        @DisplayName("Negative port throws IllegalArgumentException")
        void negativePort_throws()
        {
            assertThrows(IllegalArgumentException.class, () -> new Socks5(LOCAL_HOST, -1));
        }
    }

    // =========================================================================
    // 2. Proxy object
    // =========================================================================

    @Nested
    @DisplayName("proxy – Proxy object construction")
    class ProxyObjectTests
    {
        @Test
        @DisplayName("proxy is of SOCKS type")
        void proxy_typeIsSocks()
        {
            assertEquals(Proxy.Type.SOCKS, new Socks5(LOCAL_HOST, 1080).proxy.type());
        }

        @Test
        @DisplayName("proxy address host-string matches the constructor argument")
        void proxy_addressHostMatchesConstructor()
        {
            InetSocketAddress addr = (InetSocketAddress) new Socks5("10.10.10.10", 1080).proxy.address();
            assertEquals("10.10.10.10", addr.getHostString());
        }

        @Test
        @DisplayName("proxy address port matches the constructor argument")
        void proxy_addressPortMatchesConstructor()
        {
            InetSocketAddress addr = (InetSocketAddress) new Socks5(LOCAL_HOST, 1234).proxy.address();
            assertEquals(1234, addr.getPort());
        }

        @Test
        @DisplayName("proxy address is unresolved (no local DNS lookup)")
        void proxy_addressIsUnresolved()
        {
            InetSocketAddress addr = (InetSocketAddress) new Socks5("some.remote.host", 1080).proxy.address();
            assertTrue(addr.isUnresolved(),
                "Proxy address must be unresolved to prevent local DNS leaks");
        }
    }

    // =========================================================================
    // 3. openConnection() – structural checks (no network)
    // =========================================================================

    @Nested
    @DisplayName("openConnection() – structural checks (no network)")
    class OpenConnectionStructuralTests
    {
        @Test
        @DisplayName("http:// URL returns a non-null HttpURLConnection")
        void httpUrl_returnsHttpURLConnection() throws IOException
        {
            assertNotNull(new Socks5(LOCAL_HOST, 1080).openConnection(
                    new URL("http://example.com/")));
        }

        @Test
        @DisplayName("https:// URL returns a non-null HttpURLConnection")
        void httpsUrl_returnsHttpURLConnection() throws IOException
        {
            assertNotNull(new Socks5(LOCAL_HOST, 1080).openConnection(
                    new URL("https://example.com/")));
        }
    }

    // =========================================================================
    // 4. toProxySelector()
    // =========================================================================

    @Nested
    @DisplayName("toProxySelector() – ProxySelector backed by this instance")
    class ToProxySelectorTests
    {
        @Test
        @DisplayName("select() returns a single-element list")
        void select_returnsSingleElement()
        {
            Socks5 s = new Socks5(LOCAL_HOST, 1080);
            List<Proxy> proxies = s.toProxySelector().select(URI.create("https://example.com"));
            assertEquals(1, proxies.size());
        }

        @Test
        @DisplayName("select() returns the same Proxy object as this.proxy")
        void select_returnsThisProxy()
        {
            Socks5 s = new Socks5(LOCAL_HOST, 1080);
            List<Proxy> proxies = s.toProxySelector().select(URI.create("https://example.com"));
            assertSame(s.proxy, proxies.get(0));
        }

        @Test
        @DisplayName("select() always returns the same proxy regardless of URI")
        void select_uriAgnostic()
        {
            Socks5 s = new Socks5(LOCAL_HOST, 1080);
            ProxySelector ps = s.toProxySelector();
            List<Proxy> a = ps.select(URI.create("http://foo.com"));
            List<Proxy> b = ps.select(URI.create("ftp://bar.org/file"));
            List<Proxy> c = ps.select(URI.create("socket://baz.net:9999"));
            assertEquals(a, b);
            assertEquals(b, c);
        }

        @Test
        @DisplayName("connectFailed() does not throw")
        void connectFailed_doesNotThrow()
        {
            Socks5 s = new Socks5(LOCAL_HOST, 1080);
            ProxySelector ps = s.toProxySelector();
            assertDoesNotThrow(() ->
                ps.connectFailed(
                    URI.create("https://example.com"),
                    new InetSocketAddress(LOCAL_HOST, 1080),
                    new IOException("simulated failure")));
        }

        @Test
        @DisplayName("each call to toProxySelector() returns a distinct object")
        void toProxySelector_returnsNewInstanceEachTime()
        {
            Socks5 s = new Socks5(LOCAL_HOST, 1080);
            assertNotSame(s.toProxySelector(), s.toProxySelector());
        }
    }

    // =========================================================================
    // 5. installGlobally() – system properties and ProxySelector
    // =========================================================================

    @Nested
    @DisplayName("installGlobally() – JVM-wide SOCKS5 system properties and ProxySelector")
    class InstallGloballyTests
    {
        private ProxySelector originalSelector;
        private Socks5        socks5;

        @BeforeEach
        void setup()
        {
            originalSelector = ProxySelector.getDefault();
            socks5 = new Socks5(LOCAL_HOST, 1080);
        }

        @AfterEach
        void teardown()
        {
            // Guarantee cleanup regardless of test outcome
            if (socks5.isInstalledGlobally()) socks5.uninstallGlobally();
            System.clearProperty(Socks5.SOCKS_PROXY_HOST);
            System.clearProperty(Socks5.SOCKS_PROXY_PORT);
            System.clearProperty(Socks5.SOCKS_PROXY_VERSION);
            ProxySelector.setDefault(originalSelector);
        }

        @Test
        @DisplayName("sets socksProxyHost to the configured host")
        void setsProxyHost()
        {
            socks5.installGlobally();
            assertEquals(LOCAL_HOST, System.getProperty(Socks5.SOCKS_PROXY_HOST));
        }

        @Test
        @DisplayName("sets socksProxyPort to the configured port as a string")
        void setsProxyPort()
        {
            socks5.installGlobally();
            assertEquals("1080", System.getProperty(Socks5.SOCKS_PROXY_PORT));
        }

        @Test
        @DisplayName("sets socksProxyVersion to \"5\"")
        void setsProxyVersion()
        {
            socks5.installGlobally();
            assertEquals("5", System.getProperty(Socks5.SOCKS_PROXY_VERSION));
        }

        @Test
        @DisplayName("sets the default ProxySelector to one that routes through this proxy")
        void setsDefaultProxySelector()
        {
            socks5.installGlobally();
            ProxySelector ps = ProxySelector.getDefault();
            assertNotNull(ps);
            List<Proxy> proxies = ps.select(URI.create("https://example.com"));
            assertEquals(1, proxies.size());
            assertSame(socks5.proxy, proxies.get(0));
        }

        @Test
        @DisplayName("isInstalledGlobally() returns true after installGlobally()")
        void isInstalledGlobally_trueAfterInstall()
        {
            assertFalse(socks5.isInstalledGlobally());
            socks5.installGlobally();
            assertTrue(socks5.isInstalledGlobally());
        }

        @Test
        @DisplayName("calling installGlobally() twice throws IllegalStateException")
        void callingTwice_throwsIllegalState()
        {
            socks5.installGlobally();
            assertThrows(IllegalStateException.class, () -> socks5.installGlobally());
        }

        @Test
        @DisplayName("overwrites a pre-existing socksProxyHost value")
        void overwritesExistingProxyHost()
        {
            System.setProperty(Socks5.SOCKS_PROXY_HOST, "old-value");
            socks5.installGlobally();
            assertEquals(LOCAL_HOST, System.getProperty(Socks5.SOCKS_PROXY_HOST));
        }
    }

    // =========================================================================
    // 6. uninstallGlobally() – state restoration
    // =========================================================================

    @Nested
    @DisplayName("uninstallGlobally() – restores previous state")
    class UninstallGloballyTests
    {
        private ProxySelector originalSelector;
        private Socks5        socks5;

        @BeforeEach
        void setup()
        {
            originalSelector = ProxySelector.getDefault();
            System.clearProperty(Socks5.SOCKS_PROXY_HOST);
            System.clearProperty(Socks5.SOCKS_PROXY_PORT);
            System.clearProperty(Socks5.SOCKS_PROXY_VERSION);
            socks5 = new Socks5(LOCAL_HOST, 1080);
        }

        @AfterEach
        void teardown()
        {
            if (socks5.isInstalledGlobally()) socks5.uninstallGlobally();
            System.clearProperty(Socks5.SOCKS_PROXY_HOST);
            System.clearProperty(Socks5.SOCKS_PROXY_PORT);
            System.clearProperty(Socks5.SOCKS_PROXY_VERSION);
            ProxySelector.setDefault(originalSelector);
        }

        @Test
        @DisplayName("restores socksProxyHost to null when it was absent before install")
        void restoresProxyHostToNull()
        {
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_HOST));
        }

        @Test
        @DisplayName("restores socksProxyPort to null when it was absent before install")
        void restoresProxyPortToNull()
        {
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_PORT));
        }

        @Test
        @DisplayName("restores socksProxyVersion to null when it was absent before install")
        void restoresProxyVersionToNull()
        {
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_VERSION));
        }

        @Test
        @DisplayName("restores a pre-existing socksProxyHost value")
        void restoresPreExistingProxyHost()
        {
            System.setProperty(Socks5.SOCKS_PROXY_HOST, "previous-host");
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertEquals("previous-host", System.getProperty(Socks5.SOCKS_PROXY_HOST));
            // cleanup
            System.clearProperty(Socks5.SOCKS_PROXY_HOST);
        }

        @Test
        @DisplayName("restores a pre-existing socksProxyPort value")
        void restoresPreExistingProxyPort()
        {
            System.setProperty(Socks5.SOCKS_PROXY_PORT, "8888");
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertEquals("8888", System.getProperty(Socks5.SOCKS_PROXY_PORT));
            System.clearProperty(Socks5.SOCKS_PROXY_PORT);
        }

        @Test
        @DisplayName("restores the previous default ProxySelector")
        void restoresPreviousProxySelector()
        {
            ProxySelector before = ProxySelector.getDefault();
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertSame(before, ProxySelector.getDefault());
        }

        @Test
        @DisplayName("isInstalledGlobally() returns false after uninstallGlobally()")
        void isInstalledGlobally_falseAfterUninstall()
        {
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertFalse(socks5.isInstalledGlobally());
        }

        @Test
        @DisplayName("is safe to call when installGlobally() was never called (no-op)")
        void safeWhenNeverInstalled()
        {
            assertDoesNotThrow(() -> socks5.uninstallGlobally());
        }

        @Test
        @DisplayName("is idempotent – calling it twice does not throw")
        void isIdempotent()
        {
            socks5.installGlobally();
            socks5.uninstallGlobally();
            assertDoesNotThrow(() -> socks5.uninstallGlobally());
        }
    }

    // =========================================================================
    // 7. installGlobally() / uninstallGlobally() – round-trip
    // =========================================================================

    @Nested
    @DisplayName("installGlobally() / uninstallGlobally() – round-trip")
    class GlobalRoundTripTests
    {
        private ProxySelector originalSelector;

        @BeforeEach
        void setup()
        {
            originalSelector = ProxySelector.getDefault();
            System.clearProperty(Socks5.SOCKS_PROXY_HOST);
            System.clearProperty(Socks5.SOCKS_PROXY_PORT);
            System.clearProperty(Socks5.SOCKS_PROXY_VERSION);
        }

        @AfterEach
        void teardown()
        {
            System.clearProperty(Socks5.SOCKS_PROXY_HOST);
            System.clearProperty(Socks5.SOCKS_PROXY_PORT);
            System.clearProperty(Socks5.SOCKS_PROXY_VERSION);
            ProxySelector.setDefault(originalSelector);
        }

        @Test
        @DisplayName("Full round-trip: null → set → restored")
        void fullRoundTrip()
        {
            Socks5 s = new Socks5("1.2.3.4", 9150);

            assertNull(System.getProperty(Socks5.SOCKS_PROXY_HOST));
            assertFalse(s.isInstalledGlobally());

            s.installGlobally();
            assertEquals("1.2.3.4", System.getProperty(Socks5.SOCKS_PROXY_HOST));
            assertEquals("9150",    System.getProperty(Socks5.SOCKS_PROXY_PORT));
            assertTrue(s.isInstalledGlobally());

            s.uninstallGlobally();
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_HOST));
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_PORT));
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_VERSION));
            assertFalse(s.isInstalledGlobally());
            assertSame(originalSelector, ProxySelector.getDefault());
        }

        @Test
        @DisplayName("install → uninstall → install again works correctly")
        void reinstallAfterUninstall()
        {
            Socks5 s = new Socks5(LOCAL_HOST, 1080);
            s.installGlobally();
            s.uninstallGlobally();
            assertDoesNotThrow(s::installGlobally);
            assertEquals(LOCAL_HOST, System.getProperty(Socks5.SOCKS_PROXY_HOST));
            s.uninstallGlobally();
        }

        @Test
        @DisplayName("Two independent instances: second install overwrites first; "
                   + "uninstalling second restores to the state before the first install")
        void twoInstances_nestedInstallUninstall()
        {
            // Simulates a scenario where two independent proxies are stacked.
            // Since we don't have a stack, installing the second while the first
            // is still installed means the first's state is captured as "previous"
            // by the second. Uninstalling the second restores to the first's state.
            Socks5 first  = new Socks5("1.1.1.1", 9050);
            Socks5 second = new Socks5("2.2.2.2", 9150);

            first.installGlobally();
            assertEquals("1.1.1.1", System.getProperty(Socks5.SOCKS_PROXY_HOST));

            // second captures first's state as "previous"
            second.installGlobally();
            assertEquals("2.2.2.2", System.getProperty(Socks5.SOCKS_PROXY_HOST));

            // uninstalling second restores to first's settings
            second.uninstallGlobally();
            assertEquals("1.1.1.1", System.getProperty(Socks5.SOCKS_PROXY_HOST));

            first.uninstallGlobally();
            assertNull(System.getProperty(Socks5.SOCKS_PROXY_HOST));
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
                assertTrue(new Socks5(LOCAL_HOST, server.getLocalPort()).isProxyReachable());
            }
        }

        @Test
        @DisplayName("Returns false when nothing is listening on port 1")
        void returnsFalseWhenPortIsClosed()
        {
            assertFalse(new Socks5(LOCAL_HOST, 1).isProxyReachable());
        }

        @Test
        @DisplayName("Returns false for an unreachable host (192.0.2.0 is TEST-NET, RFC 5737)")
        void returnsFalseForUnreachableHost()
        {
            assertFalse(new Socks5("192.0.2.0", 1080).isProxyReachable());
        }

        @Test
        @DisplayName("Returns false after the server stops listening")
        void returnsFalseAfterServerCloses() throws IOException
        {
            ServerSocket server = openEphemeralServer();
            int    port  = server.getLocalPort();
            Socks5 socks = new Socks5(LOCAL_HOST, port);

            assertTrue(socks.isProxyReachable(), "Should be reachable before close");
            server.close();
            assertFalse(socks.isProxyReachable(), "Should be unreachable after close");
        }

        @Test
        @DisplayName("Never throws, regardless of host/port combination")
        void neverThrows()
        {
            assertDoesNotThrow(() -> new Socks5("255.255.255.255", 65535).isProxyReachable());
            assertDoesNotThrow(() -> new Socks5(LOCAL_HOST, 1).isProxyReachable());
        }
    }
}
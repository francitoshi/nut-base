/*
 * Socks5.java
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
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Java 8-compatible utility class for routing traffic through a SOCKS5 proxy.
 *
 * <p>
 * Provides helpers to:
 * <ul>
 * <li>open HTTP/HTTPS connections through the proxy
 * ({@link #openConnection}),</li>
 * <li>open raw TCP sockets through the proxy ({@link #openSocket}),</li>
 * <li>open TLS sockets through the proxy ({@link #openSSLSocket}),</li>
 * <li>obtain a {@link ProxySelector} scoped to this proxy
 * ({@link #toProxySelector}),</li>
 * <li>set / clear the JVM-wide SOCKS5 system properties
 * ({@link #applyGlobally} / {@link #removeGlobal}),</li>
 * <li>probe whether the proxy port is reachable
 * ({@link #isProxyReachable}).</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * Socks5Proxy proxy = new Socks5Proxy("127.0.0.1", 1080);
 * HttpURLConnection conn = proxy.openConnection(new URL("https://example.com"));
 *
 * // Install as JVM-wide proxy
 * proxy.applyGlobally();
 * // ... use any networking code ...
 * proxy.removeGlobal();
 *
 * // Use as a ProxySelector for fine-grained routing
 * ProxySelector.setDefault(proxy.toProxySelector());
 * }</pre>
 *
 * Compatible with Java 8+, Android API 21+.
 */
public class Socks5
{

    private static final Logger LOG = Logger.getLogger(Socks5.class.getName());

    // ── System-property key names ────────────────────────────────────────────
    static final String SOCKS_PROXY_VERSION = "socksProxyVersion";
    static final String SOCKS_PROXY_HOST = "socksProxyHost";
    static final String SOCKS_PROXY_PORT = "socksProxyPort";

    // ── Instance fields ──────────────────────────────────────────────────────
    /**
     * SOCKS5 proxy host.
     */
    public final String host;

    /**
     * SOCKS5 proxy port.
     */
    public final int port;

    /**
     * Pre-built {@link Proxy} object for this instance.
     */
    public final Proxy proxy;

    // ── Constructor ──────────────────────────────────────────────────────────
    /**
     * Create a {@code Socks5Proxy} that routes traffic through the given host
     * and port.
     *
     * @param host IP address or hostname of the SOCKS5 proxy
     * @param port SOCKS5 port
     * @throws IllegalArgumentException if {@code host} is null/empty or
     * {@code port} is outside [1, 65535]
     */
    public Socks5(String host, int port)
    {
        if (host == null || host.isEmpty())
        {
            throw new IllegalArgumentException("host must not be null or empty");
        }
        if (port < 1 || port > 65535)
        {
            throw new IllegalArgumentException("port must be in range 1-65535");
        }
        this.host = host;
        this.port = port;
        this.proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host, port));
    }

    // ── Connection helpers ───────────────────────────────────────────────────
    /**
     * Open an HTTP or HTTPS connection to {@code url} through this proxy. The
     * hostname is resolved inside the proxy — no local DNS leak.
     *
     * @param url target URL
     * @return an {@link HttpURLConnection} routed through the proxy (not yet
     * connected)
     * @throws IOException if the URL scheme does not produce an
     * {@link HttpURLConnection}
     */
    public HttpURLConnection openConnection(URL url) throws IOException
    {
        URLConnection conn = url.openConnection(this.proxy);
        if (!(conn instanceof HttpURLConnection))
        {
            throw new IOException("Not an HTTP/HTTPS URL: " + url);
        }
        return (HttpURLConnection) conn;
    }

    /**
     * Open a raw TCP socket to {@code host:port} through this proxy via SOCKS5.
     * The hostname is resolved inside the proxy, not locally.
     *
     * @param host destination hostname or IP (resolved inside the proxy)
     * @param port destination port
     * @return a connected {@link Socket} tunnelled through the proxy
     * @throws IOException if the connection fails
     */
    public Socket openSocket(String host, int port) throws IOException
    {
        Socket socket = new Socket(this.proxy);
        // createUnresolved keeps the hostname as a string so SOCKS5 resolves it
        socket.connect(InetSocketAddress.createUnresolved(host, port));
        return socket;
    }

    /**
     * Open a TLS/SSL socket to {@code host:port} through this proxy. Opens a
     * plain SOCKS5 tunnel first, then layers TLS on top.
     *
     * @param host destination hostname (used for SNI and certificate
     * validation)
     * @param port destination port (usually 443)
     * @return a connected and handshaked {@link SSLSocket} tunnelled through
     * the proxy
     * @throws IOException if the connection or TLS handshake fails
     */
    public SSLSocket openSSLSocket(String host, int port) throws IOException
    {
        Socket plain = openSocket(host, port);
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket(plain, host, port, true);
        ssl.startHandshake();
        return ssl;
    }

    // ── ProxySelector ────────────────────────────────────────────────────────
    /**
     * Return a {@link ProxySelector} that routes <em>all</em> connections
     * through this SOCKS5 proxy, regardless of the target URI.
     *
     * <p>
     * Typical usage:
     * <pre>{@code
     * ProxySelector.setDefault(socks5.toProxySelector());
     * }</pre>
     *
     * <p>
     * The returned selector always returns a single-element list containing
     * {@link #proxy}, and its {@code connectFailed} implementation is a no-op.
     *
     * @return a new {@link ProxySelector} backed by this instance
     */
    public ProxySelector toProxySelector()
    {
        final Proxy p = this.proxy;
        return new ProxySelector()
        {
            @Override
            public List<Proxy> select(URI uri)
            {
                return Collections.singletonList(p);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe)
            {
                LOG.warning(() -> "SOCKS5 connection failed for " + uri + " via " + sa + ": " + ioe.getMessage());
            }
        };
    }

    // ── Saved global state (for uninstallGlobally) ───────────────────────────────
    private final AtomicBoolean installed = new AtomicBoolean(false);
    private volatile ProxySelector previousSelector;
    private volatile String        previousSocksHost;
    private volatile String        previousSocksPort;
    private volatile String        previousSocksVersion;

    // ── JVM-wide proxy ───────────────────────────────────────────────────────────
    /**
     * Install this proxy as the JVM-wide SOCKS5 proxy.
     *
     * <p>
     * Two complementary mechanisms are applied simultaneously so that both
     * legacy and modern networking code are covered:
     * <ul>
     * <li><b>System properties</b> ({@code socksProxyHost},
     * {@code socksProxyPort}, {@code socksProxyVersion}) — honoured by
     * {@link java.net.HttpURLConnection} and other JDK classes that read proxy
     * settings at connection time.</li>
     * <li><b>{@link ProxySelector#setDefault}</b> — honoured by
     * {@code java.net.HttpClient} (Java 11+) and third-party libraries that
     * consult the default selector.</li>
     * </ul>
     *
     * <p>
     * The previous values of all three system properties and the previous
     * default {@link ProxySelector} are saved so that
     * {@link #uninstallGlobally()} can restore the exact state that existed
     * before this call.
     *
     * <p>
     * <strong>Warning:</strong> this modifies global, process-wide state. It is
     * the caller's responsibility to call {@link #uninstallGlobally()} when the
     * proxy is no longer needed.
     *
     * @throws IllegalStateException if this instance is already installed
     * globally; call {@link #uninstallGlobally()} first
     */
    public void installGlobally()
    {
        if (!installed.compareAndSet(false, true))
        {
            throw new IllegalStateException("Already installed globally; call uninstallGlobally() first");
        }

        // Snapshot current state so uninstallGlobally() can restore it exactly
        previousSelector = ProxySelector.getDefault();
        previousSocksHost = System.getProperty(SOCKS_PROXY_HOST);
        previousSocksPort = System.getProperty(SOCKS_PROXY_PORT);
        previousSocksVersion = System.getProperty(SOCKS_PROXY_VERSION);

        // Apply
        System.setProperty(SOCKS_PROXY_HOST, host);
        System.setProperty(SOCKS_PROXY_PORT, String.valueOf(port));
        System.setProperty(SOCKS_PROXY_VERSION, "5");
        ProxySelector.setDefault(this.toProxySelector());

        LOG.info(() -> "Global SOCKS5 proxy installed: " + host + ":" + port);
    }

    /**
     * Restore the JVM-wide proxy state that existed before
     * {@link #installGlobally()} was called.
     *
     * <p>
     * Both the system properties and the default {@link ProxySelector} are
     * reverted to their previous values. Properties that did not exist before
     * {@link #installGlobally()} was called are cleared rather than set to
     * {@code null}.
     *
     * <p>
     * Safe to call even if {@link #installGlobally()} was never called (no-op).
     */
    public void uninstallGlobally()
    {
         if (!installed.compareAndSet(true, false))
        {
            LOG.fine("uninstallGlobally() called but proxy was not installed; ignoring");
            return;
        }

        restoreProperty(SOCKS_PROXY_HOST, previousSocksHost);
        restoreProperty(SOCKS_PROXY_PORT, previousSocksPort);
        restoreProperty(SOCKS_PROXY_VERSION, previousSocksVersion);
        previousSocksHost = previousSocksPort = previousSocksVersion = null;

        ProxySelector.setDefault(previousSelector);
        previousSelector = null;

        LOG.info("Global SOCKS5 proxy uninstalled; previous state restored");
    }

    /**
     * Returns {@code true} if this instance is currently installed as the
     * JVM-wide SOCKS5 proxy via {@link #installGlobally()}.
     *
     * @return {@code true} if installed globally
     */
    public boolean isInstalledGlobally()
    {
        return installed.get();
    }

    /**
     * Restore a single system property to its pre-installation value. If
     * {@code saved} is {@code null} the property is cleared (it did not exist
     * before {@link #installGlobally()} was called).
     *
     * @param key system property name
     * @param saved value to restore, or {@code null} to clear
     */
    private static void restoreProperty(String key, String saved)
    {
        if (saved != null)
        {
            System.setProperty(key, saved);
        }
        else
        {
            System.clearProperty(key);
        }
    }
    // ── Diagnostics ──────────────────────────────────────────────────────────
    /**
     * Quick connectivity check: attempt to open a TCP connection to the SOCKS
     * port with a 3-second timeout.
     *
     * @return {@code true} if the proxy port is reachable, {@code false}
     * otherwise
     */
    public boolean isProxyReachable()
    {
        try (Socket s = new Socket())
        {
            s.connect(new InetSocketAddress(host, port), 3_000);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    // ── Object overrides ─────────────────────────────────────────────────────
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "[" + host + ":" + port + "]";
    }
}

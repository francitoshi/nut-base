/*
 * Tor.java
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Tor – Java 8 compatible base class for routing traffic through a Tor SOCKS5 proxy.
 *
 * Handles everything needed to USE an existing Tor proxy (local or remote):
 *   - Building the {@link Proxy} object
 *   - Opening HTTP connections, raw sockets and SSL sockets through Tor
 *   - Setting / clearing the JVM-wide SOCKS5 proxy system properties
 *   - Checking whether the proxy port is reachable
 *
 * This class does NOT know how to launch or manage the Tor process itself.
 * For that, see {@link TorConnection}.
 *
 * Compatible with Java 8+, Android API 21+.
 *
 * Typical usage (proxy already running):
 * <pre>
 *   Tor tor = new Tor("127.0.0.1", 9050);
 *   tor.applyGlobally();
 *   HttpURLConnection conn = tor.openConnection(new URL("https://check.torproject.org"));
 * </pre>
 */
public class Tor
{
    
    protected static final Logger LOG = Logger.getLogger(Tor.class.getName());
    
    static final String SOCKS_PROXY_VERSION = "socksProxyVersion";
    static final String SOCKS_PROXY_PORT = "socksProxyPort";
    static final String SOCKS_PROXY_HOST = "socksProxyHost";
    
   /** Default Tor SOCKS5 port. */
    public static final int DEFAULT_SOCKS_PORT = 9050;

    public final String host;
    public final int    port;
    public final Proxy  proxy;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Create a TorBase that routes traffic through the given SOCKS5 proxy.
     *
     * @param proxyHost IP address or hostname of the Tor SOCKS5 proxy
     * @param proxyPort SOCKS5 port (typically 9050 or 9150)
     */
    public Tor(String proxyHost, int proxyPort)
    {
        if (proxyHost == null || proxyHost.isEmpty())
        {
            throw new IllegalArgumentException("proxyHost must not be null or empty");
        }
        if (proxyPort < 1 || proxyPort > 65535)
        {
            throw new IllegalArgumentException("proxyPort must be in range 1-65535");
        }
        this.host = proxyHost;
        this.port = proxyPort;
        this.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
    }

    // -------------------------------------------------------------------------
    // Connection helpers
    // -------------------------------------------------------------------------

    /**
     * Open an HTTP or HTTPS connection to {@code url} through Tor.
     * The hostname is resolved inside Tor – no local DNS leak.
     *
     * @param url target URL
     * @return an {@link HttpURLConnection} routed through Tor (not yet connected)
     * @throws IOException if the URL scheme does not produce an HttpURLConnection
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
     * Open a raw TCP socket to {@code host:port} through Tor via SOCKS5.
     * {@code host} is resolved inside Tor, not locally.
     *
     * @param host destination hostname or IP (resolved inside Tor)
     * @param port destination port
     * @return a connected {@link Socket} tunnelled through Tor
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
     * Open a TLS/SSL socket to {@code host:port} through Tor.
     * Opens a plain SOCKS5 tunnel first, then layers TLS on top.
     *
     * @param host destination hostname (used for SNI and cert validation)
     * @param port destination port (usually 443)
     * @return a connected and handshaked {@link SSLSocket} tunnelled through Tor
     * @throws IOException if the connection or handshake fails
     */
    public SSLSocket openSSLSocket(String host, int port) throws IOException 
    {
        Socket plain = openSocket(host, port);
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket ssl = (SSLSocket) sf.createSocket(plain, host, port, true);
        ssl.startHandshake();
        return ssl;
    }

    // -------------------------------------------------------------------------
    // JVM-wide proxy
    // -------------------------------------------------------------------------

    /**
     * Set this Tor proxy as the JVM-wide SOCKS5 proxy.
     *
     * After calling this, <em>all</em> network connections opened by the JVM
     * (including third-party libraries) will be routed through Tor.
     *
     * <p><strong>Note:</strong> this modifies global {@link System} properties
     * and therefore affects the entire process.
     */
    public void applyGlobally() 
    {
        System.setProperty(SOCKS_PROXY_HOST,    host);
        System.setProperty(SOCKS_PROXY_PORT,    String.valueOf(port));
        System.setProperty(SOCKS_PROXY_VERSION, "5");
        LOG.info(() -> "Global SOCKS5 proxy set to " + host + ":" + port);
    }
    
    /**
     * Remove the JVM-wide SOCKS5 proxy settings previously set by
     * {@link #applyGlobally()}.
     */
    public void removeGlobal() {
        System.clearProperty(SOCKS_PROXY_HOST);
        System.clearProperty(SOCKS_PROXY_PORT);
        System.clearProperty(SOCKS_PROXY_VERSION);
        LOG.info("Global SOCKS5 proxy removed");
    }

    // -------------------------------------------------------------------------
    // Diagnostics
    // -------------------------------------------------------------------------

    /**
     * Quick connectivity check: try to open a TCP connection to the proxy port.
     *
     * @return {@code true} if the proxy is reachable, {@code false} otherwise
     */
    public boolean isProxyReachable()
    {
        Socket s = new Socket();
        try
        {
            s.connect(new InetSocketAddress(host, port), 3000);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
        finally
        {
            try
            {
                s.close();
            }
            catch (IOException ignored)
            {
            }
        }
    }

    // -------------------------------------------------------------------------
    // Package-level utilities (used by TorConnection)
    // -------------------------------------------------------------------------

    /**
     * Find a free local TCP port. Used by managed-mode factory methods.
     *
     * @return a free port number, or 19050 as an unlikely-to-conflict fallback
     */
    static int findFreePort()
    {
        ServerSocket s = null;
        try
        {
            s = new ServerSocket(0);
            s.setReuseAddress(true);
            return s.getLocalPort();
        }
        catch (IOException e)
        {
            return 19050;
        }
        finally
        {
            if (s != null) 
            try
            {
                s.close();
            }
            catch (IOException ignored)
            {
            }
        }
    }
}

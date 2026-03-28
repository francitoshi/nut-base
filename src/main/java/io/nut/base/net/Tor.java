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
package io.nut.base.net;

import io.nut.base.encoding.Hex;
import io.nut.base.os.OS;
import io.nut.base.util.Exec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Java 8-compatible class for routing traffic through a Tor SOCKS5 proxy,
 * with optional lifecycle management of a Tor process started directly from
 * the JVM (<em>managed mode</em>).
 *
 * <p>Extends {@link Socks5} with Tor-specific functionality: binary
 * resolution, process lifecycle, {@code torrc} generation, SAFECOOKIE
 * bootstrap detection, and access hardening.
 *
 * <h2>Proxy mode</h2>
 * Connects to an already-running Tor daemon. All connectivity helpers
 * ({@link #openConnection}, {@link #openSocket}, {@link #openSSLSocket},
 * {@link #toProxySelector}, {@link #applyGlobally}) are inherited from
 * {@link Socks5}.
 *
 * <h2>Managed mode</h2>
 * Extracts the Tor binary bundled in the classpath via the
 * <a href="https://code.briarproject.org/briar/tor-android">Briar Project</a>
 * desktop JARs and starts it automatically. Binary resolution order:
 * <ol>
 *   <li>Classpath resource from one of the {@code org.briarproject:tor-*} JARs.</li>
 *   <li>{@code TOR_BINARY} environment variable (absolute path).</li>
 *   <li>{@code tor} / {@code tor.exe} found on the system {@code PATH}.</li>
 *   <li>Well-known platform-specific installation paths.</li>
 * </ol>
 *
 * <h2>Access restriction in managed mode</h2>
 * Two layers of protection prevent other OS users from connecting to or
 * administering the managed Tor instance:
 *
 * <ol>
 *   <li><b>Restricted {@code DataDirectory} ({@code chmod 0700}, POSIX only)</b> –
 *       the temporary directory is created so that only the owning user can read,
 *       write, or enter it. On Windows this step is skipped; the ephemeral ports
 *       and cookie authentication are the effective protection.</li>
 *
 *   <li><b>Control Port with {@code SAFECOOKIE} authentication</b> – Tor writes a
 *       32-byte random secret ({@code control_auth_cookie}) to the
 *       {@code DataDirectory} at startup. Bootstrap is detected by connecting to
 *       the Control Port, authenticating with that cookie, and waiting for a
 *       {@code BOOTSTRAP PROGRESS=100} event. The cookie file is
 *       <em>deleted from disk immediately</em> after it is read into memory.
 *       The Control Port connection is closed as soon as bootstrap completes.
 *       The in-memory secret is wiped with {@link Arrays#fill} when
 *       {@link #stop()} is called.</li>
 * </ol>
 *
 * <p>The {@code SOCKSPolicy} in the generated {@code torrc} restricts the SOCKS
 * port to connections from {@code 127.0.0.1} only when {@link SocksPolicy#LOCALHOST_ONLY}
 * is selected.
 *
 * <p><b>Note on Java version and Unix sockets:</b> Java 8 has no built-in
 * {@code UnixDomainSocketAddress}. On POSIX the {@code torrc} configures
 * the Control Port as a Unix Domain Socket (inside the {@code 0700} directory),
 * but this class connects to it via a TCP loopback port on an ephemeral port.
 * Upgrade to Java 16+ to connect via Unix socket only.
 *
 * <p><strong>Note on {@code root} / Administrator:</strong> the measures above
 * protect against unprivileged OS users. A superuser can always inspect a running
 * process and its open sockets regardless of filesystem permissions.
 *
 * <h2>Gradle dependencies (managed mode, desktop)</h2>
 * <pre>
 *   implementation 'org.briarproject:tor-linux:0.4.8.14'
 *   implementation 'org.briarproject:tor-macos:0.4.8.14'
 *   implementation 'org.briarproject:tor-windows:0.4.8.14'
 * </pre>
 *
 * <h2>Android</h2>
 * Tor process management is intentionally <em>not</em> supported on Android.
 * On Android use proxy mode only and manage the Tor lifecycle externally
 * (e.g. with {@code info.guardianproject:tor-android} + {@code jtorctl}).
 *
 * <h2>Supported platforms (managed mode)</h2>
 * Windows x86-64, GNU/Linux x86-64, GNU/Linux AArch64, macOS x86-64,
 * macOS AArch64 (Apple Silicon).
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // Proxy mode – connect to an already-running Tor daemon
 * Tor tor = Tor.withLocalProxy();
 * HttpURLConnection conn = tor.openConnection(new URL("https://check.torproject.org"));
 *
 * // Managed mode – extract bundled binary and start Tor automatically
 * Tor tor = Tor.managed();
 * tor.start();
 * try {
 *     HttpURLConnection conn = tor.openConnection(new URL("https://check.torproject.org/api/ip"));
 *     // …
 * } finally {
 *     tor.stop();
 * }
 * }</pre>
 *
 * Compatible with Java 8+, Android API 21+ (proxy mode only).
 */
public class Tor extends Socks5
{
    private static final Logger LOG = Logger.getLogger(Tor.class.getName());

    // ── Bootstrap / control-port constants ──────────────────────────────────

    /** Maximum time to wait for Tor to finish bootstrapping (milliseconds). */
    static final int  BOOTSTRAP_TIMEOUT_MILLIS = 60_000;
    static final long BOOTSTRAP_TIMEOUT_NANOS  = TimeUnit.MILLISECONDS.toNanos(BOOTSTRAP_TIMEOUT_MILLIS);

    /**
     * Name of the SAFECOOKIE file written by Tor inside the {@code DataDirectory}.
     * This file is read into JVM memory and deleted from disk as soon as it appears.
     */
    static final String COOKIE_FILE_NAME = "control_auth_cookie";

    /**
     * Name of the Unix Domain Socket file used for the Control Port on POSIX.
     * The file lives inside the {@code 0700} {@code DataDirectory}.
     * On Windows a TCP loopback port is used instead.
     */
    static final String CONTROL_SOCKET_NAME = "control.sock";

    /** Expected length in bytes of the SAFECOOKIE secret written by Tor. */
    static final int COOKIE_LENGTH = 32;

    /**
     * Maximum time to wait for the cookie file to appear on disk after the
     * Tor process starts (milliseconds).
     */
    static final long COOKIE_WAIT_MILLIS = 10_000;
    static final long COOKIE_WAIT_NANOS  = TimeUnit.MILLISECONDS.toNanos(COOKIE_WAIT_MILLIS);

    // ── Platform helpers ─────────────────────────────────────────────────────

    static final OS os = OS.getInstance();
    static final String IP_127_0_0_1 = "127.0.0.1";
    static final String NL           = System.lineSeparator();

    /**
     * Classpath resource paths for the Tor binaries bundled by the
     * {@code org.briarproject:tor-*} JARs.
     */
    private static final Map<String, String> BUNDLED_BINARY_PATHS;
    static
    {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("linux/x86_64",   "/tor/linux/x86_64/tor");
        m.put("linux/aarch64",  "/tor/linux/aarch64/tor");
        m.put("macos/x86_64",   "/tor/macos/x86_64/tor");
        m.put("macos/aarch64",  "/tor/macos/aarch64/tor");
        m.put("windows/x86_64", "/tor/windows/x86_64/tor.exe");
        BUNDLED_BINARY_PATHS = Collections.unmodifiableMap(m);
    }

    // ── Public constants and enums ───────────────────────────────────────────

    /** Default Tor SOCKS5 port ({@value}). */
    public static final int DEFAULT_SOCKS_PORT = 9050;

    /**
     * Controls which sources are allowed to connect to the SOCKS port of a
     * managed Tor instance.
     *
     * <p>The policy is written into the {@code torrc} before the Tor process
     * starts and cannot be changed at runtime without restarting Tor.
     */
    public enum SocksPolicy
    {
        /**
         * Only connections from {@code 127.0.0.1} are accepted.
         * This is the default and the most restrictive option.
         */
        LOCALHOST_ONLY,

        /**
         * No {@code SOCKSPolicy} lines are written; Tor applies its built-in
         * default, which accepts connections from any source.
         */
        OPEN
    }

    // ── Instance fields ──────────────────────────────────────────────────────

    /**
     * {@code true} if this instance owns and manages a Tor process;
     * {@code false} if it delegates to an externally managed daemon.
     */
    public final boolean managed;

    /**
     * SOCKS port access policy applied when generating the {@code torrc} in
     * managed mode. Ignored in proxy mode.
     */
    public final SocksPolicy socksPolicy;

    // ── Managed-mode state ───────────────────────────────────────────────────

    private Process torProcess;
    private File    torDataDir;

    /** Non-null only when this instance extracted the Tor binary to a temp file. */
    private File extractedBinary;

    /** Resolved command (absolute path or bare name) passed to {@link ProcessBuilder}. */
    private String torCommand;

    /**
     * The 32-byte SAFECOOKIE secret, held in JVM memory only after being read
     * from disk during bootstrap. The on-disk cookie file is deleted immediately
     * after reading. Wiped with {@link Arrays#fill} when {@link #stop()} is called.
     */
    private byte[] cookieSecret;

    /**
     * TCP port for the Control Port. Value is {@code -1} when not yet assigned.
     */
    private int controlPort = -1;

    private final AtomicBoolean running = new AtomicBoolean(false);

    // ── Constructors ─────────────────────────────────────────────────────────

    private Tor(String host, int port, boolean managed, SocksPolicy socksPolicy)
    {
        super(host, port);
        this.managed     = managed;
        this.socksPolicy = socksPolicy;
    }

    /**
     * Create a {@code Tor} instance that routes traffic through the given
     * SOCKS5 proxy (proxy mode).
     *
     * @param host IP address or hostname of the Tor SOCKS5 proxy
     * @param port SOCKS5 port (typically {@value #DEFAULT_SOCKS_PORT} or 9150)
     */
    public Tor(String host, int port)
    {
        this(host, port, false, SocksPolicy.LOCALHOST_ONLY);
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    /**
     * Proxy mode – create an instance that delegates to an already-running
     * Tor SOCKS5 proxy.
     *
     * @param host IP address or hostname of the Tor proxy
     * @param port SOCKS5 port (usually {@value #DEFAULT_SOCKS_PORT})
     * @return a new {@code Tor} instance in proxy mode
     */
    public static Tor withProxy(String host, int port)
    {
        return new Tor(host, port, false, SocksPolicy.LOCALHOST_ONLY);
    }

    /**
     * Proxy mode – shortcut for the default local proxy at
     * {@code 127.0.0.1:}{@value #DEFAULT_SOCKS_PORT}.
     *
     * @return a new {@code Tor} instance in proxy mode
     */
    public static Tor withLocalProxy()
    {
        return withProxy(IP_127_0_0_1, DEFAULT_SOCKS_PORT);
    }

    /**
     * Managed mode – create an instance that will extract the bundled Tor binary
     * and launch it on a random free port above 19050.
     * Call {@link #start()} to start the process.
     *
     * @return a new {@code Tor} instance in managed mode
     */
    public static Tor managed()
    {
        return new Tor(IP_127_0_0_1, Networks.findFreePort(19050), true, SocksPolicy.LOCALHOST_ONLY);
    }

    /**
     * Managed mode – create an instance that will extract the bundled Tor binary
     * and launch it on the specified port.
     * Call {@link #start()} to start the process.
     *
     * @param socksPort local port Tor will listen on
     * @return a new {@code Tor} instance in managed mode
     */
    public static Tor managed(int socksPort)
    {
        return new Tor(IP_127_0_0_1, socksPort, true, SocksPolicy.LOCALHOST_ONLY);
    }

    /**
     * Managed mode – create an instance that will extract the bundled Tor binary
     * and launch it on the specified port with the given {@link SocksPolicy}.
     * Call {@link #start()} to start the process.
     *
     * @param socksPort   local port Tor will listen on
     * @param socksPolicy access policy for the SOCKS port
     * @return a new {@code Tor} instance in managed mode
     */
    public static Tor managed(int socksPort, SocksPolicy socksPolicy)
    {
        return new Tor(IP_127_0_0_1, socksPort, true, socksPolicy);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this instance owns and manages a Tor process.
     *
     * @return {@code true} for managed mode, {@code false} for proxy mode
     */
    public boolean isManaged()
    {
        return managed;
    }

    /**
     * Returns {@code true} if the managed Tor process is currently running.
     * Always returns {@code false} in proxy mode.
     *
     * @return {@code true} if the process is running
     */
    public boolean isRunning()
    {
        return running.get();
    }

    // ── Lifecycle (managed mode) ─────────────────────────────────────────────

    /**
     * Extract the Tor binary, start the Tor process, and block until Tor
     * finishes bootstrapping or the timeout expires.
     *
     * <p>The following hardening steps are applied automatically in managed mode:
     * <ul>
     *   <li>The {@code DataDirectory} is created with {@code chmod 0700} on POSIX.</li>
     *   <li>The SOCKS port accepts connections from {@code 127.0.0.1} only
     *       (when {@link SocksPolicy#LOCALHOST_ONLY}).</li>
     *   <li>{@code SAFECOOKIE} authentication is enabled; the cookie file is read
     *       into JVM memory and deleted from disk immediately.</li>
     *   <li>The Control Port connection is closed as soon as bootstrap completes.
     *       The in-memory secret is wiped when {@link #stop()} is called.</li>
     * </ul>
     *
     * <p>Calling {@code start()} on an already-running instance is a no-op.
     *
     * @throws IllegalStateException if called on a proxy-mode instance
     * @throws IOException           if the binary cannot be extracted, the process
     *                               fails to start, the cookie cannot be read, or
     *                               bootstrap fails
     * @throws InterruptedException  if the calling thread is interrupted while
     *                               waiting for bootstrap
     * @throws TimeoutException      if Tor does not bootstrap within
     *                               {@value #BOOTSTRAP_TIMEOUT_MILLIS} ms
     */
    public boolean start() throws IOException, InterruptedException, TimeoutException
    {
        if (!managed)
        {
            throw new IllegalStateException("start() is only valid in managed mode");
        }
        if (running.get())
        {
            LOG.info("Tor already running");
            return true;
        }

        torDataDir = createTempDir("tor-data-");
        restrictDirectory(torDataDir);          // chmod 0700 on POSIX; no-op on Windows

        if (!resolveTorCommand())
        {
            return false;
        }

        // Allocate an ephemeral TCP port for the Control Port on all platforms.
        controlPort = Networks.findFreePort(19051);

        File torrc = writeTorrc(torDataDir, port, controlPort, socksPolicy);

        LOG.info(() -> "Starting Tor: " + torCommand + " -f " + torrc);
        ProcessBuilder pb = new ProcessBuilder(torCommand, "-f", torrc.getAbsolutePath());
        pb.redirectErrorStream(true);
        pb.environment().put("HOME", torDataDir.getAbsolutePath());

        torProcess = pb.start();
        running.set(true);

        // Stream Tor output to the logger in a background daemon thread
        Thread logThread = new Thread(() ->
        {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(torProcess.getInputStream())))
            {
                String line;
                while ((line = r.readLine()) != null)
                {
                    LOG.fine("[tor] " + line);
                }
            }
            catch (IOException ignored) {}
        }, "tor-log");
        logThread.setDaemon(true);
        logThread.start();

        // Wait for the cookie file, read it into memory, delete it from disk.
        cookieSecret = readAndDeleteCookie(torDataDir);
        waitForBootstrap(cookieSecret);

        LOG.info(() -> "Tor ready on " + IP_127_0_0_1 + ":" + port);
        return true;
    }

    /**
     * Stop the managed Tor process, wipe the in-memory cookie secret, and delete
     * all temporary files created by this instance.
     * Safe to call even if Tor is not running.
     *
     * <p>Calling {@code stop()} on a proxy-mode instance is a no-op.
     */
    public void stop()
    {
        if (!managed)
        {
            return;
        }
        if (torProcess != null)
        {
            torProcess.destroy();
            try
            {
                torProcess.waitFor(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            torProcess = null;
        }
        running.set(false);

        if (cookieSecret != null)
        {
            Arrays.fill(cookieSecret, (byte) 0);
            cookieSecret = null;
        }
        controlPort = -1;

        deleteDirectory(torDataDir);
        torDataDir = null;

        if (extractedBinary != null)
        {
            extractedBinary.delete();
            extractedBinary = null;
        }
        torCommand = null;
        LOG.info("Tor stopped");
    }

    // ── Binary resolution (private) ──────────────────────────────────────────

    /**
     * Populate {@link #torCommand} (and {@link #extractedBinary} when applicable).
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Bundled classpath resource from an {@code org.briarproject:tor-*} JAR.</li>
     *   <li>{@code TOR_BINARY} environment variable.</li>
     *   <li>Bare name {@code "tor"} / {@code "tor.exe"} on the system {@code PATH}.</li>
     *   <li>Well-known platform-specific installation paths.</li>
     * </ol>
     *
     * @return {@code true} if a usable binary was found
     * @throws IOException if the {@code TOR_BINARY} env var is set but not executable
     */
    private boolean resolveTorCommand() throws IOException
    {
        // 1 – bundled classpath resource
        String resourcePath = detectBundledResourcePath();
        if (resourcePath != null)
        {
            InputStream in = Tor.class.getResourceAsStream(resourcePath);
            if (in != null)
            {
                extractedBinary = extractToTemp(in, resourcePath.endsWith(".exe") ? "tor.exe" : "tor");
                torCommand = extractedBinary.getAbsolutePath();
                return true;
            }
        }

        LOG.warning("Bundled Tor binary not found in classpath – falling back to system binary");

        // 2 – TOR_BINARY environment variable
        String envBin = System.getenv("TOR_BINARY");
        if (envBin != null && !envBin.isEmpty())
        {
            if (!new File(envBin).canExecute())
            {
                throw new IOException("TOR_BINARY is set but not executable: " + envBin);
            }
            torCommand = envBin;
            return true;
        }

        // 3 – bare name resolved via PATH
        if (Exec.isBinaryOnPath("tor"))
        {
            torCommand = os.isWindows() ? "tor.exe" : "tor";
            return true;
        }

        // 4 – well-known absolute installation paths
        for (String candidate : systemTorCandidates())
        {
            if (candidate != null && new File(candidate).canExecute())
            {
                torCommand = candidate;
                return true;
            }
        }

        throw new IOException("Tor binary not found. Add org.briarproject:tor-* to your Gradle dependencies, install Tor, or set the TOR_BINARY environment variable.");
    }

    /**
     * Map the current OS and CPU architecture to the classpath resource path
     * used by the {@code org.briarproject:tor-*} JAR files.
     *
     * @return resource path string, or {@code null} if not supported
     */
    private static String detectBundledResourcePath()
    {
        String normOs;
        if(os.isLinux())
        {
            normOs = "linux";
        }
        else if(os.isMacos())
        {
            normOs = "macos";
        }
        else if(os.isWindows())
        {
            normOs = "windows";
        }
        else
        {
            LOG.warning(() -> "Unknown OS: " + os);
            return null;
        }

        String normArch;
        if (os.isArm64())
        {
            normArch = "aarch64";
        }
        else if (os.isX86_64())
        {
            normArch = "x86_64";
        }
        else
        {
            normArch = os.getArch();
        }
        
        String key  = normOs + "/" + normArch;
        String path = BUNDLED_BINARY_PATHS.get(key);

        if (path == null)
        {
            LOG.warning(() -> "No bundled binary mapping for: " + key);
        }
        return path;
    }

    /**
     * Copy an {@link InputStream} to a temporary file, mark it executable,
     * and return it.
     *
     * @param in       source stream (closed after copying)
     * @param filename desired file name suffix
     * @return the extracted, executable temporary {@link File}
     * @throws IOException if the file cannot be written or made executable
     */
    private static File extractToTemp(InputStream in, String filename) throws IOException
    {
        File tmp = File.createTempFile("tor-extracted-", "-" + filename);
        tmp.deleteOnExit();

        try (InputStream src = in; FileOutputStream out = new FileOutputStream(tmp))
        {
            byte[] buf = new byte[8192];
            int n;
            while ((n = src.read(buf)) != -1)
            {
                out.write(buf, 0, n);
            }
        }

        if (!os.isWindows())
        {
            try
            {
                Set<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(tmp.toPath()));
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                Files.setPosixFilePermissions(tmp.toPath(), perms);
            }
            catch (UnsupportedOperationException ignored) 
            {
            }
        }

        LOG.info(() -> "Extracted Tor binary to: " + tmp.getAbsolutePath());
        return tmp;
    }

    /**
     * Return a list of absolute paths where Tor is commonly installed on the
     * current platform.
     *
     * @return platform-specific candidate paths (some entries may not exist)
     */
    private static List<String> systemTorCandidates()
    {
        if(os.isWindows())
        {
            return Arrays.asList(
                "C:\\Program Files\\Tor\\tor.exe",
                "C:\\Program Files (x86)\\Tor\\tor.exe",
                System.getenv("APPDATA")      + "\\tor\\tor.exe",
                System.getenv("LOCALAPPDATA") + "\\Tor Browser\\Browser\\TorBrowser\\Tor\\tor.exe"
            );
        }
        else if (os.isMacos())
        {
            return Arrays.asList(
                "/usr/local/bin/tor",
                "/opt/homebrew/bin/tor",
                "/opt/local/bin/tor",
                "/Applications/Tor Browser.app/Contents/MacOS/Tor/tor.real"
            );
        }
        else
        {
            return Arrays.asList(
                "/usr/bin/tor",
                "/usr/sbin/tor",
                "/usr/local/bin/tor",
                "/snap/bin/tor"
            );
        }
    }

    // ── Access-restriction helpers (private) ─────────────────────────────────

    /**
     * Apply {@code chmod 0700} to {@code dir} so that only the owning user can
     * read, write, or enter it. No-op on non-POSIX systems.
     *
     * @param dir directory to restrict
     */
    private static void restrictDirectory(File dir)
    {
        try
        {
            Set<PosixFilePermission> ownerOnly = EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);
            Files.setPosixFilePermissions(dir.toPath(), ownerOnly);
            LOG.fine(() -> "DataDirectory restricted to 0700: " + dir);
        }
        catch (UnsupportedOperationException ignored)
        {
            LOG.fine(() -> "DataDirectory chmod 0700 skipped (non-POSIX): " + dir);
        }
        catch (IOException e)
        {
            LOG.warning(() -> "Could not restrict DataDirectory permissions: " + e.getMessage());
        }
    }

    /**
     * Wait for Tor to write the {@value #COOKIE_FILE_NAME} file, read it into
     * JVM memory, and <em>immediately delete it from disk</em>.
     *
     * @param dataDir the Tor {@code DataDirectory}
     * @return the cookie bytes (exactly {@value #COOKIE_LENGTH} bytes)
     * @throws IOException if the file does not appear, cannot be read,
     *                     or has an unexpected length
     */
    private static byte[] readAndDeleteCookie(File dataDir) throws IOException
    {
        File cookieFile = new File(dataDir, COOKIE_FILE_NAME);

        long deadline = System.nanoTime() + COOKIE_WAIT_NANOS;
        while (!cookieFile.exists() && System.nanoTime() < deadline)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for cookie file");
            }
        }

        if (!cookieFile.exists())
        {
            throw new IOException(
                "Control cookie file not found after " + COOKIE_WAIT_MILLIS + " ms: " + cookieFile);
        }

        byte[] cookie = Files.readAllBytes(cookieFile.toPath());

        if (!cookieFile.delete())
        {
            LOG.warning("Could not delete cookie file from disk: " + cookieFile);
        }
        else
        {
            LOG.fine("Cookie file deleted from disk; secret is now in memory only");
        }

        if (cookie.length != COOKIE_LENGTH)
        {
            Arrays.fill(cookie, (byte) 0);
            throw new IOException(
                "Unexpected cookie length: expected " + COOKIE_LENGTH + " bytes, got " + cookie.length);
        }

        return cookie;
    }

    /**
     * Connect to the Tor Control Port, authenticate with the SAFECOOKIE secret,
     * subscribe to bootstrap events, and block until {@code BOOTSTRAP PROGRESS=100}
     * or the timeout expires.
     *
     * <p>Control Protocol exchange:
     * <pre>
     *   → AUTHENTICATE &lt;hex-cookie&gt;
     *   ← 250 OK
     *   → SETEVENTS EXTENDED STATUS_CLIENT
     *   ← 250 OK
     *   ← 650 STATUS_CLIENT NOTICE BOOTSTRAP PROGRESS=100 TAG=done SUMMARY="Done"
     *   → QUIT
     * </pre>
     *
     * @param cookie the 32-byte SAFECOOKIE secret
     * @throws IOException          if the Control Port cannot be reached or auth fails
     * @throws InterruptedException if the calling thread is interrupted
     * @throws TimeoutException     if bootstrap does not complete in time
     */
    private void waitForBootstrap(byte[] cookie) throws IOException, InterruptedException, TimeoutException
    {
        long deadline = System.nanoTime() + BOOTSTRAP_TIMEOUT_NANOS;

        Socket ctrlSocket = connectToControlPort(deadline);

        try (Socket      s = ctrlSocket;
             PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.US_ASCII)), true);
             BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII)))
        {
            w.println("AUTHENTICATE " + Hex.encode(cookie, true));
            String authReply = r.readLine();
            if (authReply == null || !authReply.startsWith("250"))
            {
                throw new IOException("Control Port authentication failed: " + authReply);
            }
            LOG.fine("Control Port: authenticated");

            w.println("SETEVENTS EXTENDED STATUS_CLIENT");
            String eventsReply = r.readLine();
            if (eventsReply == null || !eventsReply.startsWith("250"))
            {
                throw new IOException("SETEVENTS failed: " + eventsReply);
            }

            String line;
            while ((line = r.readLine()) != null)
            {
                LOG.fine("[ctrl] " + line);

                if (line.contains("BOOTSTRAP") && line.contains("PROGRESS=100"))
                {
                    LOG.fine("Control Port: bootstrap complete");
                    w.println("QUIT");
                    return;
                }

                if (!torProcess.isAlive())
                {
                    throw new IOException("Tor process exited unexpectedly before bootstrapping");
                }

                if (System.nanoTime() >= deadline)
                {
                    throw new TimeoutException("Tor did not bootstrap within " + BOOTSTRAP_TIMEOUT_MILLIS + " ms");
                }
            }

            throw new IOException("Control Port stream closed before bootstrap completed");
        }
    }

    /**
     * Attempt to open a TCP connection to {@code 127.0.0.1:}{@link #controlPort},
     * retrying every 500 ms until the deadline.
     *
     * @param deadline absolute nanosecond deadline ({@link System#nanoTime()})
     * @return a connected {@link Socket} to the Control Port
     * @throws IOException if no connection can be established before the deadline
     */
    private Socket connectToControlPort(long deadline) throws IOException
    {
        IOException lastException = null;
        while (System.nanoTime() < deadline)
        {
            try
            {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(IP_127_0_0_1, controlPort), 2_000);
                return s;
            }
            catch (IOException e)
            {
                lastException = e;
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for Control Port");
                }
            }
        }
        throw new IOException("Control Port not reachable after " + BOOTSTRAP_TIMEOUT_MILLIS + " ms", lastException);
    }

    /**
     * Write the {@code torrc} configuration file into {@code dataDir}.
     *
     * @param dataDir     directory where {@code torrc} will be written
     * @param socksPort   SOCKS5 port Tor should listen on
     * @param ctrlPortTcp TCP port for the Control Port (all platforms)
     * @param socksPolicy access policy for the SOCKS port
     * @return the written {@link File}
     * @throws IOException if the file cannot be created or written
     */
    private static File writeTorrc(File dataDir, int socksPort, int ctrlPortTcp, SocksPolicy socksPolicy) throws IOException
    {
        File   torrc      = new File(dataDir, "torrc");
        String cookiePath = new File(dataDir, COOKIE_FILE_NAME).getAbsolutePath();

        String controlPortLines;
        if(os.isWindows())
        {
            controlPortLines = "ControlPort " + ctrlPortTcp + NL;
        }
        else
        {
            String sockPath = new File(dataDir, CONTROL_SOCKET_NAME).getAbsolutePath();
            controlPortLines =
                "ControlPort unix:" + sockPath + NL +
                "ControlPort "      + ctrlPortTcp + NL;
        }

        String socksPolicyLines = (socksPolicy == SocksPolicy.LOCALHOST_ONLY)
            ? "SOCKSPolicy accept " + IP_127_0_0_1 + NL + "SOCKSPolicy reject *" + NL
            : "";

        String content =
            "SocksPort "                 + socksPort                 + NL
          + socksPolicyLines
          + "DataDirectory "             + dataDir.getAbsolutePath() + NL
          + controlPortLines
          + "CookieAuthentication 1"                                  + NL
          + "CookieAuthFileGroupReadable 0"                           + NL
          + "CookieAuthFile "            + cookiePath                 + NL
          + "Log notice stdout"                                       + NL
          + "AvoidDiskWrites 1";

        try (FileOutputStream fos = new FileOutputStream(torrc))
        {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return torrc;
    }

    // ── Temp-file / directory utilities ─────────────────────────────────────

    /**
     * Create a temporary directory with the given prefix.
     *
     * @param prefix prefix for the directory name
     * @return the created directory as a {@link File}
     * @throws IOException if the directory cannot be created
     */
    private static File createTempDir(String prefix) throws IOException
    {
        Path tmp = Files.createTempDirectory(prefix);
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }

    /**
     * Recursively delete {@code dir} and all its contents.
     * A {@code null} or non-existent argument is silently ignored.
     *
     * @param dir directory to delete (may be {@code null})
     */
    private static void deleteDirectory(File dir)
    {
        if (dir == null || !dir.exists())
        {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null)
        {
            for (File child : children)
            {
                deleteDirectory(child);
            }
        }
        dir.delete();
    }
}

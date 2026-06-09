/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.net.stun;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal STUN server implementing RFC 5389 Binding Request/Response over UDP.
 *
 * <p>Responds to Binding Requests with the client's public (server-reflexive)
 * address encoded in an XOR-MAPPED-ADDRESS attribute, plus a MAPPED-ADDRESS
 * attribute for RFC 3489 backward compatibility.
 *
 * <p>No external dependencies — pure Java 8 SE.
 *
 * <h3>Usage</h3>
 * <pre>
 *   // Default port 3478, single-threaded:
 *   StunServer server = new StunServer();
 *   server.start();
 *
 *   // Custom port, multi-threaded:
 *   StunServer server = new StunServer(3478, 4);
 *   server.start();
 *
 *   // Stop gracefully:
 *   server.stop();
 * </pre>
 *
 * @see <a href="https://tools.ietf.org/html/rfc5389">RFC 5389 – STUN</a>
 * @see StunClient
 */
public final class StunServer {

    // ── RFC 5389 constants ────────────────────────────────────────────────────

    /** STUN magic cookie (fixed value defined by RFC 5389 §6). */
    static final int MAGIC_COOKIE = 0x2112A442;

    /** Binding Request message type. */
    static final short MSG_TYPE_BINDING_REQUEST  = 0x0001;

    /** Binding Response (success) message type. */
    static final short MSG_TYPE_BINDING_RESPONSE = 0x0101;

    /** Binding Error Response message type. */
    static final short MSG_TYPE_BINDING_ERROR    = 0x0111;

    /** XOR-MAPPED-ADDRESS attribute type (RFC 5389 §15.2). */
    static final short ATTR_XOR_MAPPED_ADDRESS = 0x0020;

    /** MAPPED-ADDRESS attribute type (RFC 3489 §11.2.1 – for backward compat). */
    static final short ATTR_MAPPED_ADDRESS = 0x0001;

    /** ERROR-CODE attribute type (RFC 5389 §15.6). */
    static final short ATTR_ERROR_CODE = 0x0009;

    /** Address family: IPv4. */
    static final byte FAMILY_IPV4 = 0x01;

    /** STUN header size in bytes. */
    static final int HEADER_LENGTH = 20;

    /** Transaction ID length in bytes (96 bits). */
    static final int TRANSACTION_ID_LENGTH = 12;

    /** Maximum UDP datagram size we will read. */
    private static final int RECV_BUFFER_SIZE = 576;

    // ── Defaults ─────────────────────────────────────────────────────────────

    /** Standard STUN port (IANA assigned). */
    public static final int DEFAULT_PORT = 3478;

    private static final Logger LOG = Logger.getLogger(StunServer.class.getName());

    // ── State ─────────────────────────────────────────────────────────────────

    private final int            port;
    private final int            workerThreads;
    private volatile boolean     running;
    private DatagramSocket       socket;
    private Thread               listenerThread;
    private ExecutorService      workers;

    // ── Construction ─────────────────────────────────────────────────────────

    /**
     * Creates a server on {@link #DEFAULT_PORT} with a single worker thread.
     */
    public StunServer() {
        this(DEFAULT_PORT, 1);
    }

    /**
     * Creates a server on the given port with a single worker thread.
     *
     * @param port UDP port to listen on (1–65535)
     */
    public StunServer(int port) {
        this(port, 1);
    }

    /**
     * Creates a server on the given port with the specified number of worker threads.
     *
     * <p>The listener thread reads datagrams and dispatches them to the worker pool,
     * so the listener is never blocked by response I/O.
     *
     * @param port          UDP port to listen on (1–65535)
     * @param workerThreads number of worker threads for response dispatch (≥ 1)
     */
    public StunServer(int port, int workerThreads) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        if (workerThreads < 1) {
            throw new IllegalArgumentException("workerThreads must be >= 1, got: " + workerThreads);
        }
        this.port          = port;
        this.workerThreads = workerThreads;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Starts the server. Binds the UDP socket and begins accepting requests.
     * Returns immediately; the server runs on background threads.
     *
     * @throws IOException          if the socket cannot be bound
     * @throws IllegalStateException if already running
     */
    public synchronized void start() throws IOException {
        if (running) throw new IllegalStateException("Server is already running");

        socket  = new DatagramSocket(port);
        running = true;
        workers = Executors.newFixedThreadPool(workerThreads, r -> {
            Thread t = new Thread(r, "stun-worker");
            t.setDaemon(true);
            return t;
        });

        listenerThread = new Thread(this::listenLoop, "stun-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        LOG.info("STUN server listening on UDP port " + port
                + " (" + workerThreads + " worker(s))");
    }

    /**
     * Stops the server gracefully, closing the socket and waiting for in-flight
     * responses to complete (up to 5 seconds).
     */
    public synchronized void stop() {
        if (!running) return;
        running = false;
        socket.close();       // interrupts the blocking receive() in listenLoop
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("STUN server stopped");
    }

    /** Returns {@code true} if the server is currently running. */
    public boolean isRunning() { return running; }

    /** Returns the UDP port this server is bound to. */
    public int getPort() { return port; }

    // ── Listener loop ─────────────────────────────────────────────────────────

    private void listenLoop() {
        byte[] buf = new byte[RECV_BUFFER_SIZE];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                // Copy only the received bytes so the worker has its own buffer
                byte[] data   = Arrays.copyOf(packet.getData(), packet.getLength());
                InetAddress clientAddr = packet.getAddress();
                int         clientPort = packet.getPort();
                workers.submit(() -> handleRequest(data, clientAddr, clientPort));
            } catch (IOException e) {
                if (running) {
                    LOG.log(Level.WARNING, "Error receiving datagram", e);
                }
                // If !running, the socket was closed intentionally — exit quietly
            }
        }
    }

    // ── Request handling ──────────────────────────────────────────────────────

    /**
     * Processes a single STUN datagram. Validates the message, then sends
     * a Binding Response or an Error Response back to the client.
     *
     * <p>Package-private for unit testing.
     */
    void handleRequest(byte[] data, InetAddress clientAddr, int clientPort) {
        try {
            byte[] response = buildResponse(data, clientAddr, clientPort);
            DatagramPacket out = new DatagramPacket(response, response.length, clientAddr, clientPort);
            socket.send(out);
        } catch (InvalidStunMessageException e) {
            LOG.fine("Invalid STUN message from " + clientAddr + ":" + clientPort + " — " + e.getMessage());
            // RFC 5389: silently discard unknown/malformed messages
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to send response to " + clientAddr + ":" + clientPort, e);
        }
    }

    // ── Response building (package-private for unit tests) ────────────────────

    /**
     * Validates a Binding Request and builds the corresponding Binding Response.
     *
     * <p>Response attributes (in order):
     * <ol>
     *   <li>XOR-MAPPED-ADDRESS — RFC 5389 §15.2</li>
     *   <li>MAPPED-ADDRESS     — RFC 3489 §11.2.1 (backward compat)</li>
     * </ol>
     *
     * @param request    raw UDP payload received from the client
     * @param clientAddr client's IP address as seen by this server
     * @param clientPort client's UDP port as seen by this server
     * @return serialized Binding Response ready to send
     * @throws InvalidStunMessageException if the request is malformed or not a Binding Request
     */
    static byte[] buildResponse(byte[] request, InetAddress clientAddr, int clientPort)
            throws InvalidStunMessageException {

        validateRequest(request);

        byte[] transactionId = extractTransactionId(request);
        byte[] xorAttr       = buildXorMappedAddress(clientAddr, clientPort);
        byte[] mapAttr       = buildMappedAddress(clientAddr, clientPort);

        int totalAttrLen = xorAttr.length + mapAttr.length;

        ByteBuffer response = ByteBuffer.allocate(HEADER_LENGTH + totalAttrLen);
        response.putShort(MSG_TYPE_BINDING_RESPONSE);
        response.putShort((short) totalAttrLen);
        response.putInt(MAGIC_COOKIE);
        response.put(transactionId);
        response.put(xorAttr);
        response.put(mapAttr);
        return response.array();
    }

    // ── Attribute builders ────────────────────────────────────────────────────

    /**
     * Builds an XOR-MAPPED-ADDRESS attribute (RFC 5389 §15.2).
     *
     * <pre>
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |           Type (0x0020)       |            Length (8)         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |x x x x x x x x|    Family     |         X-Port               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                       X-Address                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     */
    static byte[] buildXorMappedAddress(InetAddress addr, int port) {
        byte[] ip         = addr.getAddress();          // 4 bytes for IPv4
        byte[] cookieBytes = ByteBuffer.allocate(4).putInt(MAGIC_COOKIE).array();
        int    xorPort    = port ^ (MAGIC_COOKIE >>> 16);

        ByteBuffer buf = ByteBuffer.allocate(4 + 8);   // TLV header (4) + value (8)
        buf.putShort(ATTR_XOR_MAPPED_ADDRESS);
        buf.putShort((short) 8);
        buf.put((byte) 0x00);                           // reserved
        buf.put(FAMILY_IPV4);
        buf.put((byte) ((xorPort >> 8) & 0xFF));
        buf.put((byte) (xorPort & 0xFF));
        for (int i = 0; i < 4; i++) buf.put((byte) (ip[i] ^ cookieBytes[i]));
        return buf.array();
    }

    /**
     * Builds a MAPPED-ADDRESS attribute (RFC 3489 §11.2.1) — plain, no XOR.
     */
    static byte[] buildMappedAddress(InetAddress addr, int port) {
        byte[] ip = addr.getAddress();

        ByteBuffer buf = ByteBuffer.allocate(4 + 8);
        buf.putShort(ATTR_MAPPED_ADDRESS);
        buf.putShort((short) 8);
        buf.put((byte) 0x00);                           // reserved
        buf.put(FAMILY_IPV4);
        buf.put((byte) ((port >> 8) & 0xFF));
        buf.put((byte) (port & 0xFF));
        buf.put(ip);
        return buf.array();
    }

    // ── Request validation ────────────────────────────────────────────────────

    /**
     * Validates the basic structure of a STUN message.
     *
     * @throws InvalidStunMessageException on any structural violation
     */
    static void validateRequest(byte[] data) throws InvalidStunMessageException {
        if (data == null || data.length < HEADER_LENGTH) {
            throw new InvalidStunMessageException(
                "Message too short: " + (data == null ? "null" : data.length) + " bytes");
        }

        ByteBuffer buf = ByteBuffer.wrap(data);

        // RFC 5389 §6: the two most-significant bits of the message type MUST be 0
        int firstByte = buf.get(0) & 0xFF;
        if ((firstByte & 0xC0) != 0) {
            throw new InvalidStunMessageException(
                String.format("Top two bits of message type must be 0, got: 0x%02X", firstByte));
        }

        short msgType = buf.getShort();
        if (msgType != MSG_TYPE_BINDING_REQUEST) {
            throw new InvalidStunMessageException(
                String.format("Unsupported message type: 0x%04X", msgType & 0xFFFF));
        }

        int declaredLength = buf.getShort() & 0xFFFF;
        if (data.length < HEADER_LENGTH + declaredLength) {
            throw new InvalidStunMessageException(
                "Payload shorter than declared length: " + data.length
                + " < " + (HEADER_LENGTH + declaredLength));
        }

        int cookie = buf.getInt();
        if (cookie != MAGIC_COOKIE) {
            throw new InvalidStunMessageException(
                String.format("Invalid magic cookie: 0x%08X", cookie));
        }
    }

    /** Extracts the 12-byte transaction ID from a validated STUN header. */
    static byte[] extractTransactionId(byte[] data) {
        byte[] txId = new byte[TRANSACTION_ID_LENGTH];
        System.arraycopy(data, 8, txId, 0, TRANSACTION_ID_LENGTH);
        return txId;
    }

    // ── Exception type ────────────────────────────────────────────────────────

    /**
     * Thrown when a received datagram is not a valid STUN Binding Request.
     * RFC 5389 mandates silent discard for unknown/malformed messages.
     */
    static final class InvalidStunMessageException extends Exception {
        InvalidStunMessageException(String message) { super(message); }
    }
}

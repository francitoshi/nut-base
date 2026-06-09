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
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Minimal STUN client implementing RFC 5389 Binding Request/Response.
 *
 * <p>Discovers the public IP address and port (server-reflexive candidate)
 * of this host as seen by a STUN server. Supports both the default Google
 * public STUN servers and any custom STUN server address.
 *
 * <p>No external dependencies — pure Java 8 SE.
 *
 * <p>Usage:
 * <pre>
 *   // Using default Google STUN servers (tries each in order):
 *   StunClient.MappedAddress addr = StunClient.discover();
 *
 *   // Using a custom STUN server:
 *   StunClient.MappedAddress addr = StunClient.discover("stun.example.com", 3478);
 * </pre>
 *
 * @see <a href="https://tools.ietf.org/html/rfc5389">RFC 5389 – STUN</a>
 */
public final class StunClient 
{

    // ── RFC 5389 constants ────────────────────────────────────────────────────

    /** STUN magic cookie (fixed value defined by RFC 5389). */
    private static final int MAGIC_COOKIE = 0x2112A442;

    /** Binding Request message type. */
    private static final short MSG_TYPE_BINDING_REQUEST  = 0x0001;

    /** Binding Response (success) message type. */
    private static final short MSG_TYPE_BINDING_RESPONSE = 0x0101;

    /** MAPPED-ADDRESS attribute type (RFC 3489 compatibility). */
    private static final short ATTR_MAPPED_ADDRESS     = 0x0001;

    /** XOR-MAPPED-ADDRESS attribute type (RFC 5389). */
    private static final short ATTR_XOR_MAPPED_ADDRESS = 0x0020;

    /** Address family: IPv4. */
    private static final byte FAMILY_IPV4 = 0x01;

    /** Total size of a STUN message header in bytes. */
    private static final int HEADER_LENGTH = 20;

    /** Transaction ID length in bytes (96 bits). */
    private static final int TRANSACTION_ID_LENGTH = 12;

    // ── Defaults ─────────────────────────────────────────────────────────────

    /** Default timeout in milliseconds for a single STUN server attempt. */
    public static final int DEFAULT_TIMEOUT_MS = 3_000;

    /** Google public STUN servers, tried in order until one responds. */
    public static final String[][] GOOGLE_STUN_SERVERS = 
    {
        {"stun.l.google.com",  "19302"},
        {"stun1.l.google.com", "3478"},
        {"stun2.l.google.com", "19302"},
        {"stun3.l.google.com", "3478"},
        {"stun4.l.google.com", "19302"},
    };

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── Construction ─────────────────────────────────────────────────────────

    private StunClient() { /* utility class */ }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Discovers the public (server-reflexive) address using Google STUN servers.
     * Tries each server in {@link #GOOGLE_STUN_SERVERS} until one succeeds.
     *
     * @return the discovered {@link MappedAddress}
     * @throws IOException if no server could be reached or the response is invalid
     */
    public static MappedAddress discover() throws IOException
    {
        IOException last = null;
        for (String[] server : GOOGLE_STUN_SERVERS)
        {
            try
            {
                return discover(server[0], Integer.parseInt(server[1]), DEFAULT_TIMEOUT_MS);
            } 
            catch (IOException e)
            {
                last = e;
            }
        }
        throw new IOException("All Google STUN servers failed", last);
    }

    /**
     * Discovers the public address using a custom STUN server with default timeout.
     *
     * @param host STUN server hostname or IP
     * @param port STUN server port (typically 3478)
     * @return the discovered {@link MappedAddress}
     * @throws IOException on network or protocol error
     */
    public static MappedAddress discover(String host, int port) throws IOException
    {
        return discover(host, port, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Discovers the public address using a custom STUN server.
     *
     * @param host      STUN server hostname or IP
     * @param port      STUN server port (typically 3478)
     * @param timeoutMs socket read timeout in milliseconds
     * @return the discovered {@link MappedAddress}
     * @throws IllegalArgumentException if host/port/timeout are invalid
     * @throws IOException on network or protocol error
     */
    public static MappedAddress discover(String host, int port, int timeoutMs) throws IOException 
    {
        validateArgs(host, port, timeoutMs);

        byte[] transactionId = generateTransactionId();
        byte[] request       = buildBindingRequest(transactionId);

        InetAddress serverAddr = InetAddress.getByName(host);

        try (DatagramSocket socket = new DatagramSocket())
        {
            socket.setSoTimeout(timeoutMs);

            DatagramPacket outPacket = new DatagramPacket(request, request.length, serverAddr, port);
            socket.send(outPacket);

            byte[] buf       = new byte[512];
            DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
            socket.receive(inPacket);

            byte[] response = Arrays.copyOf(inPacket.getData(), inPacket.getLength());
            return parseResponse(response, transactionId);
        }
    }

    // ── Packet building ───────────────────────────────────────────────────────

    /**
     * Builds a 20-byte STUN Binding Request.
     *
     * <pre>
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |0 0|     STUN Message Type     |         Message Length        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Magic Cookie                          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                                                               |
     * |                     Transaction ID (96 bits)                  |
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     */
    static byte[] buildBindingRequest(byte[] transactionId) 
    {
        if (transactionId == null || transactionId.length != TRANSACTION_ID_LENGTH) 
        {
            throw new IllegalArgumentException("Transaction ID must be exactly 12 bytes");
        }
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH);
        buf.putShort(MSG_TYPE_BINDING_REQUEST);
        buf.putShort((short) 0);           // message length: no attributes
        buf.putInt(MAGIC_COOKIE);
        buf.put(transactionId);
        return buf.array();
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    /**
     * Parses a STUN Binding Response, extracting the mapped address.
     * Prefers XOR-MAPPED-ADDRESS (RFC 5389) over MAPPED-ADDRESS (RFC 3489).
     *
     * @param response      raw UDP payload
     * @param transactionId the transaction ID sent in the request
     * @return the parsed {@link MappedAddress}
     * @throws IOException if the response is malformed or the transaction ID mismatches
     */
    static MappedAddress parseResponse(byte[] response, byte[] transactionId) throws IOException
    {
        if (response.length < HEADER_LENGTH)
        {
            throw new IOException("STUN response too short: " + response.length + " bytes");
        }

        ByteBuffer buf = ByteBuffer.wrap(response);

        short msgType = buf.getShort();
        if (msgType != MSG_TYPE_BINDING_RESPONSE)
        {
            throw new IOException(String.format("Unexpected STUN message type: 0x%04X", msgType & 0xFFFF));
        }

        int msgLength = buf.getShort() & 0xFFFF;
        int cookie    = buf.getInt();

        if (cookie != MAGIC_COOKIE)
        {
            throw new IOException(String.format("Invalid magic cookie: 0x%08X", cookie));
        }

        byte[] respTxId = new byte[TRANSACTION_ID_LENGTH];
        buf.get(respTxId);
        if (!Arrays.equals(respTxId, transactionId))
        {
            throw new IOException("Transaction ID mismatch — possible spoofed response");
        }

        if (response.length < HEADER_LENGTH + msgLength)
        {
            throw new IOException("STUN response payload shorter than declared length");
        }

        // Scan attributes; prefer XOR-MAPPED-ADDRESS
        MappedAddress mappedAddress    = null;
        MappedAddress xorMappedAddress = null;

        int limit = HEADER_LENGTH + msgLength;
        while (buf.position() < limit)
        {
            if (buf.remaining() < 4) break;

            short attrType   = buf.getShort();
            int   attrLength = buf.getShort() & 0xFFFF;

            if (buf.remaining() < attrLength) break;

            byte[] attrValue = new byte[attrLength];
            buf.get(attrValue);

            // Attributes are padded to 4-byte boundaries
            int padding = (4 - (attrLength % 4)) % 4;
            if (buf.remaining() >= padding) buf.position(buf.position() + padding);

            if (attrType == ATTR_XOR_MAPPED_ADDRESS)
            {
                xorMappedAddress = parseXorMappedAddress(attrValue);
            } 
            else if (attrType == ATTR_MAPPED_ADDRESS && mappedAddress == null)
            {
                mappedAddress = parseMappedAddress(attrValue);
            }
        }

        MappedAddress result = (xorMappedAddress != null) ? xorMappedAddress : mappedAddress;
        if (result == null)
        {
            throw new IOException("No MAPPED-ADDRESS or XOR-MAPPED-ADDRESS found in STUN response");
        }
        return result;
    }

    /**
     * Parses an XOR-MAPPED-ADDRESS attribute (RFC 5389 §15.2).
     * The IP and port are XOR-ed with the magic cookie to obfuscate them.
     */
    private static MappedAddress parseXorMappedAddress(byte[] attr) throws IOException
    {
        if (attr.length < 8) throw new IOException("XOR-MAPPED-ADDRESS attribute too short");

        byte family = attr[1];
        if (family != FAMILY_IPV4) throw new IOException("Only IPv4 is supported");

        int xorPort = ((attr[2] & 0xFF) << 8) | (attr[3] & 0xFF);
        int port    = xorPort ^ (MAGIC_COOKIE >>> 16);

        byte[] xorIp = { attr[4], attr[5], attr[6], attr[7] };
        byte[] cookieBytes = ByteBuffer.allocate(4).putInt(MAGIC_COOKIE).array();
        byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++)
        {
            ip[i] = (byte) (xorIp[i] ^ cookieBytes[i]);
        }

        return new MappedAddress(ip, port);
    }

    /**
     * Parses a MAPPED-ADDRESS attribute (RFC 3489 §11.2.1).
     */
    private static MappedAddress parseMappedAddress(byte[] attr) throws IOException
    {
        if (attr.length < 8) throw new IOException("MAPPED-ADDRESS attribute too short");

        byte family = attr[1];
        if (family != FAMILY_IPV4) throw new IOException("Only IPv4 is supported");

        int port = ((attr[2] & 0xFF) << 8) | (attr[3] & 0xFF);
        byte[] ip = { attr[4], attr[5], attr[6], attr[7] };

        return new MappedAddress(ip, port);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Generates a cryptographically-random 12-byte transaction ID. */
    static byte[] generateTransactionId() 
    {
        byte[] id = new byte[TRANSACTION_ID_LENGTH];
        SECURE_RANDOM.nextBytes(id);
        return id;
    }

    private static void validateArgs(String host, int port, int timeoutMs) 
    {
        if (host == null || host.trim().isEmpty()) 
        {
            throw new IllegalArgumentException("Host must not be null or empty");
        }
        if (port < 1 || port > 65535) 
        {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
        if (timeoutMs <= 0) 
        {
            throw new IllegalArgumentException("Timeout must be positive, got: " + timeoutMs);
        }
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * The server-reflexive (public) IP address and port as seen by the STUN server.
     */
    public static final class MappedAddress
    {

        private final InetAddress address;
        private final int         port;

        MappedAddress(byte[] ipBytes, int port) throws IOException
        {
            this.address = InetAddress.getByAddress(ipBytes);
            this.port    = port;
        }

        /** Returns the public IP address. */
        public InetAddress getAddress() { return address; }

        /** Returns the public port. */
        public int getPort() { return port; }

        /** Returns {@code "ip:port"} notation. */
        @Override
        public String toString()
        {
            return address.getHostAddress() + ":" + port;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof MappedAddress)) return false;
            MappedAddress other = (MappedAddress) o;
            return port == other.port && address.equals(other.address);
        }

        @Override
        public int hashCode()
        {
            return 31 * address.hashCode() + port;
        }
    }
}

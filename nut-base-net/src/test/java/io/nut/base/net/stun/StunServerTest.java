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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit and integration tests for {@link StunServer}.
 *
 * <p>Tests are split into:
 * <ul>
 *   <li><b>Pure unit tests</b> — no sockets; test {@code buildResponse},
 *       attribute builders, and validation in isolation.</li>
 *   <li><b>Integration tests</b> — spin up a real {@link StunServer} and a
 *       real {@link StunClient} on localhost to verify the full round-trip.</li>
 * </ul>
 */
public class StunServerTest 
{
    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds a minimal valid 20-byte Binding Request with the given transaction ID. */
    static byte[] buildBindingRequest(byte[] transactionId) 
    {
        return StunClient.buildBindingRequest(transactionId);
    }

    /** Parses the XOR-MAPPED-ADDRESS attribute value out of a Binding Response. */
    static int[] parseXorMappedFromResponse(byte[] response) throws IOException 
    {
        ByteBuffer buf = ByteBuffer.wrap(response);
        buf.position(20); // skip header
        while (buf.remaining() >= 4) 
        {
            short type = buf.getShort();
            int   len  = buf.getShort() & 0xFFFF;
            if (type == StunServer.ATTR_XOR_MAPPED_ADDRESS && len == 8) 
            {
                buf.get(); // reserved
                buf.get(); // family
                int xorPort = buf.getShort() & 0xFFFF;
                int port    = xorPort ^ (StunServer.MAGIC_COOKIE >>> 16);
                byte[] cookieBytes = ByteBuffer.allocate(4).putInt(StunServer.MAGIC_COOKIE).array();
                byte[] ip = new byte[4];
                for (int i = 0; i < 4; i++) ip[i] = (byte) (buf.get() ^ cookieBytes[i]);
                return new int[]{ip[0] & 0xFF, ip[1] & 0xFF, ip[2] & 0xFF, ip[3] & 0xFF, port};
            }
            byte[] skip = new byte[len + ((4 - len % 4) % 4)];
            buf.get(skip, 0, Math.min(skip.length, buf.remaining()));
        }
        throw new IOException("XOR-MAPPED-ADDRESS not found in response");
    }

    /** Parses the MAPPED-ADDRESS attribute value out of a Binding Response. */
    static int[] parseMappedFromResponse(byte[] response) throws IOException 
    {
        ByteBuffer buf = ByteBuffer.wrap(response);
        buf.position(20);
        while (buf.remaining() >= 4) 
        {
            short type = buf.getShort();
            int   len  = buf.getShort() & 0xFFFF;
            if (type == StunServer.ATTR_MAPPED_ADDRESS && len == 8) 
            {
                buf.get(); // reserved
                buf.get(); // family
                int port = buf.getShort() & 0xFFFF;
                byte[] ip = new byte[4];
                buf.get(ip);
                return new int[]{ip[0] & 0xFF, ip[1] & 0xFF, ip[2] & 0xFF, ip[3] & 0xFF, port};
            }
            byte[] skip = new byte[len + ((4 - len % 4) % 4)];
            buf.get(skip, 0, Math.min(skip.length, buf.remaining()));
        }
        throw new IOException("MAPPED-ADDRESS not found in response");
    }

    // ── validateRequest ───────────────────────────────────────────────────────

    @Test
    public void validateRequest_validRequest_doesNotThrow() throws Exception 
    {
        byte[] txId    = StunClient.generateTransactionId();
        byte[] request = buildBindingRequest(txId);
        StunServer.validateRequest(request); // must not throw
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void validateRequest_null_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> StunServer.validateRequest(null));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void validateRequest_tooShort_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> StunServer.validateRequest(new byte[10]));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void validateRequest_wrongMessageType_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> 
        {
            byte[] txId = StunClient.generateTransactionId();
            byte[] req  = buildBindingRequest(txId);
            req[1] = 0x02;  // flip to unsupported type
            StunServer.validateRequest(req);
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void validateRequest_invalidMagicCookie_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> 
        {
            byte[] txId = StunClient.generateTransactionId();
            byte[] req  = buildBindingRequest(txId);
            req[4] = (byte) 0xDE; req[5] = (byte) 0xAD;
            req[6] = (byte) 0xBE; req[7] = (byte) 0xEF;
            StunServer.validateRequest(req);
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void validateRequest_topTwoBitsSet_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> 
        {
            byte[] txId = StunClient.generateTransactionId();
            byte[] req  = buildBindingRequest(txId);
            req[0] = (byte) 0xC0;  // top two bits set
            StunServer.validateRequest(req);
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void validateRequest_declaredLengthTooLarge_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> 
        {
            byte[] txId = StunClient.generateTransactionId();
            byte[] req  = buildBindingRequest(txId);
            req[2] = 0x01; req[3] = 0x00; // declare 256 bytes of attributes
            StunServer.validateRequest(req);
        });
    }

    // ── extractTransactionId ──────────────────────────────────────────────────

    @Test
    public void extractTransactionId_returnsCorrectBytes() 
    {
        byte[] txId = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        byte[] req  = buildBindingRequest(txId);
        assertArrayEquals(txId, StunServer.extractTransactionId(req));
    }

    @Test
    public void extractTransactionId_allZeros() 
    {
        byte[] txId = new byte[12];
        byte[] req  = buildBindingRequest(txId);
        assertArrayEquals(new byte[12], StunServer.extractTransactionId(req));
    }

    // ── buildXorMappedAddress ─────────────────────────────────────────────────

    @Test
    public void buildXorMappedAddress_correctSize() throws Exception 
    {
        InetAddress addr = InetAddress.getByName("1.2.3.4");
        byte[] attr = StunServer.buildXorMappedAddress(addr, 12345);
        assertEquals(12, attr.length, "TLV header (4) + value (8)");
    }

    @Test
    public void buildXorMappedAddress_correctType() throws Exception 
    {
        InetAddress addr = InetAddress.getByName("1.2.3.4");
        byte[] attr = StunServer.buildXorMappedAddress(addr, 12345);
        int type = ((attr[0] & 0xFF) << 8) | (attr[1] & 0xFF);
        assertEquals(0x0020, type);
    }

    @Test
    public void buildXorMappedAddress_portIsXored() throws Exception 
    {
        int port = 54321;
        InetAddress addr = InetAddress.getByName("1.2.3.4");
        byte[] attr = StunServer.buildXorMappedAddress(addr, port);
        int xorPort = ((attr[6] & 0xFF) << 8) | (attr[7] & 0xFF);
        int decoded = xorPort ^ (StunServer.MAGIC_COOKIE >>> 16);
        assertEquals(port, decoded);
    }

    @Test
    public void buildXorMappedAddress_ipIsXored() throws Exception 
    {
        InetAddress addr = InetAddress.getByName("203.0.113.5");
        byte[] attr  = StunServer.buildXorMappedAddress(addr, 3478);
        byte[] cookieBytes = ByteBuffer.allocate(4).putInt(StunServer.MAGIC_COOKIE).array();
        byte[] rawIp = addr.getAddress();
        for (int i = 0; i < 4; i++) 
        {
            byte expected = (byte) (rawIp[i] ^ cookieBytes[i]);
            assertEquals((int) expected & 0xFF, (int) attr[8 + i] & 0xFF, "XOR byte " + i);
        }
    }

    // ── buildMappedAddress ────────────────────────────────────────────────────

    @Test
    public void buildMappedAddress_correctSize() throws Exception 
    {
        byte[] attr = StunServer.buildMappedAddress(InetAddress.getByName("1.2.3.4"), 80);
        assertEquals(12, attr.length);
    }

    @Test
    public void buildMappedAddress_correctType() throws Exception 
    {
        byte[] attr = StunServer.buildMappedAddress(InetAddress.getByName("1.2.3.4"), 80);
        int type = ((attr[0] & 0xFF) << 8) | (attr[1] & 0xFF);
        assertEquals(0x0001, type);
    }

    @Test
    public void buildMappedAddress_portIsNotXored() throws Exception 
    {
        int port = 9876;
        byte[] attr = StunServer.buildMappedAddress(InetAddress.getByName("1.2.3.4"), port);
        int rawPort = ((attr[6] & 0xFF) << 8) | (attr[7] & 0xFF);
        assertEquals(port, rawPort, "MAPPED-ADDRESS port must be plain (no XOR)");
    }

    @Test
    public void buildMappedAddress_ipIsNotXored() throws Exception 
    {
        InetAddress addr = InetAddress.getByName("10.20.30.40");
        byte[] attr = StunServer.buildMappedAddress(addr, 80);
        byte[] rawIp = addr.getAddress();
        for (int i = 0; i < 4; i++) 
        {
            assertEquals(rawIp[i] & 0xFF, attr[8 + i] & 0xFF, "MAPPED-ADDRESS IP byte " + i);
        }
    }

    // ── buildResponse ─────────────────────────────────────────────────────────

    @Test
    public void buildResponse_correctMessageType() throws Exception 
    {
        byte[] txId     = StunClient.generateTransactionId();
        byte[] response = StunServer.buildResponse(buildBindingRequest(txId), InetAddress.getByName("1.2.3.4"), 5000);
        int type = ((response[0] & 0xFF) << 8) | (response[1] & 0xFF);
        assertEquals(0x0101, type);
    }

    @Test
    public void buildResponse_transactionIdMirrored() throws Exception 
    {
        byte[] txId     = {9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 11, 12};
        byte[] response = StunServer.buildResponse(buildBindingRequest(txId), InetAddress.getByName("1.2.3.4"), 5000);
        byte[] respTxId = new byte[12];
        System.arraycopy(response, 8, respTxId, 0, 12);
        assertArrayEquals(txId, respTxId, "Transaction ID must be mirrored");
    }

    @Test
    public void buildResponse_magicCookieMirrored() throws Exception 
    {
        byte[] txId     = StunClient.generateTransactionId();
        byte[] response = StunServer.buildResponse(buildBindingRequest(txId), InetAddress.getByName("1.2.3.4"), 5000);
        int cookie = ByteBuffer.wrap(response, 4, 4).getInt();
        assertEquals(StunServer.MAGIC_COOKIE, cookie);
    }

    @Test
    public void buildResponse_containsBothAttributes() throws Exception 
    {
        byte[] txId     = StunClient.generateTransactionId();
        byte[] response = StunServer.buildResponse(buildBindingRequest(txId), InetAddress.getByName("5.6.7.8"), 4321);

        // Both attributes must be present and parseable
        int[] xor    = parseXorMappedFromResponse(response);
        int[] mapped = parseMappedFromResponse(response);

        // XOR-MAPPED-ADDRESS
        assertEquals(5, xor[0], "XOR ip[0]");
        assertEquals(4321, xor[4], "XOR port");

        // MAPPED-ADDRESS
        assertEquals(5, mapped[0], "MAP ip[0]");
        assertEquals(4321, mapped[4], "MAP port");
    }

    @Test
    public void buildResponse_xorAndMappedAgreeOnAddress() throws Exception 
    {
        byte[] txId     = StunClient.generateTransactionId();
        InetAddress addr = InetAddress.getByName("192.168.1.100");
        int port = 7777;
        byte[] response = StunServer.buildResponse(buildBindingRequest(txId), addr, port);

        int[] xor    = parseXorMappedFromResponse(response);
        int[] mapped = parseMappedFromResponse(response);

        assertEquals(xor[0], mapped[0], "IP must match between XOR and MAPPED (byte 0)");
        assertEquals(xor[3], mapped[3], "IP must match between XOR and MAPPED (byte 3)");
        assertEquals(xor[4], mapped[4], "Port must match between XOR and MAPPED");
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void buildResponse_invalidRequest_throws() throws Exception 
    {
        assertThrows(StunServer.InvalidStunMessageException.class, () -> StunServer.buildResponse(new byte[5], InetAddress.getByName("1.2.3.4"), 1234));
    }

    // ── Constructor validation ────────────────────────────────────────────────

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void constructor_portZero_throws() 
    {
        assertThrows(IllegalArgumentException.class, () -> new StunServer(0));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void constructor_portAboveRange_throws() 
    {
        assertThrows(IllegalArgumentException.class, () -> new StunServer(65536));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void constructor_workerThreadsZero_throws() 
    {
        assertThrows(IllegalArgumentException.class, () -> new StunServer(3478, 0));
    }

    @Test
    public void constructor_defaultPort_isStandardStunPort() 
    {
        StunServer s = new StunServer();
        assertEquals(3478, s.getPort());
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Test
    public void lifecycle_startAndStop() throws Exception 
    {
        StunServer server = new StunServer(findFreePort());
        assertFalse(server.isRunning(), "Should not be running before start()");
        server.start();
        assertTrue(server.isRunning(), "Should be running after start()");
        server.stop();
        assertFalse(server.isRunning(), "Should not be running after stop()");
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void lifecycle_doubleStart_throws() throws Exception 
    {
        assertThrows(IllegalStateException.class, () -> 
        {
            StunServer server = new StunServer(findFreePort());
            try 
            {
                server.start();
                server.start();  // must throw
            } 
            finally 
            {
                server.stop();
            }
        });
    }

    @Test
    public void lifecycle_stopWhenNotRunning_isIdempotent() {
        StunServer server = new StunServer(findFreePort());
        server.stop(); // must not throw
    }

    // ── Integration: full round-trip via localhost ────────────────────────────

    @Test
    public void integration_roundTrip_clientReceivesOwnAddress() throws Exception 
    {
        int port = findFreePort();
        StunServer server = new StunServer(port, 2);
        server.start();
        try         
        {
            StunClient.MappedAddress addr = StunClient.discover("127.0.0.1", port, 2000);
            assertNotNull(addr);
            // When talking to localhost the reflexive address must be 127.0.0.1
            assertEquals("127.0.0.1", addr.getAddress().getHostAddress());
            assertTrue(addr.getPort() > 0 && addr.getPort() <= 65535, "Port must be in valid range");
        } 
        finally 
        {
            server.stop();
        }
    }

    @Test
    public void integration_multipleRequests_allSucceed() throws Exception 
    {
        int port = findFreePort();
        StunServer server = new StunServer(port, 2);
        server.start();
        try 
        {
            for (int i = 0; i < 5; i++) 
            {
                StunClient.MappedAddress addr = StunClient.discover("127.0.0.1", port, 2000);
                assertNotNull(addr);
                assertEquals("127.0.0.1", addr.getAddress().getHostAddress());
            }
        } 
        finally 
        {
            server.stop();
        }
    }

    @Test
    public void integration_transactionIdIsEchoed() throws Exception 
    {
        int port = findFreePort();
        StunServer server = new StunServer(port, 1);
        server.start();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);
            byte[] txId    = StunClient.generateTransactionId();
            byte[] request = StunClient.buildBindingRequest(txId);

            InetAddress addr = InetAddress.getByName("127.0.0.1");
            socket.send(new DatagramPacket(request, request.length, addr, port));

            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            socket.receive(resp);

            byte[] respTxId = new byte[12];
            System.arraycopy(resp.getData(), 8, respTxId, 0, 12);
            assertArrayEquals(txId, respTxId, "Transaction ID must be echoed verbatim");
        } finally {
            server.stop();
        }
    }

    @Test
    public void integration_malformedDatagramIsIgnored() throws Exception 
    {
        int port = findFreePort();
        StunServer server = new StunServer(port, 1);
        server.start();
        try (DatagramSocket socket = new DatagramSocket()) 
        {
            socket.setSoTimeout(500);
            InetAddress addr = InetAddress.getByName("127.0.0.1");

            // Send garbage
            byte[] garbage = {0x00, 0x01, 0x02, 0x03};
            socket.send(new DatagramPacket(garbage, garbage.length, addr, port));

            // Now send a valid request — server must still respond
            byte[] txId    = StunClient.generateTransactionId();
            byte[] request = StunClient.buildBindingRequest(txId);
            socket.send(new DatagramPacket(request, request.length, addr, port));

            byte[] buf = new byte[512];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            socket.setSoTimeout(2000);
            socket.receive(resp); // must succeed

            int msgType = ((resp.getData()[0] & 0xFF) << 8) | (resp.getData()[1] & 0xFF);
            assertEquals(0x0101, msgType, "Must be a Binding Response");
        } 
        finally 
        {
            server.stop();
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Finds an available UDP port by briefly binding to port 0. */
    static int findFreePort() 
    {
        try (DatagramSocket s = new DatagramSocket(0)) 
        {
            return s.getLocalPort();
        } 
        catch (IOException ex) 
        {
            throw new RuntimeException("Cannot find free port", ex);
        }
    }
}

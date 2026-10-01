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
import java.nio.ByteBuffer;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StunClient}.
 *
 * <p>Tests are split into:
 * <ul>
 *   <li><b>Pure unit tests</b> – no network, test packet building and parsing in isolation.</li>
 *   <li><b>Integration tests</b> – require internet; skipped gracefully when unavailable.</li>
 * </ul>
 *
 * <p>Compatible with JUnit 4 (via {@code @Test} / {@code expected}) or this project's
 * zero-dependency {@link TestRunner} (via {@link TestCase}).
 */
public class StunClientTest 
{

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds a minimal valid Binding Response wrapping the given attributes. */
    static byte[] buildBindingResponse(byte[] transactionId, byte[] attributes) 
    {
        int attrLen = (attributes != null) ? attributes.length : 0;
        ByteBuffer buf = ByteBuffer.allocate(20 + attrLen);
        buf.putShort((short) 0x0101);   // Binding Response
        buf.putShort((short) attrLen);  // message length
        buf.putInt(0x2112A442);         // magic cookie
        buf.put(transactionId);
        if (attributes != null) buf.put(attributes);
        return buf.array();
    }

    /** Encodes an XOR-MAPPED-ADDRESS attribute for the given IPv4 + port. */
    static byte[] xorMappedAddressAttr(byte[] ip, int port) 
    {
        int    magicCookie = 0x2112A442;
        int    xorPort     = port ^ (magicCookie >>> 16);
        byte[] cookieBytes = ByteBuffer.allocate(4).putInt(magicCookie).array();

        ByteBuffer attr = ByteBuffer.allocate(4 + 8);
        attr.putShort((short) 0x0020); // type
        attr.putShort((short) 8);      // value length
        attr.put((byte) 0x00);         // reserved
        attr.put((byte) 0x01);         // family: IPv4
        attr.put((byte) ((xorPort >> 8) & 0xFF));
        attr.put((byte) (xorPort & 0xFF));
        for (int i = 0; i < 4; i++) attr.put((byte) (ip[i] ^ cookieBytes[i]));
        return attr.array();
    }

    /** Encodes a MAPPED-ADDRESS attribute for the given IPv4 + port. */
    static byte[] mappedAddressAttr(byte[] ip, int port) 
    {
        ByteBuffer attr = ByteBuffer.allocate(4 + 8);
        attr.putShort((short) 0x0001); // type
        attr.putShort((short) 8);
        attr.put((byte) 0x00);         // reserved
        attr.put((byte) 0x01);         // family: IPv4
        attr.put((byte) ((port >> 8) & 0xFF));
        attr.put((byte) (port & 0xFF));
        attr.put(ip);
        return attr.array();
    }

    // ── buildBindingRequest ───────────────────────────────────────────────────

    @Test
    public void buildBindingRequest_hasCorrectLength() 
    {
        byte[] packet = StunClient.buildBindingRequest(new byte[12]);
        assertEquals(20, packet.length, "STUN header must be 20 bytes");
    }

    @Test
    public void buildBindingRequest_messageTypeIsBindingRequest() 
    {
        byte[] packet = StunClient.buildBindingRequest(new byte[12]);
        int msgType = ((packet[0] & 0xFF) << 8) | (packet[1] & 0xFF);
        assertEquals(0x0001, msgType);
    }

    @Test
    public void buildBindingRequest_messageLengthIsZero() 
    {
        byte[] packet = StunClient.buildBindingRequest(new byte[12]);
        int msgLen = ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        assertEquals(0, msgLen, "No attributes → message length must be 0");
    }

    @Test
    public void buildBindingRequest_containsMagicCookie() 
    {
        byte[] packet = StunClient.buildBindingRequest(new byte[12]);
        ByteBuffer buf = ByteBuffer.wrap(packet);
        buf.position(4);
        assertEquals(0x2112A442, buf.getInt());
    }

    @Test
    public void buildBindingRequest_transactionIdEmbedded() 
    {
        byte[] txId = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        byte[] packet = StunClient.buildBindingRequest(txId);
        byte[] embedded = new byte[12];
        System.arraycopy(packet, 8, embedded, 0, 12);
        assertArrayEquals(txId, embedded);
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void buildBindingRequest_nullTransactionId_throws() 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.buildBindingRequest(null));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void buildBindingRequest_shortTransactionId_throws() 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.buildBindingRequest(new byte[8]));
    }

    // ── parseResponse – XOR-MAPPED-ADDRESS ───────────────────────────────────

    @Test
    public void parseResponse_xorMappedAddress_decodesCorrectly() throws IOException 
    {
        byte[] txId = StunClient.generateTransactionId();
        byte[] ip   = {(byte) 203, (byte) 0, (byte) 113, (byte) 5};
        int    port = 54321;

        byte[] response = buildBindingResponse(txId, xorMappedAddressAttr(ip, port));
        StunClient.MappedAddress addr = StunClient.parseResponse(response, txId);

        assertArrayEquals(ip, addr.getAddress().getAddress());
        assertEquals(port, addr.getPort());
    }

    @Test
    public void parseResponse_xorMappedAddress_commonPublicIp() throws IOException {
        byte[] txId = StunClient.generateTransactionId();
        byte[] ip   = {8, 8, 8, 8};

        byte[] response = buildBindingResponse(txId, xorMappedAddressAttr(ip, 12345));
        StunClient.MappedAddress addr = StunClient.parseResponse(response, txId);

        assertEquals("8.8.8.8", addr.getAddress().getHostAddress());
        assertEquals(12345, addr.getPort());
    }

    @Test
    public void parseResponse_mappedAddressFallback_decodesCorrectly() throws IOException {
        byte[] txId = StunClient.generateTransactionId();
        byte[] ip   = {(byte) 198, 51, (byte) 100, 1};

        byte[] response = buildBindingResponse(txId, mappedAddressAttr(ip, 7777));
        StunClient.MappedAddress addr = StunClient.parseResponse(response, txId);

        assertArrayEquals(ip, addr.getAddress().getAddress());
        assertEquals(7777, addr.getPort());
    }

    @Test
    public void parseResponse_prefersXorOverMapped_whenBothPresent() throws IOException {
        byte[] txId    = StunClient.generateTransactionId();
        byte[] xorIp   = {1, 2, 3, 4};
        byte[] plainIp = {5, 6, 7, 8};

        // MAPPED-ADDRESS first, then XOR-MAPPED-ADDRESS
        byte[] mapped   = mappedAddressAttr(plainIp, 2222);
        byte[] xored    = xorMappedAddressAttr(xorIp, 1111);
        byte[] combined = new byte[mapped.length + xored.length];
        System.arraycopy(mapped, 0, combined, 0, mapped.length);
        System.arraycopy(xored,  0, combined, mapped.length, xored.length);

        StunClient.MappedAddress addr = StunClient.parseResponse(
                buildBindingResponse(txId, combined), txId);

        assertArrayEquals(xorIp, addr.getAddress().getAddress(), "Must prefer XOR-MAPPED-ADDRESS");
        assertEquals(1111, addr.getPort());
    }

    // ── parseResponse – error cases ───────────────────────────────────────────

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void parseResponse_tooShort_throws() throws IOException 
    {
        assertThrows(IOException.class, () -> StunClient.parseResponse(new byte[10], new byte[12]));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void parseResponse_wrongMessageType_throws() throws IOException 
    {
        assertThrows(IOException.class, () -> 
        {
                byte[] txId = new byte[12];
            ByteBuffer buf = ByteBuffer.allocate(20);
            buf.putShort((short) 0x0002);
            buf.putShort((short) 0);
            buf.putInt(0x2112A442);
            buf.put(txId);
            StunClient.parseResponse(buf.array(), txId);
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void parseResponse_wrongMagicCookie_throws() throws IOException 
    {
        assertThrows(IOException.class, () -> 
        {
            byte[] txId = new byte[12];
            ByteBuffer buf = ByteBuffer.allocate(20);
            buf.putShort((short) 0x0101);
            buf.putShort((short) 0);
            buf.putInt(0xDEADBEEF);
            buf.put(txId);
            StunClient.parseResponse(buf.array(), txId);
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void parseResponse_transactionIdMismatch_throws() throws IOException 
    {
        assertThrows(IOException.class, () -> 
        {
            byte[] txId        = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
            byte[] differentId = {9, 9, 9, 9, 9, 9, 9, 9, 9,  9,  9,  9};
            byte[] response = buildBindingResponse(txId, xorMappedAddressAttr(new byte[]{1, 2, 3, 4}, 1234));
            StunClient.parseResponse(response, differentId);
        });
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void parseResponse_noAddressAttribute_throws() throws IOException 
    {
        assertThrows(IOException.class, () -> 
        {
            byte[] txId = StunClient.generateTransactionId();
            StunClient.parseResponse(buildBindingResponse(txId, null), txId);
        });
    }

    // ── generateTransactionId ─────────────────────────────────────────────────

    @Test
    public void generateTransactionId_is12Bytes() 
    {
        assertEquals(12, StunClient.generateTransactionId().length);
    }

    @Test
    public void generateTransactionId_isRandom() 
    {
        byte[] a = StunClient.generateTransactionId();
        byte[] b = StunClient.generateTransactionId();
        assertFalse(Arrays.equals(a, b), "Two consecutive IDs must differ");
    }

    // ── discover() – argument validation ─────────────────────────────────────

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void discover_nullHost_throws() throws IOException 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.discover(null, 3478, 1000));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void discover_emptyHost_throws() throws IOException 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.discover("", 3478, 1000));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void discover_portZero_throws() throws IOException 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.discover("stun.l.google.com", 0, 1000));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void discover_portAboveRange_throws() throws IOException 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.discover("stun.l.google.com", 70000, 1000));
    }

    @Test
    @SuppressWarnings("ThrowableResultIgnored")
    public void discover_negativeTimeout_throws() throws IOException 
    {
        assertThrows(IllegalArgumentException.class, () -> StunClient.discover("stun.l.google.com", 3478, -1));
    }

    // ── MappedAddress ─────────────────────────────────────────────────────────

    @Test
    public void mappedAddress_toStringFormat() throws IOException 
    {
        byte[] txId = StunClient.generateTransactionId();
        byte[] response = buildBindingResponse(txId,
                xorMappedAddressAttr(new byte[]{1, 2, 3, 4}, 8080));
        StunClient.MappedAddress addr = StunClient.parseResponse(response, txId);
        assertEquals("1.2.3.4:8080", addr.toString());
    }

    @Test
    public void mappedAddress_equalsAndHashCode() throws IOException {
        byte[] txId = StunClient.generateTransactionId();
        byte[] response = buildBindingResponse(txId,
                xorMappedAddressAttr(new byte[]{10, 0, 0, 1}, 9999));
        StunClient.MappedAddress a = StunClient.parseResponse(response, txId);
        StunClient.MappedAddress b = StunClient.parseResponse(response, txId);
        assertEquals(a, b);
        assertEquals((long) a.hashCode(), (long) b.hashCode());
    }

    @Test
    public void mappedAddress_notEqualDifferentPort() throws IOException {
        byte[] txId = StunClient.generateTransactionId();
        byte[] ip   = {10, 0, 0, 1};
        StunClient.MappedAddress a = StunClient.parseResponse(
                buildBindingResponse(txId, xorMappedAddressAttr(ip, 1111)), txId);
        StunClient.MappedAddress b = StunClient.parseResponse(
                buildBindingResponse(txId, xorMappedAddressAttr(ip, 2222)), txId);
        assertNotEquals(a, b);
    }

    // ── Integration test (requires internet) ─────────────────────────────────

    @Test
    public void integration_googleStun_returnsPublicAddress()
    {
        try
        {
            StunClient.MappedAddress addr = StunClient.discover();
            assertNotNull(addr);
            assertNotNull(addr.getAddress());
            assertTrue(addr.getPort() > 0 && addr.getPort() <= 65535, "Port must be in valid range");
            System.out.println("          → Public address: " + addr);
        }
        catch (IOException e)
        {
            System.out.println("          → Skipped (no internet): " + e.getMessage());
        }
    }

    @Test
    public void integration_customStunServer_returnsPublicAddress()
    {
        try
        {
            StunClient.MappedAddress addr = StunClient.discover("stun1.l.google.com", 3478);
            assertNotNull(addr);
            assertTrue(addr.getPort() > 0, "Port must be in valid range");
            System.out.println("          → Custom server result: " + addr);
        }
        catch (IOException e)
        {
            System.out.println("          → Skipped (no internet): " + e.getMessage());
        }
    }
}

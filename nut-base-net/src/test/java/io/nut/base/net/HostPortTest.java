/*
 * HostPort.java
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HostPortTest
{
    
    /**
     * Test of toString method, of class HostPort.
     */
    @Test
    public void testToString()
    {
        assertEquals("127.0.0.1:9050", new HostPort("127.0.0.1", 9050).toString());
        assertEquals("localhost:9150", new HostPort("localhost", 9150).toString());
    }

    /**
     * Test of hashCode method, of class HostPort.
     */
    @Test
    public void testHashCode()
    {
        assertEquals(new HostPort("127.0.0.1", 1111).hashCode(), new HostPort("127.0.0.1", 1111).hashCode());
        assertEquals(new HostPort("127.0.0.1", 2222).hashCode(), new HostPort("127.0.0.1", 2222).hashCode());
        assertEquals(new HostPort("localhost", 3333).hashCode(), new HostPort("localhost", 3333).hashCode());
    }

    /**
     * Test of equals method, of class HostPort.
     */
    @Test
    public void testEquals()
    {
        assertEquals(new HostPort("127.0.0.1", 1111), new HostPort("127.0.0.1", 1111));
        assertNotEquals(new HostPort("127.0.0.1", 1111), new HostPort("127.0.0.1", 2222));
        assertNotEquals(new HostPort("127.0.0.1", 3333), new HostPort("localhost", 3333));
    }

    /**
     * Test of parse method, of class HostPort.
     */
    @Test
    public void testParse()
    {
        String[] list = {"127.0.0.1:9050", "localhostt:9050", "127.0.0.1:1111", "localhostt:2222"};
        for(String item : list)
        {
            HostPort hp = HostPort.parse(item);
            assertEquals(item, hp.toString());
        }
    }

    // -------------------------------------------------------------------------
    // Constructor & field access
    // -------------------------------------------------------------------------
 
    @Test
    void constructor_storesHostAndPort()
    {
        HostPort hp = new HostPort("localhost", 8080);
        assertEquals("localhost", hp.host);
        assertEquals(8080, hp.port);
    }
 
    @Test
    void constructor_allowsZeroPort()
    {
        HostPort hp = new HostPort("localhost", 0);
        assertEquals(0, hp.port);
    }
 
    @Test
    void constructor_allowsMaxPort()
    {
        HostPort hp = new HostPort("localhost", 65535);
        assertEquals(65535, hp.port);
    }
 
    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------
 
    @Test
    void toString_returnsHostColonPort()
    {
        assertEquals("localhost:9050", new HostPort("localhost", 9050).toString());
    }
 
    @Test
    void toString_withIpv4Address()
    {
        assertEquals("127.0.0.1:9050", new HostPort("127.0.0.1", 9050).toString());
    }
 
    // -------------------------------------------------------------------------
    // parse – valid inputs
    // -------------------------------------------------------------------------
 
    @Test
    void parse_localhostWithPort()
    {
        HostPort hp = HostPort.parse("localhost:9050");
        assertNotNull(hp);
        assertEquals("localhost", hp.host);
        assertEquals(9050, hp.port);
    }
 
    @Test
    void parse_ipv4WithPort()
    {
        HostPort hp = HostPort.parse("127.0.0.1:9050");
        assertNotNull(hp);
        assertEquals("127.0.0.1", hp.host);
        assertEquals(9050, hp.port);
    }
 
    @Test
    void parse_portZero()
    {
        HostPort hp = HostPort.parse("localhost:0");
        assertNotNull(hp);
        assertEquals(0, hp.port);
    }
 
    @Test
    void parse_portMax()
    {
        HostPort hp = HostPort.parse("localhost:65535");
        assertNotNull(hp);
        assertEquals(65535, hp.port);
    }
 
    @Test
    void parse_isInverseOfToString()
    {
        HostPort original = new HostPort("example.com", 443);
        HostPort parsed = HostPort.parse(original.toString());
        assertNotNull(parsed);
        assertEquals(original, parsed);
    }
 
    @Test
    void parse_ipv6LikeAddressUsesLastColon()
    {
        // Bare IPv6 without brackets — lastIndexOf(':') finds the port colon
        HostPort hp = HostPort.parse("[::1]:8080");
        assertNotNull(hp);
        assertEquals("[::1]", hp.host);
        assertEquals(8080, hp.port);
    }
 
    // -------------------------------------------------------------------------
    // parse – invalid inputs  →  must return null
    // -------------------------------------------------------------------------
 
    @Test
    void parse_nullReturnsNull()
    {
        assertNull(HostPort.parse(null));
    }
 
    @Test
    void parse_emptyStringReturnsNull()
    {
        assertNull(HostPort.parse(""));
    }
 
    @ParameterizedTest
    @ValueSource(strings = {
        "localhost",          // no colon at all
        ":9050",              // empty host (colon at index 0)
        "localhost:",         // empty port
        "localhost:abc",      // non-numeric port
        "localhost:99999",    // port > 65535
        "localhost:-1",       // negative port
        "localhost:65536",    // one above max
        "localhost:1.5",      // fractional port
        "localhost: 80",      // port with leading space
    })
    void parse_invalidInputReturnsNull(String input)
    {
        assertNull(HostPort.parse(input));
    }
 
    // -------------------------------------------------------------------------
    // equals
    // -------------------------------------------------------------------------
 
    @Test
    void equals_sameInstance()
    {
        HostPort hp = new HostPort("localhost", 80);
        assertEquals(hp, hp);
    }
 
    @Test
    void equals_equalObjects()
    {
        assertEquals(new HostPort("localhost", 80), new HostPort("localhost", 80));
    }
 
    @Test
    void equals_differentHost()
    {
        assertNotEquals(new HostPort("localhost", 80), new HostPort("127.0.0.1", 80));
    }
 
    @Test
    void equals_differentPort()
    {
        assertNotEquals(new HostPort("localhost", 80), new HostPort("localhost", 81));
    }
 
    @Test
    void equals_nullReturnsFalse()
    {
        assertNotEquals(null, new HostPort("localhost", 80));
    }
 
    @Test
    void equals_differentTypeReturnsFalse()
    {
        assertNotEquals("localhost:80", new HostPort("localhost", 80));
    }
 
    @Test
    void equals_nullHostBothSides()
    {
        assertEquals(new HostPort(null, 80), new HostPort(null, 80));
    }
 
    @Test
    void equals_nullHostOnlyOneSide()
    {
        assertNotEquals(new HostPort(null, 80), new HostPort("localhost", 80));
    }
 
    // -------------------------------------------------------------------------
    // hashCode
    // -------------------------------------------------------------------------
 
    @Test
    void hashCode_equalObjectsHaveSameHashCode()
    {
        HostPort a = new HostPort("localhost", 80);
        HostPort b = new HostPort("localhost", 80);
        assertEquals(a.hashCode(), b.hashCode());
    }
 
    @Test
    void hashCode_differentObjectsLikelyDifferentHashCode()
    {
        // Not guaranteed by contract, but a reasonable sanity check
        HostPort a = new HostPort("localhost", 80);
        HostPort b = new HostPort("example.com", 443);
        assertNotEquals(a.hashCode(), b.hashCode());
    }
 
    @Test
    void hashCode_consistentAcrossMultipleCalls()
    {
        HostPort hp = new HostPort("localhost", 80);
        assertEquals(hp.hashCode(), hp.hashCode());
    }
}

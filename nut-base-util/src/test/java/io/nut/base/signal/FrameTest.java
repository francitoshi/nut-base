/*
 *  FrameTest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.signal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class FrameTest
{
    Frame framer = new Frame();
    /**
     * Test of readShort method, of class Framer.
     */
    @Test
    public void testDemo()
    {
        char  id    = (char) 0x0042;
        byte[] data  = "Hello, Framer!".getBytes();
        byte[] frame = framer.createData('\0', '\0', id, data);

        System.out.printf("Frame   (%2d bytes): %s%n", frame.length, toHex(frame));
        System.out.println("check()            : " + framer.check(frame));
        System.out.printf("id()               : 0x%04X%n", framer.getId(frame) & 0xFFFF);
        System.out.println("payload()          : " + new String(framer.getPayload(frame)));

        // ACK
        byte[] ack = framer.createAck('\0','\0', id);
        System.out.printf("%nACK     (%2d bytes): %s%n", ack.length, toHex(ack));
        System.out.println("check(ack)         : " + framer.check(ack));
        System.out.println("isAck()            : " + framer.isAck(ack));
        System.out.printf("referencedId()     : 0x%04X%n", framer.getId(ack) & 0xFFFF);

        // NACK
        byte[] nack = framer.createNack('\0','\0', id, framer.STATUS_CRC_ERROR);
        System.out.printf("%nNACK    (%2d bytes): %s%n", nack.length, toHex(nack));
        System.out.println("check(nack)        : " + framer.check(nack));
        System.out.println("isNack()           : " + framer.isNack(nack));
        System.out.printf("referencedId()     : 0x%04X%n", framer.getId(nack) & 0xFFFF);
        System.out.printf("referencedStatus() : 0x%02X%n", framer.getStatus(nack) & 0xFF);

        // Corrupted frame
        byte[] bad = frame.clone();
        bad[9] ^= 0xFF;
        System.out.println("\ncheck(corrupted)   : " + framer.check(bad) + "  (expected -5)");
    }

    private static String toHex(byte[] data) 
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }    

    /**
     * Test of createAck method, of class Frame.
     */
    @Test
    public void testCreateAck()
    {
        char id = 0;
        byte[] ack = framer.createAck('\0', '\0', id);
        assertFalse(framer.isData(ack));
        assertTrue(framer.isAck(ack));
        assertFalse(framer.isNack(ack));
    }

    /**
     * Test of createNack method, of class Frame.
     */
    @Test
    public void testCreateNack()
    {
        char id = 0;
        byte[] ack = framer.createNack('\0', '\0', id, framer.STATUS_CRC_ERROR);
        assertFalse(framer.isData(ack));
        assertFalse(framer.isAck(ack));
        assertTrue(framer.isNack(ack));
    }
}

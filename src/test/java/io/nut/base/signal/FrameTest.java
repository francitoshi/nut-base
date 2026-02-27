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
    
    /**
     * Test of readShort method, of class Framer.
     */
    @Test
    public void testDemo()
    {
        short  id    = (short) 0x0042;
        byte[] data  = "Hello, Framer!".getBytes();
        byte[] frame = Frame.createData(id, data);

        System.out.printf("Frame   (%2d bytes): %s%n", frame.length, toHex(frame));
        System.out.println("check()            : " + Frame.check(frame));
        System.out.printf("id()               : 0x%04X%n", Frame.getId(frame) & 0xFFFF);
        System.out.println("payload()          : " + new String(Frame.getPayload(frame)));

        // ACK
        byte[] ack = Frame.createAck(id);
        System.out.printf("%nACK     (%2d bytes): %s%n", ack.length, toHex(ack));
        System.out.println("check(ack)         : " + Frame.check(ack));
        System.out.println("isAck()            : " + Frame.isAck(ack));
        System.out.printf("referencedId()     : 0x%04X%n", Frame.getId(ack) & 0xFFFF);

        // NACK
        byte[] nack = Frame.createNack(id, Frame.STATUS_CRC_ERROR);
        System.out.printf("%nNACK    (%2d bytes): %s%n", nack.length, toHex(nack));
        System.out.println("check(nack)        : " + Frame.check(nack));
        System.out.println("isNack()           : " + Frame.isNack(nack));
        System.out.printf("referencedId()     : 0x%04X%n", Frame.getId(nack) & 0xFFFF);
        System.out.printf("referencedStatus() : 0x%02X%n", Frame.getStatus(nack) & 0xFF);

        // Corrupted frame
        byte[] bad = frame.clone();
        bad[9] ^= 0xFF;
        System.out.println("\ncheck(corrupted)   : " + Frame.check(bad) + "  (expected -5)");
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
        short id = 0;
        byte[] ack = Frame.createAck(id);
        assertFalse(Frame.isData(ack));
        assertTrue(Frame.isAck(ack));
        assertFalse(Frame.isNack(ack));
    }

    /**
     * Test of createNack method, of class Frame.
     */
    @Test
    public void testCreateNack()
    {
        short id = 0;
        byte[] ack = Frame.createNack(id, Frame.STATUS_CRC_ERROR);
        assertFalse(Frame.isData(ack));
        assertFalse(Frame.isAck(ack));
        assertTrue(Frame.isNack(ack));
    }
}

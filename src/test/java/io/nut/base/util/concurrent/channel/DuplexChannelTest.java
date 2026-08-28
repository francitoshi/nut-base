/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DuplexChannelTest
{

    @Test
    public void testDuplexSameType() throws Exception
    {
        Channel<String> inChan = Channel.unbuffered();
        Channel<String> outChan = Channel.unbuffered();

        DuplexChannel<String> duplex = Channel.duplex(inChan, outChan);

        // Test writing delegates to outChan
        Thread writer = new Thread(() -> 
        {
            duplex.put("hello");
        });
        writer.start();

        assertEquals("hello", outChan.get());
        writer.join(2000);
        assertFalse(writer.isAlive());

        // Test reading delegates to inChan
        Thread reader = new Thread(() -> 
        {
            inChan.put("world");
        });
        reader.start();

        assertEquals("world", duplex.get());
        reader.join(2000);
        assertFalse(reader.isAlive());
    }

    @Test
    public void testDuplexNullValidation()
    {
        Channel<String> outChan = Channel.unbuffered();
        Channel<String> inChan = Channel.unbuffered();

        assertThrows(NullPointerException.class, () -> Channel.duplex(null, inChan));
        assertThrows(NullPointerException.class, () -> Channel.duplex(outChan, null));
    }
}

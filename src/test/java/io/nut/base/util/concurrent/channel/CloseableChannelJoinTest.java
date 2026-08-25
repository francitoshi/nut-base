/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CloseableChannelJoinTest
{
    @Test
    public void testBufferedChannelJoin() throws Exception
    {
        CloseableChannel<String> channel = Channel.closeableBuffered(4);
        testJoinLifecycle(channel);
    }

    @Test
    public void testUnbufferedChannelJoin() throws Exception
    {
        CloseableChannel<String> channel = Channel.closeableUnbuffered();
        testJoinLifecycle(channel);
    }

    @Test
    public void testUnlimitedChannelJoin() throws Exception
    {
        CloseableChannel<String> channel = Channel.closeableUnlimited();
        testJoinLifecycle(channel);
    }

    private void testJoinLifecycle(CloseableChannel<String> channel) throws Exception
    {
        // 1. Calling join on an open channel should block
        assertFalse(channel.isClosed());
        
        CountDownLatch joinedLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(1);
        
        Thread joinerThread = new Thread(() -> {
            try
            {
                startedLatch.countDown();
                channel.join();
                joinedLatch.countDown();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        joinerThread.start();
        
        // Wait for joiner thread to start and block
        assertTrue(startedLatch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        
        // It shouldn't have finished joining yet since channel is open
        assertEquals(1, joinedLatch.getCount());
        
        // Close the channel
        assertTrue(channel.close());
        assertTrue(channel.isClosed());
        
        // The joiner thread should now successfully finish
        assertTrue(joinedLatch.await(5, TimeUnit.SECONDS));
        joinerThread.join(1000);
        assertFalse(joinerThread.isAlive());
        
        // Calling join on an already closed channel should return immediately
        channel.join(); 
    }
}

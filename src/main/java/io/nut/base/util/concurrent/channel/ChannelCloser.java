/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

public interface ChannelCloser
{
    public boolean close();
    public boolean isClosed();
    /**
     * Blocks the current thread until the channel is closed.
     * If the channel is already closed, this method returns immediately.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void join() throws InterruptedException;
}

/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.TimeUnit;

public interface ChannelWriter<E>
{
    public void put(E value) throws InterruptedException;

    public boolean put(E value, long timeout, TimeUnit unit) throws InterruptedException;
}

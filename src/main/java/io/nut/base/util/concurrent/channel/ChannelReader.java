/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.TimeUnit;

public interface ChannelReader<E>
{
    public E get();

    public E get(long timeout, TimeUnit unit);
}

/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.gauge;

public interface GaugeView
{
    void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full);
}
    

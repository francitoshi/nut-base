/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.gauge;

public class NullGauge implements GaugeProgress, GaugeView
{
    @Override
    public boolean isStarted()
    {
        return false;
    }
    @Override
    public boolean isPaused()
    {
        return false;
    }

    @Override
    public void pause()
    {
    }
    @Override
    public void resume()
    {
    }

    @Override
    public void start()
    {
    }
    @Override
    public void start(int max)
    {
    }
    @Override
    public void start(int max, String prefix)
    {
    }
    @Override
    public void close()
    {
    }

    @Override
    public void setPrefix(String prefix)
    {
    }

    @Override
    public String getPrefix()
    {
        return "";
    }

    @Override
    public double getDone()
    {
        return 0;
    }

    @Override
    public int getVal()
    {
        return 0;
    }

    @Override
    public int getMax()
    {
        return 0;
    }

    @Override
    public void setVal(int n)
    {
    }

    @Override
    public void setMax(int n)
    {
    }

    @Override
    public void step()
    {
    }

    @Override
    public void step(int n)
    {
    }

    @Override
    public void setShow(boolean showPrev, boolean showNext, boolean showFull)
    {
    }

    @Override
    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
    }
}

/*
 * NullGauge.java
 *
 * Copyright (c) 2012-2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
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

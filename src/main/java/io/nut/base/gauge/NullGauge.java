/*
 * NullGauge.java
 *
 * Copyright (c) 2012-2025 francitoshi@gmail.com
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

/**
 *
 * @author franci
 */
public class NullGauge implements GaugeProgress, GaugeView
{
    public boolean isStarted()
    {
        return false;
    }
    public boolean isPaused()
    {
        return false;
    }

    public void pause()
    {
    }
    public void resume()
    {
    }

    public void start()
    {
    }
    public void start(int max)
    {
    }
    public void start(int max, String prefix)
    {
    }
    public void close()
    {
    }

    public void setPrefix(String prefix)
    {
    }

    public String getPrefix()
    {
        return "";
    }

    public double getDone()
    {
        return 0;
    }

    public int getVal()
    {
        return 0;
    }

    public int getMax()
    {
        return 0;
    }

    public void setVal(int n)
    {
    }

    public void setMax(int n)
    {
    }

    public void step()
    {
    }

    public void step(int n)
    {
    }

    public void setShow(boolean showPrev, boolean showNext, boolean showFull)
    {
    }

    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
    }
}

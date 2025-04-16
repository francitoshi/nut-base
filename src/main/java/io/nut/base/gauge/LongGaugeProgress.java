/*
 * LongGaugeProgress.java
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

public class LongGaugeProgress
{
    volatile long max;
    volatile long val;
    volatile long ratio;
    final GaugeProgress gp;

    public LongGaugeProgress(GaugeProgress gp)
    {
        this.gp = gp;
        this.max=gp.getMax();
        this.val=gp.getVal();
        this.ratio=1;
    }
    private long setup(long max)
    {
        this.max=max;
        long r=1;
        while(max>Integer.MAX_VALUE)
        {
            max>>=2;
            r<<=2;
        }
        return r;
    }
    private int reduce(long value)
    {
        return (int)(ratio>1?(value/ratio):value);
    }
    private long extend(int value)
    {
        return (ratio>1&&value!=max)?value*ratio:value;
    }

    public void close()
    {
        gp.close();
    }

    public void setPrefix(String prefix)
    {
        gp.setPrefix(prefix);
    }

    public void setMax(long max)
    {
        this.ratio = setup(max);
        gp.setMax(reduce(max));
    }

    public long getVal()
    {
        return val;
    }

    public void setShow(boolean showPrev, boolean showNext, boolean showFull)
    {
        gp.setShow(showPrev, showNext, showFull);
    }

    public void start(long max)
    {
        this.ratio = setup(max);
        gp.start(reduce(max));
    }

    public boolean isStarted()
    {
        return gp.isStarted();
    }

    public void step(long n)
    {
        val+=n;
        gp.setVal(reduce(val));
    }

    public void step()
    {
        step(1);
    }

    public void setVal(long n)
    {
        this.val=n;
        gp.setVal(reduce(n));
    }

    public long getMax()
    {
        return max;
    }

    public void start()
    {
        gp.start();
    }

    public String getPrefix()
    {
        return gp.getPrefix();
    }

    public void start(long max, String prefix)
    {
        this.ratio = setup(max);
        gp.start(reduce(max), prefix);
    }
    public double getDone()
    {
        return gp.getDone();
    }
}
    

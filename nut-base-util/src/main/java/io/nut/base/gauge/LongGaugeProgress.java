/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
    

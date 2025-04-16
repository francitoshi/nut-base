/*
 *  AbstractGaugeProgress.java
 *
 *  Copyright (c) 2012-2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.gauge;

import io.nut.base.time.JavaTime;
import io.nut.base.time.JavaTime.Resolution;
import io.nut.base.util.Utils;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;

/**
 *
 * @author franci
 */
public abstract class AbstractGaugeProgress implements GaugeProgress, GaugeView
{
    private static final long NANOS_PER_MILLIS=Utils.NANOS_PER_MILLIS;//1000000;
    //minimal difference to paint again
    private static final int MIN_DIFF_STEP = 1;
    private static final double MIN_DIFF_TIME = 2000;
    //enough value to show times
    private static final double MIN_SHOW_DONE = 0.01;
    private static final long MIN_SHOW_TIME = 30000*NANOS_PER_MILLIS;

    private final Object lock = new Object();
    //--- time variables
    private volatile long accuNanos = 0;   // nanosegundos acumulados
    private volatile long lastNanos = 0;   // momento del ultimo cálculo
    private volatile long accuValue = 0;   // acumula el contador
    private volatile long lastPaint = 0;   // momento del ultimo paint
    // --- value variables
    private volatile int curValue = 0;     // valor actual
    private volatile int maxValue = 100;   // valor máximo
    //--- porcentajes
    private volatile double done = 0.0;  // porcentaje atual
    //--- otros
    private volatile boolean started=false;
    private volatile boolean paused =false;
    private volatile boolean force = false;
    private volatile String prefix = "";
    // --- modos
    private volatile boolean showPrev = false;
    private volatile boolean showNext = true;
    private volatile boolean showFull = false;
    static final NumberFormat fmt = NumberFormat.getPercentInstance(Locale.US);
    
    static final int FMT_TIME_MIN_ELEMENTS = 1;
    static final int FMT_TIME_MAX_ELEMENTS = 3;
    static final Resolution FMT_TIME_RES = Resolution.S;
    
    static
    {
        fmt.setMinimumIntegerDigits(2);
        fmt.setMinimumFractionDigits(2);
        fmt.setMaximumFractionDigits(2);
    }

    @Override
    public String getPrefix()
    {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix)
    {
        force = force || !prefix.equals(this.prefix);
        this.prefix = prefix;
    }

    public AbstractGaugeProgress()
    {
        super();
    }
    @Override
    public boolean isStarted()
    {
        return started;
    }
    public boolean isPaused()
    {
        return paused;
    }

    public void start()
    {
        start(100,"");
    }
    public void start(int max)
    {
        start(max,"");
    }
    public void start(int max, String prefix)
    {
        synchronized(lock)
        {
            this.started =true;
            this.paused =false;
            this.prefix = prefix;
            this.accuNanos = 0;
            this.lastNanos = nanoTime();
            this.accuValue = 0;
            this.lastPaint = 0;
            this.curValue = 0;
            this.maxValue = max;
            this.done = 0.0;
            this.force = true;
        }
        this.paintLazy();
    }

    public void pause()
    {
        synchronized(lock)
        {
            setValue(curValue);
            paused = true;
            paintLazy();
        }
    }

    public void resume()
    {
        synchronized(lock)
        {
            setValue(curValue);
            paintLazy();
        }
    }

    public void close()
    {
        synchronized(lock)
        {
            started = false;
            curValue = maxValue;
            force = true;
        }
        paintLazy();
    }

    public double getDone()
    {
        return done;
    }

    public int getVal()
    {
        return curValue;
    }

    public int getMax()
    {
        return maxValue;
    }

    private void setValue(int n)
    {
        synchronized(lock)
        {
            long now = nanoTime();
            if(paused)
            {
                lastNanos = now;
                paused = false;
            }
            accuNanos += (now-lastNanos);
            accuValue += (n>curValue ? n-curValue : 0);
            lastNanos  = now;
            curValue   = n;
        }
    }
    public void setVal(int n)
    {
        setValue(n);
        paintLazy();
    }

    public void setMax(int n)
    {
        if(maxValue!=n)
        {
            synchronized(lock)
            {
                maxValue = n;
            }
            paintLazy();
        }
    }

    public void step()
    {
        synchronized(lock)
        {
            setValue(curValue+1);
        }
        paintLazy();
    }

    public void step(int n)
    {
        synchronized(lock)
        {
            setVal(curValue+n);
        }
        paintLazy();
    }

    public abstract void paint(boolean started, int max, int val, String prefix, double done, String msg);

    private void paintLazy()
    {
        synchronized(lock)
        {
            long now = nanoTime();
            double cur = (double) curValue / (double) maxValue;
            double dif = Math.abs(done - cur);
            long difTime = now - lastPaint;

            if (dif >= MIN_DIFF_STEP || difTime >= MIN_DIFF_TIME || force)
            {
                done = cur;
                lastPaint = now;

                String txt = prefix + " " + fmt.format(done);

                if (done >= MIN_SHOW_DONE || (accuNanos) >= MIN_SHOW_TIME)
                {
                    long unitNanos = accuValue>0 ? accuNanos/accuValue : 0;
                    long prevNanos = accuNanos;
                    long nextNanos = (maxValue-curValue)*unitNanos;
                    long fullNanos = prevNanos + nextNanos;
                    if (showPrev)
                    {
                        txt += " (" + fmtTime(prevNanos) + ")";
                    }
                    if (showNext)
                    {
                        txt += " [" + fmtTime(nextNanos) + "]";
                    }
                    if (showFull)
                    {
                        txt += " <" + fmtTime(fullNanos) + ">";
                    }
                }
                force = false;
                paint(started, maxValue, curValue, prefix, done, txt);
            }
        }
    }

    public boolean isShowPrev()
    {
        return showPrev;
    }

    public void setShowPrev(boolean showPrev)
    {
        this.showPrev = showPrev;
    }

    public boolean isShowNext()
    {
        return showNext;
    }

    public void setShowNext(boolean showNext)
    {
        this.showNext = showNext;
    }

    public boolean isShowFull()
    {
        return showFull;
    }

    public void setShowFull(boolean showFull)
    {
        this.showFull = showFull;
    }
    public void setShow(boolean showPrev, boolean showNext, boolean showFull)
    {
        this.showPrev = showPrev;
        this.showNext = showNext;
        this.showFull = showFull;
    }

    final public void invalidate()
    {
        this.force = true;
        paintLazy();
    }
    
    protected long nanoTime()
    {
        return System.nanoTime();
    }

    protected long getAccuNanos()
    {
        return accuNanos;
    }

    protected long getAccuValue()
    {
        return accuValue;
    }

    public long getLastNanos()
    {
        return lastNanos;
    }

    private String fmtTime(long nanos)
    {
        Duration duration = Duration.ofNanos(nanos);
        return JavaTime.toString(duration, FMT_TIME_MIN_ELEMENTS, FMT_TIME_MAX_ELEMENTS, FMT_TIME_RES);
    }

}

/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.gauge;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class DebugGauge implements GaugeProgress, GaugeView 
{
    private final GaugeProgress gp;
    private final GaugeView gv;
    private final String name;
    private final Logger logger;
    
    private DebugGauge(GaugeProgress gp)
    {
        this.gp = gp;
        this.gv = null;
        this.name = gp.getClass().getName();
        this.logger =  Logger.getLogger(this.name);
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "<init>({0})", name);
        }
    }
    private DebugGauge(GaugeView gv)
    {
        this.gp = null;
        this.gv = gv;
        this.name = gv.getClass().getName();
        this.logger =  Logger.getLogger(this.name);
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "<init>({0})", name);
        }
    }
    private DebugGauge(Gauge gauge)
    {
        this.gp = gauge;
        this.gv = gauge;
        this.name = gauge.getClass().getName();
        this.logger =  Logger.getLogger(this.name);
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "<init>({0})", name);
        }
    }
    
    public static GaugeProgress wrap(GaugeProgress gp)
    {
        return new DebugGauge(gp);
    }
    public static GaugeView wrap(GaugeView gv)
    {
        return new DebugGauge(gv);
    }
    public static DebugGauge wrap(Gauge gauge)
    {
        return new DebugGauge(gauge);
    }

    @Override
    public void start()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.start()", name);
        }
        gp.start();
    }

    @Override
    public void start(int max)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.start({1})", new Object[]{name, max});
        }
        gp.start(max);
    }
    @Override
    public void start(int max, String prefix)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.start({1},{2})", new Object[]{name, max, prefix});
        }
        gp.start(max, prefix);
    }

    @Override
    public void close()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.close()", name);
        }
        gp.close();
    }

    @Override
    public void setPrefix(String prefix)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setPrefix({1})", new Object[]{name, prefix});
        }
        gp.setPrefix(prefix);
    }

    @Override
    public String getPrefix()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getPrefix()", name);
        }
        return gp.getPrefix();
    }

    @Override
    public double getDone()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getDone()", name);
        }
        return gp.getDone();
    }

    @Override
    public int getVal()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getVal()", name);
        }
        return gp.getVal();
    }

    @Override
    public int getMax()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getMax()", name);
        }
        return gp.getMax();
    }

    @Override
    public void setVal(int n)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setVal({1})", new Object[]{name, n});
        }
        gp.setVal(n);
    }

    @Override
    public void setMax(int n)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setMax({1})", new Object[]{name, n});
        }
        gp.setMax(n);
    }

    @Override
    public void step()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.step()", name);
        }
        gp.step();
    }
    @Override
    public void step(int n)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.step({1})", new Object[]{name, n});
        }
        gp.step(n);
    }

    @Override
    public void setShow(boolean showPrev, boolean showNext, boolean showFull)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setShow({1}, {2}, {3})", new Object[]{name, showPrev, showNext, showFull});
        }
        gp.setShow(showPrev, showNext, showFull);
    }

    @Override
    public boolean isStarted()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.isStarted()", name);
        }
        return gp.isStarted();
    }
    @Override
    public boolean isPaused()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.isPaused()", name);
        }
        return gp.isPaused();
    }
    @Override
    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.paint({1}, {2}, {3}, {4}, {5}, {6}, {7}, {8})", new Object[]{name, started, max, val, done, prefix, prev, next, full});
        }
        gv.paint(started, max, val, done, prefix, prev, next, full);
    }

    @Override
    public void pause()
    {
        gp.pause();
    }

    @Override
    public void resume()
    {
        gp.resume();
    }
}

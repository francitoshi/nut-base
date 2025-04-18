/*
 * DebugGauge.java
 *
 * Copyright (c) 2012-2025  francitoshi@gmail.com
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package io.nut.base.gauge;

import io.nut.base.gauge.Gauge;
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

    public void start()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.start()", name);
        }
        gp.start();
    }

    public void start(int max)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.start({1})", new Object[]{name, max});
        }
        gp.start(max);
    }
    public void start(int max, String prefix)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.start({1},{2})", new Object[]{name, max, prefix});
        }
        gp.start(max, prefix);
    }

    public void close()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.close()", name);
        }
        gp.close();
    }

    public void setPrefix(String prefix)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setPrefix({1})", new Object[]{name, prefix});
        }
        gp.setPrefix(prefix);
    }

    public String getPrefix()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getPrefix()", name);
        }
        return gp.getPrefix();
    }

    public double getDone()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getDone()", name);
        }
        return gp.getDone();
    }

    public int getVal()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getVal()", name);
        }
        return gp.getVal();
    }

    public int getMax()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.getMax()", name);
        }
        return gp.getMax();
    }

    public void setVal(int n)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setVal({1})", new Object[]{name, n});
        }
        gp.setVal(n);
    }

    public void setMax(int n)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setMax({1})", new Object[]{name, n});
        }
        gp.setMax(n);
    }

    public void step()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.step()", name);
        }
        gp.step();
    }
    public void step(int n)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.step({1})", new Object[]{name, n});
        }
        gp.step(n);
    }

    public void setShow(boolean showPrev, boolean showNext, boolean showFull)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.setShow({1}, {2}, {3})", new Object[]{name, showPrev, showNext, showFull});
        }
        gp.setShow(showPrev, showNext, showFull);
    }

    public boolean isStarted()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.isStarted()", name);
        }
        return gp.isStarted();
    }
    public boolean isPaused()
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.isPaused()", name);
        }
        return gp.isPaused();
    }
    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, "{0}.paint({1}, {2}, {3}, {4}, {5}, {6}, {7}, {8})", new Object[]{name, started, max, val, done, prefix, prev, next, full});
        }
        gv.paint(started, max, val, done, prefix, prev, next, full);
    }

    public void pause()
    {
        gp.pause();
    }

    public void resume()
    {
        gp.resume();
    }
}

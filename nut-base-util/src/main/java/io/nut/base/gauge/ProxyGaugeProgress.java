/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.gauge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author franci
 */
final public class ProxyGaugeProgress extends AbstractGauge
{
    private volatile List<GaugeView> views =  new ArrayList<>();

    public ProxyGaugeProgress()
    {
    }
    public ProxyGaugeProgress(GaugeView... view)
    {
        setView(view);
    }

    public void setView(GaugeView... view)
    {
        this.views = view != null ? Arrays.asList(view) : new ArrayList<>();
        super.invalidate();
    }

    @Override
    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
        for(GaugeView item : views)
        {
            if(item!=null)
            {
                item.paint(started, max, val, done, prefix, prev, next, full);
            }
        }
    }
    
}

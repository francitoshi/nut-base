/*
 *  ProxyGaugeProgress.java
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

/*
 *  SimpleMovingAverage.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.stats;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author franci
 */
public class SimpleMovingAverage extends MovingAverage 
{
    private int count;
    private final int period;
    
    private double sum = 0.0;
    private double sma;
    private final Queue<Double> queue;
    
    public SimpleMovingAverage(int period)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.queue = new ArrayBlockingQueue<>(period);
    }

    @Override
    public double next(double value)
    {
        if(count++ < period)
        {
            sum += value;
            sma = sum/count;
        }
        else
        {
            sum = sum + value - queue.remove();
            sma = sum / period;
        }
        this.queue.add(value);
        return sma;
    }
    
}

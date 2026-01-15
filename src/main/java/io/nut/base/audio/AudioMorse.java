/*
 * MorseDepattern.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.audio;

import io.nut.base.morse.Morse;
import io.nut.base.stats.SimpleMovingAverage;
import io.nut.base.util.concurrent.Generator;
import java.util.Arrays;
import javax.sound.sampled.AudioInputStream;

public class AudioMorse extends Generator<String>
{
    private volatile boolean active;
    private final AudioInputStream ais;
    private final int blockMillis;
    private final int flushMillis;
    private final Morse morse;

    private volatile double threshold  = 5;
    
    private final AudioGoertzel audioGoertzel;
    public AudioMorse(AudioInputStream ais, int hz, boolean hannWindow, boolean overlap, int blockMillis, int capacity)
    {
        super(capacity);
        this.ais = ais;
        this.blockMillis = blockMillis;
        this.audioGoertzel = new AudioGoertzel(ais, hz, hannWindow, overlap, blockMillis, capacity);
        this.morse = new Morse();
        this.flushMillis = Math.max(blockMillis*this.morse.maxUnits*4, 2000);
    }
    
    private final Generator<Integer> audio2pattern = new Generator<Integer>()
    {
        @Override
        public void run()
        {
            int status = 0;

            SimpleMovingAverage sma = new SimpleMovingAverage(blockMillis);
            int i=0;
            int ms = 0;

            for(double e : audioGoertzel)
            {
                if(!active) 
                {
                    return;
                }
                
                //666 threshold = audioGoertzel.getThreshold();
                
                if(status>0 && e>threshold)
                {
                    status = 1;
                    ms += blockMillis;
                    continue;
                }
                else if(status<=0 && e<=threshold)
                {
                    status = -1;
                    ms += blockMillis;
                    continue;
                }

                this.yield(ms);

                if(e>threshold)
                {
                    status = 1;
                }
                else if(e<=threshold)
                {
                    status = -1;
                }
                ms = blockMillis;
            }
            this.yield(ms);
        }
    };

    private final Generator<int[]> pattern2chunks = new Generator<int[]>()
    {
        @Override
        public void run()
        {
            int count = 0;
            int acum = 0;
            int[] chunk = new int[flushMillis];
            for(int ms : audio2pattern)
            {
                if(!active) 
                {
                    return;
                }
                chunk[count++] = ms;
                acum += ms;
                if(acum>flushMillis || count>=chunk.length)
                {
                    int[] pattern = Arrays.copyOf(chunk, count);
                    this.yield(pattern);
                    count = 0;
                    acum = 0;
                }
            }
            if(count>0)
            {
                int[] pattern = Arrays.copyOf(chunk, count);
                this.yield(pattern);
            }
        }
    };

    @Override
    public void run()
    {
        active = true;
        try
        {
            morse.decodePattern(pattern2chunks, (letter) -> this.yield(letter));
        }
        finally
        {
            active = false;
        }
    }
    
}

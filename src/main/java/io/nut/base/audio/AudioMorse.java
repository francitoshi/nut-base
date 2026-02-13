/*
 * AudioMorse.java
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

import io.nut.base.signal.Morse;
import io.nut.base.stats.MovingAverage;
import io.nut.base.util.concurrent.Generator;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioInputStream;

public class AudioMorse extends Generator<String>
{
    static final double BETA = 0.2; // Factor para cálculo conservador del umbral (0.1-0.3)
    
    private final int blockMillis;
    private final int flushChunks;
    private final Morse morse;

    private final AudioEnergy audioGoertzel;
    private final AdaptiveThreshold adaptiveThreshold;

    public AudioMorse(AudioInputStream ais, int hz, int flags, int blockMillis, int capacity)
    {
        super(capacity);
        this.blockMillis = blockMillis;
        this.morse = new Morse();
        this.flushChunks = this.morse.maxTerms*2+2;
        this.audioGoertzel = new AudioEnergy(ais, hz, flags, blockMillis, capacity);
        this.adaptiveThreshold = new AdaptiveThreshold(BETA, blockMillis*blockMillis);
    }
    
    private final Generator<Integer> audio2pattern = new Generator<Integer>(capacity)
    {
        @Override
        public void run()
        {
            final MovingAverage msEMA = MovingAverage.createEMA(morse.maxTerms*2);
            int status = 0;
            int splitMillis = 1234;
            int ms = 0;
            for(double[] e : audioGoertzel)
            {
                if(isTerminated()) 
                {
                    return;
                }
                
                boolean pulse = adaptiveThreshold.update(e[0]);

                if(status>0 && pulse)
                {
                    status = 1;
                    ms += blockMillis;
                    continue;
                }
                else if(status<=0 && !pulse)
                {
                    status = -1;
                    ms += blockMillis;
                    if(ms>splitMillis)
                    {
                        this.yield(ms);
                        this.yield(0);
                        ms = 0;
                    }
                    continue;
                }
                
                status = pulse ? +1 : -1;

                splitMillis = ms>0 ? (int) (Math.min(ms, msEMA.next(ms))*morse.maxTerms*2) : splitMillis;
                this.yield(ms);
                ms = blockMillis;
            }
            if(status<0)
            {
                ms = (int) Math.max(ms,msEMA.average());
            }
            this.yield(ms);
        }
    };

    private final Generator<int[]> pattern2chunks = new Generator<int[]>(capacity)
    {
        @Override
        public void run()
        {
            int count = 0;
            int[] chunk = new int[flushChunks];
            for(int ms : audio2pattern)
            {
                if(isTerminated()) 
                {
                    return;
                }
                chunk[count++] = ms;
                if(count>=chunk.length)
                {
                    int[] pattern = Arrays.copyOf(chunk, count);
                    this.yield(pattern);
                    count = 0;
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
        try
        {
            morse.decodePattern(pattern2chunks, (letter) -> this.yield(letter));
        }
        finally
        {

        }
    }

    @Override
    public void shutdownNow()
    {
        audioGoertzel.shutdownNow();
        audio2pattern.shutdownNow();
        pattern2chunks.shutdownNow();
        super.shutdownNow();
    }

    @Override
    public void shutdown()
    {
        audioGoertzel.shutdown();
        audio2pattern.shutdown();
        pattern2chunks.shutdown();
        super.shutdown();
    }

    public long skipAvailable() throws IOException
    {
        return audioGoertzel.skipAvailable();
    }    
    
}

/*
 * AudioGoertzel.java
 *
 * Copyright (c) 2025-2026 francitoshi@gmail.com
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

import static io.nut.base.audio.Audio.DCOFFSET;
import static io.nut.base.audio.Audio.HANNWINDOW;
import static io.nut.base.audio.Audio.OVERLAP;
import io.nut.base.util.concurrent.Generator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class AudioGoertzel extends Generator<double[]>
{
    private final Object lock = new Object();
    private final AudioInputStream ais;
    private final int hz;
    private final boolean hannWindow;
    private final boolean overlap;
    private final boolean detectDCOffset;
    private final int blockMillis;

    public AudioGoertzel(AudioInputStream ais, int hz, int flags, int blockMillis, int capacity)
    {
        super(capacity);
        this.ais = ais;
        this.hz = hz;
        this.hannWindow     = (flags & HANNWINDOW) == HANNWINDOW;
        this.overlap        = (flags & OVERLAP)    == OVERLAP;
        this.detectDCOffset = (flags & DCOFFSET)   == DCOFFSET;
        this.blockMillis = blockMillis;
    }
    
    @Override
    public void run()
    {
        try
        {
            AudioFormat fmt = Audio.getFloatMono(ais.getFormat(), false);

            int blockBytes = Audio.requiredBytes(fmt, blockMillis);
            int blockSamples = Audio.requiredSamples(fmt, blockMillis);
            int workSamples = overlap ? blockSamples*2 : blockSamples;
            
            float sampleRate = fmt.getFrameRate();
            boolean be = fmt.isBigEndian();
            
            AudioInputStream input = Audio.getAudioInputStream(ais, fmt);
            
            float[] hann = hannWindow ? Wave.hannWindow(new float[workSamples]) : null;
            float[] work = new float[workSamples];
            float[][] half = overlap ? new float[2][blockSamples] : new float[1][workSamples];
            byte[] read = new byte[Float.BYTES*half[0].length];
            double[] energies = new double[3];
            FloatBuffer buffer = ByteBuffer.wrap(read).order(be ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            int w = overlap ? 1 : 0;
            int time = 0;
            int pendingStableBlocks = detectDCOffset ? 2 : 0;
            for(int round=0; !isShutdown() ;round++, time+= blockMillis)
            {
                int r;
                synchronized (lock)
                {
                    r = input.read(read);
                    if(r<0)
                    {
                        break;
                    }
                }
                int readSamples = r/Float.BYTES;
                for(int i=0;i<readSamples && i<half[w].length;i++)
                {
                    half[w][i] = buffer.get(i);
                }
                for(int i=readSamples;i<half[w].length;i++)
                {
                    half[w][i] = 0;
                }
                if(round>0 || !overlap)
                {
                    //build work buffer
                    for(int i=0, p=0; i < half.length; i++)
                    {
                        for(int j=0;j<half[i].length;j++,p++)
                        {
                            work[p] = half[i][j]; 
                        }
                    }

                    if(pendingStableBlocks>0)
                    {
                        if(Audio.detectDCOff(work, sampleRate, 0.1f))
                        {
                            pendingStableBlocks = detectDCOffset ? 2 : 0;
                            continue;
                        }                        
                        pendingStableBlocks--;
                        if(pendingStableBlocks>0)
                        {
                            continue;
                        }
                        if(pendingStableBlocks==0)
                        {
                            Audio.applyFadeIn(work);
                        }
                    }                   

                    float freq = this.hz!=0 ? this.hz : Audio.detectHz(work, sampleRate, 0.01f);
                    
                    if(hann!=null)
                    {
                        for (int i = 0; i < blockSamples; i++)
                        {
                            work[i] *= hann[i]; 
                        }
                    }
                    
                    if(freq!=0)
                    {
                        energies[0] = Audio.goertzelPower(work, sampleRate, freq);
                    }
                    else
                    {
                        energies[0] = 0;
                        energies[1] = 0;
                        energies[2] = 0;
                    }
                    this.yield(energies.clone());
                }
                if(overlap)
                {
                    float[] tmp = half[0];
                    half[0] = half[1];
                    half[1] = tmp;
                }
            }            
        }
        catch (IOException ex)
        {
            Logger.getLogger(AudioGoertzel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public long skipAvailable() throws IOException
    {
        synchronized (lock)
        {
            int n = ais.available();
            return n>0 ? ais.skip(n) : 0;
        }
    }

}

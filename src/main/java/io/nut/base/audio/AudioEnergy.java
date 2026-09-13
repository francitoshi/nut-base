/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.audio;

import static io.nut.base.audio.Audio.ADJUST_START;
import static io.nut.base.audio.Audio.DCOFFSET;
import static io.nut.base.audio.Audio.HANNWINDOW;
import static io.nut.base.audio.Audio.OVERLAP;
import io.nut.base.util.concurrent.Generator;
import io.nut.base.util.Exceptions;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class AudioEnergy extends Generator<double[]>
{
    private static final Logger LOG = Logger.getLogger(AudioEnergy.class.getName());

    private final Object lock = new Object();
    private final AudioInputStream ais;
    private final int[] hz;
    private final boolean hannWindow;
    private final boolean overlap;
    private final boolean detectDCOffset;
    private final boolean adjustStart;
    private final int blockMillis;
    private final EnergyDetector energyDetector;

    public AudioEnergy(AudioInputStream ais, int hz, int flags, int blockMillis, int capacity)
    {
        this(ais, new int[]{hz}, flags, blockMillis, capacity, null);
    }
    public AudioEnergy(AudioInputStream ais, int hz, int flags, int blockMillis, int capacity, EnergyDetector energyDetector)
    {
        this(ais, new int[]{hz}, flags, blockMillis, capacity, energyDetector);
    }
    public AudioEnergy(AudioInputStream ais, int[] hz, int flags, int blockMillis, int capacity)
    {
        this(ais, hz, flags, blockMillis, capacity, null);
    }
    public AudioEnergy(AudioInputStream ais, int[] hz, int flags, int blockMillis, int capacity, EnergyDetector energyDetector)
    {
        super(capacity);
        this.ais = ais;
        this.hz = hz;
        this.hannWindow     = (flags & HANNWINDOW) == HANNWINDOW;
        this.overlap        = (flags & OVERLAP)    == OVERLAP;
        this.detectDCOffset = (flags & DCOFFSET)   == DCOFFSET;
        this.adjustStart    = (flags & ADJUST_START)== ADJUST_START;
        this.blockMillis = blockMillis;
        this.energyDetector = energyDetector!=null ? energyDetector : EnergyDetector.GOERTZEL_POWER;
    }
    
    @Override
    public void run()
    {
        try
        {
            AudioFormat fmt = Audio.getFloatMono(ais.getFormat(), false);

            int blockSamples = Audio.msToSamples(blockMillis, fmt);
            int workSamples = overlap ? blockSamples*2 : blockSamples;
            
            float sampleRate = fmt.getFrameRate();
            boolean be = fmt.isBigEndian();
            
            AudioInputStream input = Audio.getAudioInputStream(ais, fmt);
            
            float[] hann = hannWindow ? Wave.hannWindow(new float[workSamples]) : null;
            float[] work = new float[workSamples];
            float[][] half = overlap ? new float[2][blockSamples] : new float[1][workSamples];
            byte[] read = new byte[Float.BYTES*half[0].length];
            double[] energies = new double[hz.length];
            FloatBuffer buffer = ByteBuffer.wrap(read).order(be ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            int w = overlap ? 1 : 0;

            if(detectDCOffset)
            {
                Audio.skipDCOff(ais, 0.1f, blockMillis, 2);
            }
            if(adjustStart)
            {
                Audio.skipSilence(ais, 0.1f, blockMillis);
            }
            
            for(int round=0; !isShutdown() ;round++)
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

                    int[] freq = (hz.length==1 && hz[0]==0) ? new int[]{ (int)Audio.detectHz(work, sampleRate, 0.01f) } : hz;
                    
                    if(hann!=null)
                    {
                        for (int i = 0; i < blockSamples; i++)
                        {
                            work[i] *= hann[i]; 
                        }
                    }

                    for(int i=0;i<freq.length;i++)
                    {
                        energies[i] = freq[i]!=0 ? energyDetector.getEnergy(work, sampleRate, freq[i]) : 0;
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
            Exceptions.severe(LOG, ex);
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

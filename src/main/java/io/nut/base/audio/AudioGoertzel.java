/*
 * MorseGoertzel.java
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

import io.nut.base.stats.MovingAverage;
import io.nut.base.util.concurrent.Generator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

public class AudioGoertzel extends Generator<Double>
{
    private volatile boolean active;
    private final AudioInputStream ais;
    private final int hz;
    private final boolean hannWindow;
    private final boolean overlap;
    private final int blockMillis;
    private volatile double threshold;

    public AudioGoertzel(AudioInputStream ais, int hz, boolean hannWindow, boolean overlap, int blockMillis, int capacity)
    {
        super(capacity);
        this.ais = ais;
        this.hz = hz;
        this.hannWindow = hannWindow;
        this.overlap = overlap;
        this.blockMillis = blockMillis;
    }
    
    @Override
    public void run()
    {
        try
        {
            this.active = true;
            AudioFormat fmt = Audio.getFloatMono(ais.getFormat(), false);
            int blockSize = Audio.bytesNeeded(fmt, blockMillis);
            float sampleRate = fmt.getFrameRate();
            boolean be = fmt.isBigEndian();
            
            AudioInputStream input = Audio.getAudioInputStream(ais, fmt);          
            
            double[] hann = hannWindow ? Wave.hanningWindow(new double[blockSize]) : null;
            double[] work = new double[blockSize];
            double[][] half = overlap ? new double[2][blockSize/2] : new double[1][blockSize];
            byte[] read = new byte[Float.BYTES*half[0].length];
            FloatBuffer buffer = ByteBuffer.wrap(read).order(be ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            int w = overlap ? 1 : 0;
            for(int round=0;this.active;round++)
            {
                int r = input.read(read);
                if(r<0)
                {
                    break;
                }
                int s = r/Float.BYTES;
                for(int i=0;i<s && i<half[w].length;i++)
                {
                    half[w][i] = buffer.get(i);
                }
                for(int i=s;i<half[w].length;i++)
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

                    double freq = this.hz!=0 ? this.hz : Audio.detectHz(work, sampleRate, 0.01f);

                    if(hann!=null)
                    {
                        for (int i = 0; i < blockSize; i++)
                        {
                            work[i] *= hann[i]; 
                        }
                    }
                    double energy = freq!=0 ? Audio.goertzelPower(work, sampleRate, freq) : 0;
                    threshold = updateThresholdSimple(energy);
                    this.yield(energy);                    
                }
                if(overlap)
                {
                    double[] tmp = half[0];
                    half[0] = half[1];
                    half[1] = tmp;
                }
            }            
        }
        catch (IOException ex)
        {
            Logger.getLogger(AudioGoertzel.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            this.active = false;
        }
    }
 
// Parámetros de configuración
    private static final double ALPHA_FAST = 0.1;    // Adaptación rápida (señal)
    private static final double ALPHA_SLOW = 0.01;   // Adaptación lenta (ruido)
    private static final double ALPHA_VARIANCE = 0.05; // Para varianza
    private static final double INITIAL_THRESHOLD = 0.015;
    private static final double MIN_THRESHOLD = 0.005;
    private static final double MAX_THRESHOLD = 0.5;
    
    // Estado interno (lo único que guardamos)
    private volatile double emaSignal = INITIAL_THRESHOLD * 2;   // Media móvil exponencial de señal
    private volatile double emaNoise = INITIAL_THRESHOLD * 0.5;       // Media móvil exponencial de ruido
    private volatile double emaVariance = 0.001;    // Varianza estimada
    private volatile double currentThreshold = INITIAL_THRESHOLD;
    private volatile boolean lastWasTone = false;

    private double updateThresholdSimple(double energy) 
    {
        // Decidir si actualizar como señal o ruido
        if (energy > threshold) 
        {
            emaSignal = 0.9 * emaSignal + 0.1 * energy;
        } 
        else 
        {
            emaNoise = 0.99 * emaNoise + 0.01 * energy;
        }
        
        // Umbral = promedio ponderado
        threshold = 0.3 * emaNoise + 0.7 * emaSignal;
        threshold = Math.max(MIN_THRESHOLD, Math.min(MAX_THRESHOLD, threshold));
        
        return threshold;
    }    

    public double getThreshold()
    {
        return threshold;
    }
    
}

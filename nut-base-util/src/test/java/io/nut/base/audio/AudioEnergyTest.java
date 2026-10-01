/*
 * AudioEnergyTest.java
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

import static io.nut.base.audio.Audio.HANNWINDOW;
import static io.nut.base.audio.Audio.OVERLAP;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.jupiter.api.Test;

public class AudioEnergyTest
{
    static final String MORSE1 = "morse1-quixote-8000-mono-u8.wav.gz";
    
    static final int HZ = 550;
    static final int BR = 8000;
    static final boolean SHOW = false;
    
    static AudioInputStream getAIS() throws UnsupportedAudioFileException, IOException
    {
        InputStream in = new BufferedInputStream(new GZIPInputStream(AudioEnergyTest.class.getResourceAsStream(MORSE1)));
        return AudioSystem.getAudioInputStream(in);
    }

    @Test
    public void testRun() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = getAIS();
        AudioEnergy mg = new AudioEnergy(ais, HZ, 0, 5, 5);
        int num = 0;
        
        if(SHOW) System.out.println("testRun");
        for(double[] energy : mg)
        {
            if(SHOW) System.out.printf("%.2f\n", energy[0]);
            if(num++>BR)break;
        }
    }

    /**
     * Test of run method, of class MorseGoertzel.
     */
    @Test
    public void testRunHann() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = getAIS();
        AudioEnergy mg = new AudioEnergy(ais, HZ, HANNWINDOW, 5, 5);

        int num = 0;
        if(SHOW) System.out.println("testRunHann");
        for(double[] energy : mg)
        {
            if(SHOW) System.out.printf("%.2f\n",energy[0]);
            if(num++>BR)break;
        }
    }
    
    /**
     * Test of run method, of class MorseGoertzel.
     */
    @Test
    public void testRunOverlap() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = getAIS();
        AudioEnergy mg = new AudioEnergy(ais, HZ, OVERLAP, 5, 5);

        int num = 0;
        if(SHOW) System.out.println("testRunOverlap");
        for(double[] energy : mg)
        {
            if(SHOW) System.out.printf("%.2f\n",energy[0]);
            if(num++>BR)break;
        }
    }

    /**
     * Test of run method, of class MorseGoertzel.
     */
    @Test
    public void testRunHannOverlap() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = getAIS();
        AudioEnergy mg = new AudioEnergy(ais, HZ, HANNWINDOW|OVERLAP, 5, 5);

        int num = 0;
        if(SHOW) System.out.println("testRunHannOverlap");
        for(double[] energy : mg)
        {
            if(SHOW) System.out.printf("%.2f\n", energy[0]);
            if(num++>BR)break;
        }
    }   
}

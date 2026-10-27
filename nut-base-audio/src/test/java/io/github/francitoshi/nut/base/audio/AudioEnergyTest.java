/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

import static io.github.francitoshi.nut.base.audio.Audio.HANNWINDOW;
import static io.github.francitoshi.nut.base.audio.Audio.OVERLAP;
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

/*
 * AudioTest.java
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class AudioTest
{
    static InputStream getIS() throws UnsupportedAudioFileException, IOException
    {
        return new BufferedInputStream(new GZIPInputStream(MorseGoertzelTest.class.getResourceAsStream("morse-8000hz-mono-u8.wav.gz")));
    }
    
    @Test
    public void testGetFloatMono() throws LineUnavailableException, IOException, UnsupportedAudioFileException
    {        
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS());
        AudioFormat fmt = Audio.getFloatMono(ais.getFormat(), false);
        try (AudioInputStream mono = Audio.getAudioInputStream(ais, fmt))
        {
            byte[] buffer = new byte[(int)(fmt.getSampleRate()*Float.BYTES)];
            while(mono.available()>0)
            {
                mono.read(buffer);
            }
        }
    }
    
    @Test
    public void testGetDoubleMono() throws LineUnavailableException, IOException, UnsupportedAudioFileException
    {        
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS());
        AudioFormat fmt = Audio.getDoubleMono(ais.getFormat(), false);
        try (AudioInputStream mono = Audio.getAudioInputStream(ais, fmt))
        {
            byte[] buffer = new byte[(int)(fmt.getSampleRate()*Double.BYTES)];
            while(mono.available()>0)
            {
                mono.read(buffer);
            }
        }
    }

    /**
     * Test of getAudioInputStream method, of class Audio.
     */
    @Test
    public void testGetAudioInputStream_InputStream() throws Exception
    {
        InputStream src = getIS();

        try (AudioInputStream ais = Audio.getAudioInputStream(src))
        {
            byte[] buffer = new byte[(int)(ais.getFormat().getSampleRate()*Double.BYTES)];
            while(ais.available()>0)
            {
                ais.read(buffer);
            }
        }
    }
    
    static final boolean ALLOW_SOUND = false;
    static final boolean SHOW = false;
    
    /**
     * Test of detectHz method, of class Audio.
     */
    @Test
    public void testDetectHz1() throws LineUnavailableException
    {
        Wave wave = Wave.SINE;
        AudioFormat format = Audio.PCM_8BIT_MONO;

        SourceDataLine lineOut = ALLOW_SOUND ? Audio.getLineOut(format) : null;
        
        for(int hz=400;hz<1200; hz += 100)
        {
            byte[] src = wave.build(format, hz, new byte[(int)format.getSampleRate()], 0.33);
            double[] dst = Audio.i8ToDouble(src);
            double result = Audio.detectHz(dst, format.getSampleRate(), 0.01f);

            if(lineOut!=null)
            {
                lineOut.write(src, 0, src.length);
                lineOut.drain();
            }            
            if(SHOW)
            {
                System.out.printf("%d => %.1f\n", hz, result);
            }
            assertEquals(hz, result, 0.1);
        }
    }
    @Test
    public void testDetectHz2() throws LineUnavailableException
    {
        Wave wave = Wave.SINE;
        AudioFormat format = Audio.PCM_CD_MONO;

        SourceDataLine lineOut = ALLOW_SOUND ? Audio.getLineOut(format) : null;
        
        for(int hz=400;hz<1200; hz += 100)
        {
            byte[] src = wave.build(format, hz, new byte[(int)format.getSampleRate()], 0.33);
            double[] dst = Audio.i16ToDouble(src, format.isBigEndian());
            double result = Audio.detectHz(dst, format.getSampleRate(), 0.01f);

            if(lineOut!=null)
            {
                lineOut.write(src, 0, src.length);
                lineOut.drain();
            }
            if(SHOW)
            {
                System.out.printf("%d => %.1f\n", hz, result);
            }
            assertEquals(hz, result, 0.1);
        }
    }
    
}

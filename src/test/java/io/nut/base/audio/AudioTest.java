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
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.jupiter.api.Test;

public class AudioTest
{
    @Test
    public void testGetFloatMono() throws LineUnavailableException, IOException, UnsupportedAudioFileException
    {        
        InputStream in = new BufferedInputStream(new GZIPInputStream(AudioTest.class.getResourceAsStream("morse-8000hz-mono-u8.wav.gz")));
        AudioInputStream ais = AudioSystem.getAudioInputStream(in);
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
        InputStream in = new BufferedInputStream(new GZIPInputStream(AudioTest.class.getResourceAsStream("morse-8000hz-mono-u8.wav.gz")));
        AudioInputStream ais = AudioSystem.getAudioInputStream(in);
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
    
    
}

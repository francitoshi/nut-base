/*
 * WaveTest.java
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

import org.junit.jupiter.api.Test;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author franci
 */
public class WaveTest
{
    static final boolean ALLOW_SOUND = false;
    static final boolean FADING = true;
    @Test
    public void testAll() throws LineUnavailableException
    {    
        AudioFormat[] formats = {Audio.PCM_CD_MONO, Audio.PCM_CD_STEREO, Audio.PCM_STUDIO_STEREO, Audio.PCM_RADIO_MONO, Audio.PCM_VOICE_WIDEBAND, Audio.PCM_8BIT_MONO, Audio.ALAW_TELEPHONY};
        
        for(int millis=0;millis<100;millis += formats.length)
        {
            for(AudioFormat format : formats)
            {
                int size = Audio.msToBytes(millis, format);
                int fading = FADING ? Audio.msToBytes(5, format) : 0;
                byte[] buffer = new byte[size];

                Wave wave = Wave.WAVES[millis%Wave.WAVES.length];
                wave.build(format, 440, buffer, 0.50, fading);
                
                if(ALLOW_SOUND)
                {
                    System.out.printf("MILLIS = %d | fading = %d\n", millis, fading);
                    try (SourceDataLine lineOut = Audio.getLineOut(format))
                    {
                        lineOut.start();
                        long t0 = System.nanoTime();
                        lineOut.write(buffer, 0, buffer.length);

                        lineOut.drain();
                        long t1 = System.nanoTime();
                        //System.out.printf("%s %d ms\n", wave.name, TimeUnit.NANOSECONDS.toMillis(t1-t0));
                        lineOut.stop();
                    }
                }
            }
        }
    }
}

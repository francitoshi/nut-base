/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

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
    @Test
    public void testAll() throws LineUnavailableException
    {    
        AudioFormat[] formats = {Audio.PCM_CD_MONO, Audio.PCM_CD_STEREO, Audio.PCM_STUDIO_STEREO, Audio.PCM_RADIO_MONO, Audio.PCM_VOICE_WIDEBAND, Audio.PCM_8BIT_MONO, Audio.ALAW_TELEPHONY};
        
        for(int millis=0;millis<100;millis += formats.length)
        {
            for(AudioFormat format : formats)
            {
                int size = Audio.msToBytes(millis, format);

                Wave wave = Wave.WAVES[millis%Wave.WAVES.length];
                byte[] buffer = wave.build(format, 440, 0.50);
                
                if(ALLOW_SOUND)
                {
                    System.out.printf("MILLIS = %d\n", millis);
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

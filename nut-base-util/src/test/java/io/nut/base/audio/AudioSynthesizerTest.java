/*
 * AudioModem.java
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
import java.io.File;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class AudioSynthesizerTest
{

    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testPlay_3args_1() throws Exception
    {
        try(AudioSynthesizer modem = new AudioSynthesizer(Audio.getLineOut(Audio.PCM_CD_MONO)))
        {
            modem.play(440, 100, 0.80);
            modem.drain();
        }
    }

    @Test
    public void testPlay_3args_2() throws Exception
    {
        File wav = File.createTempFile("tmp", ".wav");
        System.out.println(wav);
        
        try(AudioSynthesizer modem = new AudioSynthesizer(wav,Audio.PCM_CD_MONO))
        {
            modem.play(440, 100, 0.80);
            modem.drain();
        }
        finally
        {
            wav.delete();
        }
    }

    /**
     * Test of play method, of class AudioModem.
     */
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testPlay_3args_3() throws Exception
    {
        Morse morse = new Morse(20,20, 0, 0);
        
        int[] pattern = morse.encodePattern("hello world");
        try(AudioSynthesizer modem = new AudioSynthesizer(Audio.getLineOut(Audio.PCM_CD_MONO)))
        {
            modem.play(750, pattern, 0.80);
            modem.drain();
        }
    }
    
}

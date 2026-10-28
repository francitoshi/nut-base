/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

import io.github.francitoshi.nut.base.signal.Morse;
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

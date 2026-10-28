/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

import io.github.francitoshi.nut.base.signal.Frame;
import io.github.francitoshi.nut.base.commons.lang.Strings;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.LineUnavailableException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class AudioMorseTransceiverTest
{
    static final String PANGRAM = "Quick nymph bugs vex fjord waltz.";
    static final Frame framer = new Frame();
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testPangram() throws LineUnavailableException
    {
        String plaintext = "01234 56789 "+PANGRAM;

        byte[] frame = framer.createData('\0', '\0', (char)1, plaintext.getBytes(StandardCharsets.UTF_8));

        try(AudioMorseTransceiver instance = new AudioMorseTransceiver(800, 40, Wave.SQUARE).open())
        {
            CompletableFuture.runAsync(()-> instance.write(frame));
            byte[] frame2 = instance.read();
            framer.check(frame2);
            framer.getId(frame2);
            byte[] payload =framer.getPayload(frame2);
            framer.isData(frame2);
            
            String plaintext2 = new String(payload, StandardCharsets.UTF_8);
            
            assertEquals(plaintext, plaintext2);
        }
        
    }
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testLoop() throws LineUnavailableException
    {
        
        try(AudioMorseTransceiver instance = new AudioMorseTransceiver(800, 60, Wave.SQUARE).open())
        {
            CompletableFuture.runAsync(()-> 
            {
                for(int i=1;i<10;i++)
                {
                    char c = (char) ('0'+i);
                    String s = Strings.repeat(c, i);
                    byte[] frame = framer.createData('\0', '\0', (char)i, s.getBytes(StandardCharsets.UTF_8));
                    instance.write(frame);
                }
            });
            
            for(int i = 0;i<10;i++)
            {
                byte[] frame = instance.read();
                byte[] payload =framer.getPayload(frame);
                String plaintext = new String(payload, StandardCharsets.UTF_8);
                System.out.println(plaintext);
            }
        }
        
    }

    
}

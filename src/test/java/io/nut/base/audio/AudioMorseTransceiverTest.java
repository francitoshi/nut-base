/*
 * AudioMorseTransceiverTest.java
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

import io.nut.base.signal.Frame;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import java.nio.charset.StandardCharsets;
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
            Utils.async(()-> instance.write(frame));
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
            Utils.async(()-> 
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

/*
 * AudioMorseTransceiver.java
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

import static io.nut.base.audio.Audio.ADJUST_START;
import static io.nut.base.audio.Audio.DCOFFSET;
import static io.nut.base.audio.Audio.HANNWINDOW;
import static io.nut.base.audio.Audio.OVERLAP;
import io.nut.base.encoding.Encoding;
import io.nut.base.math.Nums;
import io.nut.base.signal.Morse;
import io.nut.base.signal.Transceiver;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;

/**
 *
 * @author franci
 */
public class AudioMorseTransceiver implements Transceiver, AutoCloseable
{
    static final String _CT_ = "<CT>";
    static final String _SK_ = "<SK>";
    
    final Morse morse;
    final AudioSynthesizer audioModem;
    final AudioMorse audioMorse;
    final int hz;
    final Wave wave;
    
    public AudioMorseTransceiver(int hz, int wpm, Wave wave) throws LineUnavailableException
    {
        int ms = Nums.gcd(5, Math.max(1, 1200 / wpm));
        this.morse = new Morse(wpm, wpm, 0, 5);
        this.audioModem = new AudioSynthesizer(Audio.getLineOut(Audio.PCM_CD_MONO));
        AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO, 441000));
        ais = Audio.getMarkable(ais);
        this.audioMorse = new AudioMorse(ais, hz, HANNWINDOW|OVERLAP|DCOFFSET|ADJUST_START, ms, 16);
        this.hz = hz;
        this.wave = wave;
    }
            
    @Override
    public AudioMorseTransceiver open()
    {
        return this;
    }

    @Override
    public void close()
    {
        try
        {
            audioModem.close();
        }
        catch (IOException ex)
        {
            Logger.getLogger(AudioMorseTransceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
        audioMorse.shutdown();
    }
    
    @Override
    public void write(byte[] frame)
    {
        try
        {
            String plaintext = _CT_+" "+Encoding.BASE32.encode(frame)+" "+_SK_;
            final int[] pattern = morse.encodePattern(plaintext);
            audioModem.play(hz, pattern, 1, wave);
        }
        catch (IOException ex)
        {
            Logger.getLogger(AudioMorseTransceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public byte[] read()
    {
        for(String s : audioMorse)
        {
            if (s.equalsIgnoreCase(_CT_))
            {
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for(String s : audioMorse)
        {
            if (s.equalsIgnoreCase(_SK_))
            {
                break;
            }
            sb.append(s);
        }
        return Encoding.BASE32.decode(sb.toString());
    }

}

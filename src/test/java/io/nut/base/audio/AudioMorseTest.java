/*
 * AudioMorseTest.java
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

import static io.nut.base.audio.Wave.DUTY_CYCLE_025;
import static io.nut.base.audio.Wave.SAWTOOTH;
import static io.nut.base.audio.Wave.SINE;
import static io.nut.base.audio.Wave.SQUARE;
import static io.nut.base.audio.Wave.TRIANGLE;
import io.nut.base.morse.Morse;
import io.nut.base.util.Utils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

/**
 *
 * @author franci
 */
public class AudioMorseTest
{
    static final String MORSE1 = "morse1-quixote-8000-mono-u8.wav.gz";
    static final String MORSE2 = "morse2-pangram-800hz-20wpm-sine-44100-2ch-s16le.wav.gz";
    static final String MORSE3 = "morse3-pangram-800hz-20wpm-square-44100-2ch-s16le.wav.gz";
    static final String MORSE4 = "morse4-pangram-1600hz-30wpm-sine-44100-1ch-s16le.wav.gz";
    static final String MORSE5 = "morse5-pangram-1600hz-30wpm-square-44100-1ch-s16le.wav.gz";    
    
    InputStream getIS(String fileName) throws UnsupportedAudioFileException, IOException
    {
        return new BufferedInputStream(new GZIPInputStream(MorseGoertzelTest.class.getResourceAsStream(fileName)));
    }
    
    static final String QUIJOTE = "EN UN LUGAR DE LA MANCHA, DE CUYO NOMBRE NO QUIERO ACORDARME, NO HA MUCHO TIEMPO QUE VIVIA UN HIDALGO DE LOS DE LANZA EN ASTILLERO, ADARGA ANTIGUA, ROCIN FLACO Y GALGO CORREDOR.";
    
    @Test
    public void testWav1() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE1));
        AudioMorse instance = new AudioMorse(ais, 550, true, true, false, 5, 99);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(QUIJOTE.toUpperCase(), sb.toString().trim());
    }
    
    @Test
    public void testWav2() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE2));
        AudioMorse instance = new AudioMorse(ais, 800, true, true, false, 5, 99);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(PANGRAM.toUpperCase(), sb.toString().trim());
    }
    
    @Test
    public void testWav3() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE3));
        AudioMorse instance = new AudioMorse(ais, 800, true, true, true, 5, 99);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(PANGRAM.toUpperCase(), sb.toString().trim());
    }

    @Test
    public void testWav4() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE4));
        AudioMorse instance = new AudioMorse(ais, 1600, true, true, true, 5, 99);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(PANGRAM.toUpperCase(), sb.toString().trim());
    }

    @Test
    public void testWav5() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE5));
        AudioMorse instance = new AudioMorse(ais, 1600, true, true, true, 5, 99);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(PANGRAM.toUpperCase(), sb.toString().trim());
    }

    static final String PANGRAM = "Quick nymph bugs vex fjord waltz.";
    static final String PANGRAM2 = "Quick nymph bugs vex fjord waltz 01234 56789.";
//    static final String ETIANS = "ETIANS";
//    static final String ETIANS = "ETIANS TIANSE IANSET ANSETI NSETIA SETIAN";
    
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testBuildWavWithPangram() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        //ffmpeg -f alsa -i default morse-pangram-800hz-20wpm-sine.wav
        //ffmpeg -f alsa -i default morse-pangram-800hz-20wpm-square.wav
        int hz = 800;
        int wpm = 30;
        String plaintext = PANGRAM.toUpperCase();
        final AudioModem am = new AudioModem(Audio.getLineOut(Audio.PCM_CD_MONO), 16, Wave.SQUARE);
        Morse morse = new Morse(wpm, wpm, 0, 2);
        
        final int[] pattern = morse.encodePattern(plaintext);
        String decodedText = morse.decodePattern(pattern).trim().toUpperCase();
        
        assertEquals(plaintext, decodedText);
        
        am.play(0, 1000, 1);
        am.play(hz, pattern, 1);
        am.play(0, 1000, 1);
        am.drain();
        am.close();
        
    }

    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testFindMaxSpeed() throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException, BrokenBarrierException
    {
        Wave[] cleanWaves = { SQUARE};//, SINE};//, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025};
        int hz = 800;
        String plaintext = PANGRAM;
//        String plaintext = QUIJOTE;
        ArrayList<Wave> waves = new ArrayList<>(Arrays.asList(cleanWaves));
        
        final AudioModem audioModem = new AudioModem(Audio.getLineOut(Audio.PCM_CD_MONO), 16);
        AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO, 441000));
        
        for(int wpm=40;!waves.isEmpty();wpm++)
        {
            int count = 0;
            for(Wave wave : waves.toArray(new Wave[0]))
            {
                System.out.println("---------- "+wave.name+" "+hz+"Hz "+wpm+"wpm ----------");
                final AudioMorse instance = new AudioMorse(ais, hz, true, true, true, 5, 0);
                
                Morse morse = new Morse(wpm, wpm, 0, 4);
                final int[] pattern = morse.encodePattern(plaintext);
                //System.out.println(Arrays.toString(pattern));
                String decodedText = morse.decodePattern(pattern).trim();
                if(decodedText.compareToIgnoreCase(plaintext)!=0)
                {
                    System.out.println("ERROR");
                }
                final CyclicBarrier openBarrier = new CyclicBarrier(2);
                Utils.execute(()-> 
                {
                    try
                    {
                        openBarrier.await();
                        instance.skipAvailable();
                        audioModem.play(0, 2000, 1);
                        audioModem.play(hz, pattern, 1, wave);
                        audioModem.play(0, 2000, 1);
                        audioModem.drain();
                    }
                    catch (IOException | InterruptedException | BrokenBarrierException ex)
                    {
                        Logger.getLogger(AudioMorseTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    instance.shutdown();
                });

                StringBuilder word = new StringBuilder();
                StringBuilder phrase = new StringBuilder();
                openBarrier.await();
                for(String s : instance)
                {
                    word.append(s);
                    phrase.append(s);
                    if(s.equals(" ") || s.equals(".") || s.equals(","))
                    {
                        System.out.println(word);
                        word = new StringBuilder();
                        if(word.toString().endsWith(".")) 
                        {
                            break;
                        }
                    }
                }
                System.out.println(word);

                if(plaintext.equalsIgnoreCase(phrase.toString().trim()))
                {
                    count++;
                    System.out.println("-----"+phrase.toString()+"-----");
                }
                else
                {
                    waves.remove(wave);
                    System.out.println("!!!!!!!!!! "+wave.name+" !!!!!!!!!!");
                }
            }
            System.out.println("---------- "+wpm+" => "+count+" ----------");

        }        
    }
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testListenCW() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO));
        AudioMorse instance = new AudioMorse(ais, 800, true, true, true, 5, 100);

        StringBuilder word = new StringBuilder();
        for(String s : instance)
        {
            word.append(s);
            if(s.equals(" ") || s.equals(".") || s.equals(","))
            {
                System.out.println(word);
                word = new StringBuilder();
            }
        }
    }

}

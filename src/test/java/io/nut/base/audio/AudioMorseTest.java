package io.nut.base.audio;

import static io.nut.base.audio.Wave.DUTY_CYCLE_025;
import static io.nut.base.audio.Wave.SAWTOOTH;
import static io.nut.base.audio.Wave.SINE;
import static io.nut.base.audio.Wave.SQUARE;
import static io.nut.base.audio.Wave.TRIANGLE;
import io.nut.base.morse.Morse;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
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
    static final String MORSE1 = "morse-quixote-8000-mono-u8.wav.gz";
    static final String MORSE2 = "morse-pangram-800hz-20wpm-sine-44100-2ch-s16le.wav.gz";
    static final String MORSE3 = "morse-pangram-800hz-20wpm-square-44100-2ch-s16le.wav.gz";
    static final String MORSE4 = "morse-pangram-1600hz-30wpm-sine-44100-1ch-s16le.wav.gz";
    static final String MORSE5 = "morse-pangram-1600hz-30wpm-square-44100-1ch-s16le.wav.gz";    
    
    InputStream getIS(String fileName) throws UnsupportedAudioFileException, IOException
    {
        return new BufferedInputStream(new GZIPInputStream(MorseGoertzelTest.class.getResourceAsStream(fileName)));
    }
    
    static final String QUIJOTE = "EN UN LUGAR DE LA MANCHA, DE CUYO NOMBRE NO QUIERO ACORDARME, NO HA MUCHO TIEMPO QUE VIVIA UN HIDALGO DE LOS DE LANZA EN ASTILLERO, ADARGA ANTIGUA, ROCIN FLACO Y GALGO CORREDOR.";
    
    @Test
    public void testWav1() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE1));
        AudioMorse instance = new AudioMorse(ais, 0, true, true, 5, 99);
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
        AudioMorse instance = new AudioMorse(ais, 800, true, true, 5, 99);
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
        AudioMorse instance = new AudioMorse(ais, 800, true, true, 5, 99);
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
        AudioMorse instance = new AudioMorse(ais, 1600, true, true, 5, 99);
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
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS(MORSE4));
        AudioMorse instance = new AudioMorse(ais, 1600, true, true, 5, 99);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(PANGRAM.toUpperCase(), sb.toString().trim());
    }

    static final String PANGRAM = "Quick nymph bugs vex fjord waltz.";
//    static final String ETIANS = "ETIANS";
//    static final String ETIANS = "ETIANS TIANSE IANSET ANSETI NSETIA SETIAN";
    
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testBuildWavWithPangram() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
//        ffmpeg -f alsa -i default morse-pangram-800hz-20wpm-sine.wav
        //ffmpeg -f alsa -i default morse-pangram-800hz-20wpm-square.wav
        int hz = 1600;
        int wpm = 30;
        String plaintext = PANGRAM.toUpperCase();
        final AudioModem am = new AudioModem((TargetDataLine)null, Audio.getLineOut(Audio.PCM_CD_MONO), 16, Wave.SQUARE);
        Morse morse = new Morse(wpm, wpm, Morse.FLAG_LAST_WGAP);
        
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
    public void testRun0() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        Wave[] cleanWaves = { SQUARE, SINE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025};
        int hz = 800;
        String plaintext = PANGRAM;
        ArrayList<Wave> waves = new ArrayList<>(Arrays.asList(cleanWaves));
        for(int wpm=30;waves.size()>0;wpm++)
        {
            int count = 0;
            for(Wave wave : waves.toArray(new Wave[0]))
            {
                System.out.println("---------- "+wave.name+" "+wpm+" ----------");
                final AudioModem am = new AudioModem((TargetDataLine)null, Audio.getLineOut(Audio.PCM_CD_MONO), 16, wave);
                AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO, 441000));
                AudioMorse instance = new AudioMorse(ais, hz, true, true, 5, 1000);
                AtomicBoolean terminated = new AtomicBoolean();
                Morse morse = new Morse(wpm, wpm, Morse.FLAG_LAST_WGAP);
                final int[] pattern = morse.encodePattern(plaintext);
                String decodedText = morse.decodePattern(pattern).trim();
                if(decodedText.compareToIgnoreCase(plaintext)!=0)
                {
                    System.out.println("ERROR");
                }
                Utils.execute(()-> 
                {
                    try
                    {
                        am.play(0, 2000, 1);
                        am.play(hz, pattern, 1);
                        am.play(0, 4000, 1);
                        am.drain();
                    }
                    catch (IOException ex)
                    {
                        Logger.getLogger(AudioMorseTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    instance.shutdown();
                });

                StringBuilder word = new StringBuilder();
                StringBuilder phrase = new StringBuilder();
                for(String s : instance)
                {
                    word.append(s);
                    phrase.append(s);
                    if(s.equals(" ") || s.equals(".") || s.equals(","))
                    {
                        System.out.println(word);
                        word = new StringBuilder();
                    }
                    if(terminated.get()) break;
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
    public void testRun1() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        
        AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO));
        AudioMorse instance = new AudioMorse(ais, 800, true, true, 4, 0);

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

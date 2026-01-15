package io.nut.base.audio;

import io.nut.base.morse.Morse;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
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
    static InputStream getIS() throws UnsupportedAudioFileException, IOException
    {
        return new BufferedInputStream(new GZIPInputStream(MorseGoertzelTest.class.getResourceAsStream("morse-8000hz-mono-u8.wav.gz")));
    }
    
    static final String QUIJOTE = "EN UN LUGAR DE LA MANCHA, DE CUYO NOMBRE NO QUIERO ACORDARME, NO HA MUCHO TIEMPO QUE VIVIA UN HIDALGO DE LOS DE LANZA EN ASTILLERO, ADARGA ANTIGUA, ROCIN FLACO Y GALGO CORREDOR.";
    
    /**
     * Test of run method, of class AudioMorse.
     */
    @Test
    public void testRun() throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream ais = AudioSystem.getAudioInputStream(getIS());
        AudioMorse instance = new AudioMorse(ais, 0, true, true, 5, 0);
        StringBuilder sb = new StringBuilder();
        for(String s : instance)
        {
            sb.append(s);
        }
        assertEquals(QUIJOTE, sb.toString());
    }

    static final String PANGRAM = "Quick nymph bugs vex fjord waltz.";
    
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testRun0() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO));
        AudioMorse instance = new AudioMorse(ais, 800, true, true, 5, 100);

        final AudioModem am = new AudioModem(Audio.getLineOut(Audio.PCM_CD_MONO));
        AtomicBoolean terminated = new AtomicBoolean();
        Morse morse = new Morse(15, 15, 0);
        final int[] pattern = morse.encodePattern(QUIJOTE);
        
        Utils.execute(()-> 
        {
            try
            {
                am.play(800, pattern, 1);
            }
            catch (IOException ex)
            {
                Logger.getLogger(AudioMorseTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            Utils.sleep(1000);
            terminated.set(true);
        });
       
        StringBuilder word = new StringBuilder();
        for(String s : instance)
        {
            word.append(s);
            if(s.equals(" ") || s.equals(".") || s.equals(","))
            {
                System.out.println(word);
                word = new StringBuilder();
            }
            if(terminated.get()) break;
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
    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testRun2() throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        
        AudioInputStream ais = Audio.getAudioInputStream(Audio.getLineIn(Audio.PCM_CD_MONO));
        AudioGoertzel instance = new AudioGoertzel(ais, 800, true, true, 20, 10);
        for(double s : instance)
        {
            int ss = (int) Math.abs(s);
            String x = Strings.repeat(' ', ss>=1 ? (int)Math.log1p(ss) : 0);
            System.out.println(x+"* "+s);
        }
    }
    
}

/*
 * Audio.java
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

import io.nut.base.util.Java;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Utility class for managing audio formats, hardware lines, and stream conversions
 * using the Java Sound API.
 * <p>
 * Provides predefined {@link AudioFormat} constants for common standards and 
 * helper methods for audio input/output and buffer size calculations.
 */
public class Audio
{
    // --- LINEAR PCM (Uncompressed audio, maximum compatibility) ---

    /**
     * CD Quality: PCM 44.1kHz, 16-bit, Stereo, Little-Endian, Signed.
     */
    public static final AudioFormat PCM_CD_STEREO = new AudioFormat(44100, 16, 2, true, false);

    /**
     * CD Quality: PCM 44.1kHz, 16-bit, Mono, Little-Endian, Signed.
     */
    public static final AudioFormat PCM_CD_MONO = new AudioFormat(44100, 16, 1, true, false);

    /**
     * Studio Quality: PCM 48kHz, 16-bit, Stereo, Little-Endian, Signed.
     */
    public static final AudioFormat PCM_STUDIO_STEREO = new AudioFormat(48000, 16, 2, true, false);

    /**
     * Radio/Voice Quality: PCM 22.05kHz, 16-bit, Mono, Little-Endian, Signed.
     */
    public static final AudioFormat PCM_RADIO_MONO = new AudioFormat(22050, 16, 1, true, false);

    /**
     * Wideband Telephony Quality: PCM 16kHz, 16-bit, Mono, Signed.
     */
    public static final AudioFormat PCM_VOICE_WIDEBAND = new AudioFormat(16000, 16, 1, true, false);

    // --- G.711 (Telephony / Companding Formats) ---

    /**
     * Europe/Latam Standard: A-LAW, 8kHz, 8-bit, Mono.
     */
    public static final AudioFormat ALAW_TELEPHONY = new AudioFormat(AudioFormat.Encoding.ALAW, 8000, 8, 1, 1, 8000, false);

    /**
     * USA/Japan Standard: U-LAW (mu-law), 8kHz, 8-bit, Mono.
     */
    public static final AudioFormat ULAW_TELEPHONY = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);

    // --- LEGACY FORMATS / LOW RESOLUTION ---

    /**
     * PCM 8-bit Unsigned: Common in legacy .wav files.
     */
    public static final AudioFormat PCM_8BIT_MONO = new AudioFormat(8000, 8, 1, false, false);

    /**
     * Obtains, opens, and starts a {@link TargetDataLine} (Microphone/Input) 
     * for the specified format.
     *
     * @param format The desired audio format for the input line.
     * @return An opened and started TargetDataLine.
     * @throws LineUnavailableException If the line cannot be opened due to resource restrictions.
     */
    public static TargetDataLine getLineIn(AudioFormat format) throws LineUnavailableException
    {
        TargetDataLine lineIn = AudioSystem.getTargetDataLine(format);
        lineIn.open(format);
        lineIn.start();
        return lineIn;
    }

    /**
     * Obtains, opens, and starts a {@link SourceDataLine} (Speakers/Output) 
     * for the specified format.
     *
     * @param format The desired audio format for the output line.
     * @return An opened and started SourceDataLine.
     * @throws LineUnavailableException If the line cannot be opened due to resource restrictions.
     */
    public static SourceDataLine getLineOut(AudioFormat format) throws LineUnavailableException
    {
        SourceDataLine lineOut = AudioSystem.getSourceDataLine(format);
        lineOut.open(format);
        lineOut.start();
        return lineOut;
    }

    /**
     * Converts an existing {@link AudioInputStream} to a destination format.
     *
     * @param src    The source audio input stream.
     * @param dstFmt The desired target audio format.
     * @return A new AudioInputStream in the requested format.
     * @throws IllegalArgumentException If the conversion between formats is not supported by the system.
     */
    public static AudioInputStream getAudioInputStream(AudioInputStream src, AudioFormat dstFmt)
    {
        AudioFormat srcFmt = src.getFormat();

        if (!AudioSystem.isConversionSupported(dstFmt, srcFmt))
        {
            throw new IllegalArgumentException("Conversion not supported");
        }
        return AudioSystem.getAudioInputStream(dstFmt, src);
    }
        
    public static AudioInputStream getAudioInputStream(TargetDataLine line)
    {
        return new AudioInputStream(line);
    }
    
    public static AudioInputStream getAudioInputStream(TargetDataLine line, AudioFormat dstFmt)
    {
        return getAudioInputStream(new AudioInputStream(line), dstFmt);
    }
        
    public static AudioInputStream getAudioInputStream(InputStream input) throws UnsupportedAudioFileException, IOException
    {
        input = (input instanceof BufferedInputStream) ? input : new BufferedInputStream(input);
        return AudioSystem.getAudioInputStream(input);
    }
    
    public static AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException
    {
        return getAudioInputStream(new FileInputStream(file));
    }
    
    public static AudioFormat getFloatMono(AudioFormat fmt, boolean bigEndian)
    {
        Encoding encoding = fmt.getEncoding();
        float sampleRate = fmt.getSampleRate();
        int sampleSizeInBits = fmt.getSampleSizeInBits();
        int channels = fmt.getChannels();
        
        if(encoding.equals(Encoding.PCM_FLOAT) && channels==1 && sampleSizeInBits==Java.FLOAT_BITS && bigEndian==fmt.isBigEndian())
        {
            return fmt;
        }
        float frameRate = fmt.getFrameRate();
        return new AudioFormat(Encoding.PCM_FLOAT, sampleRate, Java.FLOAT_BITS, 1, Float.BYTES, frameRate, bigEndian);
    }
    
    public static AudioFormat getDoubleMono(AudioFormat fmt, boolean bigEndian)
    {
        Encoding encoding = fmt.getEncoding();
        float sampleRate = fmt.getSampleRate();
        int sampleSizeInBits = fmt.getSampleSizeInBits();
        int channels = fmt.getChannels();
        
        if(encoding.equals(Encoding.PCM_FLOAT) && channels==1 && sampleSizeInBits==Java.DOUBLE_BITS &&  bigEndian==fmt.isBigEndian())
        {
            return fmt;
        }
        float frameRate = fmt.getFrameRate();
        return new AudioFormat(Encoding.PCM_FLOAT, sampleRate, Java.DOUBLE_BITS, 1, Double.BYTES, frameRate, false);
    }


    /**
     * Calculates the number of bytes required to hold a specific duration of audio.
     *
     * @param format The audio format to use for calculation.
     * @param millis The duration in milliseconds.
     * @return The number of bytes corresponding to the duration.
     */
    public static int bytesNeeded(AudioFormat format, int millis)
    {
        float sampleRate = format.getFrameRate();
        int channels = format.getChannels();
        int sampleBits = format.getSampleSizeInBits();

        return bytesNeeded(sampleRate, sampleBits, channels, millis);
    }

    /**
     * Calculates the number of bytes required based on raw audio parameters.
     *
     * @param sampleRate The number of samples per second (Hz).
     * @param sampleBits The number of bits per sample (e.g., 8 or 16).
     * @param channels   The number of audio channels (1 for mono, 2 for stereo).
     * @param millis     The duration in milliseconds.
     * @return The number of bytes corresponding to the duration.
     */
    public static int bytesNeeded(float sampleRate, int sampleBits, int channels, int millis)
    {
        int sampleBytes = sampleBits / 8;
        int frameBytes = sampleBytes * channels;
        int bytes = (int) ((sampleRate * frameBytes * millis) / 1000);
        return roundUpToFrameSize(bytes, frameBytes);
    }
    public static int roundUpToFrameSize(int bytes, int bytesPerFrame) 
    {
        return ((bytes + bytesPerFrame - 1) / bytesPerFrame) * bytesPerFrame;
    }
  
    public static double goertzelPower(double[] data, float sampleRate, double hz)
    {
        double sPrev = 0, sPrev2 = 0;
        double normalizedFreq = 2.0 * Math.PI * hz / sampleRate;
        double coeff = 2.0 * Math.cos(normalizedFreq);
        int i = 0;
        for (double d : data)
        {
            double s = d + coeff * sPrev - sPrev2;
            sPrev2 = sPrev;
            sPrev = s;
            i++;
        }
        return sPrev2 * sPrev2 + sPrev * sPrev - coeff * sPrev * sPrev2;
    }

    public static float detectHz(double[] data, float sampleRate, float threshold)
    {
        int crossings = -1;
        int firstCrossingIdx = -1;
        int lastCrossingIdx = -1;

        // 1 = looking for an upward crossing, -1 = looking for a downward crossing
        int state = 0;

        for (int i = 0; i < data.length; i++)
        {
            // We only process if the signal exceeds the threshold (there is sound)
            if(state == 0 && data[i] > threshold)
            {
                state = 1;
            }
            else if(state == 0 && data[i] < -threshold)
            {
                state = -1;
            }
            else if (state != 1 && data[i] > threshold)
            {
                state = 1;
                crossings++;
                if (firstCrossingIdx == -1)
                {
                    firstCrossingIdx = i;
                    if(i>0 && data[i-1] >= 0)
                    {
                        firstCrossingIdx--;
                    }
                }
                lastCrossingIdx = i;
            }
            else if (state != -1 && data[i] < -threshold)
            {
                state = -1;
                crossings++;
                if (firstCrossingIdx == -1)
                {
                    firstCrossingIdx = i;
                    if(i>0 && data[i-1] <= 0)
                    {
                        firstCrossingIdx--;
                    }
                }
                lastCrossingIdx = i;
            }
        }

        // If there were not enough crossings or the signal is very weak, there is no tone.
        if (crossings < 10)
        {
            return 0;
        }
        
        float durationSec = (lastCrossingIdx - firstCrossingIdx) / sampleRate;
        float frequency = (crossings/2) / durationSec;
        
        return frequency;
    }    
    
    static double[] i16ToDouble(byte[] src, boolean bigEndian)
    {
        ShortBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        
        double[] dst = new double[src.length/Short.BYTES];
        
        for(int i=0;i<dst.length;i++)
        {
            dst[i] = buffer.get(i)/(double)Short.MAX_VALUE;
        }
        return dst;
    }
    
    private static float[] i16ToFloat(byte[] src, boolean bigEndian)
    {
        ShortBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        
        float[] dst = new float[src.length/Short.BYTES];
        
        for(int i=0;i<dst.length;i++)
        {
            dst[i] = buffer.get(i)/(float)Short.MAX_VALUE;
        }
        return dst;
    }

    static double[] i8ToDouble(byte[] src)
    {
        double[] dst = new double[src.length];
        
        for(int i=0;i<dst.length;i++)
        {
            dst[i] = src[i]/(double)Byte.MAX_VALUE;
        }
        return dst;
    }
    static float[] i8ToFloat(byte[] src)
    {
        float[] dst = new float[src.length];
        
        for(int i=0;i<dst.length;i++)
        {
            dst[i] = src[i]/(float)Byte.MAX_VALUE;
        }
        return dst;
    }
}

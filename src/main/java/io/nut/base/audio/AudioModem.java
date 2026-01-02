/*
 * AudioModem.java
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

import io.nut.base.cache.Cache;
import io.nut.base.cache.TinyLFUCache;
import io.nut.base.util.tuple.Pair;
import java.io.Closeable;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author franci
 */
public class AudioModem implements Closeable
{
    public static final int DEFAULT_CACHE_SIZE = 64;
    
    private final AudioInputStream lineIn;
    private final SourceDataLine lineOut;
    private final AudioFormat formatIn;
    private final AudioFormat formatOut;
    private final Wave wave;
    private final Cache<Pair<Integer, Double>, byte[]> playCache;

    public AudioModem(SourceDataLine lineOut)
    {
        this((AudioInputStream)null, lineOut, DEFAULT_CACHE_SIZE, Wave.SINE);
    }
    public AudioModem(TargetDataLine lineIn, SourceDataLine lineOut)
    {
        this(Audio.getAudioInputStream(lineIn), lineOut, DEFAULT_CACHE_SIZE, Wave.SINE);
    }
    public AudioModem(TargetDataLine lineIn, SourceDataLine lineOut, int cacheCapacity)
    {
        this(Audio.getAudioInputStream(lineIn), lineOut, cacheCapacity, Wave.SINE);
    }
    public AudioModem(TargetDataLine lineIn, SourceDataLine lineOut, int cacheCapacity, Wave wave)
    {
        this(Audio.getAudioInputStream(lineIn), lineOut, cacheCapacity, wave);
    }

    public AudioModem(AudioInputStream lineIn, SourceDataLine lineOut)
    {
        this(lineIn, lineOut, 64, Wave.SINE);
    }
    public AudioModem(AudioInputStream lineIn, SourceDataLine lineOut, int cacheCapacity)
    {
        this(lineIn, lineOut, cacheCapacity, Wave.SINE);
    }
    public AudioModem(AudioInputStream lineIn, SourceDataLine lineOut, int cacheCapacity, Wave wave)
    {
        this.lineIn = lineIn;
        this.lineOut = lineOut;
        this.formatIn = lineIn != null ? lineIn.getFormat() : null;
        this.formatOut = lineOut != null ? lineOut.getFormat() : null;
        this.playCache = new TinyLFUCache<>(cacheCapacity);
        this.wave = wave;
    }

    public void play(int hz, int ms, double vol) throws IOException
    {
        int playBytes = Audio.bytesNeeded(this.formatOut, ms);
        Pair<Integer, Double> key = new Pair<>(hz, vol);
        byte[] bytes = this.playCache.get(key);
        if (bytes == null || bytes.length < playBytes)
        {
            bytes = this.wave.build(formatOut, hz, new byte[playBytes * 3], vol);
            this.playCache.put(key, bytes);
        }
        this.lineOut.write(bytes, 0, playBytes);
    }
    
    public void play(int hz, int[] pattern, double vol) throws IOException
    {
        for(int i=0;i<pattern.length;i++)
        {
            play(hz, pattern[i], i%2!=0 ? vol : 0);
        }
    }
    public void drain()
    {
        this.lineOut.drain();
    }

    @Override
    public void close() throws IOException
    {
        if(this.lineIn!=null)
        {
            this.lineIn.close();
        }
        if(this.lineOut!=null)
        {
            this.lineOut.drain();
            this.lineOut.close();
        }
        this.playCache.clear();
    }
}

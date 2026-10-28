/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

import io.github.francitoshi.nut.base.commons.cache.Cache;
import io.github.francitoshi.nut.base.commons.cache.TinyLFUCache;
import io.github.francitoshi.nut.base.commons.tuple.Trio;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;


public class AudioSynthesizer implements Closeable
{
    static final int CACHE_SIZE_64 = 64;
    static final Wave WAVE_SINE = Wave.SINE;
    
    private final AudioWriter audioWriter;
    private final AudioFormat format;
    private final Wave defaultWave;
    private final Cache<Trio<Integer, Double, String>, byte[]> playCache;

    private AudioSynthesizer(AudioWriter audioWriter, int cacheCapacity, Wave defaultWave)
    {
        this.audioWriter = audioWriter;
        this.format = audioWriter != null ? audioWriter.getFormat() : null;
        this.playCache = new TinyLFUCache<>(cacheCapacity);
        this.defaultWave = defaultWave;
    }
    public AudioSynthesizer(SourceDataLine lineOut, int cacheCapacity, Wave defaultWave)
    {
        this(new LineOutWriter(lineOut), cacheCapacity, defaultWave);
    }
    public AudioSynthesizer(SourceDataLine lineOut, int cacheCapacity)
    {
        this(new LineOutWriter(lineOut), cacheCapacity, WAVE_SINE);
    }
    public AudioSynthesizer(SourceDataLine lineOut)
    {
        this(new LineOutWriter(lineOut), CACHE_SIZE_64, WAVE_SINE);
    }
    
    public AudioSynthesizer(File wavFile, AudioFormat format, int cacheCapacity, Wave defaultWave) throws IOException
    {
        this(new WavWriter(wavFile, format), cacheCapacity, defaultWave);
    }
    public AudioSynthesizer(File wavFile, AudioFormat format, int cacheCapacity) throws IOException
    {
        this(new WavWriter(wavFile, format), cacheCapacity, WAVE_SINE);
    }
    public AudioSynthesizer(File wavFile, AudioFormat format) throws IOException
    {
        this(new WavWriter(wavFile, format), CACHE_SIZE_64, WAVE_SINE);
    }

    public void play(int hz, int ms, double vol, Wave wave) throws IOException
    {
        if(ms>0)
        {
            wave = wave!=null ? wave : defaultWave;
            int playBytes = Audio.msToBytes(ms, this.format);
            Trio<Integer, Double, String> key = new Trio<>(hz, vol, wave.name);
            byte[] bytes = this.playCache.get(key);
            if (bytes == null || bytes.length < playBytes)
            {
                bytes = wave.build(format, hz, vol);
                this.playCache.put(key, bytes);
            }
            this.audioWriter.write(bytes, 0, bytes.length, playBytes);
        }
    }
    
    public void play(int hz, int ms, double vol) throws IOException
    {
        play(hz, ms, vol, defaultWave);
    }
    
    public void play(int hz, int[] pattern, double vol, Wave wave) throws IOException
    {
        for(int i=0;i<pattern.length;i++)
        {
            play(hz, pattern[i], i%2!=0 ? vol : 0, wave);
        }
    }
    
    public void play(int hz, int[] pattern, double vol) throws IOException
    {
        play(hz, pattern, vol, defaultWave);
    }

    public void drain()
    {
        this.audioWriter.drain();
    }

    @Override
    public void close() throws IOException
    {
        if(this.audioWriter!=null)
        {
            this.audioWriter.drain();
            this.audioWriter.close();
        }
        this.playCache.clear();
    }
}

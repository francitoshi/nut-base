/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Deflate compression utility using a dictionary.
 * Provides compression and decompression methods that use a dictionary
 * for improved compression ratios when the same data is compressed multiple times.
 */
public class DictionaryDeflate
{
    final int level;

    /**
     * Creates a new DictionaryDeflate with default compression level (BEST_COMPRESSION).
     */
    public DictionaryDeflate()
    {
        this(false);
    }

    /**
     * Creates a new DictionaryDeflate with the specified speed/compression tradeoff.
     *
     * @param bestSpeed if true, uses Deflater.BEST_SPEED for faster compression;
     *                  if false, uses Deflater.BEST_COMPRESSION for better compression ratio
     */
    public DictionaryDeflate(boolean bestSpeed)
    {
        this.level = bestSpeed ? Deflater.BEST_SPEED : Deflater.BEST_COMPRESSION;
    }

    /**
     * Compresses input data using the given dictionary.
     *
     * @param dictionary the dictionary bytes to use for compression
     * @param input the data to compress
     * @return the compressed data
     * @throws IOException if an I/O error occurs
     */
    public byte[] deflate(byte[] dictionary, byte[] input) throws IOException
    {
        return deflate(dictionary, input, 0, input.length);
    }

    /**
     * Compresses a subset of input data using the given dictionary.
     *
     * @param dictionary the dictionary bytes to use for compression
     * @param input the data to compress
     * @param index the starting position in the input array
     * @param size the number of bytes to compress
     * @return the compressed data
     * @throws IOException if an I/O error occurs
     */
    public byte[] deflate(byte[] dictionary, byte[] input, int index, int size) throws IOException
    {
        Deflater deflater = new Deflater(this.level);
        deflater.setDictionary(dictionary);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
        dos.write(input, index, size);
        dos.finish();
        return baos.toByteArray();
    }

    /**
     * Decompresses input data using the given dictionary.
     *
     * @param dictionary the dictionary bytes used during compression
     * @param input the compressed data to decompress
     * @return the decompressed data
     * @throws DataFormatException if the data is not in the expected format
     * @throws IOException if an I/O error occurs
     */
    public byte[] inflate(byte[] dictionary, byte[] input) throws DataFormatException, IOException
    {
        return inflate(dictionary, input, 0, input.length);
    }

    /**
     * Decompresses a subset of input data using the given dictionary.
     *
     * @param dictionary the dictionary bytes used during compression
     * @param input the compressed data to decompress
     * @param index the starting position in the input array
     * @param size the number of bytes to decompress
     * @return the decompressed data
     * @throws DataFormatException if the data is not in the expected format
     * @throws IOException if an I/O error occurs
     */
    public byte[] inflate(byte[] dictionary, byte[] input, int index, int size) throws DataFormatException, IOException
    {
        // Decompress the bytes
        Inflater inflater = new Inflater();
        inflater.setInput(input, index, size);
        if(inflater.inflate(new byte[0])==0 && inflater.needsDictionary()) 
        {
            inflater.setDictionary(dictionary);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(input, index, size);
        InflaterInputStream iis = new InflaterInputStream(bais, inflater);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r;
        byte[] buf = new byte[8*1024];
        while( (r=iis.read(buf))>=0)
        {
            baos.write(buf, 0, r);
        }
        return baos.toByteArray();
    }
}
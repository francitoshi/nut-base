/*
 * AudioReader.java
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

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public abstract class AudioReader
{
    public final int bitsPerSample;
    public final int bytesPerSample;

    public AudioReader(int bitsPerSample)
    {
        this.bitsPerSample = bitsPerSample;
        this.bytesPerSample = bitsPerSample/8;
    }

    public abstract float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst);

    public static AudioReader getInstance(AudioFormat fmt)
    {
        AudioFormat.Encoding encoding = fmt.getEncoding();
        int bits = fmt.getSampleSizeInBits();
        
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
        {
            switch (bits)
            {
                case 8:
                    return new AudioReaderSigned8();
                case 16:
                    return new AudioReaderSigned16();
                case 24:
                    return new AudioReaderSigned24();
                case 32:
                    return new AudioReaderSigned32();
            }
        }
        else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED))
        {
            switch (bits)
            {
                case 8:
                    return new AudioReaderUnsigned8();
                case 16:
                    return new AudioReaderUnsigned16();
                case 24:
                    return new AudioReaderUnsigned24();
                case 32:
                    return new AudioReaderUnsigned32();
            }
        }
        else if (encoding.equals(AudioFormat.Encoding.PCM_FLOAT))
        {
            if(bits==32)
            {
                return new AudioReaderFloat32();
            }
            else if(bits==64)
            {
                return new AudioReaderFloat64();
            }
        }
        else if (encoding.equals(AudioFormat.Encoding.ALAW))
        {
            return new AudioReaderALaw8();
        }
        else if (encoding.equals(AudioFormat.Encoding.ULAW))
        {
            return new AudioReaderULaw8();
        }
        return null;
    }
        
    public static float aLawToFloat(byte alawByte)
    {
        // Tabla de decodificación A-law estándar
        int sign = (alawByte & 0x80) == 0 ? 1 : -1;
        int exponent = (alawByte & 0x70) >> 4;
        int mantissa = alawByte & 0x0F;

        int value;
        if (exponent == 0)
        {
            value = (mantissa << 4) + 8;
        }
        else
        {
            value = ((mantissa << 4) + 0x108) << (exponent - 1);
        }

        return sign * value / 32768.0f;
    }

    public static float uLawToFloat(byte ulawByte)
    {
        // Invertir todos los bits (μ-law almacena complemento a 1)
        int inverted = ~ulawByte & 0xFF;

        int sign = (inverted & 0x80) == 0 ? 1 : -1;
        int exponent = (inverted & 0x70) >> 4;
        int mantissa = inverted & 0x0F;

        int value = ((mantissa << 3) + 0x84) << exponent;
        value -= 0x84;

        return sign * value / 32768.0f;
    }
}

class AudioReaderSigned8 extends AudioReader
{
    public AudioReaderSigned8()
    {
        super(8);
    }
    
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        dst = dst==null ? new float[readBytes] : dst;
        for (int i = 0; i < readBytes && i < dst.length; i++)
        {
            // Convert signed 8-bit PCM to float range [-1.0, 1.0]
            dst[i] = src[i] / 128.0f;
        }
        return dst;
    }
}
class AudioReaderSigned16 extends AudioReader
{
    public AudioReaderSigned16()
    {
        super(16);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 2; // 2 bytes per 16-bit sample
        dst = dst==null ? new float[samples] : dst;
        ShortBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            // Convert signed 16-bit PCM to float range [-1.0, 1.0]
            dst[i] = buffer.get(i) / 32768.0f;
        }
        return dst;
    }
}
class AudioReaderSigned24 extends AudioReader
{
    public AudioReaderSigned24()
    {
        super(24);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 3; // 3 bytes per 24-bit sample
        dst = dst==null ? new float[samples] : dst;
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            int offset = i * 3;
            int sample;

            if (bigEndian)
            {
                sample = ((src[offset] & 0xFF) << 16)
                        | ((src[offset + 1] & 0xFF) << 8)
                        | (src[offset + 2] & 0xFF);
            }
            else
            {
                sample = (src[offset] & 0xFF)
                        | ((src[offset + 1] & 0xFF) << 8)
                        | ((src[offset + 2] & 0xFF) << 16);
            }

            // Sign extend from 24-bit to 32-bit
            if ((sample & 0x800000) != 0)
            {
                sample |= 0xFF000000;
            }

            // Convert signed 24-bit PCM to float range [-1.0, 1.0]
            dst[i] = sample / 8388608.0f; // 2^23
        }
        return dst;
    }
}
class AudioReaderSigned32 extends AudioReader
{
    public AudioReaderSigned32()
    {
        super(32);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 4; // 4 bytes per 32-bit sample
        dst = dst==null ? new float[samples] : dst;
        IntBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            // Convert signed 32-bit PCM to float range [-1.0, 1.0]
            dst[i] = buffer.get(i) / 2147483648.0f; // 2^31
        }
        return dst;
    }
}

class AudioReaderUnsigned8 extends AudioReader
{
    public AudioReaderUnsigned8()
    {
        super(8);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        dst = dst==null ? new float[readBytes] : dst;
        for (int i = 0; i < readBytes && i < dst.length; i++)
        {
            // Convert unsigned 8-bit PCM (0-255) to float range [-1.0, 1.0]
            int unsigned = src[i] & 0xFF;
            dst[i] = (unsigned - 128) / 128.0f;
        }
        return dst;
    }
}
class AudioReaderUnsigned16 extends AudioReader
{
    public AudioReaderUnsigned16()
    {
        super(16);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 2; // 2 bytes per 16-bit sample
        dst = dst==null ? new float[samples] : dst;
        ShortBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            // Convert unsigned 16-bit PCM (0-65535) to float range [-1.0, 1.0]
            int unsigned = buffer.get(i) & 0xFFFF;
            dst[i] = (unsigned - 32768) / 32768.0f;
        }
        return dst;
    }
}
class AudioReaderUnsigned24 extends AudioReader
{
    public AudioReaderUnsigned24()
    {
        super(24);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 3; // 3 bytes per 24-bit sample
        dst = dst==null ? new float[samples] : dst;
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            int offset = i * 3;
            int sample;

            if (bigEndian)
            {
                sample = ((src[offset] & 0xFF) << 16)
                        | ((src[offset + 1] & 0xFF) << 8)
                        | (src[offset + 2] & 0xFF);
            }
            else
            {
                sample = (src[offset] & 0xFF)
                        | ((src[offset + 1] & 0xFF) << 8)
                        | ((src[offset + 2] & 0xFF) << 16);
            }

            // Convert unsigned 24-bit PCM (0-16777215) to float range [-1.0, 1.0]
            dst[i] = (sample - 8388608) / 8388608.0f; // 2^23
        }
        return dst;
    }
}
class AudioReaderUnsigned32 extends AudioReader
{
    public AudioReaderUnsigned32()
    {
        super(32);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 4; // 4 bytes per 32-bit sample
        dst = dst==null ? new float[samples] : dst;
        IntBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            // Convert unsigned 32-bit PCM to float range [-1.0, 1.0]
            long unsigned = buffer.get(i) & 0xFFFFFFFFL;
            dst[i] = (float) ((unsigned - 2147483648L) / 2147483648.0); // 2^31
        }
        return dst;
    }
}

class AudioReaderFloat32 extends AudioReader
{
    public AudioReaderFloat32()
    {
        super(32);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        dst = dst==null ? new float[readBytes] : dst;
        FloatBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        for (int i = 0; i < readBytes && i < dst.length; i++)
        {
            dst[i] = buffer.get(i);
        }
        return dst;
    }
}

class AudioReaderFloat64 extends AudioReader
{
    public AudioReaderFloat64()
    {
        super(64);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        int samples = readBytes / 8; // 8 bytes per 64-bit double
        dst = dst==null ? new float[samples] : dst;
        DoubleBuffer buffer = ByteBuffer.wrap(src).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN).asDoubleBuffer();
        for (int i = 0; i < samples && i < dst.length; i++)
        {
            // Convert double to float (already in range [-1.0, 1.0])
            dst[i] = (float) buffer.get(i);
        }
        return dst;
    }
}

class AudioReaderALaw8 extends AudioReader
{
    public AudioReaderALaw8()
    {
        super(8);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        dst = dst==null ? new float[readBytes] : dst;
        for (int i = 0; i < readBytes && i < dst.length; i++)
        {
            // Convert signed 8-bit PCM to float range [-1.0, 1.0]
            dst[i] = aLawToFloat(src[i]);
        }
        return dst;
    }
}
class AudioReaderULaw8 extends AudioReader
{
    public AudioReaderULaw8()
    {
        super(8);
    }
    @Override
    public float[] readFloats(byte[] src, int readBytes, boolean bigEndian, float[] dst)
    {
        dst = dst==null ? new float[readBytes] : dst;
        for (int i = 0; i < readBytes && i < dst.length; i++)
        {
            // Convert signed 8-bit PCM to float range [-1.0, 1.0]
            dst[i] = uLawToFloat(src[i]);
        }
        return dst;
    }
}


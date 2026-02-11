/*
 * Wave.java
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;

public abstract class Wave
{
    public final String name;
    
    public abstract double getValue(float sampleRate, int i, int hz, double volume);

    public Wave(String name)
    {
        this.name = name;
    }
    
    public static final Wave SINE = new Wave("Sine")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double angle = 2.0 * Math.PI * i * hz / sampleRate;
            return Math.sin(angle) * volume;
        }
    };
    
    public static final Wave SQUARE = new Wave("Square")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (phase < 0.5) ? volume : -volume;
        }
    };
    
    public static final Wave SAWTOOTH = new Wave("Sawtooth")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (2.0 * phase - 1.0) * volume;
        }
    };
    
    public static final Wave TRIANGLE = new Wave("Triangle")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (Math.abs(4.0 * phase - 2.0) - 1.0) * volume;
        }
    };
    
    public static final Wave WHITE_NOISE = new Wave("WhiteNoise")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            return (Math.random() * 2.0 - 1.0) * volume;
        }
    };
    
    public static final Wave DUTY_CYCLE_025 = new Wave("DutyCycle0.25")
    {
        final double dutyCycle = 0.25;
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (phase < dutyCycle) ? volume : -volume;
        }
    };
    public static final Wave DUTY_CYCLE_033 = new Wave("DutyCycle0.33")
    {
        final double dutyCycle = 0.33;
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (phase < dutyCycle) ? volume : -volume;
        }
    };
    
    public static final Wave PWM = new Wave("PWM")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double lfo = Math.sin(2.0 * Math.PI * i * 0.5 / sampleRate); // LFO de 0.5 Hz
            double dutyCycle = 0.5 + (0.4 * lfo); // El ancho varía entre 10% y 90%
            double phase = (i * hz / sampleRate) % 1.0;
            return (phase < dutyCycle) ? volume : -volume;
        }
    };
    
    public static final Wave BROWNIAN_NOISE = new Wave("BrownianNoise")
    {
        volatile double lastValue;
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            // Fuera del bucle: double lastValue = 0;
            double brownOut = lastValue + (Math.random() * 2.0 - 1.0) * 0.05;
            // Mantener el valor en el rango [-1, 1]
            if (brownOut > 1.0)
            {
                brownOut = 1.0;
            }
            if (brownOut < -1.0)
            {
                brownOut = -1.0;
            }
            lastValue = brownOut;
            return brownOut * volume;
        }
    };
    
    public static final Wave FM = new Wave("FM")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double modHz = hz * 2.0; // Frecuencia moduladora
            double modulationIndex = 5.0; // Intensidad del efecto
            double modulator = Math.sin(2.0 * Math.PI * i * modHz / sampleRate) * modulationIndex;
            return Math.sin(2.0 * Math.PI * i * hz / sampleRate + modulator) * volume;
        }
    };
    
    public static final Wave PARABOLIC = new Wave("Parabolic")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (8.0 * phase * (1.0 - phase) - 1.0) * volume;
        }
    };
    
    public static final Wave ADITIVE = new Wave("Aditive")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double angle = 2.0 * Math.PI * i * hz / sampleRate;
            // Fundamental + 2do armónico (mitad vol) + 3er armónico (un tercio vol)
            return (Math.sin(angle) + 0.5 * Math.sin(angle * 2) + 0.3 * Math.sin(angle * 3)) / 1.8 * volume;
        }
    };
    
    public static final Wave[] WAVES = { SINE, SQUARE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025, DUTY_CYCLE_033, PWM, FM, PARABOLIC, ADITIVE, WHITE_NOISE, BROWNIAN_NOISE};
    public static final Wave[] CLEAN_WAVES = { SINE, SQUARE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025, DUTY_CYCLE_033, PWM, FM, PARABOLIC, ADITIVE};

    public byte[] build(AudioFormat format, int hz, byte[] bytes, double volume)
    {
        return build(format, hz, bytes, volume, 0);
    }
    
    public byte[] build(AudioFormat format, int hz, byte[] bytes, double volume, int fading)
    {
        AudioFormat.Encoding encoding = format.getEncoding();
        int sampleRate = (int) format.getFrameRate();
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();
        int sampleBits = format.getSampleSizeInBits();
        return build(encoding, sampleRate, sampleBits, channels, bigEndian, hz, bytes, volume, fading);
    }

    public byte[] build(AudioFormat.Encoding encoding, float sampleRate, int sampleBits, int channels, boolean bigEndian, int hz, byte[] bytes, double volume)
    {
        return build(encoding, sampleRate, sampleBits, channels, bigEndian, hz, bytes, volume, 0);
    }
    public byte[] build(AudioFormat.Encoding encoding, float sampleRate, int sampleBits, int channels, boolean bigEndian, int hz, byte[] bytes, double volume, int fading)
    {
        Objects.requireNonNull(bytes, "bytes must not be null");

        boolean signed = encoding.equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean unsigned = encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED);
        boolean pcmFloat = encoding.equals(AudioFormat.Encoding.PCM_FLOAT);
        boolean alaw = encoding.equals(AudioFormat.Encoding.ALAW);
        boolean ulaw = encoding.equals(AudioFormat.Encoding.ULAW);

        int sampleBytes = sampleBits / 8;
        int samples = bytes.length / (sampleBytes * channels);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        if (hz != 0 && volume != 0)
        {
            fading = Math.min(fading, samples/3);
            for (int i = 0; i < samples; i++)
            {
                double vol = (fading <= 0) ? volume : volume * Math.max(0, Math.min(1.0, Math.min((double) i / fading, (double) (samples - 1 - i) / fading)));
                double value = this.getValue(sampleRate, i, hz, vol);
                
                for (int ch = 0; ch < channels; ch++)
                {
                    if (signed)
                    {
                        putSignedPCM(buffer, value, sampleBits);
                    }
                    else if(unsigned)
                    {
                        putUnsignedPCM(buffer, value, sampleBits);
                    }
                    else if(pcmFloat)
                    {
                        putFloatPCM(buffer, value, sampleBits);
                    } 
                    else if (alaw) 
                    {
                        putALaw(buffer, value);
                    } 
                    else if(ulaw)
                    {
                        putULaw(buffer, value);
                    }
                }
            }
        }
        return buffer.array();
    }

    private static void putSignedPCM(ByteBuffer buffer, double value, int bits)
    {
        switch (bits)
        {
            case 8:
                buffer.put((byte) (value * 127));
                break;
            case 16:
                buffer.putShort((short) (value * 32767));
                break;
            case 24:
                int value24 = (int) (value * 8388607);
                if (buffer.order() == ByteOrder.BIG_ENDIAN)
                {
                    buffer.put((byte) (value24 >> 16));
                    buffer.put((byte) (value24 >> 8));
                    buffer.put((byte) value24);
                }
                else
                {
                    buffer.put((byte) value24);
                    buffer.put((byte) (value24 >> 8));
                    buffer.put((byte) (value24 >> 16));
                }
                break;
            case 32:
                buffer.putInt((int) (value * 2147483647));
                break;
        }
    }

    private static void putUnsignedPCM(ByteBuffer buffer, double value, int bits)
    {
        switch (bits)
        {
            case 8:
                buffer.put((byte) ((value * 127) + 128));
                break;
            case 16:
                buffer.putShort((short) ((value * 32767) + 32768));
                break;
            case 24:
                int value24 = (int) ((value * 8388607) + 8388608);
                if (buffer.order() == ByteOrder.BIG_ENDIAN)
                {
                    buffer.put((byte) (value24 >> 16));
                    buffer.put((byte) (value24 >> 8));
                    buffer.put((byte) value24);
                }
                else
                {
                    buffer.put((byte) value24);
                    buffer.put((byte) (value24 >> 8));
                    buffer.put((byte) (value24 >> 16));
                }
                break;
            case 32:
                buffer.putInt((int) ((value * 2147483647L) + 2147483648L));
                break;
        }
    }

    private static void putFloatPCM(ByteBuffer buffer, double value, int bits)
    {
        if (bits == 32)
        {
            buffer.putFloat((float) value);
        }
        else if (bits == 64)
        {
            buffer.putDouble(value);
        }
    }

    private static void putALaw(ByteBuffer buffer, double value)
    {
        // Convertir a PCM lineal de 16 bits y luego a A-law
        short pcm = (short) (value * 32767);
        buffer.put(linearToALaw(pcm));
    }

    private static void putULaw(ByteBuffer buffer, double value)
    {
        // Convertir a PCM lineal de 16 bits y luego a μ-law
        short pcm = (short) (value * 32767);
        buffer.put(linearToULaw(pcm));
    }

    private static byte linearToALaw(short pcm)
    {
        int mask;
        int seg;
        byte aval;

        pcm >>= 3;

        if (pcm >= 0)
        {
            mask = 0xD5;
        }
        else
        {
            mask = 0x55;
            pcm = (short) -pcm;
            if (pcm < 0)
            {
                pcm = 32767;
            }
        }

        if (pcm > 32635)
        {
            pcm = 32635;
        }

        if (pcm >= 256)
        {
            seg = 8;
            for (int i = 0x4000; (pcm & i) == 0 && seg > 0; i >>= 1, seg--);
        }
        else
        {
            seg = 1;
        }

        if (seg >= 8)
        {
            aval = (byte) (0x7F ^ mask);
        }
        else
        {
            aval = (byte) (seg << 4);
            if (seg < 2)
            {
                aval |= (pcm >> 1) & 0x0F;
            }
            else
            {
                aval |= (pcm >> seg) & 0x0F;
            }
            aval ^= mask;
        }

        return aval;
    }

    private static byte linearToULaw(short pcm)
    {
        int mask;
        int seg;
        byte uval;

        if (pcm < 0)
        {
            pcm = (short) -pcm;
            mask = 0x7F;
        }
        else
        {
            mask = 0xFF;
        }

        if (pcm > 32635)
        {
            pcm = 32635;
        }
        pcm += 0x84;

        seg = 8;
        for (int i = 0x4000; (pcm & i) == 0 && seg > 0; i >>= 1, seg--);

        if (seg >= 8)
        {
            uval = (byte) (0x7F ^ mask);
        }
        else
        {
            uval = (byte) ((seg << 4) | ((pcm >> (seg + 3)) & 0x0F));
            uval ^= mask;
        }

        return uval;
    }
    
    public static float[] hannWindow(float[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = (float) (0.5 * (1 - Math.cos(2 * Math.PI * i / (window.length - 1))));
        }
        return window;
    }
    
    public static double[] hannWindow(double[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (window.length - 1)));
        }
        return window;
    }
    
    public static float[] hammingWindow(float[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = (float) (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (window.length - 1)));
        }
        return window;
    }
    
    public static double[] hammingWindow(double[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (window.length - 1));
        }
        return window;
    }
    
    
}

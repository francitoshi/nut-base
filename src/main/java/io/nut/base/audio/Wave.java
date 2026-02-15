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

/**
 * Abstract base class for audio waveform generators.
 *
 * <p>A {@code Wave} defines a mathematical function that maps a sample index
 * to an amplitude value in the range {@code [-1.0, 1.0]}. Concrete waveforms
 * are provided as public static constants (e.g. {@link #SINE}, {@link #SQUARE}).
 * The class also provides methods to render a waveform into a raw PCM byte buffer
 * and utility methods for computing analysis window functions.
 *
 * <p>Typical usage:
 * <pre>{@code
 * AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
 * byte[] buffer = new byte[44100 * 2]; // 1 second, 16-bit mono
 * Wave.SINE.build(format, 440, buffer, 0.8);
 * }</pre>
 *
 * <p>All built-in waveform instances are stateless except {@link #BROWNIAN_NOISE},
 * which maintains a running value between successive calls to
 * {@link #getValue(float, int, int, double)} and is therefore not thread-safe.
 */
public abstract class Wave
{
    /** Human-readable name of this waveform (e.g. {@code "Sine"}, {@code "Square"}). */
    public final String name;
    
    /**
     * Computes the amplitude of this waveform at a given sample index.
     *
     * <p>The returned value is in the range {@code [-volume, +volume]}, which
     * itself should be within {@code [-1.0, 1.0]} to avoid clipping when the
     * result is written to a PCM buffer.
     *
     * @param sampleRate the audio sample rate in Hz (e.g. {@code 44100.0f}).
     * @param i          the zero-based sample index.
     * @param hz         the desired tone frequency in Hz.
     * @param volume     the peak amplitude scale factor; {@code 1.0} produces
     *                   full-scale output.
     * @return the waveform amplitude at sample {@code i}.
     */
    public abstract double getValue(float sampleRate, int i, int hz, double volume);

    /**
     * Constructs a new {@code Wave} with the given display name.
     *
     * @param name a human-readable identifier for this waveform.
     */
    public Wave(String name)
    {
        this.name = name;
    }
    
    /**
     * Standard sine wave: {@code sin(2π·i·hz / sampleRate) × volume}.
     *
     * <p>Produces a pure, band-limited tone with no harmonic content above the
     * fundamental frequency. Suitable for clear Morse code tones and general-purpose
     * audio generation.
     */
    public static final Wave SINE = new Wave("Sine")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double angle = 2.0 * Math.PI * i * hz / sampleRate;
            return Math.sin(angle) * volume;
        }
    };
    
    /**
     * Square wave with a 50 % duty cycle.
     *
     * <p>Alternates between {@code +volume} and {@code -volume} at the given
     * frequency. Contains all odd harmonics (1st, 3rd, 5th, …) with amplitudes
     * that decrease as {@code 1/n}, giving a bright, hollow timbre.
     */
    public static final Wave SQUARE = new Wave("Square")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (phase < 0.5) ? volume : -volume;
        }
    };
    
    /**
     * Sawtooth wave that ramps linearly from {@code -volume} to {@code +volume}
     * over each period.
     *
     * <p>Contains all harmonics (odd and even) with amplitudes decreasing as
     * {@code 1/n}, producing a bright, buzzy timbre characteristic of many
     * synthesizer lead sounds.
     */
    public static final Wave SAWTOOTH = new Wave("Sawtooth")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (2.0 * phase - 1.0) * volume;
        }
    };
    
    /**
     * Triangle wave that rises and falls linearly between {@code -volume} and
     * {@code +volume}.
     *
     * <p>Contains only odd harmonics with amplitudes decreasing as {@code 1/n²},
     * producing a softer, more flute-like timbre than the square or sawtooth waves.
     */
    public static final Wave TRIANGLE = new Wave("Triangle")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (Math.abs(4.0 * phase - 2.0) - 1.0) * volume;
        }
    };
    
    /**
     * White noise: uniformly distributed random samples in {@code [-volume, +volume]}.
     *
     * <p>Each sample is independent with a flat power spectral density across all
     * frequencies. The {@code hz} parameter is ignored. Not repeatable between runs.
     */
    public static final Wave WHITE_NOISE = new Wave("WhiteNoise")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            return (Math.random() * 2.0 - 1.0) * volume;
        }
    };
    
    /**
     * Pulse wave with a fixed 25 % duty cycle.
     *
     * <p>The signal is at {@code +volume} for the first quarter of each period
     * and at {@code -volume} for the remaining three-quarters. Produces a thinner,
     * more nasal timbre than the 50 % square wave.
     */
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

    /**
     * Pulse wave with a fixed 33 % duty cycle.
     *
     * <p>The signal is at {@code +volume} for the first third of each period
     * and at {@code -volume} for the remaining two-thirds. Timbre sits between
     * {@link #DUTY_CYCLE_025} and the 50 % {@link #SQUARE} wave.
     */
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
    
    /**
     * Pulse-width modulation (PWM) wave with a slowly varying duty cycle.
     *
     * <p>A 0.5 Hz low-frequency oscillator (LFO) modulates the duty cycle
     * continuously between roughly 10 % and 90 %, creating a characteristic
     * chorus-like movement. The {@code hz} parameter controls the fundamental
     * frequency of the pulse carrier.
     */
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
    
    /**
     * Brownian (red) noise generated by a random walk.
     *
     * <p>Each sample is the previous value plus a small random step
     * ({@code ±0.05}), clamped to {@code [-1.0, 1.0]}. This produces a
     * low-frequency-heavy noise spectrum, useful for simulating rumble,
     * wind, or ocean sounds. The {@code hz} parameter is ignored.
     *
     * <p><strong>Note:</strong> this instance is <em>not</em> thread-safe because
     * it stores state ({@code lastValue}) between calls.
     */
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
    
    /**
     * Frequency-modulation (FM) synthesis wave.
     *
     * <p>A modulator sine wave at twice the carrier frequency ({@code hz × 2})
     * with a modulation index of {@code 5.0} is applied to the phase of the
     * carrier, producing rich sidebands and a bell-like or metallic timbre
     * depending on the carrier frequency.
     */
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
    
    /**
     * Parabolic wave approximating a sine wave with a simpler polynomial shape.
     *
     * <p>Defined as {@code (8·p·(1−p) − 1) × volume} where {@code p} is the
     * fractional phase in {@code [0, 1)}. Produces a smoother sound than a
     * triangle wave with slightly less computational cost than a true sine.
     */
    public static final Wave PARABOLIC = new Wave("Parabolic")
    {
        @Override
        public double getValue(float sampleRate, int i, int hz, double volume)
        {
            double phase = (i * hz / sampleRate) % 1.0;
            return (8.0 * phase * (1.0 - phase) - 1.0) * volume;
        }
    };
    
    /**
     * Additive synthesis wave combining the fundamental with its 2nd and 3rd harmonics.
     *
     * <p>The output is:
     * <pre>
     *   (sin(f) + 0.5·sin(2f) + 0.3·sin(3f)) / 1.8 × volume
     * </pre>
     * where {@code f = 2π·i·hz / sampleRate}. The result is normalised by
     * {@code 1.8} to prevent clipping. Produces a warmer, organ-like timbre.
     */
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
    
    /**
     * All available waveforms including noise sources.
     * Order: SINE, SQUARE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025, DUTY_CYCLE_033,
     * PWM, FM, PARABOLIC, ADITIVE, WHITE_NOISE, BROWNIAN_NOISE.
     */
    public static final Wave[] WAVES = { SINE, SQUARE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025, DUTY_CYCLE_033, PWM, FM, PARABOLIC, ADITIVE, WHITE_NOISE, BROWNIAN_NOISE};

    /**
     * Subset of {@link #WAVES} excluding stochastic noise sources.
     * Contains only deterministic, pitch-stable waveforms suitable for tonal use.
     * Order: SINE, SQUARE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025, DUTY_CYCLE_033,
     * PWM, FM, PARABOLIC, ADITIVE.
     */
    public static final Wave[] CLEAN_WAVES = { SINE, SQUARE, SAWTOOTH, TRIANGLE, DUTY_CYCLE_025, DUTY_CYCLE_033, PWM, FM, PARABOLIC, ADITIVE};
    
    /**
     * Renders this waveform into an existing byte buffer using the given
     * {@link AudioFormat}, with optional fade-in and fade-out.
     *
     * <p>Audio format parameters are extracted from {@code format} and forwarded
     * to {@link #build(AudioFormat.Encoding, float, int, int, boolean, int, byte[], double, int)}.
     *
     * @param format the target audio format.
     * @param hz     the tone frequency in Hz; pass {@code 0} to produce silence.
     * @param volume peak amplitude in the range {@code [0.0, 1.0]}.
     * @return the same {@code bytes} array, now filled with rendered audio data.
     */
    public byte[] build(AudioFormat format, int hz, double volume)
    {
        AudioFormat.Encoding encoding = format.getEncoding();
        int sampleRate = (int) format.getFrameRate();
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();
        int sampleBits = format.getSampleSizeInBits();
        return build(encoding, sampleRate, sampleBits, channels, bigEndian, hz, volume);
    }

    /**
     * Renders this waveform into an existing byte buffer using explicit audio
     * parameters, with optional linear fade-in and fade-out.
     *
     * <p>The number of samples is inferred from {@code bytes.length},
     * {@code sampleBits}, and {@code channels}. If either {@code hz} or
     * {@code volume} is zero, the buffer is returned unmodified (filled with
     * whatever data it already contains, typically zeros).
     *
     * <p>When {@code fading > 0}, a linear amplitude ramp is applied over the
     * first and last {@code fading} samples (clamped to at most one-third of
     * the total sample count) to avoid click artefacts at buffer boundaries.
     *
     * <p>Each sample is written to every channel in sequence, using the helper
     * methods appropriate to the encoding:
     * <ul>
     *   <li>{@code PCM_SIGNED}   → {@link #putSignedPCM}</li>
     *   <li>{@code PCM_UNSIGNED} → {@link #putUnsignedPCM}</li>
     *   <li>{@code PCM_FLOAT}    → {@link #putFloatPCM}</li>
     *   <li>{@code ALAW}         → {@link #putALaw}</li>
     *   <li>{@code ULAW}         → {@link #putULaw}</li>
     * </ul>
     *
     * @param encoding   the PCM encoding.
     * @param sampleRate audio sample rate in Hz.
     * @param sampleBits bits per sample.
     * @param channels   number of audio channels.
     * @param bigEndian  {@code true} for big-endian byte order.
     * @param hz         tone frequency in Hz; {@code 0} produces silence.
     * @param volume     peak amplitude in the range {@code [0.0, 1.0]}.
     * @return the same {@code bytes} array filled with rendered audio data.
     * @throws NullPointerException if {@code bytes} is {@code null}.
     */
    public byte[] build(AudioFormat.Encoding encoding, float sampleRate, int sampleBits, int channels, boolean bigEndian, int hz, double volume)
    {
        boolean signed = encoding.equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean unsigned = encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED);
        boolean pcmFloat = encoding.equals(AudioFormat.Encoding.PCM_FLOAT);
        boolean alaw = encoding.equals(AudioFormat.Encoding.ALAW);
        boolean ulaw = encoding.equals(AudioFormat.Encoding.ULAW);

        int sampleBytes = sampleBits / 8;
        int wavetableSize = hz!=0 ? Audio.wavetableSize((int) sampleRate, hz) : 1;
        byte[] bytes = new byte[wavetableSize * channels * sampleBytes];
        
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        if (hz != 0 && volume != 0)
        {
            for (int i = 0; i < wavetableSize; i++)
            {
                double value = this.getValue(sampleRate, i, hz, volume);

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

    /**
     * Writes a normalised amplitude value to {@code buffer} as a signed PCM sample.
     *
     * <p>The value is scaled to the full range of the target bit depth:
     * <ul>
     *   <li>8-bit:  {@code value × 127} → {@code byte}</li>
     *   <li>16-bit: {@code value × 32767} → {@code short}</li>
     *   <li>24-bit: {@code value × 8388607} → 3 bytes in the buffer's byte order</li>
     *   <li>32-bit: {@code value × 2147483647} → {@code int}</li>
     * </ul>
     *
     * @param buffer the target {@link ByteBuffer}, positioned at the write location.
     * @param value  normalised amplitude in {@code [-1.0, 1.0]}.
     * @param bits   bits per sample: 8, 16, 24, or 32.
     */
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

    /**
     * Writes a normalised amplitude value to {@code buffer} as an unsigned PCM sample.
     *
     * <p>The signed value is shifted to the unsigned range by adding a mid-point offset:
     * <ul>
     *   <li>8-bit:  {@code (value × 127) + 128}</li>
     *   <li>16-bit: {@code (value × 32767) + 32768}</li>
     *   <li>24-bit: {@code (value × 8388607) + 8388608}, written as 3 bytes</li>
     *   <li>32-bit: {@code (value × 2147483647) + 2147483648}</li>
     * </ul>
     *
     * @param buffer the target {@link ByteBuffer}, positioned at the write location.
     * @param value  normalised amplitude in {@code [-1.0, 1.0]}.
     * @param bits   bits per sample: 8, 16, 24, or 32.
     */
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

    /**
     * Writes a normalised amplitude value to {@code buffer} as a floating-point PCM sample.
     *
     * <p>Supports 32-bit ({@code float}) and 64-bit ({@code double}) formats only.
     * The value is written as-is without rescaling.
     *
     * @param buffer the target {@link ByteBuffer}, positioned at the write location.
     * @param value  normalised amplitude in {@code [-1.0, 1.0]}.
     * @param bits   bits per sample: 32 or 64.
     */
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

    /**
     * Converts a normalised amplitude value to an A-law encoded byte and writes
     * it to {@code buffer}.
     *
     * <p>The value is first scaled to a 16-bit signed PCM sample, then converted
     * to A-law via {@link #linearToALaw(short)}.
     *
     * @param buffer the target {@link ByteBuffer}, positioned at the write location.
     * @param value  normalised amplitude in {@code [-1.0, 1.0]}.
     */
    private static void putALaw(ByteBuffer buffer, double value)
    {
        // Convertir a PCM lineal de 16 bits y luego a A-law
        short pcm = (short) (value * 32767);
        buffer.put(linearToALaw(pcm));
    }

    /**
     * Converts a normalised amplitude value to a μ-law (u-law) encoded byte and
     * writes it to {@code buffer}.
     *
     * <p>The value is first scaled to a 16-bit signed PCM sample, then converted
     * to μ-law via {@link #linearToULaw(short)}.
     *
     * @param buffer the target {@link ByteBuffer}, positioned at the write location.
     * @param value  normalised amplitude in {@code [-1.0, 1.0]}.
     */
    private static void putULaw(ByteBuffer buffer, double value)
    {
        // Convertir a PCM lineal de 16 bits y luego a μ-law
        short pcm = (short) (value * 32767);
        buffer.put(linearToULaw(pcm));
    }

    /**
     * Converts a 16-bit linear PCM sample to an 8-bit A-law encoded byte.
     *
     * <p>Implements the ITU-T G.711 A-law companding algorithm. The input is
     * right-shifted by 3 bits before segment detection, reducing the effective
     * range to 13 bits. The sign bit is encoded via XOR masking ({@code 0xD5} for
     * positive values, {@code 0x55} for negative), and the magnitude is encoded
     * in one of 8 logarithmic segments.
     *
     * @param pcm a 16-bit signed linear PCM sample.
     * @return the A-law compressed byte.
     */
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

    /**
     * Converts a 16-bit linear PCM sample to an 8-bit μ-law (u-law) encoded byte.
     *
     * <p>Implements the ITU-T G.711 μ-law companding algorithm with μ = 255.
     * A bias of {@code 0x84} (132) is added before segment detection to linearise
     * the lower quantisation steps. The sign bit is encoded via XOR masking
     * ({@code 0xFF} for positive, {@code 0x7F} for negative), and the magnitude
     * is encoded in one of 8 logarithmic segments.
     *
     * @param pcm a 16-bit signed linear PCM sample.
     * @return the μ-law compressed byte.
     */
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
    
    /**
     * Fills a {@code float[]} array in-place with Hann window coefficients.
     *
     * <p>The Hann (von Hann) window is defined as:
     * <pre>
     *   w[i] = 0.5 × (1 − cos(2π·i / (N−1)))
     * </pre>
     * where {@code N = window.length}. It tapers smoothly to zero at both ends,
     * reducing spectral leakage in FFT-based analysis.
     *
     * @param window a pre-allocated array to fill; its length determines the
     *               window size {@code N}.
     * @return the same {@code window} array, now containing Hann coefficients
     *         in the range {@code [0.0, 1.0]}.
     */
    public static float[] hannWindow(float[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = (float) (0.5 * (1 - Math.cos(2 * Math.PI * i / (window.length - 1))));
        }
        return window;
    }
    
    /**
     * Fills a {@code double[]} array in-place with Hann window coefficients.
     *
     * <p>See {@link #hannWindow(float[])} for the formula and usage notes.
     *
     * @param window a pre-allocated array to fill.
     * @return the same {@code window} array, now containing Hann coefficients
     *         in the range {@code [0.0, 1.0]}.
     */
    public static double[] hannWindow(double[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (window.length - 1)));
        }
        return window;
    }
    
    /**
     * Fills a {@code float[]} array in-place with Hamming window coefficients.
     *
     * <p>The Hamming window is defined as:
     * <pre>
     *   w[i] = 0.54 − 0.46 × cos(2π·i / (N−1))
     * </pre>
     * where {@code N = window.length}. Unlike the Hann window it does not taper
     * fully to zero at the edges, which reduces the height of the highest sidelobe
     * in exchange for a slightly elevated floor.
     *
     * @param window a pre-allocated array to fill; its length determines the
     *               window size {@code N}.
     * @return the same {@code window} array, now containing Hamming coefficients
     *         in the range {@code [0.08, 1.0]}.
     */
    public static float[] hammingWindow(float[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = (float) (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (window.length - 1)));
        }
        return window;
    }
    
    /**
     * Fills a {@code double[]} array in-place with Hamming window coefficients.
     *
     * <p>See {@link #hammingWindow(float[])} for the formula and usage notes.
     *
     * @param window a pre-allocated array to fill.
     * @return the same {@code window} array, now containing Hamming coefficients
     *         in the range {@code [0.08, 1.0]}.
     */
    public static double[] hammingWindow(double[] window)
    {
        for (int i = 0; i < window.length; i++)
        {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (window.length - 1));
        }
        return window;
    }
}

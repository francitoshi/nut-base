/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

public abstract class EnergyDetector
{
    public final String name;

    public abstract double getEnergy(float[] data, int start, int stop, float sampleRate, double hz);

    public final double getEnergy(float[] data, float sampleRate, double hz)
    {
        return getEnergy(data, 0, data.length, sampleRate, hz);
    }

    public EnergyDetector(String name)
    {
        this.name = name;
    }

    public static EnergyDetector GOERTZEL_POWER = new EnergyDetector("GoertzelPower")
    {
        @Override
        public double getEnergy(float[] data, int start, int stop, float sampleRate, double hz)
        {
            double sPrev = 0, sPrev2 = 0;
            double normalizedFreq = 2.0 * Math.PI * hz / sampleRate;
            double coeff = 2.0 * Math.cos(normalizedFreq);

            for (int i = start; i < stop; i++)
            {
                double s = data[i] + coeff * sPrev - sPrev2;
                sPrev2 = sPrev;
                sPrev = s;
            }
            return sPrev2 * sPrev2 + sPrev * sPrev - coeff * sPrev * sPrev2;
        }
    };

    public static EnergyDetector RMS = new EnergyDetector("RootMeanSquare")
    {
        @Override
        public double getEnergy(float[] data, int start, int stop, float sampleRate, double hz)
        {
            double sum = 0;
            for(int i=start;i<stop;i++)
            {
                float value = data[i];
                sum += value*value;
            }
            return Math.sqrt(sum / (stop-start));
        }
    };

    public static EnergyDetector MS = new EnergyDetector("MeanSquare")
    {
        @Override
        public double getEnergy(float[] data, int start, int stop, float sampleRate, double hz)
        {
            double sum = 0;
            for(int i=start;i<stop;i++)
            {
                float value = data[i];
                sum += value*value;
            }
            return (sum / (stop-start));
        }
    };

}

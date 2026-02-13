/*
 * EnergyDetector.java
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

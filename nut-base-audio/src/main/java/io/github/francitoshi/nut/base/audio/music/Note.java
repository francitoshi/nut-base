/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio.music;

public class Note
{
    public final String name;	
    public final int octave;	
    public final double freq;
    public final int index;

    public Note(String name, int octave, double freq, int index)
    {
        this.name = name;
        this.octave = octave;
        this.freq = freq;
        this.index = index;
    }   

    public String getFreqText()
    {
            String unit = "";
            String freqText = "";

            double f =  this.freq;		
            if (f >= 10000d)
            {
                    f = f / 1000d;
                    f = java.lang.Math.round(f * 100d) / 100d;
                    unit = "kHz";
            }
            else {
                    if (f >= 1000)
                    {
                            f = f / 1000d;
                            f = java.lang.Math.round(f * 100d) / 100d;
                            unit = "kHz";
                    }
                    else
                    {							
                            f = java.lang.Math.round(f * 10d) / 10d;
                            unit = "Hz";
                    }
            }		

            double f_round = java.lang.Math.round(f);
            if (f_round == f)
            {
                    freqText += (int) f_round; // Nachkomma .0 wegschneiden
            }
            else
            {
                    freqText += f;
            }

            return freqText+" "+unit;	
    }

    public String toString()
    {
        return this.getFreqText()+" "+name;
    }

    public Note copy()
    {
        return new Note(this.name, this.octave, this.freq, this.index);
    }
}

/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio.music;

import java.util.HashMap;

public class Scale
{
    private static final String NAMES_ENGLISH   = "A,A♯/B♭,B,C,C♯/D♭,D,D♯/E♭,E,F,F♯/G♭,G,G♯/A♭";
    private static final String NAMES_GERMAN    = "A,A♯/H♭,H,C,C♯/D♭,D,D♯/E♭,E,F,F♯/G♭,G,G♯/A♭";
    private static final String NAMES_NEO_LATIN = "La,La♯/Si♭,Si,Do,Do♯/Re♭,Re,Re♯/Mi♭,Mi,Fa,Fa♯/Sol♭,Sol,Sol♯/La♭";
    
    static class EnglishHolder
    {
        static Scale INSTANCE = build("English", NAMES_ENGLISH);
    }
    static class GermanHolder
    {
        static Scale INSTANCE = build("German", NAMES_GERMAN);
    }
    static class NeoLatinHolder
    {
        static Scale INSTANCE = build("NeoLatin", NAMES_NEO_LATIN);
    }
        
    private static final int A4 = 440;
    
    private static double hertz(int index)
    {
        return Math.pow(2, (double) index/12) * A4;
    }
    
    private static String[][] splitNotes(String s)
    {
        String[] split1 = s.split(",");
        String[][] split2 = new String[split1.length][];
        for(int i=0;i<split1.length;i++)
        {
            split2[i] = split1[i].split("/");
        }
        return split2;
    }
    
    private static Scale build(String scale, String noteNames)
    {
        String[][] names = splitNotes(noteNames);

        int offset = 4*12;
        Note[] notes = new Note[offset*2];
        HashMap<String,Note> map = new HashMap<>();

        for(int i=0;i<notes.length;i++)
        {
            double hz = hertz(i-offset);            
            // Note frequency (hertz) based on A4: http://en.wikipedia.org/wiki/Note
            Note item = notes[i] = new Note(names[i%names.length][0], i/12, hz, i);
            map.put(item.name.toUpperCase()+item.octave, item);
            if(names[i%names.length].length>1)
            {
                item = new Note(names[i%names.length][1], (i+1)/12, hz, i);
                map.put(item.name.toUpperCase()+item.octave, item);
            }
        }
        return new Scale(scale, notes, map);
    }
    public static Scale buildEnglish()
    {
        return EnglishHolder.INSTANCE;
    }
    public static Scale buildGerman()
    {
        return GermanHolder.INSTANCE;
    }
    public static Scale buildNeoLatin()
    {
        return NeoLatinHolder.INSTANCE;
    }
    
    public final String name;
    private final Note[] notes;
    private final HashMap<String,Note> map;

    Scale(String name, Note[] notes, HashMap<String, Note> map)
    {
        this.name = name;
        this.notes = notes;
        this.map = map;
    }

    public Note get(String note)
    {
        return map.get(note.toUpperCase());
    }

}

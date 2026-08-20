/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import io.nut.base.util.Empty;
import io.nut.base.util.Strings;
import java.util.ArrayList;

/**
 *
 * @author franci
 */
public class Paragraphs
{
    static final String SEP = "\n[\\s]*\n";
    static final String UFFF = "[\ufff0-\uffff]+";
    //static final String TRIM = "(^\\s+)|(\\s+$)";

    public static String[] split(String doc)
    {
        //return doc.split(SEP);
        return split(doc, false);
    }
    public static String[] split(String doc, boolean cleanUnicode)
    {
        //split into paragraphs
        String[] array = doc.split(SEP);
        
        ArrayList<String> list = new ArrayList<>(array.length);
        for(String item : array)
        {
            //clean dirty characters
            item = cleanUnicode?item.replaceAll(UFFF, " "):item;
            //trim
            item = Strings.trimWhitespaces(item);
            if(item.length()>0)
            {
                list.add(item);
            }
        }
        return list.toArray(Empty.STRINGS);
    }
}

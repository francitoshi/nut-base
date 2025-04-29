/*
 * Sentences.java
 *
 * Copyright (c) 2012-2025 francitoshi@gmail.com
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
package io.nut.base.text;


import io.nut.base.net.URLs;
import io.nut.base.util.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author franci
 */
public class Sentences
{
    private static final Logger logger = Logger.getLogger(Sentences.class.getName());
    //(sentence)+(punct)+(spaces)+(sentence)
    static final boolean debug = false;

    static final String OPEN   = "¿¡({\\[«•—";
    static final String CLOSE  = "。.।:;?!)}\\]…|»—";// full stops => । Devanagari, ։ armenian, ෴ Sinhala, ። Amharic
    static final String NEXT   = "($|(.|\n)+)";
    static final String QUOTES  = "\"“”»«";
    static final String QUOTE  = "'‘’";
    static final String TABSPACE  = " \t";
    static final String ANYSPACE  = "\\s\u00a0";
    static final String ALL  = OPEN+CLOSE+QUOTES;

    private static String __(String exp)
    {
        String regex = exp.replace("A", ALL)
                          .replace("a", ALL+QUOTE)
                          .replace("O", OPEN)
                          .replace("C", CLOSE)
                          .replace("c", CLOSE+",")
                          .replace("Q", QUOTES)
                          .replace("q", QUOTE)
                          .replace("N", NEXT)
                          .replace("s",TABSPACE)//spaces in the same line
                          .replace("S",ANYSPACE);//all kind of spaces
        return regex;
    }
    //simple
    static final String[] REGEX =
    {
        "(([^A](\\w[.]\\w)?)+[—.:;)»][—.:;)»,]*)",//1
        "([OQqS]*[^A]+[cQqS]*([S]|$))",//2 ([open|quote]+text+[close|quote))+(quote+text)
        "([OQqS]*[^A]+[cQqS]*[Qq]([S]|$))",//3 ([open|quote]+text+[close|quote))+(quote+text)
        "([OQqS]*[^A]+[cQqS]*)",//4
        "([OQqS]*?[^A]+?[cQqS]*?)",//5 ([open|quote]+text+[close|quote))+(open+text)
        "([OQqS]*[\\wNS]+[cQqS]*)",//4 bis
    };

    private static final Object lock = new Object();
    static Pattern[] patterns = null;
    static int[] statistics = null;
    static volatile int failures = 0;
    static Pattern[] getPatterns()
    {
        synchronized(lock)
        {
            if(patterns==null)
            {
                patterns = new Pattern[REGEX.length];
                for(int i=0;i<patterns.length;i++)
                {
                    patterns[i] = Pattern.compile(__(REGEX[i]));
                }
                statistics = new int[patterns.length];
            }
            return patterns;
        }
    }   
   
    public static List<String> splitByPattern(String paragraph)
    {
        ArrayList<String> sentences = new ArrayList<>();
        Pattern[] patterns = getPatterns();        
        int index=0;
        int count=0;
        while(paragraph!=null && paragraph.length()>0)
        {
            if(paragraph.length()<=index)
            {
                break;
            }
            
            Matcher matcher=null;
            for(int i=0;i<patterns.length;i++)
            {
                try
                {
                    matcher = patterns[i].matcher(paragraph);
                    if(matcher.find(index) && matcher.start()==index)
                    {
                        statistics[i]++;
                        if(debug) System.out.println("regex[i]="+i+"-> "+REGEX[i]);
                        break;
                    }
                }
                catch(StackOverflowError ex)
                {
                    logger.log(Level.INFO, "stackoverflow on {0}", new Object[]{REGEX[i]});
                }
                matcher = null;
            }
            if(matcher != null)
            {
                String match = matcher.group();
                if(debug) System.out.println(match);
                index = matcher.end();
                sentences.add(match);
                continue;
            }
            String nomatch = paragraph.substring(index);
            if(debug) System.out.println(nomatch);
            sentences.add(nomatch);
            if(debug) logger.log(Level.CONFIG, "sentence.unmatch({0})={1}",new Object[]{count++,nomatch});
            failures++;
            break;
        }
        if(logger.isLoggable(Level.CONFIG))
        {
            logger.log(Level.CONFIG, Arrays.toString(statistics) );
        }
        return sentences;
    }
    public static List<String> splitByPattern(List<String> paragraphs)
    {
        ArrayList<String> sentences = new ArrayList<>(paragraphs.size());
        for(String item : paragraphs)
        {
            List<String> cuts = splitByPattern(item);
            sentences.addAll(cuts);
        }
        paragraphs = sentences;
        sentences = new ArrayList<>(paragraphs.size());
        return paragraphs;
    }

    public String[] split(String paragraph)
    {
        return split(paragraph, 0);
    }

    private static class Sentence
    {
        final String full;
        final String trim;
        final int size;

        public Sentence(String full)
        {
            this.full = full;
            this.trim = Strings.trimWhitespaces(full);
            this.size = this.trim.length();
        }
    }
    
    public String[] split(String paragraph, int min)
    {
        if(paragraph.length()<min)
        {
            return new String[]{paragraph};
        }
        
        List<String> cuts = splitByPattern(Arrays.asList(paragraph));
        ArrayList<Sentence> join = new ArrayList<>();

        //join possible abbreviations
        while(cuts.size()>0)
        {
            String p0 = cuts.get(0);
            int n0 = p0.trim().length();
            if(n0==0)
            {
                cuts.remove(0);
                continue;
            }
            if(cuts.size()==1)
            {
                join.add(new Sentence(cuts.remove(0)));
                break;
            }
            String p1 = cuts.get(1);
            int n1 = p1.trim().length();
            if(n1==0)
            {
                cuts.remove(1);
                continue;
            }
            boolean nbs  = p0.endsWith("\u00a0");// the last character is a nonbreaking space

            boolean dot  = !nbs && p0.endsWith(".");     // the last character is a dot
            boolean dotm = dot && !Character.isWhitespace(p1.codePointAt(0));

            boolean abb  = !nbs && (dot || p0.trim().endsWith(".")); // the last non-space character is a dot
            boolean abbm = abb && dotContinued(p1);
            
            boolean colon = !nbs && !dot && p0.endsWith(":");// the last character is a colon
            boolean colonm= colon && !Character.isWhitespace(p1.codePointAt(0));//merge if has no espace
            
            //merge if it is an url and continues the url (do nothing if it will join anyway)
            boolean url = (!nbs && !dotm && !abbm && !colonm) && joinableUrl(p0, p1);
            //merge if a filter would be applied (only for dots until needed)
            boolean fj  = (!nbs && !dotm && !abbm && !colonm && !url) && filter!=null && (dot||abb) && joinableFilter(p0,p1);
            
            if(nbs ||dotm || abbm || colonm || url || fj)
            {
                String p=p0+p1;
                cuts.set(0, p);
                cuts.remove(1);
                continue;
            }
            join.add(new Sentence(cuts.remove(0)));
        }
                
        List<String> merge = new ArrayList<>();
        //merge short sentences
        while(join.size()>0)
        {
            int n = join.size();
            Sentence p0 = join.get(0);
            Sentence p1 = (n>1)?join.get(1):null;
            Sentence p2 = (n>2)?join.get(2):null;
            Sentence p3 = (n>3)?join.get(3):null;
            
            //this would be a bug
            if(p0.size==0)
            {
                Logger.getLogger(Sentences.class.getName()).log(Level.WARNING, "p0.size==0 means a bug");
                join.remove(0);
                continue;
            }
            
            // n0+N1+... = n0N1+...
            if(n>1 && !unjoinable(p0, p1))
            {
                if( p0.size<min || (n==2 && p1.size<min) || (n>=3 && p1.size<min && p0.size+p1.size<p1.size+p2.size) || (n>=4 && p1.size<min && p1.size+p2.size+p3.size<min))
                {
                    join.set(0, new Sentence(p0.full+p1.full));
                    join.remove(1);
                    continue;
                }
            }
            merge.add(p0.trim);
            join.remove(0);
        }
        return merge.toArray(new String[0]);
    }

    private boolean unjoinable(Sentence p0, Sentence p1)
    {
        return ( p0.full.endsWith("\n") && p1.full.startsWith("•") );
    }

    private static boolean dotContinued(String sentence)
    {
        if(sentence.length()==0)
        {
            return false;
        }
        int count = sentence.codePointCount(0, sentence.length());
        int i=0;
        //spaces
        for(;i<count;i++)
        {
            int codepoint = sentence.codePointAt(i);
            if(!Character.isSpaceChar(codepoint))
                break;
        }
        if(i>=count)
        {
            return false;
        }
        int codepoint = sentence.codePointAt(i);
        return Character.isLowerCase(codepoint);
    }
    
    private static final int MIN_URL_SIZE=7;
    private static boolean joinableUrl(String s0, String s1)
    {
        //no whitespaces
        if(Character.isWhitespace(s1.codePointAt(0)))
        {
            return false;
        }
        
        //must be long enough
        int max0=s0.length();
        if(max0<MIN_URL_SIZE)
        {
            return false;
        }
        //filter by the last character to avoid overhead
        int max0cp=s0.codePointCount(0, max0);
        int cp0   =s0.codePointAt(max0cp-1);
        if(cp0!='?')
        {
            return false;
        }
                
        //get url
        int count=0;
        int index=0;
        boolean colon=false;
        boolean slash=false;
        for(int i=max0-1;i>=0;i--)
        {
            char c = s0.charAt(i);
            if(Character.isWhitespace(c))
            {
                break;
            }
            colon = colon || (c==':');
            slash = slash || (c=='/');
            index=i;
            count++;
        }
        
        //at leash one colon, an slash and long enough
        if(!colon || !slash || count<MIN_URL_SIZE)
        {
            return false;
        }
        String url=s0.substring(index);
        Matcher matcher = URLs.WEB_URL_PATTERN.matcher(url);
        return matcher.find();
    }
    
    private boolean joinableFilter(String s0, String s1)
    {
        s0 = filter.filter(s0, locale);
        s1 = filter.filter(s1);
        String s01=s0+s1;
        return !filter.filter(s01, locale).equals(s01);
    }

    public static int getFailures()
    {
        return failures;
    }
    
    final StringFilter filter;
    final Locale locale;

    public Sentences(StringFilter filter, Locale locale)
    {
        this.filter = filter;
        this.locale = locale;
    }
    public Sentences()
    {
        this(null, null);
    }
    
}

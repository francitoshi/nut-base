/*
 * URLs.java
 *
 * Copyright (c) 2014-2025 Francisco Gómez Carrasco
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: flikxxi@gmail.com
 */

package io.nut.base.net;

import io.nut.base.util.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by franci on 02-08-14.
 */
public class URLs
{
    /**
     * Good characters for Internationalized Resource Identifiers (IRI).
     * This comprises most common used Unicode characters allowed in IRI
     * as detailed in RFC 3987.
     * Specifically, those two byte Unicode characters are not included.
     */
    public static final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    /**
     *  Regular expression to match all IANA top-level domains for WEB_URL.
     *  List accurate as of 2011/07/18.  List taken from:
     *  http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     *  This pattern is auto-generated by frameworks/ex/common/tools/make-iana-tld-pattern.py
     */
    public static final String TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL =
            "(?:"
                    + "(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
                    + "|(?:biz|b[abdefghijmnorstvwyz])"
                    + "|(?:cat|com|coop|c[acdfghiklmnoruvxyz])"
                    + "|d[ejkmoz]"
                    + "|(?:edu|e[cegrstu])"
                    + "|f[ijkmor]"
                    + "|(?:gov|g[abdefghilmnpqrstuwy])"
                    + "|h[kmnrtu]"
                    + "|(?:info|int|i[delmnoqrst])"
                    + "|(?:jobs|j[emop])"
                    + "|k[eghimnprwyz]"
                    + "|l[abcikrstuvy]"
                    + "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
                    + "|(?:name|net|n[acefgilopruz])"
                    + "|(?:org|om)"
                    + "|(?:pro|p[aefghklmnrstwy])"
                    + "|qa"
                    + "|r[eosuw]"
                    + "|s[abcdeghijklmnortuvyz]"
                    + "|(?:tel|travel|t[cdfghjklmnoprtvwz])"
                    + "|u[agksyz]"
                    + "|v[aceginu]"
                    + "|w[fs]"
                    + "|(?:\u03b4\u03bf\u03ba\u03b9\u03bc\u03ae|\u0438\u0441\u043f\u044b\u0442\u0430\u043d\u0438\u0435|\u0440\u0444|\u0441\u0440\u0431|\u05d8\u05e2\u05e1\u05d8|\u0622\u0632\u0645\u0627\u06cc\u0634\u06cc|\u0625\u062e\u062a\u0628\u0627\u0631|\u0627\u0644\u0627\u0631\u062f\u0646|\u0627\u0644\u062c\u0632\u0627\u0626\u0631|\u0627\u0644\u0633\u0639\u0648\u062f\u064a\u0629|\u0627\u0644\u0645\u063a\u0631\u0628|\u0627\u0645\u0627\u0631\u0627\u062a|\u0628\u06be\u0627\u0631\u062a|\u062a\u0648\u0646\u0633|\u0633\u0648\u0631\u064a\u0629|\u0641\u0644\u0633\u0637\u064a\u0646|\u0642\u0637\u0631|\u0645\u0635\u0631|\u092a\u0930\u0940\u0915\u094d\u0937\u093e|\u092d\u093e\u0930\u0924|\u09ad\u09be\u09b0\u09a4|\u0a2d\u0a3e\u0a30\u0a24|\u0aad\u0abe\u0ab0\u0aa4|\u0b87\u0ba8\u0bcd\u0ba4\u0bbf\u0baf\u0bbe|\u0b87\u0bb2\u0b99\u0bcd\u0b95\u0bc8|\u0b9a\u0bbf\u0b99\u0bcd\u0b95\u0baa\u0bcd\u0baa\u0bc2\u0bb0\u0bcd|\u0baa\u0bb0\u0bbf\u0b9f\u0bcd\u0b9a\u0bc8|\u0c2d\u0c3e\u0c30\u0c24\u0c4d|\u0dbd\u0d82\u0d9a\u0dcf|\u0e44\u0e17\u0e22|\u30c6\u30b9\u30c8|\u4e2d\u56fd|\u4e2d\u570b|\u53f0\u6e7e|\u53f0\u7063|\u65b0\u52a0\u5761|\u6d4b\u8bd5|\u6e2c\u8a66|\u9999\u6e2f|\ud14c\uc2a4\ud2b8|\ud55c\uad6d|xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-3e0b707e|xn\\-\\-45brj9c|xn\\-\\-80akhbyknj4f|xn\\-\\-90a3ac|xn\\-\\-9t4b11yi5a|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-deba0ad|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-gecrj9c|xn\\-\\-h2brj9c|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d|xn\\-\\-kpry57d|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-s9brj9c|xn\\-\\-wgbh1c|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah|xxx)"
                    + "|y[et]"
                    + "|z[amw]))";

    public static final String WEB_URL = //fgc:2013-11-15 it ignores slash at the end of the url
            "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                    + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                    + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                    + "((?:(?:[" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]{0,64}\\.)+"   // named host
                    + TOP_LEVEL_DOMAIN_STR_FOR_WEB_URL
                    + "|(?:(?:25[0-5]|2[0-4]" // or ip address
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]"
                    + "|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9])))"
                    + "(?:\\:\\d{1,5})?)" // plus option port number
                    + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
                    + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                    + "(?:\\b|$)"; // and finally, a word boundary or end of
    // input.  This is to stop foo.sure from
    // matching as foo.su
    public static final Pattern SPACE_PATTER = Pattern.compile(" ");
    public static final Pattern WEB_URL_PATTERN = Pattern.compile(URLs.WEB_URL);

    //lenient version, using _
    public static final String GOOD_IRI_CHAR_LENIENT = GOOD_IRI_CHAR+"_";
    public static final String WEB_URL_LENIENT = WEB_URL.replace(GOOD_IRI_CHAR,GOOD_IRI_CHAR_LENIENT);
    public static final Pattern WEB_URL_PATTERN_LENIENT = Pattern.compile(WEB_URL_LENIENT);


    public static String replaceSpaces(String url)
    {
        if (url.length()>0)
        {
            url = url.trim();
            if (url.contains(" "))
            {
                Matcher spaces = SPACE_PATTER.matcher(url);
                url = spaces.replaceAll("%20");
            }
        }
        return url;
    }
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    public static String extractDomain(String url, boolean aggressive)
    {
        if (url.startsWith(HTTP))
        {
            url = url.substring(HTTP.length());
        }
        else if (url.startsWith(HTTPS))
        {
            url = url.substring(HTTPS.length());
        }

        if (aggressive)
        {
            if (url.startsWith("www."))
            {
                url = url.substring("www.".length());
            }

            // strip mobile from start
            if (url.startsWith("m."))
            {
                url = url.substring("m.".length());
            }
        }

        int slashIndex = url.indexOf("/");
        if (slashIndex > 0)
        {
            url = url.substring(0, slashIndex);
        }

        return url;
    }
    public static String simplifyDomain(String url)
    {
        return url!=null ? url.replaceAll("^[wW][wW][wW][.]","") : url;
    }

    public static boolean isWebURL(String url)
    {
        return isWebURL(url, false);
    }
    public static boolean isWebURL(String url, boolean lenient)
    {
        return (url!=null) && url.trim().matches(lenient?WEB_URL_LENIENT:WEB_URL);
    }
    
    /**
     * Provides a method to encode any string into a URL-safe form. Non-ASCII
     * characters are first encoded as sequences of two or three bytes, using the
     * UTF-8 algorithm, before being encoded as %HH escapes.
     *
     * Created: 17 April 1997 Author: Bert Bos <bert@w3.org>
     *
     * URLUTF8Encoder: http://www.w3.org/International/URLUTF8Encoder.java
     *
     * Copyright © 1997 World Wide Web Consortium, (Massachusetts Institute of
     * Technology, European Research Consortium for Informatics and Mathematics,
     * Keio University). All Rights Reserved. This work is distributed under the
     * W3C® Software License [1] in the hope that it will be useful, but WITHOUT ANY
     * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
     * A PARTICULAR PURPOSE.
     *
     * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
     */
    
    final static String[] hex =
    {
        "%00", "%01", "%02", "%03", "%04", "%05", "%06", "%07",
        "%08", "%09", "%0a", "%0b", "%0c", "%0d", "%0e", "%0f",
        "%10", "%11", "%12", "%13", "%14", "%15", "%16", "%17",
        "%18", "%19", "%1a", "%1b", "%1c", "%1d", "%1e", "%1f",
        "%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27",
        "%28", "%29", "%2a", "%2b", "%2c", "%2d", "%2e", "%2f",
        "%30", "%31", "%32", "%33", "%34", "%35", "%36", "%37",
        "%38", "%39", "%3a", "%3b", "%3c", "%3d", "%3e", "%3f",
        "%40", "%41", "%42", "%43", "%44", "%45", "%46", "%47",
        "%48", "%49", "%4a", "%4b", "%4c", "%4d", "%4e", "%4f",
        "%50", "%51", "%52", "%53", "%54", "%55", "%56", "%57",
        "%58", "%59", "%5a", "%5b", "%5c", "%5d", "%5e", "%5f",
        "%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67",
        "%68", "%69", "%6a", "%6b", "%6c", "%6d", "%6e", "%6f",
        "%70", "%71", "%72", "%73", "%74", "%75", "%76", "%77",
        "%78", "%79", "%7a", "%7b", "%7c", "%7d", "%7e", "%7f",
        "%80", "%81", "%82", "%83", "%84", "%85", "%86", "%87",
        "%88", "%89", "%8a", "%8b", "%8c", "%8d", "%8e", "%8f",
        "%90", "%91", "%92", "%93", "%94", "%95", "%96", "%97",
        "%98", "%99", "%9a", "%9b", "%9c", "%9d", "%9e", "%9f",
        "%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6", "%a7",
        "%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af",
        "%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6", "%b7",
        "%b8", "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf",
        "%c0", "%c1", "%c2", "%c3", "%c4", "%c5", "%c6", "%c7",
        "%c8", "%c9", "%ca", "%cb", "%cc", "%cd", "%ce", "%cf",
        "%d0", "%d1", "%d2", "%d3", "%d4", "%d5", "%d6", "%d7",
        "%d8", "%d9", "%da", "%db", "%dc", "%dd", "%de", "%df",
        "%e0", "%e1", "%e2", "%e3", "%e4", "%e5", "%e6", "%e7",
        "%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee", "%ef",
        "%f0", "%f1", "%f2", "%f3", "%f4", "%f5", "%f6", "%f7",
        "%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff"
    };

    public static final int PLUS_SPACE = 1;
    public static final int GEN_DELIMS = 2;
    public static final int SUB_DELIMS = 3;

    /**
     * Encode a string to the "x-www-form-urlencoded" form, enhanced with the
     * UTF-8-in-URL proposal. This is what happens:
     *
     * <ul>
     * <li><p>
     * The ASCII characters 'a' through 'z', 'A' through 'Z', and '0' through
     * '9' remain the same.
     *
     * <li><p>
     * The unreserved characters - _ . ! ~ * ' ( ) remain the same.
     *
     * <li><p>
     * The space character ' ' is converted into a plus sign '+'.
     *
     * <li><p>
     * All other ASCII characters are converted into the 3-character string
     * "%xy", where xy is the two-digit hexadecimal representation of the
     * character code
     *
     * <li><p>
     * All non-ASCII characters are encoded in two steps: first to a sequence of
     * 2 or 3 bytes, using the UTF-8 algorithm; secondly each of these bytes is
     * encoded as "%xx".
     * </ul>
     *
     * changed by franci
     *
     * @param s The string to be encoded
     * @param flags
     * @return The encoded string
     */
    public static String encode(String s, int flags)
    {
        if(s==null)
        {
            return null;
        }
        boolean plusSpace = (flags&PLUS_SPACE)==PLUS_SPACE;
        boolean genDelims = (flags&GEN_DELIMS)==GEN_DELIMS;
        boolean subDelims = (flags&SUB_DELIMS)==SUB_DELIMS;
        
        StringBuilder sbuf = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++)
        {
            int ch = s.charAt(i);
            if( ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z') || ('0' <= ch && ch <= '9') )
            {	// 'A'..'Z' // 'a'..'z' // '0'..'9'
                sbuf.append((char) ch);
            }
            else if (!genDelims && (ch==':' || ch=='/' || ch=='?' || ch=='#' || ch=='[' || ch==']' || ch=='@') )
            {   // :/?#[]@
                sbuf.append((char) ch);
            }
            else if (!subDelims && (ch=='!' || ch=='$' || ch=='&' || ch=='\'' || ch=='(' || ch==')' || ch=='*' || ch=='+' || ch==',' || ch==';' || ch=='=') )
            {   // !$&'()*+,;=
                sbuf.append((char) ch);
            }
            else if (ch == ' ' && plusSpace)
            {	// space
                sbuf.append('+');
            }
            else if (ch=='-' || ch=='_' || ch=='.' || ch=='!' || ch=='~' || ch=='*' || ch=='\'' || ch=='(' || ch==')')
            {   // unreserved
                sbuf.append((char) ch);
            }
            else if (ch <= 0x007f)
            {		// other ASCII
                sbuf.append(hex[ch]);
            }
            else if (ch <= 0x07FF)
            {	// non-ASCII <= 0x7FF
                sbuf.append(hex[0xc0 | (ch >> 6)]);
                sbuf.append(hex[0x80 | (ch & 0x3F)]);
            }
            else
            {	// 0x7FF < ch <= 0xFFFF
                sbuf.append(hex[0xe0 | (ch >> 12)]);
                sbuf.append(hex[0x80 | ((ch >> 6) & 0x3F)]);
                sbuf.append(hex[0x80 | (ch & 0x3F)]);
            }
        }
        return sbuf.toString();
    }

    /*
     * Created: 17 April 1997
     * Author: Bert Bos <bert@w3.org>
     *
     * unescape: http://www.w3.org/International/unescape.java
     *
     * Copyright © 1997 World Wide Web Consortium, (Massachusetts
     * Institute of Technology, European Research Consortium for
     * Informatics and Mathematics, Keio University). All Rights Reserved.
     * This work is distributed under the W3C® Software License [1] in the
     * hope that it will be useful, but WITHOUT ANY WARRANTY; without even
     * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
     * PURPOSE.
     *
     * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
     */
    public static String decode(String s)
    {
        if(s==null)
        {
            return null;
        }
        StringBuilder sbuf = new StringBuilder();
        int l = s.length();
        int ch = -1;
        int b, sumb = 0;
        for (int i = 0, more = -1; i < l; i++)
        {
            /* Get next byte b from URL segment s */
            switch (ch = s.charAt(i))
            {
                case '%':
                    ch = s.charAt(++i);
                    int hb = (Character.isDigit((char) ch)
                            ? ch - '0'
                            : 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
                    ch = s.charAt(++i);
                    int lb = (Character.isDigit((char) ch)
                            ? ch - '0'
                            : 10 + Character.toLowerCase((char) ch) - 'a') & 0xF;
                    b = (hb << 4) | lb;
                    break;
                case '+':
                    b = ' ';
                    break;
                default:
                    b = ch;
            }
            /* Decode byte b as UTF-8, sumb collects incomplete chars */
            if ((b & 0xc0) == 0x80)
            {			// 10xxxxxx (continuation byte)
                sumb = (sumb << 6) | (b & 0x3f);	// Add 6 bits to sumb
                if (--more == 0)
                {
                    sbuf.append((char) sumb); // Add char to sbuf
                }
            }
            else if ((b & 0x80) == 0x00)
            {		// 0xxxxxxx (yields 7 bits)
                sbuf.append((char) b);			// Store in sbuf
            }
            else if ((b & 0xe0) == 0xc0)
            {		// 110xxxxx (yields 5 bits)
                sumb = b & 0x1f;
                more = 1;				// Expect 1 more byte
            }
            else if ((b & 0xf0) == 0xe0)
            {		// 1110xxxx (yields 4 bits)
                sumb = b & 0x0f;
                more = 2;				// Expect 2 more bytes
            }
            else if ((b & 0xf8) == 0xf0)
            {		// 11110xxx (yields 3 bits)
                sumb = b & 0x07;
                more = 3;				// Expect 3 more bytes
            }
            else if ((b & 0xfc) == 0xf8)
            {		// 111110xx (yields 2 bits)
                sumb = b & 0x03;
                more = 4;				// Expect 4 more bytes
            }
            else /*if ((b & 0xfe) == 0xfc)*/ {	// 1111110x (yields 1 bit)
                sumb = b & 0x01;
                more = 5;				// Expect 5 more bytes
            }
            /* We don't test if the UTF-8 encoding is well-formed */
        }
        return sbuf.toString();
    }
    
    private static final String TRACKING_PARAMETER_ONE = "utm_(source|medium|campaign|term|content)[=][-+=%.\\p{L}\\p{N}]*";
    private static final String TRACKING_PARAMETER_FEW = "([&?]"+TRACKING_PARAMETER_ONE+")?([&]"+TRACKING_PARAMETER_ONE+")+$";
    
    public static String cleanTrackingParameters(String url)
    {
        return url.contains("utm_") ?  url.replaceAll(TRACKING_PARAMETER_FEW,"") : url;
    }
            
    public static boolean isVideoLink(String url)
    {
        url = URLs.extractDomain(url, true);
        return Strings.startsWithAny(url, "youtube.com","video.yahoo.com","vimeo.com","blip.tv");
    }
    
    public static boolean isVideo(String url)
    {
        return Strings.endsWithAny(url, ".mpeg",".mpg",".avi",".mov",".mpg4",".mp4",".flv",".wmv");
    }
    public static boolean isAudio(String url)
    {
        return Strings.endsWithAny(url,".mp3",".ogg",".m3u",".wav");
    }
    public static boolean isDoc(String url)
    {
        return Strings.endsWithAny(url,".pdf",".ppt",".doc",".swf",".rtf",".xls");
    }

    public static boolean isPackage(String url)
    {
        return Strings.endsWithAny(url,".gz",".tgz",".zip",".rar",".deb",".rpm",".7z");
    }

    public static boolean isApp(String url)
    {
        return Strings.endsWithAny(url,".exe",".bin",".bat",".dmg");
    }

    public static boolean isImage(String url)
    {
        return Strings.endsWithAny(url,".png",".jpeg",".gif",".jpg",".bmp",".ico",".eps",".svg");
    }
    /**
     * Popular sites uses the #! to indicate the importance of the following
     * chars. Ugly but true. Such as: facebook, twitter, gizmodo, ...
     */
    public static String removeHashbang(String url)
    {
        return url.replaceFirst("#!", "");
    }
            
}

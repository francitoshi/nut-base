/*
 *  CharSets.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.util;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * Operations on <code>CharSet</code>s.</p>
 *
 * <p>
 * This class handles <code>null</code> input gracefully. An exception will not
 * be thrown for a <code>null</code> input. Each method documents its behaviour
 * in more detail.</p>
 *
 */
public class CharSets
{

    /**
     * <p>
     * CharSetUtils instances should NOT be constructed in standard programming.
     * Instead, the class should be used as
     * <code>CharSetUtils.evaluateSet(null);</code>.</p>
     *
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
     */
    public CharSets()
    {
        super();
    }

    /**
     * Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the
     * Unicode character set
     */
    public static final String USASCII = StandardCharsets.US_ASCII.name();

    /**
     * ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
     */
    public static final String ISO88591 = StandardCharsets.ISO_8859_1.name();
    /**
     * Eight-bit UCS Transformation Format
     */
    public static final String UTF8 = StandardCharsets.UTF_8.name();
    /**
     * Sixteen-bit UCS Transformation Format, big-endian byte order
     */
    public static final String UTF16BE = StandardCharsets.UTF_16BE.name();
    /**
     * Sixteen-bit UCS Transformation Format, little-endian byte order
     */
    public static final String UTF16LE = StandardCharsets.UTF_16LE.name();
    /**
     * Sixteen-bit UCS Transformation Format, byte order identified by an
     * optional byte-order mark
     */
    public static final String UTF16 = StandardCharsets.UTF_16.name();

}

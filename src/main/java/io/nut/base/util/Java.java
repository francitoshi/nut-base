/*
 *  Java.java
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

/**
 *
 * @author franci
 */
public class Java
{
    private static int parseVersion(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch(NumberFormatException ex)
        {
            return 8;//minimum version is java8
        }
    }
    
    public static final int JAVA_INT_VERSION = parseVersion(System.getProperty("java.specification.version"));
    
    public static final String JAVA_HOME = System.getProperty("java.home",null);
    public static final String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir",null);
    public static final String OS_NAME = System.getProperty("os.name",null);
    public static final String OS_ARCH = System.getProperty("os.arch",null);
    public static final String OS_VERSION = System.getProperty("os.version", null);
    public static final String LINE_SEPARATOR = System.getProperty("line.separator", null);
    public static final String USER_NAME = System.getProperty("user.name", null);
    public static final String USER_HOME = System.getProperty("user.home", null);
    public static final String USER_DIR = System.getProperty("user.dir", null);
    public static final String JAVA_CLASS_PATH = System.getProperty("java.class.path", null);
    
}

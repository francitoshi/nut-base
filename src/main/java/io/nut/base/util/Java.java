/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

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
    
    public static int BYTE_BITS = 8;
    public static int SHORT_BITS = Short.BYTES * 8;
    public static int CHAR_BITS = Character.BYTES * 8;
    public static int INT_BITS = Integer.BYTES * 8;
    public static int LONG_BITS = Long.BYTES * 8;
    public static int FLOAT_BITS = Float.BYTES * 8;
    public static int DOUBLE_BITS = Double.BYTES * 8;

    private static class HolderMX
    {
        static final RuntimeMXBean BEAN = ManagementFactory.getRuntimeMXBean();
        static final long START_TIME = BEAN.getStartTime();
    }
    
    public static long getStartTime()
    {
        return HolderMX.START_TIME;
    }
    public static long getUptime()
    {
        return HolderMX.BEAN.getUptime();
    }    
    
}

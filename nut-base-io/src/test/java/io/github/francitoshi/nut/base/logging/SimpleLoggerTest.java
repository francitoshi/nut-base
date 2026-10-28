/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.logging;

import io.github.francitoshi.nut.base.commons.lang.Java;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class SimpleLoggerTest
{
    
    public SimpleLoggerTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    /**
     * Test of setFileHandler method, of class SimpleLogger.
     * @throws java.io.IOException
     */
    @Test
    public void testSetFileHandler() throws IOException
    {
        String tmpLog = new File(Java.JAVA_IO_TMPDIR,"cafecore.log").getAbsolutePath();
        
        SimpleLogger instance = new SimpleLogger(true).setConsoleOut(true, Level.FINEST, true, "out.prefix")
                                                       .setConsoleErr(true, Level.FINEST, true)
                                                       .setFileHandler(tmpLog, 12345, 9, true, Level.FINEST)
                                                       .apply();
        try
        {
            Logger logger = Logger.getLogger(SimpleLogger.class.getName());
            logger.setLevel(Level.FINEST);
            for(int i=0;i<10;i++)
            {
                logger.log(Level.SEVERE, "i={0}",i);
                logger.log(Level.WARNING, "i={0}",i);
                logger.log(Level.INFO, "i={0}",i);
                logger.log(Level.CONFIG, "i={0}",i);
                logger.log(Level.FINE, "i={0}",i);
                logger.log(Level.FINER, "i={0}",i);
                logger.log(Level.FINEST, "i={0}",i);
            }
        }
        finally
        {
            instance.close();
        }
    }
}

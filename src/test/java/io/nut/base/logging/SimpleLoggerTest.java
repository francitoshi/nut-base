/*
 * SimpleLoggerTest.java
 *
 * Copyright (c) 2017-2023 francitoshi@gmail.com
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
package io.nut.base.logging;

import io.nut.base.util.Utils;
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
        String tmpLog = new File(Utils.getTmpDir(),"cafecore.log").getAbsolutePath();
        
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

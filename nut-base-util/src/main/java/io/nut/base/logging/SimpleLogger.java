/*
 * SimpleLogger.java
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

import io.nut.base.io.ansi.AnsiPrintStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author franci
 */
public class SimpleLogger implements Closeable
{
    private static final Logger ROOT = Logger.getLogger("");
    private static final SimpleFormatter SIMPLE_FORMATER = new SimpleFormatter();
    
    private volatile boolean reset;
    
    private volatile String filePattern;
    private volatile int fileLimit;
    private volatile int fileCount;
    private volatile boolean fileAppend;
    private volatile Level fileLevel;
    
    private volatile boolean outEnabled;
    private volatile Level outLevel;
    private volatile boolean outColor;
    private volatile String outPrefix;
    
    private volatile boolean errEnabled;
    private volatile Level errLevel;
    private volatile boolean errColor;
    private volatile String errPrefix;
    
    private volatile FileHandler fileHandler;
    private volatile VerboseHandler outVerboseHandler;
    private volatile VerboseHandler errVerboseHandler;

    public SimpleLogger(boolean reset)
    {
        this.reset = reset;
    }
    
    public SimpleLogger setFileHandler(String pattern, int limit, int count, boolean append, Level level)
    {
        this.filePattern = pattern;
        this.fileLimit = limit;
        this.fileCount = count;
        this.fileAppend = append;
        this.fileLevel = level;
        return this;
    }
    public SimpleLogger setConsoleOut(boolean enabled, Level level, boolean color, String prefix)
    {
        this.outEnabled = enabled;
        this.outLevel = level;
        this.outColor = color;
        this.outPrefix = prefix;
        return this;
    }
    public SimpleLogger setConsoleOut(boolean enabled, Level level, boolean color)
    {
        return setConsoleOut(enabled, level, color, null);
    }
    public SimpleLogger setConsoleErr(boolean enabled, Level level, boolean color, String prefix)
    {
        this.errEnabled = enabled;
        this.errLevel = level;
        this.errColor = color;
        this.errPrefix = prefix;
        return this;
    }
    public SimpleLogger setConsoleErr(boolean enabled, Level level,boolean color)
    {
        return setConsoleErr(enabled, level, color, null);
    }
    
    public SimpleLogger apply() throws IOException
    {
        if(this.reset)
        {
            LogManager.getLogManager().reset();
            LogManager.getLogManager().readConfiguration();
        }
        
        if(this.filePattern!=null)
        {
            File file = new File(this.filePattern);
            file.getParentFile().mkdirs();
            this.fileHandler = new FileHandler(this.filePattern, this.fileLimit, this.fileCount, this.fileAppend);
            this.fileHandler.setFormatter(SIMPLE_FORMATER);
            this.fileHandler.setLevel(this.fileLevel);
            ROOT.addHandler(this.fileHandler);
            ensureLevel(ROOT, this.fileLevel);
        }
        if(this.outEnabled)
        {
            this.outVerboseHandler = this.outPrefix!=null ? new VerboseHandler(System.out, this.outColor, this.outPrefix) :  new VerboseHandler(System.out, this.outColor, SIMPLE_FORMATER);
            this.outVerboseHandler.setLevel(this.outLevel);
            if(this.outColor) this.setColors(this.outVerboseHandler);
            ROOT.addHandler(this.outVerboseHandler);
            ensureLevel(ROOT, this.outLevel);
        }
        if(this.errEnabled)
        {
            this.errVerboseHandler = this.errPrefix!=null ? new VerboseHandler(System.err, this.errColor, this.errPrefix) :  new VerboseHandler(System.err, this.errColor, SIMPLE_FORMATER);
            this.errVerboseHandler.setLevel(this.errLevel);
            if(this.errColor) this.setColors(this.errVerboseHandler);
            ROOT.addHandler(this.errVerboseHandler);
            ensureLevel(ROOT, this.errLevel);
        }
        return this;
    }
    private void setColors(VerboseHandler vh)
    {
        vh.setLevelColor(Level.OFF,     AnsiPrintStream.Color.White, null);
        vh.setLevelColor(Level.SEVERE,  AnsiPrintStream.Color.Red, null);
        vh.setLevelColor(Level.WARNING, AnsiPrintStream.Color.Purple, null);
        vh.setLevelColor(Level.INFO,    AnsiPrintStream.Color.Blue, null);
        vh.setLevelColor(Level.CONFIG,  AnsiPrintStream.Color.Green, null);
        vh.setLevelColor(Level.FINE,    AnsiPrintStream.Color.Black, null);
        vh.setLevelColor(Level.FINER,   AnsiPrintStream.Color.Black, null);
        vh.setLevelColor(Level.FINEST,  AnsiPrintStream.Color.Black, null);
        vh.setLevelColor(Level.ALL,     null, null);
        
    }
    
    private static void ensureLevel(Logger logger, Level level)
    {
        if(logger.getLevel().intValue()>level.intValue())
        {
            logger.setLevel(level);
        }
    }
    
    @Override
    public void close() throws IOException
    {
        if(this.fileHandler!=null)
        {
            this.fileHandler.flush();
            ROOT.removeHandler(this.fileHandler);
            this.fileHandler.close();
            this.fileHandler=null;
        }
        if(this.outVerboseHandler!=null)
        {
            this.outVerboseHandler.flush();
            ROOT.removeHandler(this.outVerboseHandler);
            this.outVerboseHandler.close();
            this.outVerboseHandler=null;
        }
        if(this.errVerboseHandler!=null)
        {
            this.errVerboseHandler.flush();
            ROOT.removeHandler(this.errVerboseHandler);
            this.errVerboseHandler.close();
            this.errVerboseHandler=null;
        }
    }
    
    
    private static final Object[][] RB = new Object[][] 
    {
        {Level.ALL.toString(),     "E"},
        {Level.SEVERE.toString(),  "E"},
        {Level.WARNING.toString(), "W"},
        {Level.INFO.toString(),    "I"},
        {Level.CONFIG.toString(),  "D"},
        {Level.FINE.toString(),    "V"},
        {Level.FINER.toString(),   "V"},
        {Level.FINEST.toString(),  "V"},
        {Level.OFF.toString(),     "V"},
    };
    private static final ResourceBundle rb = new ListResourceBundle() 
    {
        @Override
        public Set<String> keySet()
        {
            return super.keySet();
        }

        @Override
        protected Set<String> handleKeySet()
        {
            return super.handleKeySet(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void setParent(ResourceBundle rb)
        {
            super.setParent(rb); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Locale getLocale()
        {
            return super.getLocale(); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getBaseBundleName()
        {
            return "SimpleLogger";
        }
        
        @Override
        protected Object[][] getContents()
        {
            return RB;
        }

        @Override
        public boolean containsKey(String string)
        {
            return super.containsKey(string); //To change body of generated methods, choose Tools | Templates.
        }
        
    };

    public static ResourceBundle getResourceBundle()
    {
        return rb;
    }
    
}

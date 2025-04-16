/*
 *  VerboseHandler.java
 *
 *  Copyright (c) 2010-2025 francitoshi@gmail.com
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
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 *
 * @author franci
 */
public class VerboseHandler extends StreamHandler
{
    private static final Level levels[] = 
    {
        Level.SEVERE,
        Level.WARNING,
        Level.INFO,
        Level.CONFIG,
        Level.FINE,
        Level.FINER,
        Level.FINEST
    };
    private final ReentrantLock lock = new ReentrantLock();
    
    private static class Colors
    {
        final AnsiPrintStream.Color fg;
        final AnsiPrintStream.Color bg;
        public Colors(AnsiPrintStream.Color fg, AnsiPrintStream.Color bg)
        {
            this.fg = fg;
            this.bg = bg;
        }
    }
    private static final Colors NULL_COLORS = new Colors(null, null);
    private final HashMap<Level,Colors> levelColors = new HashMap<>();
    private final AnsiPrintStream ansi;
    

    public VerboseHandler()
    {
        super();
        this.ansi = null;
    }

    public VerboseHandler(OutputStream out, boolean color, Formatter formatter)
    {
        super(color?out=new AnsiPrintStream(out):out, formatter);
        this.ansi = color ? (AnsiPrintStream)out : null;
    }
    
    public VerboseHandler(OutputStream out, boolean color, final String prefix)
    {
        this(out, color, new Formatter()
        {
            @Override
            public String format(LogRecord record)
            {
                String message = record.getMessage();
                StringBuilder msg = new StringBuilder(prefix);
                boolean space=false;

                if(message!=null)
                {
                    space=true;
                    msg.append(MessageFormat.format(record.getMessage(),record.getParameters()));
                }
                record.setResourceBundle(rb);
                
                if(record.getThrown() != null)
                {
                    msg.append(space?" (":"(");
                    msg.append(record.getThrown().getMessage());
                    msg.append(")");
                }

                msg.append("\n");
                return msg.toString();
            }
            
        });    
    }

    @Override
    public synchronized void publish(LogRecord record)
    {
        lock.lock();
        try
        {
            if(this.ansi!=null)
            {
                Colors colors = this.levelColors.getOrDefault(record.getLevel(), NULL_COLORS);
                this.ansi.color(colors.fg,colors.bg, true);
            }
            super.publish(record);
            super.flush();
        }
        finally
        {
            lock.unlock();
        }
    }

    public void unlock()
    {
        lock.unlock();
    }

    public void lock()
    {
        lock.lock();
    }
    public static Level verboseLevel(int verbosity)
    {
        verbosity = Math.min(verbosity, levels.length-1);
        return levels[verbosity];
    }
    public static void register(int verbosity,VerboseHandler vh, Class<? extends Handler> uh)
    {
        final Logger root = Logger.getLogger("");
        final Level level = verboseLevel(verbosity);

        vh.setLevel(level);
        root.setLevel(getLevelProperty(".level", level));
        
        root.addHandler(vh);
        Handler[] rh = root.getHandlers();
        for(int i = 0; i < rh.length;i++)
        {           
            if(uh.equals(rh[i].getClass()))
            {
                root.removeHandler(rh[i]);
                break;
            }
        }
     }
    static Level getLevelProperty(String name, Level defaultValue)
    {
	String val = LogManager.getLogManager().getProperty(name);
	if (val == null)
        {
	    return defaultValue;
	}
	try
        {
	    return Level.parse(val.trim());
	} 
        catch (Exception ex)
        {
	    return defaultValue;
	}
    }
    
    public void setLevelColor(Level level, AnsiPrintStream.Color fg, AnsiPrintStream.Color bg)
    {
        lock.lock();
        try
        {
            this.levelColors.put(level, new Colors(fg, bg));
        }
        finally
        {
            lock.unlock();
        }
    }
    

/////////////////////////////////////////
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
        protected Object[][] getContents()
        {
            return RB;
        }
    };

    public static ResourceBundle getResourceBundle()
    {
        return rb;
    }

    
}

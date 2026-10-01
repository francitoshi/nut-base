/*
 *  FileWatcherTest.java
 *
 *  Copyright (c) 2017-2024 francitoshi@gmail.com
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
package io.nut.base.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class FileWatcherTest
{
    /**
     * Test of register method, of class FileWatcher.
     */
    @Test
    public void testSomething() throws Exception
    {
        final HashMap<String,FileWatcher.EventKind> map = new HashMap<>();
        File dir = new File(File.createTempFile("test", "FileWatcher")+".tmp");
        dir.mkdirs();
        
        final Object lock = new Object();
        
        FileWatcher instance = new FileWatcher();
        instance.register(dir, new FileWatcher.Listener()
        {
            @Override
            public void onEvent(File file, FileWatcher.EventKind ek, int count)
            {
                synchronized(lock)
                {
                    System.out.println("----------");
                    System.out.println("file="+file);
                    System.out.println("kind="+ek);
                    System.out.println("count="+count);
                    map.put(file.getAbsolutePath(), ek);
                    lock.notifyAll();
                }
            }
        }, FileWatcher.EventKind.Create, FileWatcher.EventKind.Modify, FileWatcher.EventKind.Delete);
        instance.start();
        
        synchronized(lock)
        {
            File txt = new File(dir,"test.txt");
        
            txt.createNewFile();
            lock.wait();
            assertEquals(FileWatcher.EventKind.Create, map.get(txt.getAbsolutePath()));

            OutputStream os = new FileOutputStream(txt);
            os.write("hello world".getBytes());
            os.flush();
            os.close();
            lock.wait();
            assertEquals(FileWatcher.EventKind.Modify, map.get(txt.getAbsolutePath()));
            
            txt.delete();
            lock.wait();
            assertEquals(FileWatcher.EventKind.Delete, map.get(txt.getAbsolutePath()));
            
        }
    }
    /**
     * Test of register method, of class FileWatcher.
     */
    @Test
    public void testSomething2() throws Exception
    {
        final HashMap<String,FileWatcher.EventKind> map = new HashMap<>();
        File txt = File.createTempFile("test", "FileWatcher");
        
        final Object lock = new Object();
        
        FileWatcher instance = new FileWatcher();
        instance.register(txt, new FileWatcher.Listener()
        {
            @Override
            public void onEvent(File file, FileWatcher.EventKind ek, int count)
            {
                synchronized(lock)
                {
                    System.out.println("----------");
                    System.out.println("file="+file);
                    System.out.println("kind="+ek);
                    System.out.println("count="+count);
                    map.put(file.getAbsolutePath(), ek);
                    lock.notifyAll();
                }
            }
        }, FileWatcher.EventKind.Create, FileWatcher.EventKind.Modify, FileWatcher.EventKind.Delete);
        instance.start();
        
        synchronized(lock)
        {
            OutputStream os = new FileOutputStream(txt);
            os.write("hello world".getBytes());
            os.flush();
            os.close();
            lock.wait();
            assertEquals(FileWatcher.EventKind.Modify, map.get(txt.getAbsolutePath()));
            
            txt.delete();
            lock.wait();
            assertEquals(FileWatcher.EventKind.Delete, map.get(txt.getAbsolutePath()));
            
        }
    }

}

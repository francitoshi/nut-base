/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

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

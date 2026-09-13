/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import io.nut.base.util.Exceptions;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class FileWatcher
{
    private static final String TAG = FileWatcher.class.getSimpleName();
    private static final Logger LOG = Logger.getLogger(FileWatcher.class.getName());
    
    public enum EventKind
    {
        Create, Modify, Delete
    }
    public interface Listener
    {
        void onEvent(File file, EventKind ek, int count);
    }    
    private class FileListener implements Listener
    {
        private final File file;
        private final Listener listener;
        public FileListener(File file, Listener listener)
        {
            this.file = file;
            this.listener = listener;
        }
        @Override
        public void onEvent(File file, EventKind ek, int count)
        {
            if(this.file.equals(file))
            {
                this.listener.onEvent(file, ek, count);
            }
        }
    }
    private final boolean daemon;
    private final WatchService watchService;
    private final HashMap<WatchKey,Listener> listeners = new HashMap<>();
    private final HashMap<WatchKey,File> roots = new HashMap<>();
    private volatile boolean closed = false;
    private volatile Exception exception;
    
    public FileWatcher(boolean daemon) throws IOException
    {
        this.daemon = daemon;
        this.watchService = FileSystems.getDefault().newWatchService();
    }
    public FileWatcher() throws IOException
    {
        this(false);
    }
    
    public WatchKey register(File watch, Listener listener, EventKind... kinds) throws IOException, InterruptedException
    {
        ArrayList<WatchEvent.Kind<Path>> events = new ArrayList<>();
        for(EventKind item : kinds)
        {
            switch(item)
            {
                case Create: 
                    events.add(StandardWatchEventKinds.ENTRY_CREATE); 
                    break;
                case Modify: 
                    events.add(StandardWatchEventKinds.ENTRY_MODIFY); 
                    break;
                case Delete: 
                    events.add(StandardWatchEventKinds.ENTRY_DELETE); 
                    break;
            }
        }
        WatchEvent.Kind<Path>[] evt = events.toArray(new WatchEvent.Kind[0]);
        
        boolean dir = watch.isDirectory();
        File root = dir ? watch : watch.getParentFile();
        Path path = FileSystems.getDefault().getPath(root.getAbsolutePath());

        WatchKey wk = path.register(this.watchService, evt);
        
        this.listeners.put(wk, dir ? listener : new FileListener(watch, listener));
        this.roots.put(wk, root);
        return wk;
    }
    private final Runnable loop = new Runnable()
    {
        @Override
        public void run()
        {
            try 
            {
                while(closed==false)
                {
                    final WatchKey wk = watchService.take();
                    Listener listener = listeners.get(wk);
                    File root = roots.get(wk);
                    for (WatchEvent<?> event : wk.pollEvents()) 
                    {
                        File file = new File(root,event.context().toString());
                        if(StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind()))
                        {
                            listener.onEvent(file, EventKind.Create, event.count());
                        }
                        else if(StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind()))
                        {
                            listener.onEvent(file, EventKind.Modify, event.count());
                        }
                        else if(StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind()))
                        {
                            listener.onEvent(file, EventKind.Delete, event.count());
                        }
                    }
                    wk.reset();
                }
            }
            catch(Exception ex) 
            {
                Exceptions.severe(LOG, ex);
                closed = true;
                exception = ex;
            }
        }
    };
    public void start()
    {
        Thread thread = new Thread(loop, TAG);
        thread.setDaemon(daemon);
        thread.start();
    }
    public void close() throws IOException
    {
        this.watchService.close();
    }

    public Exception getException()
    {
        return exception;
    }

}


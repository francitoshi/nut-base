/*
 *  Piper.java
 *
 *  Copyright (c) 2017-2023 francitoshi@gmail.com
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Piper
{
    private static class Worker implements Runnable
    {
        final InputStream is;
        final OutputStream os;
        final String name;
        final AtomicInteger count;

        public Worker(InputStream is, OutputStream os, String name, AtomicInteger count)
        {
            this.is = is;
            this.os = os;
            this.name = name;
            this.count = count;
        }
        @Override
        public void run()
        {
            if(count!=null) count.incrementAndGet();
            try( InputStream in = this.is; OutputStream out = this.os)
            {
                int r;
                byte[] buf = new byte[1024];                
                while((r=in.read(buf))>=0)
                {
                    out.write(buf,0,r);
                }
            }
            catch (IOException ex)
            {
                Logger.getLogger(Piper.class.getName()).log(Level.SEVERE, name, ex);
            }
            finally
            {
                if(count!=null) count.decrementAndGet();
            }
        }

    }
    
    private static final Logger logger = Logger.getLogger(Piper.class.getName());
    
    private final Worker clientServer;
    private final Worker serverClient;
    private final boolean daemon;

    public Piper(Socket client, Socket server, boolean daemon) throws IOException
    {
        this(client.getInputStream(), client.getOutputStream(),server.getInputStream(), server.getOutputStream(), daemon, null);
    }
    public Piper(InputStream clientInputStream, OutputStream clientOutputStream, InputStream serverInputStream, OutputStream serverOutputStream, boolean daemon)
    {
        this(clientInputStream, clientOutputStream, serverInputStream, serverOutputStream, daemon, null);
    }
    public Piper(InputStream clientInputStream, OutputStream clientOutputStream, InputStream serverInputStream, OutputStream serverOutputStream, boolean daemon, AtomicInteger count)
    {
        this.clientServer = new Worker(clientInputStream, serverOutputStream, PIPER_CLIENT_SERVER, count);
        this.serverClient = new Worker(serverInputStream, clientOutputStream, PIPER_SERVER_CLIENT, count);
        this.daemon = daemon;
    }
    public void start()
    {
        logger.log(Level.CONFIG,"start(server={0}, client={1})",new Object[]{PIPER_CLIENT_SERVER,PIPER_SERVER_CLIENT});
        Thread client = new Thread(clientServer, PIPER_CLIENT_SERVER);
        Thread server = new Thread(serverClient, PIPER_SERVER_CLIENT);
        client.setDaemon(daemon);
        server.setDaemon(daemon);
        server.start();
        client.start();
    }
    private static final String PIPER_SERVER_CLIENT = "piper.server.client";
    private static final String PIPER_CLIENT_SERVER = "piper.client.server";
}

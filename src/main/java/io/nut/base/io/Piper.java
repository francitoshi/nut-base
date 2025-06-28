/*
 *  Piper.java
 *
 *  Copyright (C) 2024-2025 francitoshi@gmail.com
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

import io.nut.base.util.BytesFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a bidirectional data pipe between two endpoints, often a client
 * and a server.
 * <p>
 * This class creates two dedicated threads to continuously transfer data in
 * both directions: one from a "client" {@link InputStream} to a "server"
 * {@link OutputStream}, and another from the "server" {@link InputStream} to
 * the "client" {@link OutputStream}.
 * <p>
 * It is useful for creating simple network proxies or forwarding connections.
 */
public class Piper
{
    /**
     * A private inner class that performs the actual data transfer in one
     * direction. Each {@code Worker} runs in its own thread, reading from an
     * {@link InputStream} and writing to an {@link OutputStream} until the
     * input stream is closed or an error occurs.
     */
    private static class Worker implements Runnable
    {
        private final InputStream is;
        private final OutputStream os;
        private final String name;
        private final AtomicInteger count;
        private final BytesFilter filter;
        private final Object statusLock;
        private volatile int status = 0;

        /**
         * Constructs a new Worker.
         *
         * @param is The stream to read from.
         * @param os The stream to write to.
         * @param name A descriptive name for this worker's thread, used for logging.
         * @param count An optional counter for tracking the number of active workers. Can be null.
         * @param filter An optional filter to transform bytes. If null, data is passed through unchanged.
         */
        public Worker(InputStream is, OutputStream os, String name, AtomicInteger count, BytesFilter filter, Object statusLock)
        {
            this.is = is;
            this.os = os;
            this.name = name;
            this.count = count;
            this.filter = filter;
            this.statusLock = statusLock;
        }

        /**
         * The main execution loop for the worker.
         * <p>
         * It continuously reads from the input stream and writes to the output
         * stream in a blocking loop. The loop terminates when the end of the
         * input stream is reached ({@code read} returns -1) or an
         * {@link IOException} occurs. Any exceptions are logged.
         */
        @Override
        public void run()
        {
            if (count != null)
            {
                count.incrementAndGet();
            }
            synchronized(statusLock)
            {
                status=1;
                statusLock.notifyAll();
            }
            try( InputStream in = this.is; OutputStream out = this.os)
            {
                int r;
                byte[] buf = new byte[4096];                
                while((r=in.read(buf))>=0)
                {
                    if (filter == null) 
                    {
                        out.write(buf,0,r);
                    }                    
                    else 
                    {
                        // Create a correctly-sized segment from the buffer
                        byte[] segment = Arrays.copyOf(buf, r);
                        // Apply the filter
                        byte[] filteredSegment = filter.filter(segment);
                        // Write the potentially modified data
                        out.write(filteredSegment, 0, filteredSegment.length);
                    } 
                    out.flush();
                }
            }
            catch (IOException ex)
            {
                // Log exceptions, which are expected when a connection is closed by either party.
                // Using a finer log level as this is often not a SEVERE application error.
                Logger.getLogger(Piper.class.getName()).log(Level.FINER, "Piper worker '" + name + "' stopped due to IOException: " + ex.getMessage());
            }
            finally
            {
                if(count!=null) 
                {
                    count.decrementAndGet();
                }
                synchronized(statusLock)
                {
                    status=2;
                    statusLock.notifyAll();
                }
            }
        }

        public int getStatus()
        {
            return status;
        }
    }
    
    private static final Logger logger = Logger.getLogger(Piper.class.getName());
    
    /**
     * Constant string used for naming and logging the client-to-server worker
     * thread.
     */
    private static final String PIPER_CLIENT_SERVER = "piper.client-to-server";
    /**
     * Constant string used for naming and logging the server-to-client worker
     * thread.
     */
    private static final String PIPER_SERVER_CLIENT = "piper.server-to-client";

    private static final int STATUS_STOPED = 4;

    private final Worker clientServer;
    private final Worker serverClient;
    private final boolean daemon;
    private final Object statusLock = new Object();

    /**
     * Creates a Piper for two connected {@link Socket}s.
     *
     * @param client The client socket.
     * @param server The server socket.
     * @param daemon If true, the pipe threads will be set as daemon threads.
     * @throws IOException if an I/O error occurs when getting the streams from
     * the sockets.
     */
    public Piper(Socket client, Socket server, boolean daemon) throws IOException
    {
        this(client.getInputStream(), client.getOutputStream(),server.getInputStream(), server.getOutputStream(), daemon, null, null, null);
    }

    /**
     * Creates a Piper from client and server stream pairs.
     *
     * @param clientInputStream The stream to read data from the client.
     * @param clientOutputStream The stream to write data to the client.
     * @param serverInputStream The stream to read data from the server.
     * @param serverOutputStream The stream to write data to the server.
     * @param daemon If true, the pipe threads will be set as daemon threads.
     */
    public Piper(InputStream clientInputStream, OutputStream clientOutputStream, InputStream serverInputStream, OutputStream serverOutputStream, boolean daemon)
    {
        this(clientInputStream, clientOutputStream, serverInputStream, serverOutputStream, daemon, null, null, null);
    }

    /**
     * Creates a Piper from client and server stream pairs with an optional
     * active connection counter. This is the designated constructor.
     *
     * @param clientInputStream The stream to read data from the client.
     * @param clientOutputStream The stream to write data to the client.
     * @param serverInputStream The stream to read data from the server.
     * @param serverOutputStream The stream to write data to the server.
     * @param daemon If true, the pipe threads will be set as daemon threads.
     * @param count An optional atomic counter to track the number of active
     * pipes. Can be null.
     */
    public Piper(InputStream clientInputStream, OutputStream clientOutputStream, InputStream serverInputStream, OutputStream serverOutputStream, boolean daemon, AtomicInteger count)
    {
        this.clientServer = new Worker(clientInputStream, serverOutputStream, PIPER_CLIENT_SERVER, count, null, statusLock);
        this.serverClient = new Worker(serverInputStream, clientOutputStream, PIPER_SERVER_CLIENT, count, null, statusLock);
        this.daemon = daemon;
    }
    /**
     * Creates a Piper from client and server stream pairs with an optional
     * active connection counter. This is the designated constructor.
     *
     * @param clientInputStream The stream to read data from the client.
     * @param clientOutputStream The stream to write data to the client.
     * @param serverInputStream The stream to read data from the server.
     * @param serverOutputStream The stream to write data to the server.
     * @param daemon If true, the pipe threads will be set as daemon threads.
     * @param count An optional atomic counter to track the number of active
     * pipes. Can be null.
     */
    public Piper(InputStream clientInputStream, OutputStream clientOutputStream, InputStream serverInputStream, OutputStream serverOutputStream, boolean daemon, AtomicInteger count, BytesFilter clientToServerFilter, BytesFilter serverToClientFilter)
    {
        this.clientServer = new Worker(clientInputStream, serverOutputStream, PIPER_CLIENT_SERVER, count, clientToServerFilter, statusLock);
        this.serverClient = new Worker(serverInputStream, clientOutputStream, PIPER_SERVER_CLIENT, count, serverToClientFilter, statusLock);
        this.daemon = daemon;
    }

    /**
     * Starts the bidirectional data transfer.
     * <p>
     * This method creates, configures (with daemon status), and starts two
     * threads to handle the data flow in both directions. The threads will run
     * until the underlying connections are closed.
     */
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
    
    public void join()
    {
        try
        {
            synchronized(statusLock)
            {
                while(this.clientServer.getStatus() + this.serverClient.getStatus()<STATUS_STOPED)
                {
                    statusLock.wait(30_000);
                }
            }
        }
        catch (InterruptedException ex)
        {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
}

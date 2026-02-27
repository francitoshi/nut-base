/*
 *  DuplexLayer.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.signal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reliable full-duplex framing layer built on top of an unreliable
 * {@link Transceiver}.
 *
 * <p>
 * Outgoing frames are retransmitted automatically until an ACK is received or
 * the maximum number of attempts ({@link #MAX_RETRIES}) is exhausted. The
 * retransmission back-off is linear: the {@code n}-th attempt waits
 * {@code n * }{@link #ACK_TIMEOUT_MILLIS} ms before the next send.
 *
 * <p>
 * Incoming frames are dispatched to a {@link FrameListener} by type:
 * <ul>
 * <li>DATA – payload delivered to {@link FrameListener#onReceived}; an ACK is
 * sent back automatically.</li>
 * <li>ACK – marks the matching outgoing frame as delivered and notifies
 * {@link FrameListener#onDelivered}.</li>
 * <li>NACK – marks the matching outgoing frame as failed and notifies
 * {@link FrameListener#onFailed}.</li>
 * </ul>
 *
 * <p>
 * <b>Threading:</b> {@code open()} starts two daemon-style threads
 * ({@code outgoingTask} and {@code incomingTask}). Both threads are stopped by
 * calling {@link #close()}.
 *
 * <p>
 * <b>Typical usage:</b>
 * <pre>{@code
 * DuplexLayer layer = new DuplexLayer()
 *         .setTransceiver(myTransceiver)
 *         .setListener(myListener)
 *         .open();
 *
 * short id = layer.send(payload);   // non-blocking; result notified via listener
 * ...
 * layer.close();
 * }</pre>
 */
public class DuplexLayer
{

    // ── Frame type constants ─────────────────────────────────────────────────
    private static final byte TYPE_DATA = Frame.ID_DATA;
    private static final byte TYPE_ACK  = Frame.ID_ACK;
    private static final byte TYPE_NACK = Frame.ID_NACK;

    // ── Protocol parameters (tunable) ───────────────────────────────────────
    /**
     * Maximum number of send attempts for a single frame (1 initial send +
     * {@code MAX_RETRIES - 1} retransmissions). When this limit is reached
     * without receiving an ACK, {@link FrameListener#onFailed} is invoked with
     * status code {@code -1}.
     */
    public static final int MAX_RETRIES = 5;

    /**
     * Base ACK timeout in milliseconds. The actual wait before the {@code n}-th
     * retransmission is {@code n * ACK_TIMEOUT_MILLIS}, producing a linear
     * back-off (1 s, 2 s, 3 s, …).
     */
    public static final long ACK_TIMEOUT_MILLIS = 1_000;

    /**
     * {@link #ACK_TIMEOUT_MILLIS} converted to nanoseconds for use with
     * {@link System#nanoTime()}-based scheduling.
     */
    public static final long ACK_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(ACK_TIMEOUT_MILLIS);

    /**
     * Internal wrapper that associates a raw frame with its retransmission
     * state and makes it schedulable via {@link DelayQueue}.
     */
    private static class ExtendedFrame implements Delayed
    {

        /**
         * Frame identifier, extracted from the frame header at construction
         * time.
         */
        final short id;
        /**
         * Raw frame bytes, including header and payload.
         */
        final byte[] frame;
        /**
         * Absolute {@link System#nanoTime()} timestamp of the next send
         * attempt.
         */
        volatile long nextNanos;
        /**
         * Number of times this frame has been handed to the transceiver.
         */
        volatile int triesCount;
        /**
         * Set to {@code true} once an ACK or NACK has been received, or after
         * {@link #MAX_RETRIES} attempts. Prevents duplicate delivery
         * notifications and stops further retransmissions.
         */
        volatile boolean acked;

        public ExtendedFrame(short id, byte[] frame)
        {
            this.id = id;
            this.frame = frame;
        }

        /**
         * {@inheritDoc}
         *
         * @return remaining delay until the next send attempt, in the requested
         * unit; never negative (clamped to 0).
         */
        @Override
        public long getDelay(TimeUnit unit)
        {
            return Math.max(0, TimeUnit.NANOSECONDS.convert(nextNanos - System.nanoTime(), unit));
        }

        /**
         * Orders frames by their scheduled send time, then by attempt count,
         * and finally by frame id as a tiebreaker.
         */
        @Override
        public int compareTo(Delayed other)
        {
            int cmp = (other instanceof ExtendedFrame) ? Long.compare(this.nextNanos, ((ExtendedFrame)other).nextNanos) : 0;
            if(cmp==0)
            {
                cmp = Integer.compare(this.triesCount, ((ExtendedFrame)other).triesCount);
                if(cmp==0)
                {
                    cmp = Integer.compare(this.id, ((ExtendedFrame)other).id);
                }
            }
            return cmp;
        }
    }
    
    // ── Listener interface ───────────────────────────────────────────────────
    /**
     * Callback interface for asynchronous frame delivery events.
     *
     * <p>
     * All methods are invoked from the internal {@code incomingTask} thread.
     * Implementations must be thread-safe and should avoid blocking to prevent
     * stalling the receive loop.
     */
    public interface FrameListener
    {

        /**
         * Called when a frame has been successfully delivered, i.e. an ACK has
         * been received from the remote side.
         *
         * @param id identifier of the delivered frame, as returned by
         * {@link DuplexLayer#send(byte[])}
         */
        void onDelivered(short id);

        /**
         * Called when frame delivery has permanently failed, either because a
         * NACK was received or because {@link #MAX_RETRIES} attempts elapsed
         * without an ACK.
         *
         * @param id identifier of the failed frame
         * @param statusCode NACK status code provided by the remote side, or
         * {@code -1} if the failure was caused by a timeout
         */
        void onFailed(short id, int statusCode);

        /**
         * Called when a DATA frame has been received from the remote side. An
         * ACK is sent automatically before this method is invoked.
         *
         * @param id identifier of the received frame
         * @param payload frame payload bytes (not including the frame header)
         */
        void onReceived(short id, byte[] payload);
    }

    // ── Fields ───────────────────────────────────────────────────────────────
    /**
     * Monotonically increasing source for outgoing frame identifiers.
     */
    private final AtomicInteger idCounter = new AtomicInteger(0);

    /**
     * Set to {@code true} by {@link #close()} to signal both worker threads to
     * stop.
     */
    private volatile boolean terminated = false;
    
    /**
     * Underlying transport used for raw frame I/O.
     */
    private volatile Transceiver transceiver;

    /**
     * Application-level listener notified of delivery and reception events.
     */
    private volatile FrameListener listener;

    // ── Constructor ──────────────────────────────────────────────────────────
    public DuplexLayer()
    {
    }

    // ── Configuration ────────────────────────────────────────────────────────
    /**
     * Sets the {@link Transceiver} used for raw frame I/O. Must be called
     * before {@link #open()}.
     *
     * @param transceiver the transceiver to use; must not be {@code null}
     * @return {@code this} instance for fluent chaining
     */
    public DuplexLayer setTransceiver(Transceiver transceiver)
    {
        this.transceiver = transceiver;
        return this;
    }

    /**
     * Sets the {@link FrameListener} that will receive delivery and reception
     * events. Can be changed at any time, even after {@link #open()}.
     *
     * @param listener the listener to notify; {@code null} disables callbacks
     * @return {@code this} instance for fluent chaining
     */
    public DuplexLayer setListener(FrameListener listener)
    {
        this.listener = listener;
        return this;
    }
    
    // ── Internal state ───────────────────────────────────────────────────────
    private final DelayQueue<ExtendedFrame> outgoingQueue = new DelayQueue<>();
    private final ConcurrentHashMap<Short, ExtendedFrame> outgoingMap = new ConcurrentHashMap<>();

    /**
     * Outgoing worker: dequeues frames whose retransmission deadline has
     * elapsed, writes them to the transceiver, and re-enqueues them with a
     * longer delay until an ACK/NACK is received or {@link #MAX_RETRIES} is
     * exhausted.
     */
    private final Runnable outgoinTask = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                for(ExtendedFrame item = outgoingQueue.take(); !terminated; item = outgoingQueue.take())
                {
                    if(item.acked)
                    {
                        continue;
                    }
                    transceiver.write(item.frame);
                    item.triesCount++;
                    item.nextNanos = System.nanoTime() + item.triesCount*ACK_TIMEOUT_NANOS;
                    outgoingQueue.put(item);
                }
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(DuplexLayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };
    
    /**
     * Incoming worker: reads frames from the transceiver in a tight loop and
     * dispatches them to the appropriate handler by frame type.
     */
    private final Runnable incomingTask = new Runnable()
    {
        @Override
        public void run()
        {
            for(byte[] frame = transceiver.read(); !terminated; frame = transceiver.read())
            {
                byte type = Frame.getType(frame);
                switch (type)
                {
                    case TYPE_DATA:
                        handleData(frame);
                        break;
                    case TYPE_ACK:
                        handleAck(frame);
                        break;
                    case TYPE_NACK:
                        handleNack(frame);
                        break;
                    default:
                        throw new AssertionError("Unknown frame type: " + type);
                }
            }
        }
    };
    
    // ── Lifecycle ────────────────────────────────────────────────────────────
    /**
     * Starts the outgoing and incoming worker threads. Must be called once
     * after configuring the transceiver and listener.
     *
     * @return {@code this} instance for fluent chaining
     */
    public DuplexLayer open()
    {
        new Thread(outgoinTask, "outgoinTask").start();
        new Thread(incomingTask, "incomingTask").start();
        return this;
    }

    /**
     * Signals both worker threads to stop after their current operation.
     * Already-queued frames will not be retransmitted after this call. This
     * method returns immediately without waiting for threads to terminate.
     */
    public void close()
    {
        this.terminated = true;
    }
    
    // ── Public API ───────────────────────────────────────────────────────────
    /**
     * Encodes {@code payload} into a DATA frame, assigns it a unique id, and
     * enqueues it for delivery. The frame will be retransmitted up to
     * {@link #MAX_RETRIES} times if no ACK is received within the timeout
     * window. The result of the delivery attempt is reported asynchronously via
     * {@link FrameListener#onDelivered} or {@link FrameListener#onFailed}.
     *
     * @param payload raw payload bytes to send; must not be {@code null}
     * @return the frame identifier assigned to this transmission, which can be
     * used to correlate subsequent listener callbacks
     */
    public short send(byte[] payload)
    {
        short id = (short) this.idCounter.getAndIncrement();
        write(Frame.createData(id, payload));
        return id;
    }

    /**
     * Enqueues a pre-built frame for transmission. The frame id is read
     * directly from the frame header via {@link Frame#getId(byte[])}. Prefer
     * {@link #send(byte[])} for outgoing DATA frames; this method is intended
     * for internal use (e.g. sending ACK frames).
     *
     * @param frameData complete frame bytes, including header; must not be
     * {@code null}
     */
    public void write(byte[] frameData)
    {
        ExtendedFrame ef = new ExtendedFrame(Frame.getId(frameData), frameData);
        outgoingMap.put(ef.id, ef);
        outgoingQueue.put(ef);
    }

    // ── Internal handlers ────────────────────────────────────────────────────
    /**
     * Handles an incoming DATA frame: notifies the listener and sends an ACK.
     *
     * @param frame raw frame bytes
     */
    private void handleData(byte[] frame)
    {
        short id = Frame.getId(frame);
        byte[] payload = Frame.getPayload(frame);
        listener.onReceived(id, payload);
        write(Frame.createAck(id));
    }
    
    /**
     * Handles an incoming ACK frame: marks the matching outgoing frame as
     * delivered and notifies {@link FrameListener#onDelivered}. Duplicate ACKs
     * (frame already marked as acked) are silently discarded after cleaning up
     * the outgoing map entry.
     *
     * @param frame raw frame bytes
     */
    private void handleAck(byte[] frame)
    {
        short id = Frame.getId(frame);
        ExtendedFrame ef = outgoingMap.getOrDefault(id, null);
        if(ef!=null)
        {
            if(ef.acked)
            {
                outgoingMap.remove(ef.id);
                return;
            }
            ef.acked = true;
            if (listener != null)
            {
                listener.onDelivered(id);
            }
        }
    }

    /**
     * Handles an incoming NACK frame: marks the matching outgoing frame as
     * failed and notifies {@link FrameListener#onFailed} with the NACK status.
     * Duplicate NACKs (frame already marked as acked) are silently discarded
     * after cleaning up the outgoing map entry.
     *
     * @param frame raw frame bytes
     */
    private void handleNack(byte[] frame)
    {
        short id = Frame.getId(frame);
        short status = Frame.getStatus(frame);
        ExtendedFrame ef = outgoingMap.getOrDefault(id, null);
        if(ef!=null)
        {
            if(ef.acked)
            {
                outgoingMap.remove(ef.id);
            }
            else if (listener != null)
            {
                ef.acked = true;
                listener.onFailed(id, status);
            }
        }
    }
}

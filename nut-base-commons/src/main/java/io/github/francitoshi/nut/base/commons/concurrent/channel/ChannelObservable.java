/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

/**
 * Source of notifications about successful operations on a channel.
 * <p>
 * A {@code select} registers a listener on every channel it observes and
 * rechecks its channels whenever one of them reports activity. Implementing
 * this interface on {@link ChannelReader} and {@link ChannelWriter} allows a
 * select to accept either interface without knowing the concrete channel type.
 *
 * @param <E> the element type of the channel
 */
public interface ChannelObservable<E>
{
    /**
     * Registers a listener notified on channel activity: every successful get
     * or put, and every rendezvous operation that starts waiting.
     *
     * @param listener the listener to register
     */
    void addOperationListener(OperationListener<E> listener);

    /**
     * Removes a previously registered operation listener.
     *
     * @param listener the listener to remove
     */
    void removeOperationListener(OperationListener<E> listener);

    /**
     * Kind of operation that changed the state of a channel.
     */
    enum Operation
    {
        /** A value was taken from the channel. */
        GET,
        /** A value was put into the channel. */
        PUT,
        /** The channel was closed. */
        CLOSE
    }

    /**
     * Listener notified after a value is taken from or put into a channel, or
     * after it is closed.
     */
    @FunctionalInterface
    interface OperationListener<E>
    {
        /**
         * Called when a get, a put or a close changes the channel's observable
         * state, either by completing or, in a rendezvous channel, by starting
         * to wait.
         *
         * @param channel the channel that changed
         * @param operation the operation that changed it
         * @param value the value that was received or sent, or {@code null}
         *        when a pending get is being announced or the channel was
         *        closed
         */
        void onOperation(ChannelObservable<E> channel, Operation operation, E value);
    }
}

/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

/**
 * Outcome of a {@link Select} operation.
 *
 * <p>{@link #value()} is only non-null when {@link #status()} is
 * {@link Status#VALUE}; in that case {@link #index()} is the position, in the
 * array passed to the select, of the channel that produced the value. In all
 * other cases the index is {@code -1}.
 *
 * @param <T> the element type
 */
public final class SelectResult<T>
{
    public enum Status
    {
        /** A value was read from one of the channels. */
        VALUE,
        /** Every channel is closed and drained. */
        CLOSED,
        /** The timeout elapsed with no channel ready. */
        TIMEOUT,
        /** Non-blocking select ({@code trySelect}) found no channel ready. */
        NOT_READY,
        /** The waiting thread was interrupted. */
        INTERRUPTED
    }

    private static final SelectResult<?> CLOSED = new SelectResult<>(Status.CLOSED, -1, null);
    private static final SelectResult<?> TIMEOUT = new SelectResult<>(Status.TIMEOUT, -1, null);
    private static final SelectResult<?> NOT_READY = new SelectResult<>(Status.NOT_READY, -1, null);
    private static final SelectResult<?> INTERRUPTED = new SelectResult<>(Status.INTERRUPTED, -1, null);

    private final Status status;
    private final int index;
    private final T value;

    private SelectResult(Status status, int index, T value)
    {
        this.status = status;
        this.index = index;
        this.value = value;
    }

    static <T> SelectResult<T> value(int index, T value)
    {
        return new SelectResult<>(Status.VALUE, index, value);
    }

    @SuppressWarnings("unchecked")
    static <T> SelectResult<T> closed()
    {
        return (SelectResult<T>) CLOSED;
    }

    @SuppressWarnings("unchecked")
    static <T> SelectResult<T> timeout()
    {
        return (SelectResult<T>) TIMEOUT;
    }

    @SuppressWarnings("unchecked")
    static <T> SelectResult<T> notReady()
    {
        return (SelectResult<T>) NOT_READY;
    }

    @SuppressWarnings("unchecked")
    static <T> SelectResult<T> interrupted()
    {
        return (SelectResult<T>) INTERRUPTED;
    }

    public Status status()
    {
        return status;
    }

    /** @return the index of the channel that produced the value, or {@code -1} */
    public int index()
    {
        return index;
    }

    /** @return the value read, or {@code null} if {@link #status()} is not {@link Status#VALUE} */
    public T value()
    {
        return value;
    }

    public boolean hasValue()
    {
        return status == Status.VALUE;
    }

    @Override
    public String toString()
    {
        return status == Status.VALUE
                ? "SelectResult[VALUE, index=" + index + ", value=" + value + "]"
                : "SelectResult[" + status + "]";
    }
}

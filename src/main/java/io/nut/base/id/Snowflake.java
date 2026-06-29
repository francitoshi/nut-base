package io.nut.base.id;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class implementing a Snowflake-style distributed ID generator.
 *
 * ID layout (64 bits):
 *   [sign bit: 1] [timestamp: 41] [machineId: 10] [sequence: 12]
 *
 * Subclasses define the time unit (milliseconds, seconds, etc.).
 */
public abstract class Snowflake {

    // Bit lengths
    static final int MACHINE_ID_BITS = 10;
    static final int SEQUENCE_BITS   = 12;

    // Derived masks and shifts
    static final long MAX_MACHINE_ID  = (1L << MACHINE_ID_BITS) - 1; // 1023
    static final long MAX_SEQUENCE    = (1L << SEQUENCE_BITS) - 1;    // 4095
    static final int  MACHINE_SHIFT   = SEQUENCE_BITS;                // 12
    static final int  TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS; // 22

    private final long machineId;
    private final long epoch;

    /**
     * Packed state: upper 41 bits = timestamp offset, lower 12 bits = sequence.
     * Using AtomicLong avoids locks while keeping timestamp+sequence consistent
     * for compare-and-set updates.
     */
    private final AtomicLong state = new AtomicLong(0L);
    private final String unitLabel;
    /**
     * @param machineId unique machine/node identifier (0..1023)
     * @param epoch     custom epoch in the same unit as {@link #currentTime()}
     */
    protected Snowflake(long machineId, long epoch, String unitLabel) 
    {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) 
        {
            throw new IllegalArgumentException("machineId must be in range [0, " + MAX_MACHINE_ID + "]");
        }
        this.machineId = machineId;
        this.epoch     = epoch;
        this.unitLabel = unitLabel;
    }

    /** Returns the current time in the subclass's chosen unit. */
    protected abstract long currentTime();

    /** Returns a human-readable label for the time unit (used in error messages). */
    public final String timeUnitLabel()
    {
        return this.unitLabel;
    }

    /**
     * Generates a unique 64-bit ID. Thread-safe and contention-free via CAS loop.
     *
     * @throws IllegalStateException if the clock moves backwards
     */
    public long nextId() 
    {
        while (true) 
        {
            long current = state.get();
            long lastTimestamp = current >>> SEQUENCE_BITS;
            long lastSequence  = current & MAX_SEQUENCE;

            long now = currentTime() - epoch;

            if (now < lastTimestamp) 
            {
                throw new IllegalStateException("Clock moved backwards. Refusing to generate ID for "+ (lastTimestamp - now) + " " + timeUnitLabel());
            }

            long newSequence;
            if (now == lastTimestamp) 
            {
                newSequence = (lastSequence + 1) & MAX_SEQUENCE;
                if (newSequence == 0) 
                {
                    // Sequence exhausted for this tick — spin until the next tick
                    now = waitForNextTick(lastTimestamp);
                }
            } 
            else 
            {
                newSequence = 0;
            }

            long newState = (now << SEQUENCE_BITS) | newSequence;
            if (state.compareAndSet(current, newState)) 
            {
                return (now << TIMESTAMP_SHIFT)
                     | (machineId << MACHINE_SHIFT)
                     | newSequence;
            }
            // CAS failed → another thread raced us; retry
        }
    }

    /** Spins until the time advances past {@code lastTimestamp}. */
    private long waitForNextTick(long lastTimestamp) 
    {
        long tick;
        do 
        {
            Thread.yield();
            tick = currentTime() - epoch;
        } 
        while (tick <= lastTimestamp);
        return tick;
    }

    public long getMachineId() { return machineId; }
    public long getEpoch()     { return epoch; }

    /**
     * Snowflake implementation using <b>milliseconds</b> as the time unit.
     *
     * <p>This is the original Twitter Snowflake algorithm.
     * Default epoch: 2010-11-04 01:42:54 UTC in milliseconds.
     *
     * <p>Throughput: up to 4,096 IDs per millisecond per machine.
     */
    public static final class Millis extends Snowflake 
    {
         // Twitter Epoch (2010-11-04 01:42:54 UTC)
        static final long DEFAULT_EPOCH = 1288834974657L;

        private Millis(long machineId, long epochMillis) 
        {
            super(machineId, epochMillis, "milliseconds");
        }

        /**
         * Creates a {@code MillisSnowflake} with the default epoch (2024-01-01 UTC).
         *
         * @param machineId unique node ID in range [0, 1023]
         */
        public static Millis create(long machineId) 
        {
            return new Millis(machineId, DEFAULT_EPOCH);
        }

        /**
         * Creates a {@code MillisSnowflake} with a custom epoch.
         *
         * @param machineId   unique node ID in range [0, 1023]
         * @param epochMillis custom epoch in Unix milliseconds
         */
        public static Millis create(long machineId, long epochMillis)
        {
            return new Millis(machineId, epochMillis);
        }

        @Override
        protected long currentTime() 
        {
            return System.currentTimeMillis();
        }
    }
    
    /**
     * Snowflake implementation using <b>seconds</b> as the time unit.
     *
     * <p>Trading per-tick throughput for a longer usable timestamp range:
     * with 41 bits of seconds the IDs won't overflow until ~year 2092
     * (vs ~year 2039 for a seconds-based Unix timestamp in 32 bits).
     *
     * <p>Throughput: up to 4,096 IDs per second per machine.
     *
     * <p>Default epoch: 2024-01-01 00:00:00 UTC in seconds.
     */
    public static final class Seconds extends Snowflake
    {

        /** 2024-01-01 00:00:00 UTC in seconds. */
        public static final long DEFAULT_EPOCH = 1_704_067_200L;

        private Seconds(long machineId, long epochSeconds) 
        {
            super(machineId, epochSeconds, "seconds");
        }

        /**
         * Creates a {@code SecondsSnowflake} with the default epoch (2024-01-01 UTC).
         *
         * @param machineId unique node ID in range [0, 1023]
         */
        public static Seconds create(long machineId) 
        {
            return new Seconds(machineId, DEFAULT_EPOCH);
        }

        /**
         * Creates a {@code SecondsSnowflake} with a custom epoch.
         *
         * @param machineId    unique node ID in range [0, 1023]
         * @param epochSeconds custom epoch in Unix seconds
         */
        public static Seconds create(long machineId, long epochSeconds) 
        {
            return new Seconds(machineId, epochSeconds);
        }

        @Override
        protected long currentTime() 
        {
            return System.currentTimeMillis() / 1_000L;
        }
    }
    
}

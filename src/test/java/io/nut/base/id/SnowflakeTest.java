package io.nut.base.id;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


class SnowflakeTest 
{

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static long extractTimestamp(long id) 
    {
        return id >>> Snowflake.TIMESTAMP_SHIFT;
    }

    private static long extractMachineId(long id) 
    {
        return (id >>> Snowflake.MACHINE_SHIFT) & Snowflake.MAX_MACHINE_ID;
    }

    private static long extractSequence(long id) 
    {
        return id & Snowflake.MAX_SEQUENCE;
    }

    // -----------------------------------------------------------------------
    // Construction validation
    // -----------------------------------------------------------------------

    @Test
    void create_withValidMachineId_succeeds() 
    {
        assertDoesNotThrow(() -> Snowflake.Millis.create(0));
        assertDoesNotThrow(() -> Snowflake.Millis.create(512));
        assertDoesNotThrow(() -> Snowflake.Millis.create(1023));
    }

    @Test
    void create_withInvalidMachineId_throws() 
    {
        assertThrows(IllegalArgumentException.class, () -> Snowflake.Millis.create(-1));
        assertThrows(IllegalArgumentException.class, () -> Snowflake.Millis.create(1024));
        assertThrows(IllegalArgumentException.class, () -> Snowflake.Seconds.create(-1));
        assertThrows(IllegalArgumentException.class, () -> Snowflake.Seconds.create(1024));
    }

    @Test
    void getMachineId_returnsCorrectValue() 
    {
        Snowflake.Millis sf = Snowflake.Millis.create(42);
        assertEquals(42, sf.getMachineId());
    }

    // -----------------------------------------------------------------------
    // ID structure
    // -----------------------------------------------------------------------

    @Test
    void nextId_isPositive()
    {
        Snowflake.Millis sf = Snowflake.Millis.create(1);
        assertTrue(sf.nextId() > 0, "Generated ID must be positive (sign bit must be 0)");
    }

    @Test
    void nextId_machineIdEmbeddedCorrectly()
    {
        long machineId = 777;
        Snowflake.Millis sf = Snowflake.Millis.create(machineId);
        long id = sf.nextId();
        assertEquals(machineId, extractMachineId(id));
    }

    @Test
    void nextId_sequenceStartsAtZeroForFirstCall()
    {
        Snowflake.Millis sf = Snowflake.Millis.create(1);
        long id = sf.nextId();
        assertEquals(0, extractSequence(id));
    }

    @Test
    void nextId_sequenceIncrementsWithinSameTick()
    {
        // Use a fixed-time subclass to force same tick
        Snowflake sf = fixedTimeMillis(1, 100_000L);
        long id1 = sf.nextId();
        long id2 = sf.nextId();
        assertEquals(0, extractSequence(id1));
        assertEquals(1, extractSequence(id2));
    }

    @Test
    void nextId_timestampIsEmbedded() 
    {
        Snowflake.Millis sf = Snowflake.Millis.create(1);
        long before = System.currentTimeMillis() - Snowflake.Millis.DEFAULT_EPOCH;
        long id     = sf.nextId();
        long after  = System.currentTimeMillis() - Snowflake.Millis.DEFAULT_EPOCH;
        long ts     = extractTimestamp(id);
        assertTrue(ts >= before && ts <= after, "Embedded timestamp " + ts + " must be in [" + before + ", " + after + "]");
    }

    // -----------------------------------------------------------------------
    // Monotonicity
    // -----------------------------------------------------------------------

    @Test
    void nextId_isStrictlyMonotonicallyIncreasing_singleThread()
    {
        Snowflake.Millis sf = Snowflake.Millis.create(1);
        long prev = sf.nextId();
        for (int i = 0; i < 10_000; i++)
        {
            long next = sf.nextId();
            assertTrue(next > prev, "ID must increase: prev=" + prev + " next=" + next);
            prev = next;
        }
    }

    @Test
    void nextId_secondsVariant_isStrictlyMonotonicallyIncreasing()
    {
        Snowflake.Seconds sf = Snowflake.Seconds.create(1);
        long prev = sf.nextId();
        for (int i = 0; i < 1_000; i++)
        {
            long next = sf.nextId();
            assertTrue(next > prev);
            prev = next;
        }
    }

    // -----------------------------------------------------------------------
    // Uniqueness (concurrent)
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 8, 16})
    void nextId_isUniqueAcrossThreads_millisSnowflake(int threads) throws InterruptedException 
    {
        assertUniqueness(Snowflake.Millis.create(1), threads, 5_000);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 8})
    void nextId_isUniqueAcrossThreads_secondsSnowflake(int threads) throws InterruptedException
    {
        assertUniqueness(Snowflake.Seconds.create(1), threads, 1_000);
    }

    private void assertUniqueness(Snowflake sf, int threads, int idsPerThread) throws InterruptedException
    {
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int t = 0; t < threads; t++) 
        {
            pool.submit(() -> 
            {
                ready.countDown();
                try 
                { 
                    start.await(); 
                }
                catch (InterruptedException ex) 
                { 
                    Thread.currentThread().interrupt(); 
                }
                for (int i = 0; i < idsPerThread; i++)
                {
                    ids.add(sf.nextId());
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertEquals((long) threads * idsPerThread, ids.size(), "Expected all IDs to be unique");
    }

    // -----------------------------------------------------------------------
    // Different machines produce different IDs for same timestamp+sequence
    // -----------------------------------------------------------------------

    @Test
    void nextId_differentMachines_producesDifferentIds()
    {
        Snowflake sf1 = fixedTimeMillis(1, 999L);
        Snowflake sf2 = fixedTimeMillis(2, 999L);
        assertNotEquals(sf1.nextId(), sf2.nextId());
    }

    // -----------------------------------------------------------------------
    // Clock-backwards guard
    // -----------------------------------------------------------------------

    @Test
    void nextId_throwsWhenClockMovesBackwards()
    {
        long[] time = {1_000L}; // mutable time reference
        Snowflake sf = new Snowflake(1, 0, "units")
        {
            @Override protected long currentTime() 
            { 
                return time[0]; 
            }
        };

        sf.nextId();        // generate one ID at time=1000
        time[0] = 500L;     // simulate clock going backwards
        assertThrows(IllegalStateException.class, sf::nextId);
    }

    // -----------------------------------------------------------------------
    // Factory / static creation
    // -----------------------------------------------------------------------

    @Test
    void millisSnowflake_customEpoch_isStoredCorrectly() 
    {
        long customEpoch = 1_600_000_000_000L;
        Snowflake.Millis sf = Snowflake.Millis.create(5, customEpoch);
        assertEquals(customEpoch, sf.getEpoch());
    }

    @Test
    void secondsSnowflake_customEpoch_isStoredCorrectly() 
    {
        long customEpoch = 1_600_000_000L;
        Snowflake.Seconds sf = Snowflake.Seconds.create(5, customEpoch);
        assertEquals(customEpoch, sf.getEpoch());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Returns a Snowflake whose clock always returns {@code fixedTime}. */
    private Snowflake fixedTimeMillis(long machineId, long fixedTime) 
    {
        return new Snowflake(machineId, 0, "ms")
        {
            @Override protected long currentTime() 
            { 
                return fixedTime; 
            }
            
        };
    }
}

/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplified W-TinyLFU implementation inspired by Caffeine cache. Uses a window
 * cache (LRU) for recent items and a main cache (LFU) for frequently used
 * items, admitting candidates into the main cache only when the frequency
 * sketch says they deserve it more than the current victim.
 *
 * <p>This revision folds in the pieces that separate a toy TinyLFU from the
 * real design used by Caffeine:</p>
 * <ul>
 *   <li><b>Adaptive window ("W"):</b> the window/main boundary hill-climbs
 *   based on observed hit rate, with a warm-up guard, hysteresis, and step
 *   decay so it doesn't hunt on workloads that have nothing to gain from
 *   adapting (see {@link #maybeAdapt()}).</li>
 *   <li><b>Doorkeeper:</b> a small Bloom filter gates entry into the
 *   Count-Min Sketch's counters, so one-off accesses don't spend counter
 *   budget that frequently-seen keys need (see {@link CountMinSketch}).</li>
 *   <li><b>Conservative update:</b> the sketch only increments counters that
 *   are already at the row-minimum, reducing over-estimation from hash
 *   collisions.</li>
 *   <li><b>Better hash mixing:</b> a MurmurHash3-style finalizer spreads
 *   {@code hashCode()} bits before indexing, so keys with poor natural
 *   dispersion (e.g. sequential integers) don't correlate across rows.</li>
 *   <li><b>Probabilistic tie-breaking:</b> when a candidate and the victim
 *   have the same estimated frequency, the candidate is admitted with a
 *   small probability instead of never, so the main cache doesn't freeze
 *   around stale entries once the sketch saturates.</li>
 *   <li><b>Thread-safety:</b> public operations are synchronized on the
 *   cache's own monitor. This is a single coarse-grained lock, not
 *   Caffeine's striped/asynchronous design, so it trades some throughput
 *   under contention for straightforward correctness.</li>
 * </ul>
 *
 * <p>Still simplified relative to production Caffeine: there is no
 * asynchronous maintenance queue, no read-buffer striping, and the climber
 * is a plain hill-climber rather than a PID-style controller.</p>
 */
public class TinyLFUCache<K, V> extends AbstractCache<K,V> implements Cache<K,V>
{

    private final int capacity;

    // Window/main boundary. Not fixed: resizeWindow() moves capacity
    // between the two caches at runtime as the climber adapts.
    private int windowSize;
    private int mainSize;

    // Adaptive sizing (hill-climbing), analogous to Caffeine's Climber.
    private final int minWindowSize;
    private final int maxWindowSize;
    private final int minAdaptStep;      // step never decays below this
    private int adaptStep;               // capacity moved per adaptation (decays over time)
    private int adaptDirection = 1;      // +1 grows the window, -1 shrinks it
    private double previousHitRate = 0.0;
    private final int adaptSamplePeriod; // requests observed between adaptations
    private int adaptRequests = 0;
    private int adaptHits = 0;
    private int warmupPeriodsLeft = 2;   // skip this many sample periods before acting,
                                          // so cache fill-up isn't misread as a real signal
    // Minimum hit-rate change (absolute, e.g. 0.002 = 0.2 percentage points)
    // required to treat a sample as real signal rather than noise. Below
    // this, the boundary holds still instead of "hunting" back and forth.
    private static final double HYSTERESIS = 0.002;

    // Probability of admitting a candidate on an exact frequency tie with
    // the victim: 1-in-N, so ties don't permanently favor the incumbent.
    private static final int TIE_BREAK_ADMIT_ONE_IN = 100;

    // Window cache (starts at 1% of capacity) - protects against bursts
    private final LRUCache<K, V> windowCache;

    // Main cache (starts at 99% of capacity) - for frequent items
    private final SegmentedLFUCache<K, V> mainCache;

    // Frequency sketch - probabilistic counter with doorkeeper
    private final CountMinSketch<K> sketch;
    
    private final boolean statistics;
    private final AtomicInteger countWindowHits = new AtomicInteger();
    private final AtomicInteger countMainHits = new AtomicInteger();
    private final AtomicInteger countAttempts = new AtomicInteger();

    public TinyLFUCache(int capacity, boolean statistics)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.windowSize = Math.max(1, capacity / 100); // 1% for window, initial value
        this.mainSize = capacity - windowSize;

        // The window is allowed to roam between 1% and 50% of the total
        // capacity; the climber decides where inside that range it settles
        // depending on observed hit rate.
        this.minWindowSize = Math.max(1, capacity / 100);
        this.maxWindowSize = Math.max(minWindowSize, capacity / 2);
        this.minAdaptStep = Math.max(1, capacity / 400);  // 0.25% floor
        this.adaptStep = Math.max(1, capacity / 200);     // 0.5% starting step
        this.adaptSamplePeriod = Math.max(100, capacity * 10);

        this.windowCache = new LRUCache<>(windowSize);
        this.mainCache = new SegmentedLFUCache<>(mainSize);
        this.sketch = new CountMinSketch<>(capacity * 10);
        this.statistics = statistics;
    }
    public TinyLFUCache(int capacity)
    {
        this(capacity, false);
    }

    @Override
    public synchronized V get(K key)
    {
        // Record access
        sketch.increment(key);
        adaptRequests++;
        if(statistics) countAttempts.incrementAndGet();

        // Try window cache first
        V value = windowCache.get(key);
        if (value != null)
        {
            adaptHits++;
            if(statistics) countWindowHits.incrementAndGet();
            maybeAdapt();
            return value;
        }

        // Try main cache
        value = mainCache.get(key);
        if (value != null)
        {
            adaptHits++;
            if(statistics) countMainHits.incrementAndGet();
        }
        maybeAdapt();
        return value;
    }

    @Override
    public synchronized void put(K key, V value)
    {
        sketch.increment(key);

        // Check if already exists
        if (windowCache.contains(key))
        {
            windowCache.put(key, value);
            return;
        }

        if (mainCache.contains(key))
        {
            mainCache.put(key, value);
            return;
        }

        // New item - try admission to window
        if (windowCache.size() < windowSize)
        {
            windowCache.put(key, value);
        }
        else
        {
            // Window is full - evict from window and try admission to main
            Map.Entry<K, V> evicted = windowCache.evict();
            windowCache.put(key, value);

            // Admit to main if frequency is good
            tryAdmitToMain(evicted.getKey(), evicted.getValue());
        }
    }

    private void tryAdmitToMain(K key, V value)
    {
        admitToMain(key, value, mainSize);
    }

    private void admitToMain(K key, V value, int mainCapacity)
    {
        if (mainCapacity <= 0)
        {
            // Main cache has no room at all (can happen with very small
            // overall capacities), so the evicted item is simply discarded.
            return;
        }

        if (mainCache.size() < mainCapacity)
        {
            mainCache.put(key, value);
            return;
        }

        // Compare frequencies - admit only if better than victim
        if (mainCache.isEmpty())
        {
            // Nothing to compare against; just admit the candidate.
            mainCache.put(key, value);
            return;
        }

        K victim = mainCache.peekVictim();
        int candidateFreq = sketch.estimate(key);
        int victimFreq = sketch.estimate(victim);

        if (candidateFreq > victimFreq)
        {
            mainCache.evictVictim();
            mainCache.put(key, value);
        }
        else if (candidateFreq == victimFreq && ThreadLocalRandom.current().nextInt(TIE_BREAK_ADMIT_ONE_IN) == 0)
        {
            // Small random chance to admit on ties. A strict ">" alone can
            // let the incumbent win every tie forever once the sketch
            // saturates and many keys converge on the same estimate,
            // freezing the main cache around whichever items got there
            // first rather than whichever are actually more frequent.
            mainCache.evictVictim();
            mainCache.put(key, value);
        }
    }

    /**
     * Hill-climbing adaptation of the window/main boundary. Every
     * {@link #adaptSamplePeriod} requests, compares the hit rate observed in
     * this window against the previous one. This lets the cache lean towards
     * a bigger recency-window under bursty/scanning workloads, and towards a
     * bigger LFU main cache under stable, frequency-skewed workloads.
     *
     * <p>Three guards keep a naive climber from doing more harm than good on
     * workloads that have nothing to gain from adapting:</p>
     * <ul>
     *   <li><b>Warm-up guard:</b> the first {@code warmupPeriodsLeft} samples
     *   only collect a baseline hit rate and never move the boundary, since
     *   early on the hit rate is rising simply because the cache is filling
     *   up — not because any particular direction is helping.</li>
     *   <li><b>Hysteresis:</b> a change in hit rate smaller than
     *   {@link #HYSTERESIS} is treated as sampling noise, not a real signal.
     *   The boundary holds still rather than "hunting" back and forth on
     *   workloads where the current split is already at or near optimal.</li>
     *   <li><b>Step decay:</b> once a real reversal is detected (meaning the
     *   climber has stepped past the optimum), the step size is halved down
     *   to {@link #minAdaptStep}, so it settles near the optimum instead of
     *   oscillating around it forever at full amplitude.</li>
     * </ul>
     */
    private void maybeAdapt()
    {
        if (adaptRequests < adaptSamplePeriod)
        {
            return;
        }

        double hitRate = (double) adaptHits / adaptRequests;

        if (warmupPeriodsLeft > 0)
        {
            warmupPeriodsLeft--;
            previousHitRate = hitRate;
            adaptRequests = 0;
            adaptHits = 0;
            return;
        }

        double delta = hitRate - previousHitRate;
        previousHitRate = hitRate;

        if (Math.abs(delta) > HYSTERESIS)
        {
            if (delta < 0)
            {
                // The last move made things measurably worse: reverse
                // direction and shrink the step so the climber converges
                // instead of oscillating at full amplitude forever.
                adaptDirection = -adaptDirection;
                adaptStep = Math.max(minAdaptStep, adaptStep / 2);
            }

            int newWindowSize = windowSize + (adaptDirection * adaptStep);
            newWindowSize = Math.max(minWindowSize, Math.min(maxWindowSize, newWindowSize));

            if (newWindowSize != windowSize)
            {
                resizeWindow(newWindowSize);
            }
        }
        // else: change is within noise, hold the boundary where it is.

        adaptRequests = 0;
        adaptHits = 0;
    }

    /**
     * Moves capacity between the window and main caches, evicting overflow
     * entries through the normal LRU/LFU/admission policies rather than
     * dropping them arbitrarily.
     */
    private void resizeWindow(int newWindowSize)
    {
        int newMainSize = capacity - newWindowSize;

        // Shrink main first (if applicable) so it isn't left over its new
        // capacity before we try to push newly-evicted window entries into it.
        mainCache.setCapacity(newMainSize);
        while (mainCache.size() > newMainSize)
        {
            mainCache.evictVictim();
        }

        windowCache.setCapacity(newWindowSize);
        while (windowCache.size() > newWindowSize)
        {
            Map.Entry<K, V> evicted = windowCache.evict();
            admitToMain(evicted.getKey(), evicted.getValue(), newMainSize);
        }

        this.windowSize = newWindowSize;
        this.mainSize = newMainSize;
    }

    /** Current window cache capacity (exposed mainly for tests/diagnostics). */
    public synchronized int getWindowSize()
    {
        return windowSize;
    }

    /** Current main cache capacity (exposed mainly for tests/diagnostics). */
    public synchronized int getMainSize()
    {
        return mainSize;
    }

    @Override
    public synchronized int size()
    {
        return windowCache.size() + mainCache.size();
    }

    @Override
    public synchronized boolean isEmpty()
    {
        return windowCache.isEmpty() && mainCache.isEmpty();
    }

    @Override
    public synchronized void clear()
    {
        windowCache.clear();
        mainCache.clear();
    }
    
    
    // Simple LRU for window cache
    private static class LRUCache<K, V>
    {

        private int capacity;
        private final LinkedHashMap<K, V> map;

        LRUCache(int capacity)
        {
            this.capacity = capacity;
            this.map = new LinkedHashMap<K,V>(capacity, 0.75f, true)
            {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
                {
                    return size() > capacity;
                }
            };
        }

        V get(K key)
        {
            return map.get(key);
        }

        void put(K key, V value)
        {
            map.put(key, value);
        }

        boolean contains(K key)
        {
            return map.containsKey(key);
        }

        Map.Entry<K, V> evict()
        {
            K oldestKey = map.keySet().iterator().next();
            V value = map.remove(oldestKey);
            return new AbstractMap.SimpleEntry<>(oldestKey, value);
        }

        int size()
        {
            return map.size();
        }
        boolean isEmpty()
        {
            return map.isEmpty();
        }
        void clear()
        {
            map.clear();
        }

        /** Changes the target capacity used by removeEldestEntry(). Does not
         * evict by itself; callers that shrink capacity are expected to pull
         * the overflow out explicitly via {@link #evict()}. */
        void setCapacity(int newCapacity)
        {
            this.capacity = newCapacity;
        }
    }

    // Segmented LFU (SLRU) - divides into protected and probation segments
    private static class SegmentedLFUCache<K, V>
    {

        private int capacity;
        private final LinkedHashMap<K, V> probation; // 20%
        private final LinkedHashMap<K, V> protect;   // 80%
        private int protectSize;

        SegmentedLFUCache(int capacity)
        {
            this.capacity = capacity;
            this.protectSize = protectSizeFor(capacity);
            this.probation = new LinkedHashMap<>();
            this.protect = new LinkedHashMap<>(16, 0.75f, true);
        }

        private static int protectSizeFor(int capacity)
        {
            // Guarantee at least one protected slot for any non-empty main
            // cache; a plain (int)(capacity*0.8) can round down to 0 for
            // small capacities, leaving the "protected" segment permanently
            // empty and defeating the point of segmentation.
            return capacity <= 0 ? 0 : Math.max(1, (int) (capacity * 0.8));
        }

        V get(K key)
        {
            V value = protect.get(key);
            if (value != null)
            {
                return value;
            }

            value = probation.remove(key);
            if (value != null)
            {
                // Promote to protected
                promoteToProtected(key, value);
                return value;
            }

            return null;
        }

        void put(K key, V value)
        {
            if (protect.containsKey(key))
            {
                protect.put(key, value);
            }
            else if (probation.containsKey(key))
            {
                probation.remove(key);
                promoteToProtected(key, value);
            }
            else
            {
                probation.put(key, value);
            }
        }

        private void promoteToProtected(K key, V value)
        {
            if (protect.size() >= protectSize)
            {
                // Demote oldest from protected to probation
                K demoteKey = protect.keySet().iterator().next();
                V demoteValue = protect.remove(demoteKey);
                probation.put(demoteKey, demoteValue);
            }
            protect.put(key, value);
        }

        boolean contains(K key)
        {
            return protect.containsKey(key) || probation.containsKey(key);
        }

        K peekVictim()
        {
            if (!probation.isEmpty())
            {
                return probation.keySet().iterator().next();
            }
            if (!protect.isEmpty())
            {
                return protect.keySet().iterator().next();
            }
            return null;
        }

        void evictVictim()
        {
            if (!probation.isEmpty())
            {
                K key = probation.keySet().iterator().next();
                probation.remove(key);
            }
            else if (!protect.isEmpty())
            {
                K key = protect.keySet().iterator().next();
                protect.remove(key);
            }
        }

        int size()
        {
            return probation.size() + protect.size();
        }
        boolean isEmpty()
        {
            return probation.isEmpty() && protect.isEmpty();
        }
        void clear()
        {
            probation.clear();
            protect.clear();
        }

        /** Changes the target capacity (and derived protected-segment size).
         * Does not evict by itself; callers that shrink capacity are expected
         * to pull the overflow out explicitly via {@link #evictVictim()}. */
        void setCapacity(int newCapacity)
        {
            this.capacity = newCapacity;
            this.protectSize = protectSizeFor(capacity);
        }
    }

    // Count-Min Sketch - probabilistic frequency counter, with a Bloom-filter
    // doorkeeper and conservative-update, as in the reference TinyLFU design.
    private static class CountMinSketch<K>
    {

        private final int width;
        private final int depth;
        private final int[][] counters;
        private int size;
        private final int sampleSize;

        // Doorkeeper: gates entry into the counters so that a key seen only
        // once doesn't spend counter budget that genuinely frequent keys
        // need. A key's real estimate is "doorkeeper bit set ? counters+1 :
        // counters", and only a *second* sighting actually touches the
        // counters. Implemented as a small in-place Bloom filter (2 hash
        // probes) sharing the sketch's own aging/reset cycle.
        private final long[] doorkeeper;
        private final int doorkeeperBits;

        CountMinSketch(int sampleSize)
        {
            this.width = 2048; // Should be power of 2
            this.depth = 4;
            this.counters = new int[depth][width];
            this.sampleSize = sampleSize;

            this.doorkeeperBits = nextPowerOfTwo(Math.max(64, sampleSize * 8));
            this.doorkeeper = new long[doorkeeperBits / 64];
        }

        void increment(K key)
        {
            if (++size >= sampleSize)
            {
                reset();
            }

            int hash = spread(key.hashCode());

            if (!doorkeeperContains(hash))
            {
                // First sighting since the last reset: record it in the
                // doorkeeper only, don't touch the counters yet.
                doorkeeperAdd(hash);
                return;
            }

            // Conservative update: find the current minimum across rows,
            // then only bump the counters that are already at that minimum.
            // This avoids inflating counters that happened to collide with
            // a hotter key in one row but not the others.
            int[] indices = new int[depth];
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < depth; i++)
            {
                indices[i] = indexFor(hash, i);
                min = Math.min(min, counters[i][indices[i]]);
            }
            if (min < 15)
            {
                for (int i = 0; i < depth; i++)
                {
                    if (counters[i][indices[i]] == min)
                    {
                        counters[i][indices[i]]++;
                    }
                }
            }
        }

        int estimate(K key)
        {
            int hash = spread(key.hashCode());
            int min = Integer.MAX_VALUE;

            for (int i = 0; i < depth; i++)
            {
                int index = indexFor(hash, i);
                min = Math.min(min, counters[i][index]);
            }

            // The doorkeeper contributes the "first sighting" that never
            // made it into the counters (see increment()).
            return doorkeeperContains(hash) ? min + 1 : min;
        }

        // Same mixing function used by both increment() and estimate() so that
        // a given (key, row) pair always maps to the same counter slot.
        // width is a power of two, so '& (width - 1)' is used instead of '%'
        // to also avoid Math.abs(Integer.MIN_VALUE) returning a negative value.
        private int indexFor(int hash, int row)
        {
            int mixed = hash ^ ((hash >>> 16) * (row + 1));
            return mixed & (width - 1);
        }

        private boolean doorkeeperContains(int hash)
        {
            return getBit(hash, 0) && getBit(hash, 1);
        }

        private void doorkeeperAdd(int hash)
        {
            setBit(hash, 0);
            setBit(hash, 1);
        }

        private boolean getBit(int hash, int seed)
        {
            int idx = doorkeeperIndex(hash, seed);
            return (doorkeeper[idx >>> 6] & (1L << (idx & 63))) != 0;
        }

        private void setBit(int hash, int seed)
        {
            int idx = doorkeeperIndex(hash, seed);
            doorkeeper[idx >>> 6] |= (1L << (idx & 63));
        }

        private int doorkeeperIndex(int hash, int seed)
        {
            int mixed = hash ^ ((hash >>> 15) * (seed + 2));
            return mixed & (doorkeeperBits - 1);
        }

        private void reset()
        {
            // Decay all counters by half (aging)
            for (int i = 0; i < depth; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    counters[i][j] = counters[i][j] >> 1;
                }
            }
            // The doorkeeper ages out completely: whatever was a "first
            // sighting" before the reset should be treated as a fresh first
            // sighting again, consistent with the halved counters.
            Arrays.fill(doorkeeper, 0L);
            size = 0;
        }

        // Murmur3-style finalizer: spreads hashCode() bits so keys whose
        // hashCode() has weak low-order entropy (e.g. sequential integers,
        // or identity hashes on some JVMs) don't correlate across rows and
        // the doorkeeper.
        private static int spread(int hash)
        {
            hash ^= (hash >>> 16);
            hash *= 0x85ebca6b;
            hash ^= (hash >>> 13);
            hash *= 0xc2b2ae35;
            hash ^= (hash >>> 16);
            return hash;
        }

        private static int nextPowerOfTwo(int x)
        {
            int highest = Integer.highestOneBit(Math.max(1, x));
            return (highest == x) ? x : highest << 1;
        }
    }
}

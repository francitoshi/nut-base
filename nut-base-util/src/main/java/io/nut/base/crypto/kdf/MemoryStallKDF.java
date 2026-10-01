/*
 *  MemoryStallKDF.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto.kdf;

import io.nut.base.crypto.Kripto;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;

public class MemoryStallKDF
{
    private static final int VERSION = 1;
    private static final int BLOCK_SIZE_LONGS = 1024 * 1024; // 8MB per block
    private static final int BLOCK_MASK_IDX = BLOCK_SIZE_LONGS - 1;
    private static final int CHUNK_SIZE = 128; // L1/L2 cache optimization

    // Prime constant for mixing (Golden Ratio - 64 bit)
    private static final long K_PRIME = 0x9E3779B97F4A7C15L;

    private final Kripto kripto;

    public MemoryStallKDF()
    {
        this(null);
    }

    public MemoryStallKDF(Kripto kripto)
    {
        this.kripto = kripto==null ? Kripto.getInstance() : kripto;
    }
    
    /**
     * Main entry point.
     *
     * @param key Initial key or password.
     * @param salt Random salt.
     * @param numBlocks Number of 8MB blocks (MUST be a power of 2: 16, 32, 64...).
     * @param numPasses Number of passes over memory (Recommended: >= 2).
     * @param outputLength Length in bytes of desired output.
     * @return Derived key.
     */
    public byte[] deriveKey(byte[] key, byte[] salt, int numBlocks, int numPasses, int outputLength)
    {
        // 0. Basic validations
        if ((numBlocks & (numBlocks - 1)) != 0 || numBlocks < 2)
        {
            throw new IllegalArgumentException("numBlocks must be a power of 2 greater than 1 (e.g. 16, 32, 64).");
        }

        // 1. Master Seed Initialization (SHA-512)
        byte[] seed = generateSeed(key, salt, numBlocks, numPasses, outputLength);

        // 2. Memory Allocation (long[][] is hostile for GPUs that prefer flat memory)
        long[][] memory = new long[numBlocks][BLOCK_SIZE_LONGS];

        // 3. Initial chaotic filling (Expansion Phase)
        fillMemory(memory, seed);

        // 4. Deep mixing (Mixing Phase - Latency Bound)
        mixMemory(memory, numPasses);

        // 5. Reduction, Secure Wiping and Final Expansion (Finalization Phase)
        return generateAndWipe(memory, outputLength);
    }

    // -----------------------------------------------------------------------------------------
    // PHASE 1: Seed Generation (Strict Little-Endian Determinism)
    // -----------------------------------------------------------------------------------------
    private byte[] generateSeed(byte[] key, byte[] salt, int numBlocks, int numPasses, int outputLength)
    {
        int bufferSize = (4 * 6) + salt.length + key.length;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // CRITICAL for C++ compatibility
        buffer.putInt(VERSION);
        buffer.putInt(outputLength);
        buffer.putInt(numBlocks);
        buffer.putInt(numPasses);
        buffer.putInt(salt.length);
        buffer.put(salt);
        buffer.putInt(key.length);
        buffer.put(key);
        MessageDigest digest = kripto.sha512.get();
        return digest.digest(buffer.array());
    }

    // -----------------------------------------------------------------------------------------
    // PHASE 2: Memory Filling (Forced Serialization)
    // -----------------------------------------------------------------------------------------
    private void fillMemory(long[][] memory, byte[] seedBytes)
    {
        // Expand seed to initial state
        long[] state = new long[seedBytes.length/Long.BYTES];
        ByteBuffer buf = ByteBuffer.wrap(seedBytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < state.length && buf.hasRemaining(); i++)
        {
            if (buf.remaining() >= 8)
            {
                state[i] = buf.getLong();
            }
        }

        long previousValue = 0;
        int blocks = memory.length;

        for (int b = 0; b < blocks; b++)
        {
            long[] currentBlock = memory[b]; // Local variable for JIT optimization
            for (int i = 0; i < BLOCK_SIZE_LONGS; i++)
            {
                int stateIdx = i & 7;
                long s = state[stateIdx];

                // Anti-FPGA: 64-bit multiplication is expensive in dedicated hardware.
                // Anti-Parallelism: Depends on previousValue.
                long v = (s ^ previousValue ^ i) * K_PRIME;

                // Reversible XorShift for bit avalanche
                v = v ^ (v >>> 32);

                currentBlock[i] = v;
                previousValue = v;
                state[stateIdx] = v; // Feedback to state
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // PHASE 3: Mixing (The Latency Bottleneck)
    // -----------------------------------------------------------------------------------------
    private void mixMemory(long[][] memory, int numPasses)
    {
        int numBlocks = memory.length;
        int blocksMask = numBlocks - 1;
        int chunksPerBlock = BLOCK_SIZE_LONGS / CHUNK_SIZE;

        // State cursor for pseudo-random jumps
        long cursor = 0x5555555555555555L;

        for (int p = 0; p < numPasses; p++)
        {
            for (int b = 0; b < numBlocks; b++)
            {
                long[] destBlock = memory[b];

                // Source block selection based on chaotic cursor
                // C++ cannot predict this (Guaranteed Cache Miss)
                long[] sourceA = memory[(int) (cursor & blocksMask)];
                cursor = Long.rotateLeft(cursor, 13) ^ sourceA[0];

                long[] sourceB = memory[(int) (cursor & blocksMask)];
                cursor += sourceB[0];

                long[] sourceC = memory[(int) (cursor & blocksMask)];

                // Processing by Chunks (128 longs)
                // This amortizes Java's Bounds Checking, matching C++
                for (int c = 0; c < chunksPerBlock; c++)
                {
                    int offset = c * CHUNK_SIZE;

                    // Hot loop, unrollable
                    for (int i = 0; i < CHUNK_SIZE; i++)
                    {
                        int idx = offset + i;

                        long valA = sourceA[idx]; // Sequential access (fast)

                        // Latency Chain: A -> B -> C
                        // The CPU must wait for data from RAM before requesting the next one.
                        int idxB = (int) (valA & BLOCK_MASK_IDX);
                        long valB = sourceB[idxB]; // Random jump 1 (Probable cache miss)

                        int idxC = (int) (valB & BLOCK_MASK_IDX);
                        long valC = sourceC[idxC]; // Random jump 2 (Probable cache miss)

                        // Mathematical Operation
                        // RotateLeft is intrinsic in Java (1 CPU cycle)
                        long result = (valA + valB) ^ Long.rotateLeft(valC, 29);

                        destBlock[idx] = result;
                        cursor ^= result;
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // PHASE 4: Result Generation and Wiping
    // -----------------------------------------------------------------------------------------
    private byte[] generateAndWipe(long[][] memory, int outputLength)
    {
        int numBlocks = memory.length;
        // Buffer for block summaries
        ByteBuffer summariesBuffer = ByteBuffer.allocate(numBlocks * 8);
        summariesBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Step A: Reduction + Wiping
        // By combining read and write in the same loop, we prevent C++ from using 
        // pure 'memset' optimizations, equalizing performance.
        for (int b = 0; b < numBlocks; b++)
        {
            long blockHash = 0;
            long[] currentBlock = memory[b];

            for (int i = 0; i < currentBlock.length; i++)
            {
                long val = currentBlock[i];

                // Simple non-commutative rolling hash
                blockHash = (blockHash ^ val) * K_PRIME;
                blockHash = Long.rotateLeft(blockHash, 11);

                // DATA WIPING: Immediate RAM wiping
                currentBlock[i] = 0;
            }
            summariesBuffer.putLong(blockHash);
        }

        // Step B: Final Hash and Expansion
        MessageDigest digest = kripto.sha512.get();
        byte[] baseHash = digest.digest(summariesBuffer.array());
        // If <= 64 bytes requested, return truncated
        if (outputLength <= 64)
        {
            return Arrays.copyOf(baseHash, outputLength);
        }
        // If > 64 bytes requested, Expand in Counter Mode
        return expandOutput(digest, baseHash, outputLength);
    }

    /**
     * Expands the base hash using "Feedback Chaining". Block_N =
     * SHA512(Block_N-1 + BaseHash + Counter)
     *
     * This forces generation to be strictly sequential.
     */
    private static byte[] expandOutput(MessageDigest digest, byte[] baseHash, int outputLength)
    {
        ByteBuffer resultBuffer = ByteBuffer.allocate(outputLength);

        // Buffer for counter (4 bytes Little-Endian)
        ByteBuffer counterBuf = ByteBuffer.allocate(4);
        counterBuf.order(ByteOrder.LITTLE_ENDIAN);

        int generatedBytes = 0;
        int counter = 0;

        // The "runningHash" acts as the previous Block.
        // Initially it's the BaseHash.
        byte[] runningHash = baseHash;

        while (generatedBytes < outputLength)
        {
            // 1. Feed the digest with the previous block (Feedback)
            digest.update(runningHash);

            // 2. Feed with the original BaseHash (Entropy reinjection)
            // This ensures that the "strength" of RAM is present in each step.
            digest.update(baseHash);

            // 3. Feed with counter (Avoids cycles)
            counterBuf.clear();
            counterBuf.putInt(counter);
            digest.update(counterBuf.array());

            // 4. Calculate the new block
            byte[] newBlock = digest.digest();

            // Copy to final result
            int bytesToCopy = Math.min(newBlock.length, outputLength - generatedBytes);
            resultBuffer.put(newBlock, 0, bytesToCopy);

            // Update state
            runningHash = newBlock;
            generatedBytes += bytesToCopy;
            counter++;
        }

        return resultBuffer.array();
    }
}

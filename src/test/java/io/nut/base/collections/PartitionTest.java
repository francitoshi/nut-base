/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Partition")
class PartitionTest
{
    @Test
    @DisplayName("correctly partitions elements into accepted and rejected lists")
    void testPartition()
    {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);

        // Partition by even numbers
        Partition<Integer> partitioned = Partition.partition(numbers, n -> n % 2 == 0);

        assertEquals(Arrays.asList(2, 4, 6), partitioned.accepted());
        assertEquals(Arrays.asList(1, 3, 5), partitioned.rejected());
    }

    @Test
    @DisplayName("correctly partitions elements using Lists.partition syntax")
    void testListsPartition()
    {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David");

        Partition<String> partitioned = Partition.partition(names, name -> name.startsWith("C"));

        assertEquals(Collections.singletonList("Charlie"), partitioned.accepted());
        assertEquals(Arrays.asList("Alice", "Bob", "David"), partitioned.rejected());
    }

    @Test
    @DisplayName("partitioning empty list results in empty accepted and rejected lists")
    void testEmptyList()
    {
        Partition<Integer> partitioned = Partition.partition(Collections.emptyList(), n -> n > 0);
        assertTrue(partitioned.accepted().isEmpty());
        assertTrue(partitioned.rejected().isEmpty());
    }

    @Test
    @DisplayName("accepted and rejected lists are unmodifiable")
    void testImmutability()
    {
        Partition<Integer> partitioned = Partition.partition(Arrays.asList(1, 2), n -> n == 1);

        assertThrows(UnsupportedOperationException.class, () -> partitioned.accepted().add(3));
        assertThrows(UnsupportedOperationException.class, () -> partitioned.rejected().add(4));
    }

    @Test
    @DisplayName("validates arguments strictly")
    void testArgumentValidation()
    {
        assertThrows(NullPointerException.class, () -> Partition.partition(null, n -> true));
        assertThrows(NullPointerException.class, () -> Partition.partition(Collections.emptyList(), null));
    }
}

/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * JUnit 5 tests for the Unchecked utility class.
 */
@DisplayName("Unchecked Tests")
class UncheckedTest
{

    private static final class CheckedFailure extends Exception
    {

        CheckedFailure(String message)
        {
            super(message);
        }
    }

    private static final class CustomError extends Error
    {

        CustomError(String message)
        {
            super(message);
        }
    }

    // =========================================================================
    // run(CheckedRunnable) Tests
    // =========================================================================
    @Nested
    @DisplayName("run(CheckedRunnable) Tests")
    class RunTests
    {

        @Test
        @DisplayName("Should execute the runnable without exception")
        void shouldExecuteRunnableWithoutException()
        {
            boolean[] executed =
            {
                false
            };

            Unchecked.run(() ->
            {
                executed[0] = true;
            });

            assertTrue(executed[0]);
        }

        @Test
        @DisplayName("Should rethrow checked exceptions as original instance")
        void shouldRethrowCheckedExceptionAsOriginalInstance()
        {
            CheckedFailure original = new CheckedFailure("boom");

            CheckedFailure thrown = assertThrows(CheckedFailure.class,
            () -> Unchecked.run(() ->
            {
                throw original;
            }));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should rethrow runtime exceptions unchanged")
        void shouldRethrowRuntimeExceptionUnchanged()
        {
            IllegalStateException original = new IllegalStateException("boom");

            IllegalStateException thrown = assertThrows(IllegalStateException.class,
            () -> Unchecked.run(() ->
            {
                throw original;
            }));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should rethrow errors unchanged")
        void shouldRethrowErrorsUnchanged()
        {
            CustomError original = new CustomError("boom");

            CustomError thrown = assertThrows(CustomError.class,
            () -> Unchecked.run(() ->
            {
                throw original;
            }));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should execute subsequent statements when the body itself succeeds")
        void shouldRunSubsequentStatementsWhenBodySucceeds()
        {
            int[] counter =
            {
                0
            };

            Unchecked.run(() -> counter[0]++);

            assertEquals(1, counter[0]);
        }

        @Test
        @DisplayName("Should throw NPE when runnable is null")
        void shouldThrowNPEWhenRunnableIsNull()
        {
            assertThrows(NullPointerException.class, () -> Unchecked.run(null));
        }
    }

    // =========================================================================
    // get(CheckedSupplier) Tests
    // =========================================================================
    @Nested
    @DisplayName("get(CheckedSupplier) Tests")
    class GetTests
    {

        @Test
        @DisplayName("Should return the value produced by the supplier")
        void shouldReturnValueProducedBySupplier()
        {
            String result = Unchecked.get(() -> "hello");

            assertEquals("hello", result);
        }

        @Test
        @DisplayName("Should accept a null result from the supplier")
        void shouldAcceptNullResult()
        {
            assertNull(Unchecked.get(() -> null));
        }

        @Test
        @DisplayName("Should rethrow checked exceptions as original instance")
        void shouldRethrowCheckedExceptionAsOriginalInstance()
        {
            CheckedFailure original = new CheckedFailure("boom");

            CheckedFailure thrown = assertThrows(CheckedFailure.class,
                    () -> Unchecked.get(() ->
                    {
                        throw original;
                    }));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should rethrow runtime exceptions unchanged")
        void shouldRethrowRuntimeExceptionUnchanged()
        {
            IllegalArgumentException original = new IllegalArgumentException("boom");

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> Unchecked.get(() ->
            {
                throw original;
            }));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should preserve the checked exception message")
        void shouldPreserveCheckedExceptionMessage()
        {
            String message = "expected failure message";

            CheckedFailure thrown = assertThrows(CheckedFailure.class,
            () -> Unchecked.get(() ->
            {
                throw new CheckedFailure(message);
            }));

            assertEquals(message, thrown.getMessage());
        }

        @Test
        @DisplayName("Should throw NPE when supplier is null")
        void shouldThrowNPEWhenSupplierIsNull()
        {
            assertThrows(NullPointerException.class, () -> Unchecked.get(null));
        }
    }

    // =========================================================================
    // function(CheckedFunction) Tests
    // =========================================================================
    @Nested
    @DisplayName("function(CheckedFunction) Tests")
    class FunctionTests
    {

        @Test
        @DisplayName("Should return a Function that applies the checked logic")
        void shouldReturnFunctionApplyingCheckedLogic()
        {
            Function<String, Integer> function = Unchecked.function(String::length);

            assertEquals(5, function.apply("hello"));
        }

        @Test
        @DisplayName("Should work in stream pipelines")
        void shouldWorkInStreamPipelines()
        {
            List<Integer> lengths = Arrays.asList("a", "bb", "ccc")
                    .stream()
                    .map(Unchecked.function(String::length))
                    .collect(java.util.stream.Collectors.toList());

            assertEquals(Arrays.asList(1, 2, 3), lengths);
        }

        @Test
        @DisplayName("Should accept a null result from the function")
        void shouldAcceptNullResult()
        {
            Function<Object, Object> function = Unchecked.function(o -> null);

            assertNull(function.apply(new Object()));
        }

        @Test
        @DisplayName("Should rethrow checked exceptions as original instance")
        void shouldRethrowCheckedExceptionAsOriginalInstance()
        {
            CheckedFailure original = new CheckedFailure("boom");
            Function<String, String> function = Unchecked.function(s ->
            {
                throw original;
            });

            CheckedFailure thrown = assertThrows(CheckedFailure.class, () -> function.apply("x"));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should rethrow runtime exceptions unchanged")
        void shouldRethrowRuntimeExceptionUnchanged()
        {
            IllegalArgumentException original = new IllegalArgumentException("boom");
            Function<String, String> function = Unchecked.function(s ->
            {
                throw original;
            });

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> function.apply("x"));

            assertSame(original, thrown);
        }

        @Test
        @DisplayName("Should pass the input argument through")
        void shouldPassInputArgumentThrough()
        {
            Function<String, String> function = Unchecked.function(s -> s + "!");

            assertEquals("value!", function.apply("value"));
        }

        @Test
        @DisplayName("Should throw NPE when function is null")
        void shouldThrowNPEWhenFunctionIsNull()
        {
            assertThrows(NullPointerException.class, () -> Unchecked.function(null));
        }
    }

    // =========================================================================
    // sneakyThrow(Throwable) Tests
    // =========================================================================
    @Nested
    @DisplayName("sneakyThrow(Throwable) Tests")
    class SneakyThrowTests
    {

        @Test
        @DisplayName("Should throw the exact same instance without wrapping")
        void shouldThrowSameInstanceWithoutWrapping()
        {
            CheckedFailure original = new CheckedFailure("boom");

            CheckedFailure thrown = assertThrows(CheckedFailure.class,
            () -> Unchecked.run(() ->
            {
                throw Unchecked.<CheckedFailure>sneakyThrow(original);
            }));

            assertSame(original, thrown);
        }
    }
}
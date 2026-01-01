/*
 *  RequireTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Supplier;

/**
 * Comprehensive JUnit 5 tests for Require class. Tests all methods added in
 * Java 9, 16, and 19.
 */
@DisplayName("Require Tests")
class RequireTest
{

    // =========================================================================
    // Java 9 Methods Tests
    // =========================================================================
    @Nested
    @DisplayName("requireNonNullElse(T, T) Tests")
    class RequireNonNullElseTests
    {

        @Test
        @DisplayName("Should return first argument when non-null")
        void shouldReturnFirstArgumentWhenNonNull()
        {
            String value = "test";
            String defaultValue = "default";

            String result = Require.requireNonNullElse(value, defaultValue);

            assertEquals(value, result);
        }

        @Test
        @DisplayName("Should return default when first argument is null")
        void shouldReturnDefaultWhenFirstArgumentIsNull()
        {
            String value = null;
            String defaultValue = "default";

            String result = Require.requireNonNullElse(value, defaultValue);

            assertEquals(defaultValue, result);
        }

        @Test
        @DisplayName("Should throw NPE when both arguments are null")
        void shouldThrowNPEWhenBothArgumentsAreNull()
        {
            String value = null;
            String defaultValue = null;

            assertThrows(NullPointerException.class,
                    () -> Require.requireNonNullElse(value, defaultValue));
        }

        @Test
        @DisplayName("Should work with different types")
        void shouldWorkWithDifferentTypes()
        {
            Integer value = null;
            Integer defaultValue = 42;

            Integer result = Require.requireNonNullElse(value, defaultValue);

            assertEquals(defaultValue, result);
        }
    }

    @Nested
    @DisplayName("requireNonNullElseGet(T, Supplier<T>) Tests")
    class RequireNonNullElseGetTests
    {

        @Test
        @DisplayName("Should return first argument when non-null without calling supplier")
        void shouldReturnFirstArgumentWhenNonNull()
        {
            String value = "test";
            Supplier<String> supplier = () ->
            {
                fail("Supplier should not be called");
                return "default";
            };

            String result = Require.requireNonNullElseGet(value, supplier);

            assertEquals(value, result);
        }

        @Test
        @DisplayName("Should call supplier when first argument is null")
        void shouldCallSupplierWhenFirstArgumentIsNull()
        {
            String value = null;
            String defaultValue = "default";
            Supplier<String> supplier = () -> defaultValue;

            String result = Require.requireNonNullElseGet(value, supplier);

            assertEquals(defaultValue, result);
        }

        @Test
        @DisplayName("Should throw NPE when supplier is null")
        void shouldThrowNPEWhenSupplierIsNull()
        {
            String value = null;
            Supplier<String> supplier = null;

            assertThrows(NullPointerException.class,
                    () -> Require.requireNonNullElseGet(value, supplier));
        }

        @Test
        @DisplayName("Should throw NPE when supplier returns null")
        void shouldThrowNPEWhenSupplierReturnsNull()
        {
            String value = null;
            Supplier<String> supplier = () -> null;

            assertThrows(NullPointerException.class,
                    () -> Require.requireNonNullElseGet(value, supplier));
        }

        @Test
        @DisplayName("Should provide lazy evaluation")
        void shouldProvideLazyEvaluation()
        {
            String value = "test";
            boolean[] supplierCalled =
            {
                false
            };
            Supplier<String> supplier = () ->
            {
                supplierCalled[0] = true;
                return "default";
            };

            Require.requireNonNullElseGet(value, supplier);

            assertFalse(supplierCalled[0], "Supplier should not be called when value is non-null");
        }
    }

    @Nested
    @DisplayName("checkIndex(int, int) Tests")
    class CheckIndexIntTests
    {

        @ParameterizedTest
        @CsvSource(
                {
                    "0, 10",
                    "5, 10",
                    "9, 10",
                    "0, 1",
                    "0, 100"
                })
        @DisplayName("Should return index when within bounds")
        void shouldReturnIndexWhenWithinBounds(int index, int length)
        {
            int result = Require.checkIndex(index, length);
            assertEquals(index, result);
        }

        @ParameterizedTest
        @CsvSource(
                {
                    "-1, 10",
                    "10, 10",
                    "11, 10",
                    "1, 0",
                    "-5, 5"
                })
        @DisplayName("Should throw IndexOutOfBoundsException when out of bounds")
        void shouldThrowWhenOutOfBounds(int index, int length)
        {
            IndexOutOfBoundsException exception = assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> Require.checkIndex(index, length)
            );

            assertTrue(exception.getMessage().contains("Index"));
            assertTrue(exception.getMessage().contains(String.valueOf(index)));
        }

        @Test
        @DisplayName("Should throw when length is negative")
        void shouldThrowWhenLengthIsNegative()
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkIndex(0, -1));
        }
    }

    @Nested
    @DisplayName("checkFromToIndex(int, int, int) Tests")
    class CheckFromToIndexIntTests
    {

        @ParameterizedTest
        @CsvSource(
                {
                    "0, 0, 10",
                    "0, 5, 10",
                    "0, 10, 10",
                    "5, 10, 10",
                    "3, 7, 10"
                })
        @DisplayName("Should return fromIndex when range is valid")
        void shouldReturnFromIndexWhenRangeIsValid(int fromIndex, int toIndex, int length)
        {
            int result = Require.checkFromToIndex(fromIndex, toIndex, length);
            assertEquals(fromIndex, result);
        }

        @ParameterizedTest
        @CsvSource(
                {
                    "-1, 5, 10", // fromIndex < 0
                    "5, 3, 10", // fromIndex > toIndex
                    "0, 11, 10", // toIndex > length
                    "5, 15, 10"   // toIndex > length
                })
        @DisplayName("Should throw IndexOutOfBoundsException for invalid ranges")
        void shouldThrowForInvalidRanges(int fromIndex, int toIndex, int length)
        {
            IndexOutOfBoundsException exception = assertThrows(
                    IndexOutOfBoundsException.class,
                    () -> Require.checkFromToIndex(fromIndex, toIndex, length)
            );

            assertTrue(exception.getMessage().contains("Range"));
        }

        @Test
        @DisplayName("Should handle empty range")
        void shouldHandleEmptyRange()
        {
            int result = Require.checkFromToIndex(5, 5, 10);
            assertEquals(5, result);
        }

        @Test
        @DisplayName("Should throw when length is negative")
        void shouldThrowWhenLengthIsNegative()
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromToIndex(0, 5, -1));
        }
    }

    @Nested
    @DisplayName("checkFromIndexSize(int, int, int) Tests")
    class CheckFromIndexSizeIntTests
    {

        @ParameterizedTest
        @CsvSource(
                {
                    "0, 0, 10",
                    "0, 5, 10",
                    "0, 10, 10",
                    "5, 5, 10",
                    "3, 7, 10"
                })
        @DisplayName("Should return fromIndex when range is valid")
        void shouldReturnFromIndexWhenRangeIsValid(int fromIndex, int size, int length)
        {
            int result = Require.checkFromIndexSize(fromIndex, size, length);
            assertEquals(fromIndex, result);
        }

        @ParameterizedTest
        @CsvSource(
                {
                    "-1, 5, 10", // fromIndex < 0
                    "0, -1, 10", // size < 0
                    "0, 11, 10", // fromIndex + size > length
                    "6, 5, 10"    // fromIndex + size > length
                })
        @DisplayName("Should throw IndexOutOfBoundsException for invalid ranges")
        void shouldThrowForInvalidRanges(int fromIndex, int size, int length)
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromIndexSize(fromIndex, size, length));
        }

        @Test
        @DisplayName("Should handle integer overflow")
        void shouldHandleIntegerOverflow()
        {
            int fromIndex = Integer.MAX_VALUE - 5;
            int size = 10; // This would overflow when added to fromIndex

            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromIndexSize(fromIndex, size, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Should handle zero size")
        void shouldHandleZeroSize()
        {
            int result = Require.checkFromIndexSize(5, 0, 10);
            assertEquals(5, result);
        }

        @Test
        @DisplayName("Should throw when length is negative")
        void shouldThrowWhenLengthIsNegative()
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromIndexSize(0, 5, -1));
        }
    }

    // =========================================================================
    // Java 16 Methods Tests (long overloads)
    // =========================================================================
    @Nested
    @DisplayName("checkIndex(long, long) Tests")
    class CheckIndexLongTests
    {

        @ParameterizedTest
        @CsvSource(
                {
                    "0, 10",
                    "5, 10",
                    "9, 10",
                    "0, 1000000000000",
                    "999999999999, 1000000000000"
                })
        @DisplayName("Should return index when within bounds")
        void shouldReturnIndexWhenWithinBounds(long index, long length)
        {
            long result = Require.checkIndex(index, length);
            assertEquals(index, result);
        }

        @ParameterizedTest
        @CsvSource(
                {
                    "-1, 10",
                    "10, 10",
                    "11, 10",
                    "1000000000000, 1000000000000"
                })
        @DisplayName("Should throw IndexOutOfBoundsException when out of bounds")
        void shouldThrowWhenOutOfBounds(long index, long length)
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkIndex(index, length));
        }

        @Test
        @DisplayName("Should handle large long values")
        void shouldHandleLargeLongValues()
        {
            long length = Long.MAX_VALUE;
            long index = Long.MAX_VALUE - 1;

            long result = Require.checkIndex(index, length);
            assertEquals(index, result);
        }
    }

    @Nested
    @DisplayName("checkFromToIndex(long, long, long) Tests")
    class CheckFromToIndexLongTests
    {

        @ParameterizedTest
        @CsvSource(
                {
                    "0, 0, 10",
                    "0, 5, 10",
                    "0, 10, 10",
                    "5, 10, 10",
                    "0, 1000000000000, 1000000000000"
                })
        @DisplayName("Should return fromIndex when range is valid")
        void shouldReturnFromIndexWhenRangeIsValid(long fromIndex, long toIndex, long length)
        {
            long result = Require.checkFromToIndex(fromIndex, toIndex, length);
            assertEquals(fromIndex, result);
        }

        @ParameterizedTest
        @CsvSource(
                {
                    "-1, 5, 10",
                    "5, 3, 10",
                    "0, 11, 10"
                })
        @DisplayName("Should throw IndexOutOfBoundsException for invalid ranges")
        void shouldThrowForInvalidRanges(long fromIndex, long toIndex, long length)
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromToIndex(fromIndex, toIndex, length));
        }

        @Test
        @DisplayName("Should handle large long values")
        void shouldHandleLargeLongValues()
        {
            long length = Long.MAX_VALUE;
            long fromIndex = 1000000000000L;
            long toIndex = 2000000000000L;

            long result = Require.checkFromToIndex(fromIndex, toIndex, length);
            assertEquals(fromIndex, result);
        }
    }

    @Nested
    @DisplayName("checkFromIndexSize(long, long, long) Tests")
    class CheckFromIndexSizeLongTests
    {

        @ParameterizedTest
        @CsvSource(
                {
                    "0, 0, 10",
                    "0, 5, 10",
                    "0, 10, 10",
                    "5, 5, 10"
                })
        @DisplayName("Should return fromIndex when range is valid")
        void shouldReturnFromIndexWhenRangeIsValid(long fromIndex, long size, long length)
        {
            long result = Require.checkFromIndexSize(fromIndex, size, length);
            assertEquals(fromIndex, result);
        }

        @ParameterizedTest
        @CsvSource(
                {
                    "-1, 5, 10",
                    "0, -1, 10",
                    "0, 11, 10",
                    "6, 5, 10"
                })
        @DisplayName("Should throw IndexOutOfBoundsException for invalid ranges")
        void shouldThrowForInvalidRanges(long fromIndex, long size, long length)
        {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromIndexSize(fromIndex, size, length));
        }

        @Test
        @DisplayName("Should handle long overflow")
        void shouldHandleLongOverflow()
        {
            long fromIndex = Long.MAX_VALUE - 5;
            long size = 10;

            assertThrows(IndexOutOfBoundsException.class,
                    () -> Require.checkFromIndexSize(fromIndex, size, Long.MAX_VALUE));
        }

        @Test
        @DisplayName("Should handle large long values without overflow")
        void shouldHandleLargeLongValuesWithoutOverflow()
        {
            long fromIndex = 1000000000000L;
            long size = 1000000000000L;
            long length = 3000000000000L;

            long result = Require.checkFromIndexSize(fromIndex, size, length);
            assertEquals(fromIndex, result);
        }
    }

    // =========================================================================
    // Java 19 Methods Tests
    // =========================================================================
    @Nested
    @DisplayName("toIdentityString(Object) Tests")
    static class ToIdentityStringTests
    {

        @Test
        @DisplayName("Should return identity string for object")
        void shouldReturnIdentityStringForObject()
        {
            Object obj = new Object();
            String result = Require.toIdentityString(obj);

            assertTrue(result.startsWith("java.lang.Object@"));
            assertTrue(result.contains(Integer.toHexString(System.identityHashCode(obj))));
        }

        @Test
        @DisplayName("Should return identity string even when toString is overridden")
        void shouldReturnIdentityStringWhenToStringIsOverridden()
        {
            String str = "test";
            String result = Require.toIdentityString(str);

            assertTrue(result.startsWith("java.lang.String@"));
            assertFalse(result.equals("test"));
            assertTrue(result.contains(Integer.toHexString(System.identityHashCode(str))));
        }

        @Test
        @DisplayName("Should throw NPE when argument is null")
        void shouldThrowNPEWhenArgumentIsNull()
        {
            assertThrows(NullPointerException.class,
                    () -> Require.toIdentityString(null));
        }

        @Test
        @DisplayName("Should include correct class name")
        void shouldIncludeCorrectClassName()
        {
            Integer num = 42;
            String result = Require.toIdentityString(num);

            assertTrue(result.startsWith("java.lang.Integer@"));
        }

        @Test
        @DisplayName("Should work with custom classes")
        void shouldWorkWithCustomClasses()
        {
            CustomTestClass obj = new CustomTestClass();
            String result = Require.toIdentityString(obj);

            assertTrue(result.contains("CustomTestClass@"));
            assertTrue(result.contains(Integer.toHexString(System.identityHashCode(obj))));
        }

        @Test
        @DisplayName("Should produce consistent results for same object")
        void shouldProduceConsistentResults()
        {
            Object obj = new Object();
            String result1 = Require.toIdentityString(obj);
            String result2 = Require.toIdentityString(obj);

            assertEquals(result1, result2);
        }

        @Test
        @DisplayName("Should produce different results for different objects")
        void shouldProduceDifferentResultsForDifferentObjects()
        {
            Object obj1 = new Object();
            Object obj2 = new Object();
            String result1 = Require.toIdentityString(obj1);
            String result2 = Require.toIdentityString(obj2);

            assertNotEquals(result1, result2);
        }

        private static class CustomTestClass
        {

            @Override
            public String toString()
            {
                return "CustomToString";
            }
        }
    }

    // =========================================================================
    // Edge Cases and Special Tests
    // =========================================================================
    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCasesTests
    {

        @Test
        @DisplayName("Should handle maximum integer values")
        void shouldHandleMaxIntegerValues()
        {
            assertDoesNotThrow(()
                    -> Require.checkIndex(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        }

        @Test
        @DisplayName("Should handle maximum long values")
        void shouldHandleMaxLongValues()
        {
            assertDoesNotThrow(()
                    -> Require.checkIndex(Long.MAX_VALUE - 1, Long.MAX_VALUE));
        }

    }
}

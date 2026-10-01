/*
 * SparseMatrixTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.matrix;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SparseMatrix")
class SparseMatrixTest
{
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests
    {

        @Test
        @DisplayName("Accepts a valid dimension")
        void validDimension()
        {
            assertDoesNotThrow(() -> new SparseMatrix<>(1));
            assertDoesNotThrow(() -> new SparseMatrix<>(3));
        }

        @Test
        @DisplayName("Throws exception with zero dimension")
        void zeroDimensionThrows()
        {
            assertThrows(IllegalArgumentException.class, () -> new SparseMatrix<>(0));
        }

        @Test
        @DisplayName("Throws exception with negative dimension")
        void negativeDimensionThrows()
        {
            assertThrows(IllegalArgumentException.class, () -> new SparseMatrix<>(-1));
        }
    }

    // -------------------------------------------------------------------------
    // dimensions() y count() iniciales
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("dimensions() and count()")
    class DimensionsAndCountTests
    {

        @Test
        @DisplayName("dimensions() returns the value passed to the constructor")
        void dimensionsMatchConstructor()
        {
            assertEquals(1, new SparseMatrix<>(1).dimensions());
            assertEquals(3, new SparseMatrix<>(3).dimensions());
        }

        @Test
        @DisplayName("count() is 0 after construction")
        void initialCountIsZero()
        {
            assertEquals(0, new SparseMatrix<>(2).count());
        }
    }

    // -------------------------------------------------------------------------
    // set()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("set()")
    class SetTests
    {

        private SparseMatrix<String> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Returns null when inserting into an empty position")
        void setNewReturnsNull()
        {
            assertNull(matrix.set("A", 0, 0));
        }

        @Test
        @DisplayName("Returns the previous value when replacing")
        void setReplacedReturnsPrevious()
        {
            matrix.set("A", 1, 1);
            assertEquals("A", matrix.set("B", 1, 1));
        }

        @Test
        @DisplayName("Value is updated after replacement")
        void setReplaceUpdatesValue()
        {
            matrix.set("A", 1, 1);
            matrix.set("B", 1, 1);
            assertEquals("B", matrix.get(1, 1));
        }

        @Test
        @DisplayName("count() increases only when inserting a new cell")
        void setIncreasesCountOnlyForNewCell()
        {
            matrix.set("A", 0, 0);
            assertEquals(1, matrix.count());
            matrix.set("B", 0, 0);   // replacement, not a new cell
            assertEquals(1, matrix.count());
            matrix.set("C", 0, 1);
            assertEquals(2, matrix.count());
        }

        @Test
        @DisplayName("Throws exception if the number of indices is incorrect")
        void setWrongDimensionsThrows()
        {
            assertThrows(IllegalArgumentException.class, () -> matrix.set("X", 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.set("X", 0, 0, 0));
        }
    }

    // -------------------------------------------------------------------------
    // get()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("get()")
    class GetTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Returns null for an empty position")
        void getEmptyReturnsNull()
        {
            assertNull(matrix.get(0, 0));
        }

        @Test
        @DisplayName("Returns the stored value")
        void getReturnsStoredValue()
        {
            matrix.set(42, 3, 7);
            assertEquals(42, matrix.get(3, 7));
        }

        @Test
        @DisplayName("Different positions are independent")
        void getDistinctPositionsAreIndependent()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 0, 1);
            assertEquals(1, matrix.get(0, 0));
            assertEquals(2, matrix.get(0, 1));
            assertNull(matrix.get(1, 0));
        }

        @Test
        @DisplayName("Throws exception if the number of indices is incorrect")
        void getWrongDimensionsThrows()
        {
            assertThrows(IllegalArgumentException.class, () -> matrix.get(0));
            assertThrows(IllegalArgumentException.class, () -> matrix.get(0, 0, 0));
        }
    }

    // -------------------------------------------------------------------------
    // remove()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("remove()")
    class RemoveTests
    {

        private SparseMatrix<String> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Returns null when removing an empty position")
        void removeEmptyReturnsNull()
        {
            assertNull(matrix.remove(0, 0));
        }

        @Test
        @DisplayName("Returns the removed value")
        void removeReturnsRemovedValue()
        {
            matrix.set("X", 1, 2);
            assertEquals("X", matrix.remove(1, 2));
        }

        @Test
        @DisplayName("Position is empty after removal")
        void removeEmptiesPosition()
        {
            matrix.set("X", 1, 2);
            matrix.remove(1, 2);
            assertNull(matrix.get(1, 2));
        }

        @Test
        @DisplayName("count() decreases when removing an existing cell")
        void removeDecreasesCount()
        {
            matrix.set("A", 0, 0);
            matrix.set("B", 0, 1);
            matrix.remove(0, 0);
            assertEquals(1, matrix.count());
        }

        @Test
        @DisplayName("count() does not change when removing an empty cell")
        void removeEmptyCellDoesNotChangeCount()
        {
            matrix.set("A", 0, 0);
            matrix.remove(9, 9);
            assertEquals(1, matrix.count());
        }

        @Test
        @DisplayName("Throws exception if the number of indices is incorrect")
        void removeWrongDimensionsThrows()
        {
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(0));
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(0, 0, 0));
        }
    }

    // -------------------------------------------------------------------------
    // Matriz 3D
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("3D Matrix")
    class ThreeDimensionalTests
    {

        private SparseMatrix<Double> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(3);
        }

        @Test
        @DisplayName("Stores and retrieves values in 3 dimensions")
        void storeAndRetrieve3D()
        {
            matrix.set(1.0, 0, 0, 0);
            matrix.set(2.0, 1, 2, 3);
            matrix.set(3.0, 9, 9, 9);

            assertEquals(1.0, matrix.get(0, 0, 0));
            assertEquals(2.0, matrix.get(1, 2, 3));
            assertEquals(3.0, matrix.get(9, 9, 9));
            assertEquals(3, matrix.count());
        }

        @Test
        @DisplayName("Coordinates with the same hash but different values do not collide")
        void differentCoordsWithSameHashDoNotCollide()
        {
            // Arrays.hashCode is order-sensitive, but we verify explicitly
            matrix.set(10.0, 1, 2, 3);
            matrix.set(20.0, 3, 2, 1);

            assertEquals(10.0, matrix.get(1, 2, 3));
            assertEquals(20.0, matrix.get(3, 2, 1));
        }

        @Test
        @DisplayName("Throws exception with wrong number of indices")
        void wrongDimensionsThrows3D()
        {
            assertThrows(IllegalArgumentException.class, () -> matrix.set(1.0, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.get(0, 0));
            assertThrows(IllegalArgumentException.class, () -> matrix.remove(0, 0));
        }
    }

    // -------------------------------------------------------------------------
    // Generic types
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Generic types")
    class GenericTypeTests
    {

        @Test
        @DisplayName("Works with Integer")
        void worksWithInteger()
        {
            SparseMatrix<Integer> m = new SparseMatrix<>(1);
            m.set(99, 0);
            assertEquals(99, m.get(0));
        }

        @Test
        @DisplayName("Allows null as a stored value")
        void allowsNullValue()
        {
            SparseMatrix<String> m = new SparseMatrix<>(1);
            m.set("A", 0);
            assertEquals("A", m.set(null, 0));   // returns "A"
            assertEquals(1, m.count());
        }
    }

    // -------------------------------------------------------------------------
    // forEach()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("forEach()")
    class ForEachTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Does not invoke the action on an empty matrix")
        void emptyMatrixNeverInvokesAction()
        {
            matrix.forEach((coords, value) -> fail("Action should not be called on empty matrix"));
        }

        @Test
        @DisplayName("Visits every stored element exactly once")
        void visitsEveryElementOnce()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);
            matrix.set(30, 2, 2);

            int[] count =
            {
                0
            };
            matrix.forEach((coords, value) -> count[0]++);

            assertEquals(3, count[0]);
        }

        @Test
        @DisplayName("Receives the correct value for each cell")
        void receivesCorrectValues()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);

            Map<String, Integer> visited = new HashMap<>();
            matrix.forEach((coords, value)
                    -> visited.put(coords[0] + "," + coords[1], value));

            assertEquals(10, visited.get("0,0"));
            assertEquals(20, visited.get("1,1"));
        }

        @Test
        @DisplayName("Receives the correct coordinates for each cell")
        void receivesCorrectCoordinates()
        {
            matrix.set(99, 3, 7);

            int[][] captured = new int[1][];
            matrix.forEach((coords, value) -> captured[0] = coords);

            assertArrayEquals(new int[]
            {
                3, 7
            }, captured[0]);
        }

        @Test
        @DisplayName("Does not visit cells that have been removed")
        void doesNotVisitRemovedCells()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);
            matrix.remove(0, 0);

            int[] count =
            {
                0
            };
            matrix.forEach((coords, value) -> count[0]++);

            assertEquals(1, count[0]);
        }

        @Test
        @DisplayName("Accumulates values correctly with a lambda")
        void accumulatesValues()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);
            matrix.set(30, 2, 2);

            int[] sum =
            {
                0
            };
            matrix.forEach((coords, value) -> sum[0] += value);

            assertEquals(60, sum[0]);
        }
    }

    // -------------------------------------------------------------------------
    // replaceAll()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("replaceAll()")
    class ReplaceAllTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Has no effect on an empty matrix")
        void emptyMatrixNoEffect()
        {
            assertDoesNotThrow(() -> matrix.replaceAll(v -> v * 2));
            assertEquals(0, matrix.count());
        }

        @Test
        @DisplayName("Applies the operator to every stored value")
        void appliesOperatorToAllValues()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);
            matrix.set(30, 2, 2);

            matrix.replaceAll(v -> v * 2);

            assertEquals(20, matrix.get(0, 0));
            assertEquals(40, matrix.get(1, 1));
            assertEquals(60, matrix.get(2, 2));
        }

        @Test
        @DisplayName("Does not change count() after replacement")
        void doesNotChangeCount()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);

            matrix.replaceAll(v -> v + 1);

            assertEquals(2, matrix.count());
        }

        @Test
        @DisplayName("Identity operator leaves values unchanged")
        void identityOperatorLeavesValuesUnchanged()
        {
            matrix.set(42, 3, 7);
            matrix.replaceAll(v -> v);
            assertEquals(42, matrix.get(3, 7));
        }

        @Test
        @DisplayName("Operator can replace all values with a constant")
        void replaceAllWithConstant()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);
            matrix.set(3, 2, 2);

            matrix.replaceAll(v -> 0);

            assertEquals(0, matrix.get(0, 0));
            assertEquals(0, matrix.get(1, 1));
            assertEquals(0, matrix.get(2, 2));
        }

        @Test
        @DisplayName("Works with String transformation")
        void worksWithStringTransformation()
        {
            SparseMatrix<String> m = new SparseMatrix<>(2);
            m.set("hello", 0, 0);
            m.set("world", 1, 1);

            m.replaceAll(String::toUpperCase);

            assertEquals("HELLO", m.get(0, 0));
            assertEquals("WORLD", m.get(1, 1));
        }

        @Test
        @DisplayName("Chaining two replaceAll calls applies both operators")
        void chainingTwoReplaceAllCalls()
        {
            matrix.set(3, 0, 0);

            matrix.replaceAll(v -> v * 2);   // 3 → 6
            matrix.replaceAll(v -> v + 1);   // 6 → 7

            assertEquals(7, matrix.get(0, 0));
        }
    }
    
    // -------------------------------------------------------------------------
    // anyMatch()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("anyMatch()")
    class AnyMatchTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Returns false on an empty matrix")
        void emptyMatrixReturnsFalse()
        {
            assertFalse(matrix.anyMatch(v -> true));
        }

        @Test
        @DisplayName("Returns true when at least one element matches")
        void returnsTrueWhenOneMatches()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);
            matrix.set(3, 2, 2);

            assertTrue(matrix.anyMatch(v -> v > 2));
        }

        @Test
        @DisplayName("Returns false when no element matches")
        void returnsFalseWhenNoneMatches()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);

            assertFalse(matrix.anyMatch(v -> v > 10));
        }

        @Test
        @DisplayName("Returns true when all elements match")
        void returnsTrueWhenAllMatch()
        {
            matrix.set(5, 0, 0);
            matrix.set(6, 1, 1);

            assertTrue(matrix.anyMatch(v -> v > 0));
        }
    }

    // -------------------------------------------------------------------------
    // allMatch()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("allMatch()")
    class AllMatchTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Returns true on an empty matrix (vacuous truth)")
        void emptyMatrixReturnsTrue()
        {
            assertTrue(matrix.allMatch(v -> false));
        }

        @Test
        @DisplayName("Returns true when every element matches")
        void returnsTrueWhenAllMatch()
        {
            matrix.set(2, 0, 0);
            matrix.set(4, 1, 1);
            matrix.set(6, 2, 2);

            assertTrue(matrix.allMatch(v -> v % 2 == 0));
        }

        @Test
        @DisplayName("Returns false when at least one element does not match")
        void returnsFalseWhenOneDoesNotMatch()
        {
            matrix.set(2, 0, 0);
            matrix.set(3, 1, 1);   // odd
            matrix.set(4, 2, 2);

            assertFalse(matrix.allMatch(v -> v % 2 == 0));
        }

        @Test
        @DisplayName("Returns false when no element matches")
        void returnsFalseWhenNoneMatch()
        {
            matrix.set(1, 0, 0);
            matrix.set(3, 1, 1);

            assertFalse(matrix.allMatch(v -> v % 2 == 0));
        }
    }

    // -------------------------------------------------------------------------
    // findFirst()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("findFirst()")
    class FindFirstTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Returns empty Optional on an empty matrix")
        void emptyMatrixReturnsEmptyOptional()
        {
            assertEquals(Optional.empty(), matrix.findFirst(v -> true));
        }

        @Test
        @DisplayName("Returns empty Optional when no element matches")
        void returnsEmptyWhenNoMatch()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);

            assertEquals(Optional.empty(), matrix.findFirst(v -> v > 10));
        }

        @Test
        @DisplayName("Returns a non-empty Optional when a match exists")
        void returnsNonEmptyWhenMatchExists()
        {
            matrix.set(1, 0, 0);
            matrix.set(5, 1, 1);

            Optional<Integer> result = matrix.findFirst(v -> v > 3);

            assertTrue(result.isPresent());
            assertEquals(5, result.get());
        }

        @Test
        @DisplayName("Returned value satisfies the predicate")
        void returnedValueSatisfiesPredicate()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);
            matrix.set(30, 2, 2);

            Optional<Integer> result = matrix.findFirst(v -> v >= 20);

            assertTrue(result.isPresent());
            assertTrue(result.get() >= 20);
        }

        @Test
        @DisplayName("Works with String predicate")
        void worksWithStringPredicate()
        {
            SparseMatrix<String> m = new SparseMatrix<>(1);
            m.set("alpha", 0);
            m.set("beta", 1);

            Optional<String> result = m.findFirst(s -> s.startsWith("b"));

            assertTrue(result.isPresent());
            assertEquals("beta", result.get());
        }
    }

    // -------------------------------------------------------------------------
    // removeIf()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("removeIf()")
    class RemoveIfTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Has no effect on an empty matrix")
        void emptyMatrixNoEffect()
        {
            assertDoesNotThrow(() -> matrix.removeIf(v -> true));
            assertEquals(0, matrix.count());
        }

        @Test
        @DisplayName("Removes only the elements that match")
        void removesOnlyMatchingElements()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);
            matrix.set(3, 2, 2);
            matrix.set(4, 3, 3);

            matrix.removeIf(v -> v % 2 == 0);   // remove even values

            assertEquals(2, matrix.count());
            assertNull(matrix.get(1, 1));
            assertNull(matrix.get(3, 3));
            assertEquals(1, matrix.get(0, 0));
            assertEquals(3, matrix.get(2, 2));
        }

        @Test
        @DisplayName("Removes all elements when all match")
        void removesAllWhenAllMatch()
        {
            matrix.set(2, 0, 0);
            matrix.set(4, 1, 1);

            matrix.removeIf(v -> v > 0);

            assertEquals(0, matrix.count());
        }

        @Test
        @DisplayName("Removes no elements when none match")
        void removesNoneWhenNoneMatch()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);

            matrix.removeIf(v -> v > 100);

            assertEquals(2, matrix.count());
        }

        @Test
        @DisplayName("Removed cells return null on get()")
        void removedCellsReturnNull()
        {
            matrix.set(99, 0, 0);
            matrix.removeIf(v -> v == 99);

            assertNull(matrix.get(0, 0));
        }
    }

    // -------------------------------------------------------------------------
    // clear()
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("clear()")
    class ClearTests
    {

        private SparseMatrix<Integer> matrix;

        @BeforeEach
        void setUp()
        {
            matrix = new SparseMatrix<>(2);
        }

        @Test
        @DisplayName("Has no effect on an empty matrix")
        void clearEmptyMatrixNoEffect()
        {
            assertDoesNotThrow(() -> matrix.clear());
            assertEquals(0, matrix.count());
        }

        @Test
        @DisplayName("count() is 0 after clear()")
        void countIsZeroAfterClear()
        {
            matrix.set(1, 0, 0);
            matrix.set(2, 1, 1);
            matrix.set(3, 2, 2);

            matrix.clear();

            assertEquals(0, matrix.count());
        }

        @Test
        @DisplayName("All previously stored cells return null after clear()")
        void allCellsReturnNullAfterClear()
        {
            matrix.set(10, 0, 0);
            matrix.set(20, 1, 1);

            matrix.clear();

            assertNull(matrix.get(0, 0));
            assertNull(matrix.get(1, 1));
        }

        @Test
        @DisplayName("Matrix is usable again after clear()")
        void matrixIsUsableAfterClear()
        {
            matrix.set(1, 0, 0);
            matrix.clear();

            matrix.set(42, 0, 0);

            assertEquals(1, matrix.count());
            assertEquals(42, matrix.get(0, 0));
        }

        @Test
        @DisplayName("Calling clear() twice is safe")
        void doubleClearIsSafe()
        {
            matrix.set(1, 0, 0);
            matrix.clear();

            assertDoesNotThrow(() -> matrix.clear());
            assertEquals(0, matrix.count());
        }
    }
    
}
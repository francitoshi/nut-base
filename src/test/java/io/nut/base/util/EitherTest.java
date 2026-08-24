/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.Optional;

public class EitherTest
{

    @Test
    public void left_creates_a_left_instance()
    {
        Either<String, String> either = Either.left("42");
        assertTrue(either.isLeft());
        assertFalse(either.isRight());
        assertEquals("42", either.getLeft());
        assertThrows(NoSuchElementException.class, either::getRight);
    }

    @Test
    public void right_creates_a_right_instance()
    {
        Either<String, String> either = Either.right("hello");
        assertFalse(either.isLeft());
        assertTrue(either.isRight());
        assertEquals("hello", either.getRight());
        assertThrows(NoSuchElementException.class, either::getLeft);
    }

    @Test
    public void left_with_null_throws_npe()
    {
        assertThrows(NullPointerException.class, () -> Either.left((String) null));
    }

    @Test
    public void right_with_null_throws_npe()
    {
        assertThrows(NullPointerException.class, () -> Either.right((String) null));
    }

    @Test
    public void is_left_returns_true_for_left_and_false_for_right()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("a");

        assertTrue(left.isLeft());
        assertFalse(left.isRight());
        assertFalse(right.isLeft());
        assertTrue(right.isRight());
    }

    @Test
    public void get_left_returns_the_value_on_left_and_throws_on_right()
    {
        Either<String, String> left = Either.left("10");
        Either<String, String> right = Either.right("2");

        assertEquals("10", left.getLeft());

        assertThrows(NoSuchElementException.class, right::getLeft);
    }

    @Test
    public void get_right_returns_the_value_on_right_and_throws_on_left()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("20");

        assertEquals("20", right.getRight());

        assertThrows(NoSuchElementException.class, left::getRight);
    }

    @Test
    public void get_left_optional_returns_some_on_left_and_empty_on_right()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("2");

        assertEquals(Optional.of("1"), left.getLeftOptional());
        assertEquals(Optional.empty(), right.getLeftOptional());
    }

    @Test
    public void get_right_optional_returns_some_on_right_and_empty_on_left()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("2");

        assertEquals(Optional.empty(), left.getRightOptional());
        assertEquals(Optional.of("2"), right.getRightOptional());
    }

    @Test
    public void fold_applies_the_correct_mapper()
    {
        Either<String, String> left = Either.left("10");
        Either<String, String> right = Either.right("20");

        String resultLeft = left.fold(
                l -> l + " doubled",
                r -> r + " + 100"
        );
        assertEquals("10 doubled", resultLeft);

        String resultRight = right.fold(
                l -> l + " doubled",
                r -> r + " + 100"
        );
        assertEquals("20 + 100", resultRight);
    }

    @Test
    public void map_transforms_the_right_value_and_preserves_left()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("2");

        Either<String, String> mappedLeft = left.map(r -> r + " mapped");
        assertTrue(mappedLeft.isLeft());
        assertEquals("1", mappedLeft.getLeft());

        Either<String, String> mappedRight = right.map(r -> r + " mapped");
        assertTrue(mappedRight.isRight());
        assertEquals("2 mapped", mappedRight.getRight());
    }

    @Test
    public void map_left_transforms_the_left_value_and_preserves_right()
    {
        Either<String, String> left = Either.left("2");

        Either<String, String> mappedLeft = left.mapLeft(l -> l + "L");
        assertTrue(mappedLeft.isLeft(), "mapLeft debe devolver un Left");
        assertEquals("2L", mappedLeft.getLeft());
    }

    @Test
    public void flat_map_on_right_applies_the_mapper()
    {
        Either<String, String> right = Either.right("2");

        Either<String, String> result = right.flatMap(r -> Either.right(r + " flat"));
        assertTrue(result.isRight());
        assertEquals("2 flat", result.getRight());
    }

    @Test
    public void flat_map_on_left_returns_unchanged()
    {
        Either<String, String> left = Either.left("1");

        Either<String, String> result = left.flatMap(r -> Either.right(r + " flat"));
        assertTrue(result.isLeft());
        assertEquals("1", result.getLeft());
    }

    @Test
    public void peek_invokes_the_correct_consumer()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("2");

        java.util.List<String> leftValues = new java.util.ArrayList<>();
        java.util.List<String> rightValues = new java.util.ArrayList<>();

        left.peek(
                l -> leftValues.add(l),
                r -> rightValues.add(r)
        );
        assertEquals(As.list("1"), leftValues);

        right.peek(
                l -> leftValues.add(l),
                r -> rightValues.add(r)
        );
        assertEquals(As.list("2"), rightValues);
    }

    @Test
    public void get_or_else_returns_default_on_left_and_value_on_right()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("2");

        assertEquals("10", left.getOrElse("10"));
        assertEquals("2", right.getOrElse("10"));
    }

    @Test
    public void get_or_else_get_returns_fallback_on_left_and_value_on_right()
    {
        Either<String, String> left = Either.left("1");
        Either<String, String> right = Either.right("2");

        assertEquals("10", left.getOrElseGet(l -> "10"));
        assertEquals("2", right.getOrElseGet(l -> "10"));
    }

    @Test
    public void equality_is_based_on_value_and_type()
    {
        Either<String, String> left1 = Either.left("1");
        Either<String, String> left2 = Either.left("1");
        Either<String, String> left3 = Either.left("2");
        Either<String, String> right = Either.right("1");

        assertEquals(left1, left2);
        assertNotEquals(left1, left3);
        assertNotEquals(left1, right);
    }

    @Test
    public void hash_code_is_consistent_with_equality()
    {
        Either<String, String> left1 = Either.left("1");
        Either<String, String> left2 = Either.left("1");

        assertEquals(left1.hashCode(), left2.hashCode());
    }

    @Test
    public void to_string_format()
    {
        Either<String, String> left = Either.left("42");
        Either<String, String> right = Either.right("answer");

        assertEquals("Left(42)", left.toString());
        assertEquals("Right(answer)", right.toString());
    }
}
/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A functional container representing a value of one of two possible types. By
 * convention, {@code Left} represents failure/error and {@code Right}
 * represents success.
 *
 * @param <L> Type of the Left (Error) value
 * @param <R> Type of the Right (Success) value
 */
public abstract class Either<L,R>
{
    private Either()
    {
    }

    /**
     * Creates a {@code Left} holding the given (failure) value.
     *
     * @param value the Left value; must not be {@code null}
     * @param <L>   the type of the Left value
     * @param <R>   the type of the Right value
     * @return an {@code Either} that is a Left
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <L,R> Either<L,R> left(L value)
    {
        return new Left<>(Objects.requireNonNull(value, "Left value cannot be null"));
    }

    /**
     * Creates a {@code Right} holding the given (success) value.
     *
     * @param value the Right value; must not be {@code null}
     * @param <L>   the type of the Left value
     * @param <R>   the type of the Right value
     * @return an {@code Either} that is a Right
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <L,R> Either<L,R> right(R value)
    {
        return new Right<>(Objects.requireNonNull(value, "Right value cannot be null"));
    }

    /**
     * Returns {@code true} if this is a Left (failure).
     *
     * @return {@code true} if this instance holds a Left value
     */
    public abstract boolean isLeft();

    /**
     * Returns {@code true} if this is a Right (success).
     *
     * @return {@code true} if this instance holds a Right value
     */
    public abstract boolean isRight();

    /**
     * Returns the Left value.
     *
     * @return the Left value held by this instance
     * @throws NoSuchElementException if this is a Right
     */
    public abstract L getLeft();

    /**
     * Returns the Right value.
     *
     * @return the Right value held by this instance
     * @throws NoSuchElementException if this is a Left
     */
    public abstract R getRight();

    /**
     * Returns the Left value as an {@link Optional}.
     *
     * @return an Optional containing the Left value if this is a Left,
     *         otherwise {@link Optional#empty()}
     */
    public abstract Optional<L> getLeftOptional();

    /**
     * Returns the Right value as an {@link Optional}.
     *
     * @return an Optional containing the Right value if this is a Right,
     *         otherwise {@link Optional#empty()}
     */
    public abstract Optional<R> getRightOptional();

    /**
     * Applies {@code leftMapper} if this is a Left, or {@code rightMapper} if
     * this is a Right.
     *
     * <p>This collapses both branches into a single result type and is the
     * idiomatic way to extract a value from an {@code Either}.
     *
     * @param leftMapper  function applied to the Left value if present
     * @param rightMapper function applied to the Right value if present
     * @param <T>         the result type shared by both mappings
     * @return the result of applying the corresponding mapper
     */
    public abstract <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper);

    /**
     * Transforms the Right value if present.
     *
     * <p>If this is a Left, the mapper is not invoked and this instance is
     * returned unchanged (as {@code Either<L,T>}).
     *
     * @param mapper function applied to the Right value
     * @param <T>    the type of the mapped Right value
     * @return a Right holding the mapped value, or the original Left
     */
    public abstract <T> Either<L,T> map(Function<? super R, ? extends T> mapper);

    /**
     * Transforms the Left value if present.
     *
     * <p>If this is a Right, the mapper is not invoked and this instance is
     * returned unchanged (as {@code Either<T,R>}). Useful for converting an
     * error into another error representation while propagating successes.
     *
     * @param mapper function applied to the Left value
     * @param <T>    the type of the mapped Left value
     * @return a Left holding the mapped value, or the original Right
     */
    public abstract <T> Either<T,R> mapLeft(Function<? super L, ? extends T> mapper);

    /**
     * Chains another Either-returning operation on the Right value
     * (a.k.a. monadic bind).
     *
     * <p>If this is a Right, {@code mapper} is applied to the Right value and
     * its result is returned. If this is a Left, the mapper is not invoked and
     * this instance is returned unchanged. This allows sequencing operations
     * that may themselves fail, short-circuiting on the first Left.
     *
     * @param mapper function applied to the Right value, returning a new Either
     * @param <T>    the type of the resulting Right value
     * @return the Either produced by the mapper, or the original Left
     */
    public abstract <T> Either<L,T> flatMap(Function<? super R, Either<L,T>> mapper);

    /**
     * Executes side-effects based on whether this is a Left or a Right.
     *
     * <p>Exactly one of the two consumers is invoked; the other is ignored.
     * The value itself is not modified.
     *
     * @param leftConsumer  consumer invoked with the Left value if this is a Left
     * @param rightConsumer consumer invoked with the Right value if this is a Right
     */
    public abstract void peek(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer);

    /**
     * Returns the Right value if this is a Right, otherwise the given default.
     *
     * @param defaultValue value returned when this is a Left; may be {@code null}
     * @return the Right value, or {@code defaultValue}
     */
    public abstract R getOrElse(R defaultValue);

    /**
     * Returns the Right value if this is a Right, otherwise computes a
     * fallback from the Left value.
     *
     * @param fallbackFunction function applied to the Left value to produce a
     *                         replacement Right value
     * @return the Right value, or the result of applying {@code fallbackFunction}
     *         to the Left value
     */
    public abstract R getOrElseGet(Function<? super L, ? extends R> fallbackFunction);

    // ==========================================
    // Left Implementation (Failure)
    // ==========================================
    private static final class Left<L,R> extends Either<L,R>
    {

        private final L value;

        private Left(L value)
        {
            this.value = value;
        }

        @Override
        public boolean isLeft()
        {
            return true;
        }

        @Override
        public boolean isRight()
        {
            return false;
        }

        @Override
        public L getLeft()
        {
            return value;
        }

        @Override
        public R getRight()
        {
            throw new NoSuchElementException("Called getRight() on a Left value");
        }

        @Override
        public Optional<L> getLeftOptional()
        {
            return Optional.of(value);
        }

        @Override
        public Optional<R> getRightOptional()
        {
            return Optional.empty();
        }

        @Override
        public <T> T fold(Function<? super L, ? extends T> leftMapper,Function<? super R, ? extends T> rightMapper)
        {
            return leftMapper.apply(value);
        }

        @Override
        public <T> Either<L,T> map(Function<? super R, ? extends T> mapper)
        {
            return Either.left(this.value);
        }

        @Override
        public <T> Either<T,R> mapLeft(Function<? super L, ? extends T> mapper)
        {
            return Either.left(mapper.apply(this.value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Either<L,T> flatMap(Function<? super R, Either<L,T>> mapper)
        {
            return (Either<L,T>) this;
        }

        @Override
        public void peek(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer)
        {
            leftConsumer.accept(value);
        }

        @Override
        public R getOrElse(R defaultValue)
        {
            return defaultValue;
        }

        @Override
        public R getOrElseGet(Function<? super L, ? extends R> fallbackFunction)
        {
            return fallbackFunction.apply(value);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Left))
            {
                return false;
            }
            Left<?, ?> left = (Left<?, ?>) o;
            return Objects.equals(value, left.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(value);
        }

        @Override
        public String toString()
        {
            return "Left(" + value + ")";
        }
    }

    // ==========================================
    // Right Implementation (Success)
    // ==========================================
    private static final class Right<L,R> extends Either<L,R>
    {

        private final R value;

        private Right(R value)
        {
            this.value = value;
        }

        @Override
        public boolean isLeft()
        {
            return false;
        }

        @Override
        public boolean isRight()
        {
            return true;
        }

        @Override
        public L getLeft()
        {
            throw new NoSuchElementException("Called getLeft() on a Right value");
        }

        @Override
        public R getRight()
        {
            return value;
        }

        @Override
        public Optional<L> getLeftOptional()
        {
            return Optional.empty();
        }

        @Override
        public Optional<R> getRightOptional()
        {
            return Optional.of(value);
        }

        @Override
        public <T> T fold(Function<? super L, ? extends T> leftMapper,
                Function<? super R, ? extends T> rightMapper)
        {
            return rightMapper.apply(value);
        }

        @Override
        public <T> Either<L,T> map(Function<? super R, ? extends T> mapper)
        {
            return Either.right(mapper.apply(value));
        }

        @Override
        public <T> Either<T,R> mapLeft(Function<? super L, ? extends T> mapper)
        {
            return Either.right(this.value);
        }

        @Override
        public <T> Either<L,T> flatMap(Function<? super R, Either<L,T>> mapper)
        {
            return mapper.apply(value);
        }

        @Override
        public void peek(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer)
        {
            rightConsumer.accept(value);
        }

        @Override
        public R getOrElse(R defaultValue)
        {
            return value;
        }

        @Override
        public R getOrElseGet(Function<? super L, ? extends R> fallbackFunction)
        {
            return value;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Right))
            {
                return false;
            }
            Right<?, ?> right = (Right<?, ?>) o;
            return Objects.equals(value, right.value);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(value);
        }

        @Override
        public String toString()
        {
            return "Right(" + value + ")";
        }
    }
}

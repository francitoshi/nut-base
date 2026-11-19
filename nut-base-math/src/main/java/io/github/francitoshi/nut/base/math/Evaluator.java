/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import java.util.Objects;
import java.util.Set;

/**
 * Common interface for expression evaluators. It allows different
 * implementations, such as {@link ExpressionEvaluator} or {@link MathEvaluator}, to be used
 * interchangeably through the same API.
 *
 * @author franci
 */
public interface Evaluator
{
    /**
     * Evaluates the given expression and returns its result.
     *
     * @param expression the expression to evaluate
     * @return the result of the expression; errors are reported in the
     *         result, not thrown
     * @throws NullPointerException if {@code expression} is {@code null}
     */
    Result eval(String expression);

    /**
     * The kind of error reported by an evaluation.
     */
    enum ErrorType
    {
        NONE,
        DIVISION_BY_ZERO,
        INCOMPLETE_EXPRESSION,
        UNCLOSED_PARENTHESIS,
        UNEXPECTED_CLOSING_PARENTHESIS,
        UNDEFINED_VARIABLE,
        UNEXPECTED_TOKEN,
        INVALID_CHARACTER,
        INVALID_ASSIGNMENT,
        OVERFLOW
    }

    /**
     * The immutable result of an evaluation. On success it carries the value,
     * the interpretation of the expression and the variables it modifies and
     * depends on. On error the value is {@code null} and the error type,
     * message and position describe the problem.
     */
    final class Result
    {
        /** The value of the expression; {@code null} on error or empty line. */
        public final Object value;
        /** How the expression was interpreted, e.g. {@code "1 / (2 * x)"}; may be partial or {@code null} on error. */
        public final String interpretation;
        /** The variable whose value the expression modifies, or {@code null} if it modifies none. */
        public final String modifiedVariable;
        /** The variables the expression depends on, or {@code null} if it depends on none. */
        public final Set<String> dependencies;
        /** The error kind; never {@code null}. */
        public final ErrorType error;
        /** The error message, or {@code null} if there is no error. */
        public final String message;
        /** The index of the offending character, or {@code -1} if not applicable. */
        public final int position;

        /**
         * Constructs a result without variable dependency information, for
         * evaluators that do not track the modified variable or the
         * dependencies.
         *
         * @param value            the value, or {@code null}
         * @param interpretation   the interpretation, or {@code null}
         * @param modifiedVariable the modified variable name, or {@code null}
         * @param error            the error kind; never {@code null}
         * @param message          the error message, or {@code null}
         * @param position         the offending character index, or {@code -1}
         */
        public Result(Object value, String interpretation, String modifiedVariable, ErrorType error, String message, int position)
        {
            this(value, interpretation, modifiedVariable, null, error, message, position);
        }

        /**
         * Constructs a result.
         *
         * @param value            the value, or {@code null}
         * @param interpretation   the interpretation, or {@code null}
         * @param modifiedVariable the modified variable name, or {@code null}
         * @param dependencies     the variables the expression depends on, or {@code null}
         * @param error            the error kind; never {@code null}
         * @param message          the error message, or {@code null}
         * @param position         the offending character index, or {@code -1}
         */
        public Result(Object value, String interpretation, String modifiedVariable,
                Set<String> dependencies,
                ErrorType error, String message, int position)
        {
            this.value = value;
            this.interpretation = interpretation;
            this.modifiedVariable = modifiedVariable;
            this.dependencies = dependencies;
            this.error = Objects.requireNonNull(error, "error");
            this.message = message;
            this.position = position;
        }

        /**
         * Returns whether the evaluation finished with an error.
         *
         * @return {@code true} if there is an error
         */
        public boolean isError()
        {
            return error != ErrorType.NONE;
        }

        @Override
        public String toString()
        {
            if (error != ErrorType.NONE)
            {
                return error + " at " + position + ": " + message;
            }
            return String.valueOf(value);
        }
    }
}

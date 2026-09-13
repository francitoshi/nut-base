/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helpers for the common "log a throwable and continue or rethrow" boilerplate.
 *
 * @author franci
 */
public final class Exceptions
{
    private Exceptions()
    {
    }

    /**
     * Logs the throwable at {@link Level#SEVERE} level and continues.
     *
     * @param logger the logger to use
     * @param cause  the throwable to log
     */
    public static void severe(Logger logger, Throwable cause)
    {
        logger.log(Level.SEVERE, null, cause);
    }

    /**
     * Logs the message and throwable at {@link Level#SEVERE} level and continues.
     *
     * @param logger  the logger to use
     * @param message the log message
     * @param cause   the throwable to log
     */
    public static void severe(Logger logger, String message, Throwable cause)
    {
        logger.log(Level.SEVERE, message, cause);
    }

    /**
     * Logs the throwable at {@link Level#SEVERE} level and returns a new
     * {@link RuntimeException} wrapping it, ready to be thrown.
     *
     * @param logger the logger to use
     * @param cause  the throwable to log and wrap
     * @return a {@link RuntimeException} wrapping {@code cause}
     */
    public static RuntimeException rethrow(Logger logger, Throwable cause)
    {
        severe(logger, cause);
        return new RuntimeException(cause);
    }

    /**
     * Logs the message and throwable at {@link Level#SEVERE} level and returns a
     * new {@link RuntimeException} wrapping it, ready to be thrown.
     *
     * @param logger  the logger to use
     * @param message the log message
     * @param cause   the throwable to log and wrap
     * @return a {@link RuntimeException} wrapping {@code cause}
     */
    public static RuntimeException rethrow(Logger logger, String message, Throwable cause)
    {
        severe(logger, message, cause);
        return new RuntimeException(message, cause);
    }
}
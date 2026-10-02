/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

/**
 * The tests of the backend that uses the providers of the running JDK.
 * <p>
 * This is the backend {@link Kr#getInstance()} returns, the one to use when
 * Bouncy Castle is not wanted. It is also the one whose capabilities depend on
 * the Java version, so many of its tests are skipped on Java 8.
 *
 * @author franci
 * @see KrJdk
 */
public class KrJdkTest extends KrTest
{
    @Override
    protected Kr kr()
    {
        return Kr.getInstance(false);
    }
}
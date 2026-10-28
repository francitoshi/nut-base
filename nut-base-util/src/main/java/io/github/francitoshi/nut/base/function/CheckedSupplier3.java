/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.function;

@FunctionalInterface
public interface CheckedSupplier3<T, E1 extends Exception, E2 extends Exception, E3 extends Exception>
{
    T get() throws E1, E2, E3;
}

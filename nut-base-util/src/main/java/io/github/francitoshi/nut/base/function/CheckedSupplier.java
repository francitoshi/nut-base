/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.function;

@FunctionalInterface
public interface CheckedSupplier<T,E extends Exception>
{
    T get() throws E;
}

/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import java.awt.Dimension;

/**
 * Strategy that computes the target dimension for a source image size.
 *
 * <p>Implementations decide how a {@link Dimension} is transformed: for
 * example, fitting it into bounds with {@link SimpleScaleDimension} or
 * {@link FixedRatiosScaleDimension}, forcing a fixed size with
 * {@link FixedScaleDimension}, or returning it unchanged with
 * {@link NullScaleDimension}.</p>
 *
 * @author franci
 */
public interface ScaleDimension
{
    /**
     * Computes the scaled dimension for the supplied source size.
     *
     * @param size the source image size, must not be {@code null}
     * @return the target dimension, or the source size when no scaling applies
     */
    Dimension convert(Dimension size);
}

/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import java.awt.Dimension;

/**
 * Scaling strategy that ignores the source size and always returns the same
 * fixed dimension.
 *
 * @author franci
 */
public class FixedScaleDimension implements ScaleDimension
{
    final int width;
    final int height;

    /**
     * Creates a strategy that always yields the supplied width and height.
     *
     * @param width  the target width in pixels
     * @param height the target height in pixels
     */
    public FixedScaleDimension(int width, int height)
    {
        this.width = width;
        this.height = height;
    }
    /**
     * Creates a strategy that always yields a square dimension of the supplied size.
     *
     * @param size the target width and height in pixels
     */
    public FixedScaleDimension(int size)
    {
        this.width  = size;
        this.height = size;
    }

    /**
     * Returns the fixed target dimension, ignoring the source size.
     *
     * @param size the source image size, not used
     * @return a new {@link Dimension} with the configured width and height
     */
    @Override
    public Dimension convert(Dimension size)
    {
        return new Dimension(width,height);
    }
}

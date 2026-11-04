/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import java.awt.Dimension;

/**
 * Scaling strategy that resizes an image by one of a fixed set of downscale
 * ratios ({@code 1.0, 0.9, 0.8, ...}) until the result fits within the
 * {@code min} and {@code max} bounds.
 *
 * <p>If no preset ratio produces a dimension within the bounds, the original
 * size is returned unchanged.</p>
 *
 * @author franci
 */
public class FixedRatiosScaleDimension implements ScaleDimension
{
    private final int max;
    private final int min;
    private static final double[] RATIOS = { 1.0, 0.9, 0.8, 0.75, 2.0/3.0, 0.6, 0.5, 0.4, 1.0/3.0, 0.25, 0.2, 0.1, 0.05, 0.025, 0.0125, 0.01};
    private final double[] ratios;

    /**
     * Creates a scaling strategy limited to the supplied bounds.
     *
     * @param max the maximum allowed width and height in pixels, or {@code 0}
     *            for no maximum
     * @param min the minimum allowed width and height in pixels, or {@code 0}
     *            for no minimum
     */
    public FixedRatiosScaleDimension(int max, int min)
    {
        this.max = max;
        this.min = min;
        this.ratios = RATIOS;
    }

    /**
     * Scales the size by the first preset ratio that fits within the bounds.
     *
     * @param size the source image size
     * @return the resulting dimension, or the supplied {@code size} instance
     *         itself when no preset ratio fits within the bounds
     */
    @Override
    public Dimension convert(Dimension size)
    {
        for( double r : ratios)
        {
            Dimension dim = convert(size, r);
            if(dim!=null)
                return dim;
        }
        return size;
    }
    /**
     * Scales the size by the supplied ratio and checks it against the bounds.
     *
     * @param size  the source image size
     * @param ratio the scale factor to apply
     * @return the scaled dimension, {@code null} when the result falls outside
     *         the bounds, or the supplied {@code size} instance itself when
     *         {@code ratio} equals {@code 1.0} and the size fits the bounds
     */
    public Dimension convert(Dimension size,double ratio)
    {
        final int w= (int)Math.round(size.width*ratio);
        final int h= (int)Math.round(size.height*ratio);

        if(min>0)
        {
            if(w<min || h<min)
            {
                return null;
            }
        }
        if(max>0)
        {
            if(w>max || h>max)
            {
                return null;
            }
        }
        if(ratio==1.0)
            return size;

        return new Dimension(w,h);
    }

}

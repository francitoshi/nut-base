/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import java.awt.Dimension;

/**
 * Scaling strategy that grows or shrinks an image so that its dimensions lie
 * within {@code minWidth/minHeight} and {@code maxWidth/maxHeight}, preserving
 * the aspect ratio.
 *
 * <p>The initial scale factor is {@code 1.0}, optionally seeded by the
 * {@code ratio} argument. It is then increased until the result meets the
 * minimum bounds, and decreased until it meets the maximum bounds, so the
 * image is never skewed.</p>
 *
 * @author franci
 */
public class SimpleScaleDimension implements ScaleDimension
{
    final int minWidth;
    final int minHeight;
    final int maxWidth;
    final int maxHeight;
    final double ratio;

    /**
     * Creates a scaling strategy with explicit per-axis bounds.
     *
     * @param minWidth  the minimum allowed width in pixels
     * @param minHeight the minimum allowed height in pixels
     * @param maxWidth  the maximum allowed width in pixels
     * @param maxHeight the maximum allowed height in pixels
     * @param ratio     the initial scale factor, {@code 1.0} for no bias
     */
    public SimpleScaleDimension(int minWidth, int minHeight, int maxWidth, int maxHeight, double ratio)
    {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.ratio = ratio;
    }

    /**
     * Creates a square-bounded scaling strategy with the same minimum and
     * maximum size on both axes.
     *
     * @param minSize the minimum allowed width and height in pixels
     * @param maxSize the maximum allowed width and height in pixels
     * @param ratio   the initial scale factor, {@code 1.0} for no bias
     */
    public SimpleScaleDimension(int minSize, int maxSize, double ratio)
    {
        this(minSize,minSize,maxSize,maxSize,ratio);
    }
    /**
     * Creates a square-bounded scaling strategy with no scale bias; the image
     * is grown or shrunk to the largest size that fits the square bounds.
     *
     * @param minSize the minimum allowed width and height in pixels
     * @param maxSize the maximum allowed width and height in pixels
     */
    public SimpleScaleDimension(int minSize, int maxSize)
    {
        this(minSize,minSize,maxSize,maxSize,1.0);
    }

    /**
     * Computes the largest (or smallest) dimension that satisfies the bounds
     * while keeping the aspect ratio of {@code size}.
     *
     * @param size the source image size
     * @return the resulting dimension; the supplied {@code size} instance
     *         itself when no scaling is needed
     */
    @Override
    public Dimension convert(Dimension size)
    {
        double minWidthRatio = (double)minWidth / (double)size.width;
        double minHeightRatio = (double)minHeight / (double)size.height;

        double maxWidthRatio = (double)maxWidth / (double)size.width;
        double maxHeightRatio = (double)maxHeight / (double)size.height;
        
        double ratio  = this.ratio;
        
        //grow
        if(minWidthRatio>ratio)
            ratio = minWidthRatio;
        if(minHeightRatio>ratio)
            ratio = minHeightRatio;

        //shrink
        if(maxWidthRatio<ratio)
            ratio = maxWidthRatio;
        if(maxHeightRatio<ratio)
            ratio = maxHeightRatio;
        
        if(ratio==1.0)
            return size;

        final int w= (int)Math.round(size.width*ratio);
        final int h= (int)Math.round(size.height*ratio);
        return new Dimension(w,h);
    }

}

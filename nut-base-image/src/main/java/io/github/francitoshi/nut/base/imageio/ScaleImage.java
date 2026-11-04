/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.imageio;

import io.github.francitoshi.nut.base.image.ScaleDimension;
import io.github.francitoshi.nut.base.image.SimpleScaleDimension;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 * Scales images and optionally converts them to grayscale, using the standard
 * {@code javax.imageio} read/write pipeline.
 *
 * <p>The target size is computed by a {@link ScaleDimension} strategy. If the
 * strategy returns the original size and no grayscale conversion was requested,
 * the image is left untouched.</p>
 *
 * <p>An instance configured with no scaling strategy and no grayscale
 * conversion is a no-op: {@link #filter(InputStream, String)} returns the
 * original stream unchanged instead of decoding and re-encoding it.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * ScaleImage scaler = new ScaleImage(1200, 1200);
 * scaler.filter(new File("in.jpg"), new File("out.jpg"), "jpeg");
 * }</pre>
 *
 * @author franci
 */
public class ScaleImage
{
    private final ScaleDimension scale;
    private final boolean gray;
    private final boolean nop;

    /**
     * Creates a scaler driven by the supplied {@link ScaleDimension} strategy.
     *
     * @param scale the scaling strategy, or {@code null} to keep the original size
     * @param gray  {@code true} to convert the result to grayscale
     */
    public ScaleImage(ScaleDimension scale, boolean gray)
    {
        this.scale = scale;
        this.gray  = gray;
        this.nop   = (scale==null) && !gray;
    }

    /**
     * Creates a color scaler driven by the supplied {@link ScaleDimension} strategy.
     *
     * @param scale the scaling strategy, or {@code null} to keep the original size
     */
    public ScaleImage(ScaleDimension scale)
    {
        this(scale,false);
    }

    /**
     * Creates a grayscale scaler that keeps the original size.
     *
     * @param gray {@code true} to convert the result to grayscale
     */
    public ScaleImage(boolean gray)
    {
        this(null,gray);
    }
    /**
     * Creates a scaler that fits images into a {@code maxWidth} x {@code maxHeight}
     * rectangle, preserving the aspect ratio.
     *
     * @param maxWidth  the maximum allowed width in pixels
     * @param maxHeight the maximum allowed height in pixels
     * @param grayscale {@code true} to convert the result to grayscale
     */
    public ScaleImage(int maxWidth, int maxHeight, boolean grayscale)
    {
        this(new SimpleScaleDimension(maxWidth,maxHeight),grayscale);
    }
    /**
     * Creates a color scaler that fits images into a {@code maxWidth} x
     * {@code maxHeight} rectangle, preserving the aspect ratio.
     *
     * @param maxWidth  the maximum allowed width in pixels
     * @param maxHeight the maximum allowed height in pixels
     */
    public ScaleImage(int maxWidth, int maxHeight)
    {
        this(maxWidth,maxHeight,false);
    }

    /**
     * Reads an image file, applies this instance's filter and writes the result
     * to the output file encoded in the requested format.
     *
     * @param inputImage  the image file to read
     * @param outputImage the file to write the filtered image to, overwritten on success
     * @param format      the output format, as supported by {@link ImageIO#write}
     * @return {@code true} if the image was filtered and re-encoded, or
     *         {@code false} if this instance is a no-op and the input was
     *         copied verbatim
     * @throws IOException if the image cannot be read, decoded, encoded or copied,
     *                     or if no writer is available for {@code format}
     */
    public boolean filter(File inputImage, File outputImage, String format) throws IOException
    {
        InputStream in = new FileInputStream(inputImage);
        try
        {
            InputStream in2 = filter(in, format);
            try
            {
                OutputStream out = new FileOutputStream(outputImage);
                try
                {
                    byte buf[] = new byte[64 * 1024];
                    int r;
                    while ((r = in2.read(buf)) > 0)
                    {
                        out.write(buf, 0, r);
                    }
                }
                finally
                {
                    out.close();
                }
                return (in!=in2);
            }
            finally
            {
                in2.close();
            }
        }
        finally
        {
            in.close();
        }
    }

    /**
     * Reads an image from the stream, applies this instance's scale and
     * grayscale filter, and re-encodes the result into memory.
     *
     * @param imageStream the stream to read the source image from
     * @param format      the output format, as supported by {@link ImageIO#write},
     *                    for example {@code "jpeg"} or {@code "png"}
     * @return a stream with the re-encoded image, or the original stream when
     *         this instance is a no-op
     * @throws IOException if the stream does not contain a decodable image, or
     *                     if no reader or writer is available for the requested format
     */
    public InputStream filter(InputStream imageStream, String format) throws IOException
    {
        if(nop)
        {
            return imageStream;
        }

        BufferedImage image = ImageIO.read(new BufferedInputStream(imageStream));
        if(image == null)
        {
            throw new IOException("Could not decode the image stream; either the data is corrupt or no reader is available for the format");
        }
        image = filter(image);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if(!ImageIO.write(image, format, out))
        {
            throw new IOException("No image writer found for format \"" + format + "\"");
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * Applies this instance's scale and grayscale filter to the image.
     *
     * @param img the image to filter
     * @return the filtered image, or the original image when this instance is a
     *         no-op or the computed size equals the original one; the result is
     *         always an opaque image
     */
    public BufferedImage filter(BufferedImage img)
    {
        if(nop)
        {
            return img;
        }

        // Make sure the aspect ratio is maintained, so the image is not skewed
        Dimension size  = new Dimension(img.getWidth(),img.getHeight());
        Dimension resize= (scale==null)?null:scale.convert(size);
        if(resize==null)
        {
            resize = size;
        }

        if(!resize.equals(size) || gray)
        {
            img = scaleByGraphics2D(img,resize.width, resize.height, gray);
        }
        return img;
    }

    private static BufferedImage scaleByGraphics2D(BufferedImage img, int w, int h, boolean gray)
    {
        BufferedImage scaledImage = new BufferedImage(w, h, gray?BufferedImage.TYPE_BYTE_GRAY:BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = scaledImage.createGraphics();
        try
        {
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics2D.drawImage(img, 0, 0, w, h, null);
        }
        finally
        {
            graphics2D.dispose();
        }
        return scaledImage;
    }

}

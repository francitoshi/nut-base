/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;



abstract class SwapColor
{
    final double ratio = 0.53; 
    final String rgb;
    public SwapColor(String rgb)
    {
        this.rgb = rgb;
    }
    
    int swap(int c)
    {
        Color color = new Color(c);
        color = swap(color);
        return color.getRGB();
    }
    abstract Color swap(Color c);
    String getRGB()
    {
        return rgb;
    }
    static final SwapColor[] OPS=
    {
        new SwapColorRGB(),
        new SwapColorGBR(),
        new SwapColorBRG(),
        new SwapColorRG(),
        new SwapColorRB(),
        new SwapColorGB(),
        new SwapColorR(),
        new SwapColorG(),
        new SwapColorB()
    };
            
    static SwapColor build(int op)
    {
        if(op>=0&&op<OPS.length)
        {
            return OPS[op];
        }
        return null;
    }
}

class SwapColorRGB extends SwapColor
{

    public SwapColorRGB()
    {
        super("RGB");
    }
    
    @Override
    int swap(int c)
    {
        return c;
    }
    @Override
    Color swap(Color c)
    {
        return c;
    }
}

    
class SwapColorGBR extends SwapColor
{

    public SwapColorGBR()
    {
        super("GBR");
    }
    @Override
    Color swap(Color c)
    {
        return new Color(c.getGreen(),c.getBlue(),c.getRed(),c.getAlpha());
    }
}
    
    
class SwapColorBRG extends SwapColor
{

    public SwapColorBRG()
    {
        super("BRG");
    }
    @Override
    Color swap(Color c)
    {
        return new Color(c.getBlue(),c.getRed(),c.getGreen(),c.getAlpha());
    }
}
    
    
class SwapColorRG extends SwapColor
{
    public SwapColorRG()
    {
        super("RG");
    }
    @Override
    Color swap(Color c)
    {
        return new Color(c.getGreen(),c.getRed(),c.getBlue(),c.getAlpha());
    }
}
    

class SwapColorRB extends SwapColor
{
    public SwapColorRB()
    {
        super("RB");
    }
    
    @Override
    Color swap(Color c)
    {
        return new Color(c.getBlue(),c.getGreen(),c.getRed(),c.getAlpha());
    }
}
class SwapColorGB extends SwapColor
{
    public SwapColorGB()
    {
        super("GB");
    }
    
    @Override
    Color swap(Color c)
    {
        return new Color(c.getRed(),c.getBlue(),c.getGreen(),c.getAlpha());
    }
}

class SwapColorR extends SwapColor
{
    public SwapColorR()
    {
        super("R");
    }
    
    @Override
    Color swap(Color c)
    {
        int r = (int) Math.min(255,c.getRed());
        int g = (int) (c.getGreen()*ratio);
        int b = (int) (c.getBlue()*ratio);
        return new Color(r,g,b,c.getAlpha());
    }
}
class SwapColorG extends SwapColor
{
    public SwapColorG()
    {
        super("G");
    }
    
    @Override
    Color swap(Color c)
    {
        int r = (int) (c.getRed()*ratio);
        int g = (int) Math.min(255,c.getGreen());
        int b = (int) (c.getBlue()*ratio);
        return new Color(r,g,b,c.getAlpha());
    }
}
class SwapColorB extends SwapColor
{
    public SwapColorB()
    {
        super("B");
    }
    
    @Override
    Color swap(Color c)
    {
        int r = (int) (c.getRed()*ratio);
        int g = (int) (c.getGreen()*ratio);
        int b = (int) Math.min(255,c.getBlue());
        return new Color(r,g,b,c.getAlpha());
    }
}

/**
 * Filters the colors of a {@link BufferedImage}, pixel by pixel, as a
 * {@link Runnable} task.
 *
 * <p>The operation is selected with one of the {@code RGB}, {@code GBR},
 * {@code BRG}, {@code RG}, {@code RB}, {@code GB}, {@code R}, {@code G} or
 * {@code B} constants: rotations and swaps of the color channels, or isolation
 * of a single channel while the remaining ones are attenuated. The result is
 * written into a separate destination image so the source is never modified.</p>
 *
 * @author franci
 */
public class SwapImageColor implements Runnable
{
    //no operation
    /** No color channel change; the destination is left untouched. */
    static public final int RGB=0;
    /** Rotation of the color channels to green, blue, red. */
    static public final int GBR=1;
    /** Rotation of the color channels to blue, red, green. */
    static public final int BRG=2;
    //swap
    /** Swap of the red and green color channels. */
    static public final int RG=3;
    /** Swap of the red and blue color channels. */
    static public final int RB=4;
    /** Swap of the green and blue color channels. */
    static public final int GB=5;
    //
    /** Isolation of the red channel; green and blue are attenuated. */
    static public final int R=6;
    /** Isolation of the green channel; red and blue are attenuated. */
    static public final int G=7;
    /** Isolation of the blue channel; red and green are attenuated. */
    static public final int B=8;

    /** First valid operation, equal to {@link #RGB}. */
    static public final int MIN=0;
    /** Last valid operation, equal to {@link #B}. */
    static public final int MAX=8;


    final BufferedImage src;
    final BufferedImage dst;
    final AtomicBoolean done= new AtomicBoolean(false);
    final SwapColor swap;

    /**
     * Creates a color-swap task that fills {@code dst} with the filtered
     * colors of {@code src}.
     *
     * @param src the source image, only read
     * @param dst the destination image, overwritten on {@link #run()}
     * @param op  one of the color operation constants from {@link #RGB} to
     *            {@link #B}; when out of range the task does nothing and
     *            completes immediately
     */
    public SwapImageColor(BufferedImage src, BufferedImage dst, int op)
    {
        this.src = src;
        this.dst = dst;
        this.swap= SwapColor.build(op);
    }

    /**
     * Swaps the colors of every pixel of the source image into the
     * destination image.
     */
    @Override
    public void run()
    {
        done.set(false);
        if(swap == null)
        {
            done.set(true);
            return;
        }
        int w = src.getWidth();
        int h = src.getHeight();

        for(int y=0; y<h;y++)
            for(int x=0; x<w;x++)
                dst.setRGB(x, y, swap.swap(src.getRGB(x,y)));

        done.set(true);
    }

    /**
     * Returns the symbolic name of the color operation, such as {@code "GBR"}
     * or {@code "R"}.
     *
     * @return the operation name, or {@code null} when no valid operation
     *         was supplied to the constructor
     */
    public String getRGB()
    {
        return swap == null ? null : swap.getRGB();
    }
}

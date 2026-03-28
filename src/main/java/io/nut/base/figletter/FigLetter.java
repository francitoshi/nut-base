/*
 * FigLetter.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.figletter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 *
 * @author franci
 */
public class FigLetter
{
    public enum Font
    {
        BLOCK, SLIM, DOUBLE
    }
    
    private static final FigIO figIO = new FigIO();
    
    private final Glyphs glyphs; 
    private final int scale;
    
    public FigLetter(Glyphs glyphs, int scale)
    {
        this.glyphs = glyphs;
        this.scale = scale;
    }
    
    // ──────────────────────────────────────────────
    // Definición de glifos
    // Cada glifo es un array de Strings (filas).
    // Se usan caracteres '#', '@', '=' según la fuente.
    // Ancho fijo por fuente para alineación perfecta.
    // ──────────────────────────────────────────────
    public static FigLetter getInstance(FigLetter.Font font, int scale)
    {
        return new FigLetter(Glyphs.getInstance(font), scale);
    }
    public static FigLetter getInstance(String name, InputStream inputStream, int scale) throws IOException
    {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        return new FigLetter(figIO.load(name, inputStream), scale);
    }
    public static FigLetter getInstance(File fontFile, int scale) throws FileNotFoundException, IOException
    {
        return getInstance(fontFile.getName(), new FileInputStream(fontFile), scale);
    }

    public String render(String text)
    {
        text = glyphs.caseSensitive ? text : text.toUpperCase();
        String[] rows = buildRows(text);
        return rescale(rows, scale);
    }

    public static String renderSystem(String text, String systemFont, int fontSize, int scale, char pixelChar)
    {
        return rescale( rasterize(text, systemFont, fontSize, pixelChar), scale);
    }

    /**
     * Atajo: pixelChar='#', size=SMALL.
     */
    public String renderSystem(String text, String systemFont, int fontSize)
    {
        return renderSystem(text, systemFont, fontSize, 1, '#');
    }
    
    /**
     * Escala el arte: cada carácter se repite `scale` veces horizontal y
     * verticalmente.
     */
    private static String rescale(String[] rows, int scale)
    {
        if (scale == 1)
        {
            return String.join(System.lineSeparator(), rows);
        }
        StringBuilder out = new StringBuilder();
        String sep = System.lineSeparator();
        for (String row : rows)
        {
            // Escala horizontal
            StringBuilder hscaled = new StringBuilder();
            for (char c : row.toCharArray())
            {
                for (int k = 0; k < scale; k++)
                {
                    hscaled.append(c);
                }
            }
            String hrow = hscaled.toString();
            // Escala vertical
            for (int v = 0; v < scale; v++)
            {
                out.append(hrow).append(sep);
            }
        }
        // Elimina último separador
        if (out.length() > 0)
        {
            out.setLength(out.length() - sep.length());
        }
        return out.toString();
    }

    private String[] buildRows(String text)
    {
        String[] fallback = glyphs.get('?');
        // Determina altura de glifo
        int height = fallback.length;

        // Una fila por línea de glifo
        StringBuilder[] sb = new StringBuilder[height];
        for (int i = 0; i < height; i++)
        {
            sb[i] = new StringBuilder();
        }

        for (char c : text.toCharArray())
        {
            String[] glyph = glyphs.getOrDefault(c, fallback);
            
            // Si el glifo tiene distinta altura, se rellena
            for (int row = 0; row < height; row++)
            {
                String rowStr = row < glyph.length ? glyph[row] : blank(glyph[0].length());
                //Replace hardblank with realspace
                rowStr = rowStr.replace(glyphs.hardblank, ' ');
                sb[row].append(rowStr);
                sb[row].append(' ');
            }
        }

        String[] result = new String[height];
        for (int i = 0; i < height; i++)
        {
            result[i] = sb[i].toString();
        }
        return result;
    }

    private static String blank(int width)
    {
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < width; i++)
        {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String[] rasterize(String text, String fontName, int fontSize, char pixelChar)
    {
        java.awt.Font awtFont = new java.awt.Font(fontName, java.awt.Font.PLAIN, fontSize);

        // 1. Medir el texto en un contexto temporal para saber el tamaño exacto
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D gProbe = probe.createGraphics();
        gProbe.setFont(awtFont);
        java.awt.FontMetrics fm = gProbe.getFontMetrics();
        int imgW = fm.stringWidth(text) + 4;   // pequeño margen
        int imgH = fm.getHeight() + 4;
        gProbe.dispose();

        // 2. Dibujar en imagen real (fondo blanco, texto negro)
        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, imgW, imgH);
        g.setColor(Color.BLACK);
        g.setFont(awtFont);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF); // bordes nítidos
        g.drawString(text, 2, fm.getAscent() + 2);
        g.dispose();

        // 3. Recortar bounding-box real (elimina filas/columnas vacías)
        int top = imgH, bottom = -1, left = imgW, right = -1;
        for (int y = 0; y < imgH; y++)
        {
            for (int x = 0; x < imgW; x++)
            {
                if (isDark(img.getRGB(x, y)))
                {
                    if (y < top)
                    {
                        top = y;
                    }
                    if (y > bottom)
                    {
                        bottom = y;
                    }
                    if (x < left)
                    {
                        left = x;
                    }
                    if (x > right)
                    {
                        right = x;
                    }
                }
            }
        }
        // Texto vacío o sin píxeles oscuros → devolver línea en blanco
        if (bottom < 0)
        {
            return new String[]{ "" };
        }

        // 4. Convertir píxeles a caracteres
        int rows = bottom - top + 1;
        int cols = right - left + 1;
        String[] result = new String[rows];
        StringBuilder sb = new StringBuilder(cols);
        for (int y = top; y <= bottom; y++)
        {
            sb.setLength(0);
            for (int x = left; x <= right; x++)
            {
                sb.append(isDark(img.getRGB(x, y)) ? pixelChar : ' ');
            }
            result[y - top] = sb.toString();
        }
        return result;
    }

    private static boolean isDark(int rgb)
    {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 < 128;
    }


    
    // ──────────────────────────────────────────────
    // Export / Import  FLF (FIGlet) y TLF (TOIlet)
    //
    // Formato FLF:
    //   Cabecera: flf2a<hardblank> height baseline maxLen oldLayout numComments
    //   Luego N líneas de comentario, luego glifos del código ASCII 32..126
    //   + 7 caracteres alemanes (196,214,220,228,246,252,223) → vacíos aquí
    //   Cada glifo tiene `height` líneas; todas excepto la última terminan en
    //   un endmark (@), la última en doble endmark (@@).
    //
    // Formato TLF (TOIlet):
    //   Idéntico a FLF EXCEPTO que la firma es "tlf2a" (sin hardblank
    //   pegado a la firma, el hardblank se asume espacio) y soporta Unicode.
    //   En la práctica el parser usa el mismo código; sólo cambia la firma.
    // ──────────────────────────────────────────────
    // ── Constantes de rango FIGlet ────────────────
    /**
     * Códigos ASCII requeridos por el estándar: 32 (space) .. 126 (~).
     */

    // ── IMPORT ────────────────────────────────────

    public String renderCustom(String text)
    {
        return rescale(buildRows(text), scale);
    }
}

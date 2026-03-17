/*
 * FigIO.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Contrato de lectura y escritura de fuentes FIG (FLF / TLF).
 *
 * <p>
 * Cada implementación se ocupa de un formato concreto:
 * <ul>
 * <li>{@link Flf2aIO} — formato FIGlet (.flf), endmark fijo '@'</li>
 * <li>{@link Tlf2aIO} — formato TOIlet (.tlf), endmark variable por glifo</li>
 * </ul>
 *
 * <p>
 * El mapa de glifos usa {@code char} (codepoint Unicode ≤ 0xFFFF) como clave y
 * un array de {@code String} (una entrada por fila) como valor. Todas las filas
 * de un mismo glifo tienen la misma longitud visual una vez eliminado el
 * endmark.
 */
public class FigIO
{
    public static final char FLF_HARDBLANK = '$';
    public static final char TLF_HARDBLANK = 0x7F;
    // ── Rango obligatorio ────────────────────────────────────────────────────
    /**
     * Primer código ASCII obligatorio en el bloque básico (espacio).
     */
    int FIRST_CODE = 32;

    /**
     * Último código ASCII obligatorio en el bloque básico (~).
     */
    int LAST_CODE = 126;

    /**
     * Siete caracteres alemanes requeridos por el estándar FIG inmediatamente
     * después del bloque ASCII 32-126.
     */
    int[] GERMAN_CODES =
    {
        196, 214, 220, 228, 246, 252, 223
    };
    
    static Flf2aIO flf2aIO = new Flf2aIO();
    static Tlf2aIO tlf2aIO = new Tlf2aIO();    
    
    /**
     * Lee todas las líneas del stream sin cerrarlo. Detecta y descomprime
     * automáticamente ficheros ZIP (magic bytes PK).
     */
    private static List<String> readLines(InputStream is) throws IOException
    {
        // Leer bytes crudos en memoria para poder inspeccionar magic bytes
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1)
        {
            buf.write(tmp, 0, n);
        }
        byte[] raw = buf.toByteArray();

        // Detectar ZIP: magic bytes 0x50 0x4B ('P','K')
        if (raw.length > 2 && raw[0] == 0x50 && raw[1] == 0x4B)
        {
            raw = unzip(raw);
        }

        String content = new String(raw, StandardCharsets.UTF_8);
        return new ArrayList<>(Arrays.asList(content.split("\\r?\\n", -1)));
    }

    /**
     * Descomprime el primer entry de un archivo ZIP en memoria.
     */
    private static byte[] unzip(byte[] zipBytes) throws IOException
    {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes)))
        {
            if (zis.getNextEntry() == null)
            {
                throw new IOException("ZIP vacío o sin entradas");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = zis.read(buf)) != -1)
            {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
    
    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Carga una fuente desde un {@link InputStream}.
     *
     * <p>
     * El stream se lee hasta EOF pero <b>no se cierra</b>: la responsabilidad
     * de cerrarlo recae en el llamador, siguiendo el principio de que quien
     * abre un recurso es quien lo cierra.
     *
     * <p>
     * Algunos ficheros .tlf están empaquetados como ZIP; las implementaciones
     * deben detectarlo por los magic bytes {@code PK} y descomprimir
     * transparentemente.
     *
     * @param is Stream de lectura posicionado al inicio del fichero
     * @return Mapa {@code codepoint → filas[]}, nunca {@code null}
     * @throws IOException si ocurre un error de E/S
     * @throws IllegalArgumentException si la cabecera no corresponde al formato
     */
    public Glyphs load(String name, InputStream in) throws IOException
    {
        List<String> lines = readLines(in);
        if (lines.isEmpty())
        {
            throw new IllegalArgumentException("Stream vacío");
        }

        String header = lines.get(0);
        if(header.startsWith("flf2a$"))
        {
            return flf2aIO.load(name, lines);
        }
        if(header.startsWith("tlf2a"))
        {
            return tlf2aIO.load(name, lines);
        }
        throw new IllegalArgumentException("in is not a valid FLF/TLF file: " + header.substring(0, Math.min(8, header.length())));
    }
    
    public Glyphs load(File file) throws IOException
    {
        return load(file.getName(), new FileInputStream(file));
    }
    
    /**
     * Exporta un mapa de glifos al formato correspondiente.
     *
     * <p>
     * El stream se escribe pero <b>no se cierra ni se hace flush</b>:
     * responsabilidad del llamador.
     *
     * @param glyphs Mapa {@code codepoint → filas[]} a serializar. Debe
     * contener al menos los 95 glifos ASCII 32-126; los glifos alemanes y los
     * etiquetados son opcionales.
     * @param hardblank Carácter usado como espacio protegido dentro de los
     * glifos (típicamente {@code '$'}). En TLF se escribe en la firma pero su
     * rol en los datos es idéntico al de FLF.
     * @param height Número de filas de cada glifo. Debe ser consistente con
     * todos los arrays de {@code glyphs}.
     * @param baseline Fila de la línea base (1-based, ≤ height).
     * @param comment Texto libre que se escribe como bloque de comentarios en
     * la cabecera. Puede ser {@code null} o vacío.
     * @param os Stream de escritura
     * @throws IOException si ocurre un error de E/S
     */
    public void exportFLF(Glyphs glyphs, String comment, OutputStream os) throws IOException
    {
        flf2aIO.export(glyphs, comment, os);
    }
    public void exportTLF(Glyphs glyphs, String comment, OutputStream os) throws IOException
    {
        tlf2aIO.export(glyphs, comment, os);
    }

    static int computeMaxLen(Glyphs glyphs)
    {
        int max = 0;
        for (String[] g : glyphs.values())
        {
            for (String row : g)
            {
                if (row != null)
                {
                    max = Math.max(max, row.length());
                }
            }
        }
        return max + 2;
    }
    
    static int parseCode(String s)
    {
        if (s.startsWith("0x") || s.startsWith("0X"))
        {
            return Integer.parseInt(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }

    
}

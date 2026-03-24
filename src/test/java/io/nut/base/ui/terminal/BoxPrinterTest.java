/*
 * BoxPrinterTest.java
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
package io.nut.base.ui.terminal;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BoxPrinterTest
{

    /**
     * Test of to method, of class BoxPrinter.
     */
    @Test
    public void testExamples() throws FileNotFoundException
    {
        // ─────────────────────────────────────────────────────────────────────
        // 1. Caja DOBLE centrada con márgenes laterales (cabecera de aplicación)
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("\n── Ejemplo 1: Cabecera con línea doble, centrado, márgenes ──\n");
        BoxPrinter header = new BoxPrinter(BoxPrinter.Style.DOBLE, 48, true, true);
        header.println("TORIFY – ejemplos de uso");

        // ─────────────────────────────────────────────────────────────────────
        // 2. Caja SIMPLE alineada a la izquierda, sin márgenes laterales
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 2: Lista de opciones, línea simple, izquierda ──\n");
        BoxPrinter menu = new BoxPrinter(BoxPrinter.Style.SIMPLE, 30, false, false);
        menu.println("1. Nueva partida\n2. Cargar partida\n3. Opciones\n4. Salir");

        // ─────────────────────────────────────────────────────────────────────
        // 3. Caja BLOQUE centrada, ancho pequeño → demuestra word-wrap
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 3: Word-wrap automático en caja de bloque ──\n");
        BoxPrinter narrow = new BoxPrinter(BoxPrinter.Style.BLOQUE, 20, true, true);
        narrow.println("Esta es una frase larga que debe romperse sin cortar palabras.");

        // ─────────────────────────────────────────────────────────────────────
        // 4. Caja con divisor interno (sección separada por línea)
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 4: Caja con divisor interno ──\n");
        BoxPrinter box = new BoxPrinter(BoxPrinter.Style.SIMPLE, 36, false, true);
        box.printTopBorder();
        box.printContentLine("Nombre : Arturo");
        box.printContentLine("Email  : arturo@ejemplo.com");
        box.printDivider();
        box.printContentLine("Estado : Activo");
        box.printContentLine("Rol    : Administrador");
        box.printBottomBorder();
        System.out.println();

        // ─────────────────────────────────────────────────────────────────────
        // 5. Caja DOBLE sin márgenes laterales (más compacta)
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 5: Caja doble compacta, sin padding lateral ──\n");
        BoxPrinter compact = new BoxPrinter(BoxPrinter.Style.DOBLE, 22, true, false);
        compact.println("ERROR\nAcceso denegado");

        // ─────────────────────────────────────────────────────────────────────
        // 6. Salida a System.err (PrintStream alternativo)
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 6: Salida a System.err ──\n");
        BoxPrinter errBox = new BoxPrinter(BoxPrinter.Style.SIMPLE, 40, false, true);
        errBox.to(System.err).println("Advertencia: espacio en disco bajo.");
        errBox.toStdOut(); // restaurar a stdout

        // ─────────────────────────────────────────────────────────────────────
        // 7. Salida a archivo
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 7: Salida a archivo (output.txt) ──\n");
        BoxPrinter fileBox = new BoxPrinter(BoxPrinter.Style.DOBLE, 44, true, true);
        fileBox.toFile("output.txt");
        fileBox.println("Reporte generado\n2025-03-24\nTodos los sistemas operativos.");
        fileBox.toStdOut();
        System.out.println("  → Caja escrita en output.txt\n");

        // ─────────────────────────────────────────────────────────────────────
        // 8. Componer varias cajas en un PrintStream personalizado
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 8: Múltiples cajas en el mismo PrintStream ──\n");
        PrintStream ps = System.out;
        BoxPrinter title = new BoxPrinter(BoxPrinter.Style.DOBLE, 40, true, true);
        BoxPrinter body = new BoxPrinter(BoxPrinter.Style.SIMPLE, 40, false, true);
        title.to(ps).print("INFORME DIARIO");
        body.to(ps).println("Usuarios activos : 1.234\nTransacciones   :   567\nErrores          :     0");

        // ─────────────────────────────────────────────────────────────────────
        // 9. Tabla ASCII con divisores
        // ─────────────────────────────────────────────────────────────────────
        System.out.println("── Ejemplo 9: Tabla con separadores ──\n");
        BoxPrinter table = new BoxPrinter(BoxPrinter.Style.SIMPLE, 38, false, true);
        table.printTopBorder();
        table.printContentLine(padColumns("Ciudad", "Temp.", "Estado", 14, 6, 16));
        table.printDivider();
        table.printContentLine(padColumns("Madrid", "24 °C", "Soleado", 14, 6, 16));
        table.printContentLine(padColumns("Barcelona", "21 °C", "Nublado", 14, 6, 16));
        table.printContentLine(padColumns("Sevilla", "28 °C", "Despejado", 14, 6, 16));
        table.printBottomBorder();
    }

    /**
     * Utilidad para alinear columnas en la tabla del ejemplo 9.
     */
    private static String padColumns(String c1, String c2, String c3, int w1, int w2, int w3)
    {
        return String.format("%-" + w1 + "s%-" + w2 + "s%-" + w3 + "s", c1, c2, c3);
    }

}

/*
 *  VerboseLineReader.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

/**
 * A decorator for {@link BufferedReader} that echoes every line it reads to a
 * specified {@link PrintStream}.
 * <p>
 * This class is useful for debugging or logging purposes, where you want to
 * process the input from a reader while also seeing a real-time transcript of
 * the data being read. It behaves exactly like a {@code BufferedReader}, with
 * the added side-effect of printing each successfully read line.
 *
 * @see BufferedReader
 * @see PrintStream
 */
public class VerboseLineReader extends BufferedReader
{

    /**
     * The stream to which each read line will be echoed.
     */
    private final PrintStream verbose;

    /**
     * Constructs a new VerboseLineReader that reads from the specified reader
     * and echoes output to the specified print stream, using a default-sized
     * input buffer.
     *
     * @param reader The {@code Reader} providing the underlying character
     * stream. Must not be null.
     * @param verbose The {@code PrintStream} to which each read line will be
     * echoed (e.g., {@code System.out}). Must not be null.
     */
    public VerboseLineReader(Reader reader, PrintStream verbose)
    {
        super(reader);
        this.verbose = verbose;
    }

    /**
     * Constructs a new VerboseLineReader that reads from the specified reader
     * and echoes output to the specified print stream, using an input buffer of
     * the specified size.
     *
     * @param reader The {@code Reader} providing the underlying character
     * stream. Must not be null.
     * @param sz The size of the input buffer.
     * @param verbose The {@code PrintStream} to which each read line will be
     * echoed (e.g., {@code System.out}). Must not be null.
     */
    public VerboseLineReader(Reader reader, int sz, PrintStream verbose)
    {
        super(reader, sz);
        this.verbose = verbose;
    }

    /**
     * Reads a line of text and echoes it to the verbose stream before returning
     * it.
     * <p>
     * This method delegates the actual reading to the parent
     * {@code BufferedReader.readLine()}. If a non-null line is returned (i.e.,
     * the end of the stream has not been reached), it is immediately printed to
     * the {@code PrintStream} provided during construction.
     *
     * @return A {@code String} containing the contents of the line, not
     * including any line-termination characters, or {@code null} if the end of
     * the stream has been reached.
     * @throws IOException If an I/O error occurs during the underlying read
     * operation.
     * @see java.io.BufferedReader#readLine()
     */
    @Override
    public String readLine() throws IOException
    {
        String line = super.readLine();

        // If a line was successfully read (not end-of-stream)
        if (line != null)
        {
            // Echo the line to the verbose stream.
            verbose.println(line);
        }
        return line;
    }
}

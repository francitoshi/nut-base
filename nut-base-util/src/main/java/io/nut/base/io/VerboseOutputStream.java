/*
 *  VerboseOutputStream.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author franci
 */
public class VerboseOutputStream extends FilterOutputStream
{
    private final OutputStream verbose;
    
    public VerboseOutputStream(OutputStream out, OutputStream verbose)
    {
        super(out);
        this.verbose = verbose;
    }

    @Override
    public void write(int b) throws IOException
    {
        super.write(b);
        verbose.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        super.write(b);
        verbose.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        super.write(b, off, len);
        verbose.write(b, off, len);
    }
}

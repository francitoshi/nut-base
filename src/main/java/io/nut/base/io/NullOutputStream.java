/*
 * NullOutputStream.java
 *
 * Copyright (c) 2019-2023 francitoshi@gmail.com
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
package io.tea.base.io;

import java.io.FilterOutputStream;
import java.io.IOException;

/**
 *
 * @author franci
 */
public class NullOutputStream extends FilterOutputStream
{
    public NullOutputStream()
    {
        super(null);
    }

    @Override
    public void write(int i) throws IOException
    {
    }

    @Override
    public void write(byte[] bytes) throws IOException
    {
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException
    {
    }

    @Override
    public void flush() throws IOException
    {
    }

    @Override
    public void close() throws IOException
    {
    }
}

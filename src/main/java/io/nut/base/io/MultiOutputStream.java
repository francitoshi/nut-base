/*
 * MultiOutputStream.java
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
package io.nut.base.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by franci on 30/12/14.
 */
public class MultiOutputStream extends OutputStream
{
    final OutputStream[] items;
    public MultiOutputStream(OutputStream... items)
    {
        this.items = items;
    }
    
    @Override
    public void write(int b) throws IOException
    {
        for (OutputStream item : items)
        {
            item.write(b);
        }
    }

    @Override
    public void close() throws IOException
    {
        for (OutputStream item : items)
        {
            item.close();
        }
    }

    @Override
    public void flush() throws IOException
    {
        for (OutputStream item : items)
        {
            item.flush();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        for (OutputStream item : items)
        {
            item.write(b, off, len);
        }
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        for (OutputStream item : items)
        {
            item.write(b);
        }
    }
}

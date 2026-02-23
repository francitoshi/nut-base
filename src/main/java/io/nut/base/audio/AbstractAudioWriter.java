/*
 * AbstractAudioWriter.java
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
package io.nut.base.audio;

import java.io.IOException;

public abstract class AbstractAudioWriter implements AudioWriter
{
    @Override
    public final int write(byte[] bytes, int off, int len, int count) throws IOException
    {
        int writeCount = 0;
        if(len>0)
        {
            while(count>0)
            {
                int n = Math.min(len, count);
                int w = this.write(bytes, off, n);
                if(w<0)
                {
                    return w;
                }
                writeCount += w;
                count -= w;
            }
        }
        return writeCount;
    }
   
}

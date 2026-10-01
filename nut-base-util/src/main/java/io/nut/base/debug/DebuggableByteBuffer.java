/*
 *  DebuggableByteBuffer.java
 *
 *  Copyright (c) 2023-2024 francitoshi@gmail.com
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
package io.nut.base.debug;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author franci
 */
public class DebuggableByteBuffer
{
    public static DebuggableByteBuffer wrap(ByteBuffer byteBuffer, boolean debug)
    {
        return debug ? new Debugger(byteBuffer, new ArrayList<>()) : new DebuggableByteBuffer(byteBuffer);
    }
    public static DebuggableByteBuffer wrap(ByteBuffer byteBuffer, ArrayList<int[]> debug)
    {
        return new Debugger(byteBuffer, debug);
    }

    private final ByteBuffer byteBuffer;
 
    private DebuggableByteBuffer(ByteBuffer byteBuffer)
    {
        this.byteBuffer = byteBuffer;
    }

    public final byte[] array()
    {
        return byteBuffer.array();
    }

    public int position()
    {
        return byteBuffer.position();
    }

    public byte[][] debug()
    {
        return null;
    }

    public byte get()
    {
        return byteBuffer.get();
    }

    public DebuggableByteBuffer put(byte b)
    {
        byteBuffer.put(b);
        return this;
    }

    public DebuggableByteBuffer get(byte[] dst)
    {
        byteBuffer.get(dst);
        return this;
    }

    public DebuggableByteBuffer put(byte[] src)
    {
        byteBuffer.put(src);
        return this;
    }

    public char getChar()
    {
        return byteBuffer.getChar();
    }

    public DebuggableByteBuffer putChar(char c)
    {
        byteBuffer.putChar(c);
        return this;
    }

    public short getShort()
    {
        return byteBuffer.getShort();
    }

    public DebuggableByteBuffer putShort(short s)
    {
        byteBuffer.putShort(s);
        return this;
    }

    public int getInt()
    {
        return byteBuffer.getInt();
    }

    public DebuggableByteBuffer putInt(int i)
    {
        byteBuffer.putInt(i);
        return this;
    }

    public ByteBuffer put(byte[] src, int offset, int length)
    {
        return byteBuffer.put(src, offset, length);
    }

    public long getLong()
    {
        return byteBuffer.getLong();
    }

    public DebuggableByteBuffer putLong(long l)
    {
        byteBuffer.putLong(l);
        return this;
    }

    public float getFloat()
    {
        return byteBuffer.getFloat();
    }

    public DebuggableByteBuffer putFloat(float f)
    {
        byteBuffer.putFloat(f);
        return this;
    }

    public double getDouble()
    {
        return byteBuffer.getDouble();
    }

    public DebuggableByteBuffer putDouble(double d)
    {
        byteBuffer.putDouble(d);
        return this;
    }

    private static class Debugger extends DebuggableByteBuffer
    {
        private final List<int[]> fields;

        Debugger(ByteBuffer byteBuffer, List<int[]> positions)
        {
            super(byteBuffer);
            this.fields = positions;
        }

        private void add(int size)
        {
            fields.add(new int[]{ position(), size});
        }

        @Override
        public byte[][] debug()
        {
            byte[] bytes = this.array();
            byte[][] ret = new byte[fields.size()][];
            int i = 0;
            for (int[] item : fields)
            {
                ret[i++] = Arrays.copyOfRange(bytes, item[0], item[0] + item[1]);
            }
            return ret;
        }

        @Override
        public byte get()
        {
            add(Byte.BYTES);
            return super.get();
        }

        @Override
        public DebuggableByteBuffer put(byte b)
        {
            add(Byte.BYTES);
            return super.put(b);
        }

        @Override
        public DebuggableByteBuffer get(byte[] dst)
        {
            add(dst.length);
            return super.get(dst);
        }

        @Override
        public DebuggableByteBuffer put(byte[] src)
        {
            add(src.length);
            return super.put(src);
        }

        @Override
        public char getChar()
        {
            add(Character.BYTES);
            return super.getChar();
        }

        @Override
        public DebuggableByteBuffer putChar(char c)
        {
            add(Character.BYTES);
            return super.putChar(c);
        }

        @Override
        public short getShort()
        {
            add(Short.BYTES);
            return super.getShort();
        }

        @Override
        public DebuggableByteBuffer putShort(short s)
        {
            add(Short.BYTES);
            return super.putShort(s);
        }

        @Override
        public int getInt()
        {
            add(Integer.BYTES);
            return super.getInt();
        }

        @Override
        public DebuggableByteBuffer putInt(int i)
        {
            add(Integer.BYTES);
            return super.putInt(i);
        }

        @Override
        public ByteBuffer put(byte[] src, int offset, int length)
        {
            add(length);
            return super.put(src, offset, length);
        }

        @Override
        public long getLong()
        {
            add(Long.BYTES);
            return super.getLong();
        }

        @Override
        public DebuggableByteBuffer putLong(long l)
        {
            add(Long.BYTES);
            return super.putLong(l);
        }

        @Override
        public float getFloat()
        {
            add(Float.BYTES);
            return super.getFloat();
        }

        @Override
        public DebuggableByteBuffer putFloat(float f)
        {
            add(Float.BYTES);
            return super.putFloat(f);
        }

        @Override
        public double getDouble()
        {
            add(Double.BYTES);
            return super.getDouble();
        }

        @Override
        public DebuggableByteBuffer putDouble(double d)
        {
            add(Double.BYTES);
            return super.putDouble(d);
        }
    }
}

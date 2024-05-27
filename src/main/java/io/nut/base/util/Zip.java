/*
 *  Zip.java
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
package io.nut.base.util;

import io.nut.base.encoding.Base64DecoderException;
import io.nut.base.encoding.Encoding;
//import io.nut.base.snappy.Snappy;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream​;

/**
 *
 * @author franci
 */
public class Zip
{
    
    ///// GZIP /////////////////////////////////////////////////////////////////
    
    public static byte[] gzip(byte[] src) throws IOException
    {
        return gzip(src, 0, Integer.MAX_VALUE);
    }

    public static byte[] gzip(byte[] src, int off, int len) throws IOException
    {
        if (src == null)
        {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzis = new GZIPOutputStream(baos, 64 * 1024))
        {
            gzis.write(src, off, Math.min(src.length-off, len));
        }
        return baos.toByteArray();
    }

    public static byte[] gunzip(byte[] src) throws IOException
    {
        if (src == null)
        {
            return null;
        }
        GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(src), 64 * 1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buf[] = new byte[64 * 1024];
        int r;
        int count = 0;
        while ((r = gzis.read(buf)) > 0 && count < Integer.MAX_VALUE)
        {
            baos.write(buf, 0, r);
            count += r;
        }
        return baos.toByteArray();
    }

    public static String gzip(String src, String charsetName, Encoding.Type encodingType) throws IOException
    {
        if (src == null)
        {
            return null;
        }
        byte[] data = src.getBytes(charsetName);
        data = Zip.gzip(data);
        return Encoding.encode(data, encodingType);
    }

    public static String gunzip(String src, String charsetName, Encoding.Type encodingType) throws IOException, Base64DecoderException
    {
        if (src == null)
        {
            return null;
        }
        byte[] data = Encoding.decode(src, encodingType);
        data = Zip.gunzip(data);
        return new String(data, charsetName);
    }
    
    ///// DEFLATE //////////////////////////////////////////////////////////////
    
    private static final int DEF_BUF_SIZE = 64 * 1024;

    public static byte[] deflate(byte[] src)
    {
        return deflate(src, 0, Integer.MAX_VALUE, Deflater.DEFAULT_COMPRESSION, false);
    }

    public static byte[] deflate(byte[] src, int off, int len)
    {
        return deflate(src, off, len, Deflater.DEFAULT_COMPRESSION, false);
    }

    public static byte[] deflate(byte[] src, int off, int len, int deflateLevel)
    {
        return deflate(src, 0, len, deflateLevel, false);
    }

    public static byte[] deflate(byte[] src, int off, int len, int deflateLevel, boolean nowrap)
    {
        if (src == null)
        {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream​ dos = new DeflaterOutputStream​(baos, new Deflater(deflateLevel, nowrap), DEF_BUF_SIZE))
        {
            int n = Math.min(src.length-off, len);
            dos.write(src, off, n);
        }
        catch (IOException ex)
        {
            Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        return baos.toByteArray();
    }

    public static byte[] inflate(byte[] src)
    {
        return inflate(src, false);
    }

    public static byte[] inflate(byte[] src, boolean nowrap)
    {
        try
        {
            if (src == null)
            {
                return null;
            }
            InflaterInputStream gzis = new InflaterInputStream(new ByteArrayInputStream(src), new Inflater(nowrap), DEF_BUF_SIZE);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte buf[] = new byte[DEF_BUF_SIZE];
            int r;
            int count = 0;
            while ((r = gzis.read(buf)) > 0 && count < Integer.MAX_VALUE)
            {
                baos.write(buf, 0, r);
                count += r;
            }
            return baos.toByteArray();
        }
        catch (IOException ex)
        {
            Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    ///// SNAPPY ///////////////////////////////////////////////////////////////
    
//    public static byte[] snappy(byte[] src) throws IOException
//    {
//        return Snappy.compress(src);
//    }
//    public static byte[] snappy(byte[] src, int off, int len) throws IOException
//    {
//        return Snappy.compress(src, off, len);
//    }
//    public static byte[] unsnappy(byte[] src) throws IOException
//    {
//        return Snappy.uncompress(src, 0, src.length);
//    }
    
    ///// LONGS ////////////////////////////////////////////////////////////////
    
    static final int COMPRESS_MIN_SIZE = 16;
    static final int COMPRESS_MIN_SAVED = 8;
    static final int COMPRESS_LEVEL = 5;
    static final int VAR_INT_MARK = 9;
    static final int COMPRESS_MARK = 16;
    static final int COMPRESS_MASK = 0xf;
    
    public static byte[] deflateLong(long[] plain)
    {
        if(plain==null)
        {
            return null;
        }
        final int n = plain.length;
        
        int varIntBytes = 1;
        
        byte[] data = new byte[n*Long.BYTES];
        ByteBuffer bb = ByteBuffer.wrap(data);
        for(long item : plain)
        {
            bb.putLong(item);
            varIntBytes += VarInt.sizeOf(item);
        }
        
        int gap=0;
        boolean found = false;
        for(int i=0; !found && i<Long.BYTES;i++)
        {
            gap = i;
            for(int j=0;j<n;j++)
            {
                if(data[j*Long.BYTES+i]!=0)
                {
                    found = true;
                    break;
                }
            }
        }
        int width = Long.BYTES-gap;
        int nogapBytes = width*n+1;
        
        byte[] bytes;
        if(nogapBytes<=varIntBytes)
        {
            bytes = new byte[nogapBytes];
            bytes[0]=(byte) width;

            for(int i=0,p=1;i<n;i++)
            {
                for(int j=0,q=i*Long.BYTES+gap;j<width;j++)
                {
                    bytes[p++] = data[q++];
                }
            }
        }
        else
        {
            width = VAR_INT_MARK;
            bytes = new byte[varIntBytes];
            bytes[0]=(byte) width;
            for(int i=0,p=1;i<n;i++)
            {
                VarInt vi = new VarInt(plain[i]);
                p+= vi.encode(bytes,p);
            }
        }
        if(bytes.length>COMPRESS_MIN_SIZE)
        {
            byte[] inflated = Arrays.copyOfRange(bytes, 1, bytes.length);
            byte[] deflated = deflate(inflated, 0, inflated.length, COMPRESS_LEVEL, true);
            if(deflated.length+COMPRESS_MIN_SAVED < bytes.length)
            {
                bytes = new byte[deflated.length+1];
                bytes[0] = (byte) (width | COMPRESS_MARK);
                System.arraycopy(deflated, 0, bytes, 1, deflated.length);
            }
        }
        return bytes;
    }
    
    public static long[] inflateLong(byte[] bytes)
    {
        if(bytes==null)
        {
            return null;
        }
        int minWidth = (bytes[0]&COMPRESS_MASK);
        if((bytes[0]&COMPRESS_MARK)==COMPRESS_MARK)
        {
            byte[] deflated = Arrays.copyOfRange(bytes, 1, bytes.length);
            byte[] inflated = inflate(deflated, true);

            bytes = new byte[inflated.length+1];
            bytes[0] = (byte) minWidth;
            System.arraycopy(inflated, 0, bytes, 1, inflated.length);
        }
        if(minWidth==VAR_INT_MARK)
        {
            long[] tmp = new long[bytes.length];
            int i=0;
            for(int p=1;p<bytes.length;i++)
            {
                VarInt vi = new VarInt(bytes,p);
                tmp[i] = vi.longValue();
                p+=vi.getOriginalSizeInBytes();
            }
            return Arrays.copyOf(tmp, i);
        }
        
        int gap = Long.BYTES-minWidth;
        int n = (bytes.length-1)/minWidth;
        byte[] data = new byte[n*Long.BYTES];
        for(int i=0,p=1;i<n;i++)
        {
            for(int j=0,q=i*Long.BYTES+gap;j<minWidth;j++)
            {
                data[q++] = bytes[p++];
            }
        }
        long[] plain = new long[n];
        ByteBuffer bb = ByteBuffer.wrap(data);
        for(int i=0;i<plain.length;i++)
        {
            plain[i] = bb.getLong();
        }
        return plain;
    }

    public static byte[] deflateLong2(long[][] plain)
    {
        if(plain==null)
        {
            return null;
        }
        int count = 0;
        byte[][] data = new byte[plain.length][];
        for(int i=0;i<plain.length;i++)
        {
            data[i]= deflateLong(plain[i]);
            count += data[i].length+Character.BYTES;
        }
        byte[] bytes = new byte[count];
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for(int i=0;i<data.length;i++)
        {
            bb.putChar((char) data[i].length);
            bb.put(data[i]);
        }
        return bytes;
    }
    
    public static long[][] inflateLong2(byte[] bytes)
    {
        if(bytes==null)
        {
            return null;
        }
        ArrayList<long[]> list = new ArrayList<>();
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for(int i=0;i<bytes.length;)
        {
            int n = bb.getChar();
            byte[] item = new byte[n];
            bb.get(item);
            list.add(inflateLong(item));
            i+=n+2;
        }
        return list.toArray(new long[0][]);
    }
    
}

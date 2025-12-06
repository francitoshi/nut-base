/*
 * Steganography.java
 *
 * Copyright (c) 2010-2025 francitoshi@gmail.com
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
package io.nut.base.crypto.stego;

import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Kripto.Pbkdf2;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import io.nut.base.crypto.Rand;
import io.nut.base.crypto.kdf.PBKDF2;
import io.nut.base.encoding.Ascii85;
import io.nut.base.math.Nums;
import io.nut.base.util.BitSetReader;
import io.nut.base.util.BitSetWriter;
import io.nut.base.util.Bits;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import io.nut.base.util.VarInt;
import io.nut.base.util.Zip;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 *
 * @author franci
 */
public class Steganography
{
    static final Pbkdf2 DERIVATION = Pbkdf2.PBKDF2WithHmacSHA256;
    
    private static final String PARAGRAPHS = "(\r?\n\r?){2,}";
    private static final String LINES = "(\r?\n\r?)";
    private static final String WORDS = "[ \n\r\t\f]+";
    private static final String HOLES = "\\S+";
    private static final SecretKeyTransformation AES_CFB8_NOPADDING = SecretKeyTransformation.AES_CFB8_NoPadding;

    private static final byte[] SALT = "salt".getBytes(StandardCharsets.UTF_8);
    private static final int ROUNDS = 500_000;
    private static final int KEY_BITS = 256;

//    private static final StegoPack pk = new StegoPack0();

    private final int columns;
    private final boolean splitLines;
    private final boolean mergeLines;
    private final boolean deflate;
    private final int rounds;
    private final Kripto kripto;
    private final PBKDF2 pbkdf2;
    private final Rand rand = Kripto.getRand();
    
    private volatile double bitsRatio=0;
    private volatile String bitsGauge="";
    
               
    public Steganography(int columns, boolean splitLines, boolean mergeLines, boolean deflate)
    {
        this(null, columns, splitLines, mergeLines, deflate, ROUNDS);
    }
    public Steganography(int columns, boolean splitLines, boolean mergeLines, boolean deflate, int rounds)
    {
        this(null, columns, splitLines, mergeLines, deflate, rounds);
    }
    public Steganography(Kripto kripto, int columns, boolean splitLines, boolean mergeLines, boolean deflate)
    {
        this(null, columns, splitLines, mergeLines, deflate, ROUNDS);
    }
    public Steganography(Kripto kripto, int columns, boolean splitLines, boolean mergeLines, boolean deflate, int rounds)
    {
        this.kripto = kripto==null ? kripto=Kripto.getInstance(true) : kripto;
        this.pbkdf2 = kripto.getPBKDF2(DERIVATION);
        this.columns = columns;
        this.splitLines = splitLines;
        this.mergeLines = mergeLines;
        this.deflate = deflate;
        this.rounds = rounds;
    }

    public double getBitsRatio()
    {
        return this.bitsRatio;
    }
    public String getBitsGauge()
    {
        return this.bitsGauge;
    }

    String[][] splitWords(String src)
    {
        String[] words = src.split(WORDS);

        if (!this.splitLines)
        {
            return new String[][]{ words };
        }

        ArrayList<String[]> list = new ArrayList<>();
        ArrayList<String> line = new ArrayList<>();

        int cols = -1;

        for (int i = 0; i < words.length;)
        {
            String w = words[i++];
            int ws = w.length();
            if (cols + ws >= this.columns && cols > 0)
            {
                list.add(line.toArray(new String[0]));
                line.clear();
                cols = -1;
            }

            line.add(w);
            cols += ws + 1;
        }
        if (!line.isEmpty())
        {
            list.add(line.toArray(new String[0]));
        }
        return list.toArray(new String[0][]);
    }
    
    String[][] splitLines(String src)
    {
        src = src.trim();
        ArrayList<String[]> list = new ArrayList<>();
        String[] lines = (this.splitLines && this.mergeLines) ? (new String[]{ src }) : src.split(LINES);
        for (String line : lines)
        {
            String[][] words = splitWords(line);
            Collections.addAll(list, words);
        }
        return list.toArray(new String[0][]);
    }

    String[][][] splitParagraphs(String src)
    {
        String[] paragraphs;
        String[][][] words;

        paragraphs = src.split(PARAGRAPHS);
        words = new String[paragraphs.length][][];
        for (int i = 0; i < paragraphs.length; i++)
        {
            words[i] = splitLines(paragraphs[i]);
        }
        return words;
    }
    
    static byte[] pack(byte[] bytes, boolean deflate)
    {
        if(deflate)
        {   // below 6 bytes can't be deflated
            byte[] bytes2 = bytes.length>=6 ? Zip.deflate(bytes, 0, bytes.length, 9, true) : bytes;
            bytes = (deflate = bytes2.length<bytes.length) ? bytes2 : bytes;
        }
        VarInt count = new VarInt(bytes.length);
        byte[] countBytes = count.encode();
        
        byte mask = Bits.bitSet((byte)Utils.adler32(bytes), 7, deflate);
        
        ByteBuffer buffer = ByteBuffer.allocate(countBytes.length+1+bytes.length);

        buffer.put(countBytes);
        buffer.put(mask);
        buffer.put(bytes);
        
        return buffer.array();
    }
    static byte[] unpack(byte[] packet)
    {
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        VarInt count = new VarInt(packet,0);
        int len = count.intValue();
        byte mask = buffer.get(count.getOriginalSizeInBytes());
        boolean deflate = Bits.bitGet(mask, 7);
        
        byte[] bytes = new byte[len];
        if(bytes.length>0)
        {
            //the following line is emulated because that method does not exists in java 8
            //buffer.get(count.getOriginalSizeInBytes()+1, bytes, 0, len);
            buffer.position(count.getOriginalSizeInBytes()+1);
            buffer.get(bytes, 0, len);
        }

        byte mask2 = Bits.bitSet((byte)Utils.adler32(bytes), 7, deflate);
        if(mask!=mask2)
        {
            throw new RuntimeException("invalid crc code");
        }
        return deflate ? Zip.inflate(bytes,true) : bytes;
    }

    static int lineSize(String[] words)
    {
        int size = 0;
        for(String w : words)
        {
            size += w.length();
        }
        return size;
    }

    private byte[] encrypt(byte[] packet, SecretKey key, byte[] iv128) throws RuntimeException
    {
        try
        {
            IvParameterSpec iv16 = kripto.getIv(iv128);
            packet = kripto.encrypt(key, AES_CFB8_NOPADDING, iv16, packet);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            Logger.getLogger(Steganography.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Cryptography error",ex);
        }
        return packet;
    }
    
    private byte[] decrypt(byte[] packet, SecretKey key, byte[] iv128) throws RuntimeException
    {
        try
        {
            IvParameterSpec iv16 = kripto.getIv(iv128);
            packet = kripto.decrypt(key, AES_CFB8_NOPADDING, iv16, packet);
        }
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex)
        {
            Logger.getLogger(Steganography.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Cryptography error",ex);
        }
        return packet;
    }
    
////////////////////////////////////////////////////////////////////////////////    
////////////////////////////////////////////////////////////////////////////////    
////////////////////////////////////////////////////////////////////////////////    

    static void encodeNH(int[] padding, int d, int n, int h, int index, BitSetReader br)
    {
        if(n==0)
        {
            return;
        }
        if(n>h/2)
        {
            throw new RuntimeException();
//            encodeNH(padding,-d, h-n, h, index, br);
        }
        else if((n > 1 && n < h) || (n == 1 && ( h == 2 || h == 3) ) ) 
        {
            int bit = br.get()?1:0;
            padding[index+bit] += d;
            encodeNH(padding, d, n-1,h-2,index+2, br);
        }
        else
        {
            int bits= Nums.log2(h, false);
            int pos = br.get(bits);
            padding[index+pos] += d;
        }
    }

    static void decodeNH(int[] padding, int d, int n, int h, int index, BitSetWriter bw)
    {
        if(n==0)
        {
            return;
        }
        if(n>h/2)
        {
            throw new RuntimeException();
//            decodeNH(padding,h-n,h,index, bw);
        }
        else if((n > 1 && n < h) || (n == 1 && h == 2))
        {
            boolean bit = d>0 ? padding[index]<padding[index+1] : padding[index]>padding[index+1];
            bw.put(bit);
            decodeNH(padding, d, n-1, h-2, index+2, bw);
        }
        else
        {
            byte pos = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for(byte i=0;i<h;i++)
            {
                if(d>0 ? padding[index+i]>max : padding[index+i]<min)
                {
                    pos=i;
                    min=padding[index+i];
                    max=padding[index+i];
                }
            }
            bw.put(pos, Nums.log2(h, false));
        }
    }
    int[] NHPadding(String[] words, BitSetReader bits, boolean tail)
    {
        int size = lineSize(words);
        int h = words.length-1;                                 //number of holes between words
        int[] padding = new int[h];
        if(h>0)
        {
            int cols = tail?Math.min(columns,size+h+h/2):columns;   //colums of this line
            int d=1;
            int n=(cols-size)%h;                                    //number of spaces needed to complete the columns
            int p=(cols-size)/h;
            if(n>h/2)
            {
                d=-1;
                n=h-n;
                p++;
            }
            Arrays.fill(padding,p);
            encodeNH(padding, d, n, h, 0, bits);
        }
        return padding;
    }
    private static void decodeLine(String line, BitSetWriter wbits)
    {
        String[] items = Strings.nonNullNonEmpty(line.trim().split(HOLES));
        int[] holes = new int[items.length];
        int h = 0;

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        for(int i=0;i<items.length;i++)
        {
            if(items[i].length()>0)
            {
                int val = items[i].length();
                holes[h++]=val;
                min = Math.min(min,val);
                max = Math.max(max,val);
            }
        }
        if (min == max || max-min>1)
        {
            return;
        }
        
        int n = 0;
        for(int i=0;i<h;i++)
        {
            if(holes[i]==max)
            {
                n++;
            }
        }
        int d = 1;
        if(n>h/2)
        {
            n=h-n;
            d=-1;
        }
        decodeNH(holes, d, n, h, 0, wbits);
    }

    public SecretKey deriveSecretKeyAES(char[] passphrase) throws InvalidKeySpecException
    {
        if(passphrase==null || passphrase.length==0)
        {
            return null;
        }
        return pbkdf2.deriveSecretKeyAES(passphrase, SALT, ROUNDS, KEY_BITS);
    }

    public byte[] deriveIV(char[] passphrase) throws InvalidKeySpecException
    {
        if(passphrase==null || passphrase.length==0)
        {
            return null;
        }
        return pbkdf2.deriveSecretKeyEncoded(passphrase, SALT, ROUNDS, 128); 
    }

    public String encode(String text, byte[] msg, char[] passphrase) throws InvalidKeySpecException
    {
        SecretKey key = this.deriveSecretKeyAES(passphrase);
        byte[] iv128 = this.deriveIV(passphrase);
        return this.encode(text, msg, key, iv128);
    }

    public String encode(String text, byte[] msg) 
    {
        return this.encode(text, msg, (SecretKey)null, null);
    }

    public String encode(String text, byte[] msg, SecretKey key, byte[] iv128)
    {
        byte[] packet = pack(msg,this.deflate);
        if(key!=null)
        {
            packet = encrypt(packet, key, iv128);
        }
        BitSetReader rbits = BitSetReader.build(BitSet.valueOf(packet));
        
        StringBuilder sb = new StringBuilder();
        
        String[][][] paragraphs = this.splitParagraphs(text);
        
        for(String[][] lines : paragraphs)
        {
            int tail = lines.length-1;
            for(int i=0;i<lines.length;i++)
            {
                String[] words = lines[i];
                int[] tabs = this.NHPadding(words, rbits, i==tail);
                for(int j=0;j<tabs.length;j++)
                {
                    sb.append(words[j]).append(Strings.repeat(' ', tabs[j]));
                }
                sb.append(words[tabs.length]).append('\n');
            }
            sb.append('\n');
        }
        this.bitsRatio = (packet.length*8.0)/rbits.count();
        this.bitsGauge = String.format("%d/%d=%.2f", packet.length*8, rbits.count(), this.bitsRatio);
        return sb.toString().trim();
    }

    public String justify(String text) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] msg = rand.nextBytes(new byte[1024]);
        char[] passphrase = Ascii85.encode(msg);
        SecretKey key = this.deriveSecretKeyAES(passphrase);
        byte[] iv128 = this.deriveIV(passphrase); 
        return this.encode(text, msg, key, iv128);
    }

    public byte[] decode(String text, char[] passphrase) throws InvalidKeySpecException
    {
        SecretKey key = deriveSecretKeyAES(passphrase);
        byte[] iv128 = deriveIV(passphrase);
        return decode(text, key, iv128);
    }

    public byte[] decode(String text)
    {
        return decode(text, (SecretKey)null, null);
    }   
            
    public byte[] decode(String text, SecretKey key, byte[] iv128)
    {
        BitSet bitSet = new BitSet();
        BitSetWriter wbits = BitSetWriter.build(bitSet);
        String[] lines = text.split(LINES);
        for(String line : lines)
        {
            if(line.trim().isEmpty())
            {
                continue;
            }
            decodeLine(line, wbits);
        }
        
        byte[] packet = bitSet.toByteArray();
        if(key!=null)
        {
            packet = decrypt(packet, key, iv128);
        }
        
        return unpack(packet);
    }
}

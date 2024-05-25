/*
 *  DigestTest.java
 *
 *  Copyright (C) 2018-2023 francitoshi@gmail.com
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
package io.nut.base.crypto;

import io.nut.base.encoding.Hex;
import static io.nut.base.util.CharSets.UTF8;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class DigestTest
{

    public DigestTest()
    {
    }

    @BeforeAll
    public static void setUpClass()
    {
    }

    @AfterAll
    public static void tearDownClass()
    {
    }

    @BeforeEach
    public void setUp()
    {
    }

    @AfterEach
    public void tearDown()
    {
    }

    @Test
    public void testSome() throws UnsupportedEncodingException
    {
        byte[] empty = "".getBytes(UTF8);
        byte[] satoshi_nakamoto ="Satoshi Nakamoto".getBytes(UTF8);
        byte[] hello = "hello".getBytes(UTF8);
        
        assertEquals("9e107d9d372bb6826bd81d3542a419d6", Hex.encode(Digest.md5("The quick brown fox jumps over the lazy dog".getBytes(UTF8))));
        assertEquals("e4d909c290d0fb1ca068ffaddf22cbd0", Hex.encode(Digest.md5("The quick brown fox jumps over the lazy dog.".getBytes(UTF8))));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hex.encode(Digest.md5(empty)));
        assertEquals("45a872a13366071d5e1e5788c8eb4888", Hex.encode(Digest.md5(satoshi_nakamoto)));

        assertEquals("2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", Hex.encode(Digest.sha1("The quick brown fox jumps over the lazy dog".getBytes(UTF8))));
        assertEquals("de9f2c7fd25e1b3afad3e85a0bd17d9b100db4b3", Hex.encode(Digest.sha1("The quick brown fox jumps over the lazy cog".getBytes(UTF8))));
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Hex.encode(Digest.sha1(empty)));
        assertEquals("ea8ffd7dd95cc12d42f5b240730618d57b4f1dc0", Hex.encode(Digest.sha1(satoshi_nakamoto)));

        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hex.encode(Digest.sha256(empty)));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Hex.encode(Digest.sha256(hello)));
        assertEquals("a0dc65ffca799873cbea0ac274015b9526505daaaed385155425f7337704883e", Hex.encode(Digest.sha256(satoshi_nakamoto)));

        assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e", Hex.encode(Digest.sha512(empty)));
        assertEquals("9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043", Hex.encode(Digest.sha512(hello)));
        assertEquals("3b9074fba6ad7bee4e83870f345c913095234d7b9fd2cc357c547941dd986e8ecbc2338c96dabbb20755ecc76b6456880dc15da4cb80ad864f33d17068df5cf7", Hex.encode(Digest.sha512(satoshi_nakamoto)));
        
        assertEquals("9c1185a5c5e9fc54612808977ee8f548b2258d31", Hex.encode(Digest.ripemd160(empty)));
        assertEquals("18c274d866aa04a225e1b6fb578f7e55593482da", Hex.encode(Digest.ripemd160("bfotool.com".getBytes(UTF8))));
        assertEquals("0df020ba32aa9b8b904471ff582ce6b579bf8bc8", Hex.encode(Digest.ripemd160("42".getBytes(UTF8))));
        assertEquals("108f07b8382412612c048d07d13f814118445acd", Hex.encode(Digest.ripemd160(hello)));
        assertEquals("3261a2274297534675e773ef4ef48f4ec3336ed0", Hex.encode(Digest.ripemd160("Eureka.".getBytes(UTF8))));
        assertEquals("d8819d9fbce67d331913803d8fa9337ec786bf81", Hex.encode(Digest.ripemd160(satoshi_nakamoto)));
        
        assertEquals("9595c9df90075148eb06860365df33584b75bff782a510c6cd4883a419833d50", Hex.encode(Digest.sha256Twice(hello)));
        assertEquals("b6a9c8c230722b7c748331a8b450f05566dc7d0f", Hex.encode(Digest.ripemd160(Digest.sha256(hello))));

        byte[] a = Digest.sha256(empty);
        byte[] b = Digest.sha256(empty,empty,empty);
        assertArrayEquals(a, b);
        
        a = Digest.sha256("01234".getBytes(),"56789".getBytes());
        b = Digest.sha256("0123456789".getBytes());
        assertArrayEquals(a, b);
        
    }
    
}

/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class DictionaryDeflateTest
{
    /**
     * Test of deflate method, of class DeflateWithDictionary.
     * @throws java.lang.Exception
     */
    @Test
    public void testAll() throws Exception
    {
        BigInteger data = new BigInteger("3081e030819806092a864886f70d01030130818a024100e03f372b9f7955b96ff430842829b43eb348d69140f0558e1227288134347aa740733cb820f624f5ec3cb56c8be6e9f3e01eca148ad366b75b56032741468a4f02410089a713701df953cbee3172fbf8ed4638264774f7e7b97150538807363f36380ec18f07582a2a1e820dcc8783af3a006ab3d0234169a95242b546ef3c6ddb6f9b020201fe03430002404d236a61fcbe419c4af726fe9f4ab81a93c293cb42474df460500d04756e502606060c87ce98bdbaeafef245d753256cd8e47a7aba99bc34d3d882bd41c95d89", 16);
        BigInteger dict = new BigInteger("3081e030819806092a864886f70d01030130818a024100e03f372b9f7955b96ff430842829b43eb348d69140f0558e1227288134347aa740733cb820f624f5ec3cb56c8be6e9f3e01eca148ad366b75b56032741468a4f02410089a713701df953cbee3172fbf8ed4638264774f7e7b97150538807363f36380ec18f07582a2a1e820dcc8783af3a006ab3d0234169a95242b546ef3c6ddb6f9b020201ff034300024002f667bde2b0a993847345035cf503eab15bb62c046dee054c59db5af183eeebb868ec5be355d4f6060cdd61769409bda837ef7b772ff88557d5682b63788d6d", 16);
        DictionaryDeflate dictionaryDeflate = new DictionaryDeflate(false);
        
        // Compress the bytes
        byte[] deflated = dictionaryDeflate.deflate(dict.toByteArray(), data.toByteArray());
        // Decompress the bytes
        byte[] inflated = dictionaryDeflate.inflate(dict.toByteArray(), deflated);
        
        BigInteger result = new BigInteger(inflated);
        assertEquals(data, result);
    }

    
}

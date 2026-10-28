/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.crypto.bips;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ExtKeyTest
{

    static final int[] PRV = {0x0488ade4, 0x049d7878, 0x04b2430c, 0x0295b005, 0x02aa7a99, 0x04358394, 0x044a4e28, 0x045f18bc, 0x024285b5, 0x02575048};
    static final int[] PUB = {0x0488b21e, 0x049d7cb2, 0x04b24746, 0x0295b43f, 0x02aa7ed3, 0x043587cf, 0x044a5262, 0x045f1cf6, 0x024289ef, 0x02575483};

    /**
     * Test of getPubPrvKey method, of class ExtKey.
     */
    @Test
    public void testGetPubPrvKey()
    {
        for(int item : PRV)
        {
            assertEquals(ExtKey.PRVKEY, ExtKey.getPubPrvKey(item),"item="+item);
        }
        for(int item : PUB)
        {
            assertEquals(ExtKey.PUBKEY, ExtKey.getPubPrvKey(item),"item="+item);
        }
    }

    /**
     * Test of prv2pub method, of class ExtKey.
     */
    @Test
    public void testPrv2pub()
    {
        for(int i=0;i<PRV.length;i++)
        {
            assertEquals(PUB[i], ExtKey.prv2pub(PRV[i]),"i="+i);
        }
    }
}

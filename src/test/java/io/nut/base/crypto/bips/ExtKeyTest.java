/*
 *  ExtKeyTest.java
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
package io.nut.base.crypto.bips;

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

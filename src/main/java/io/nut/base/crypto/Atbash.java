/*
 *  Atbash.java
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
package io.nut.base.crypto;

import java.util.HashMap;

/**
 *
 * @author franci
 */
public class Atbash
{
    private final HashMap<Character,Character> map = new HashMap<>();

    public Atbash(int k, boolean caseSwap)
    {
        char[][] letters = new char[2][13];
        for(int i=0,j=26-k;i<13;i++,j++)
        {
            letters[0][i] = (char) ((j%26)+'A');
        }
        for(int i=0,j=26-k+13;i<13;i++,j++)
        {
            letters[1][12-i] = (char) ((j%26)+'A');
        }
        for(int i=0;i<13;i++)
        {
            char plainUpper =letters[0][i];
            char plainLower =Character.toLowerCase(plainUpper);
            char codedUpper =letters[1][i];
            char codedLower =Character.toLowerCase(codedUpper);
            
            map.put(plainUpper, caseSwap ? codedLower : codedUpper);
            map.put(plainLower, caseSwap ? codedUpper : codedLower);

            map.put(codedLower, caseSwap ? plainUpper : plainLower);
            map.put(codedUpper, caseSwap ? plainLower : plainUpper);
        }
        
        char[][] numbers = new char[2][5];
        for(int i=0,j=10-k;i<5;i++,j++)
        {
            numbers[0][i] = (char) ((j%10)+'0');
        }
        for(int i=0,j=10-k+5;i<5;i++,j++)
        {
            numbers[1][4-i] = (char) ((j%10)+'0');
        }
        for(int i=0;i<5;i++)
        {
            map.put(numbers[0][i], numbers[1][i]);
            map.put(numbers[1][i], numbers[0][i]);
        }
    }

    public char get(char p)
    {
        return map.getOrDefault(p, p);
    }

    public String get(String s)
    {
        char[] c = s.toCharArray();
        for(int i=0;i<c.length;i++)
        {
            c[i] = get(c[i]);
        }
        return new String(c);
    }
}

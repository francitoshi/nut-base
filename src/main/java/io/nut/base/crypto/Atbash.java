/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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

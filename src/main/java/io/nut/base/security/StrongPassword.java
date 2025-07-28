/*
 *  StrongPassword.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.security;

import io.nut.base.util.Chars;
import io.nut.base.util.Strings;
import java.util.HashSet;
import java.util.Set;

/**
 * A class for checking password strength, programmed by AI Studio
 * inspired by 'strong-password.js' from Jeff Todnem.
 * 
 * https://www.uic.edu/apps/strong-password/
 * 
 */
public class StrongPassword 
{
    final int minPwdLen;

    public StrongPassword(int minPwdLen)
    {
        this.minPwdLen = minPwdLen;
    }
    
    // --- Constantes de Multiplicadores (extraídas del JS) ---
    private static final int MULT_LENGTH = 4;
    private static final int MULT_UPPERCASE_BONUS = 2;
    private static final int MULT_LOWERCASE_BONUS = 2;
    private static final int MULT_NUMBER_BONUS = 4;
    private static final int MULT_SYMBOL_BONUS = 6;
    private static final int MULT_MID_CHAR_BONUS = 2;
    private static final int MULT_REQUIREMENTS_BONUS = 2;
    private static final int MULT_CONSEC_UPPERCASE_PENALTY = 2;
    private static final int MULT_CONSEC_LOWERCASE_PENALTY = 2;
    private static final int MULT_CONSEC_NUMBER_PENALTY = 2;
    private static final int MULT_SEQ_ALPHA_PENALTY = 3;
    private static final int MULT_SEQ_NUMBER_PENALTY = 3;
    private static final int MULT_SEQ_SYMBOL_PENALTY = 3;

    // --- Cadenas para búsqueda de secuencias ---
    private static final String S_ALPHAS = "abcdefghijklmnopqrstuvwxyz";
    private static final String S_NUMERICS = "01234567890";
    private static final String S_SYMBOLS = ")!@#$%^&*()";

    public enum Level{ TooShort, VeryWeak, Weak, Good, Strong, VeryStrong }

    int analyze(String password)
    {
        return analyze(password.toCharArray());
    }
    
    /**
     * Analiza la contraseña y devuelve su puntuación y nivel de complejidad.
     * @param password La contraseña a analizar.
     * @return Un objeto StrengthResult con el resultado.
     */
    public int analyze(char[] password) 
    {
        if (password==null || password.length<minPwdLen || Chars.trim(password).length==0) 
        {
            return 0;
        }

        int numUpperCase = 0;
        int numLowerCase = 0;
        int numNumbers = 0;
        int numSymbols = 0;
        int numMidChars = 0;
        
        double repetitionIncrement = 0;
        int numConsecUpperCase = 0;
        int numConsecLowerCase = 0;
        int numConsecNumbers = 0;
        int numSeqAlpha = 0;
        int numSeqNumber = 0;
        int numSeqSymbol = 0;

        // Bonificación base por longitud
        int score = password.length * MULT_LENGTH;

        // --- Bucle principal para contar caracteres y secuencias consecutivas ---
        for (int i = 0; i < password.length; i++) 
        {
            char c = password[i];
            if (Character.isUpperCase(c)) 
            {
                numUpperCase++;
                if (i > 0 && Character.isUpperCase(password[i-1])) numConsecUpperCase++;
            } 
            else if (Character.isLowerCase(c)) 
            {
                numLowerCase++;
                if (i > 0 && Character.isLowerCase(password[i-1])) numConsecLowerCase++;
            } 
            else if (Character.isDigit(c)) 
            {
                numNumbers++;
                if (i > 0 && i < password.length - 1) numMidChars++;
                if (i > 0 && Character.isDigit(password[i-1])) numConsecNumbers++;
            } 
            else 
            { // Símbolo
                numSymbols++;
                if (i > 0 && i < password.length - 1) numMidChars++;
                // La lógica del JS para símbolos consecutivos es más compleja, pero este es un buen proxy
                if (i > 0 && !Character.isLetterOrDigit(password[i-1])) 
                {
                    // El JS original no tenía una variable nConsecSymbol, pero sí un chequeo.
                    // Lo dejamos fuera para ser fiel al script que aplica la penalización por tipo, no por símbolo.
                }
            }
        }
        
        // --- Penalización por caracteres repetidos (lógica O(n^2) del script original) ---
        Set<Character> uniqueChars = new HashSet<>();
        for(char c : password)
        {
            uniqueChars.add(c);
        }
        
        int numRepetitions = password.length - uniqueChars.size();
        if (numRepetitions > 0) 
        {
            for (int i = 0; i < password.length; i++) 
            {
                boolean charExists = false;
                for (int j = 0; j < password.length; j++) 
                {
                    if (password[i] == password[j] && i != j) 
                    {
                        charExists = true;
                        repetitionIncrement += Math.abs((double) password.length / (j - i));
                    }
                }
                if (charExists) 
                {
                    // La lógica del JS era compleja. Simplificamos a una penalización basada en la cuenta.
                    // Para ser 100% fiel, replicamos el cálculo final.
                }
            }
            int unqCharCount = password.length - numRepetitions;
            repetitionIncrement = (unqCharCount > 0) ? Math.ceil(repetitionIncrement / unqCharCount) : Math.ceil(repetitionIncrement);
        }

        // --- Búsqueda de patrones secuenciales (abc, 123, !@#) ---
        char[] lowerCasePassword = Chars.toLowerCase(password);
        for (int i = 0; i < S_ALPHAS.length() - 2; i++) 
        {
            String fwd = S_ALPHAS.substring(i, i + 3);
            String rev = Strings.reverse(fwd);
            if (Chars.contains(lowerCasePassword, fwd) || Chars.contains(lowerCasePassword, rev)) numSeqAlpha++;
        }
        for (int i = 0; i < S_NUMERICS.length() - 2; i++) 
        {
            String fwd = S_NUMERICS.substring(i, i + 3);
            String rev = Strings.reverse(fwd);
            if (Chars.contains(lowerCasePassword, fwd) || Chars.contains(lowerCasePassword, rev)) numSeqNumber++;
        }
        for (int i = 0; i < S_SYMBOLS.length() - 2; i++) 
        {
            String fwd = S_SYMBOLS.substring(i, i + 3);
            String rev = Strings.reverse(fwd);
            if (Chars.contains(lowerCasePassword, fwd) || Chars.contains(lowerCasePassword, rev)) numSeqSymbol++;
        }

        // --- Aplicar Bonificaciones ---
        if (numUpperCase > 0 && numUpperCase < password.length) score += (password.length - numUpperCase) * MULT_UPPERCASE_BONUS;
        if (numLowerCase > 0 && numLowerCase < password.length) score += (password.length - numLowerCase) * MULT_LOWERCASE_BONUS;
        if (numNumbers > 0 && numNumbers < password.length) score += numNumbers * MULT_NUMBER_BONUS;
        if (numSymbols > 0) score += numSymbols * MULT_SYMBOL_BONUS;
        if (numMidChars > 0) score += numMidChars * MULT_MID_CHAR_BONUS;

        // --- Aplicar Penalizaciones ---
        if ((numLowerCase > 0 || numUpperCase > 0) && numSymbols == 0 && numNumbers == 0) score -= password.length; // Solo letras
        if (numLowerCase == 0 && numUpperCase == 0 && numSymbols == 0 && numNumbers > 0) score -= password.length; // Solo números
        if (numRepetitions > 0) score -= (int) repetitionIncrement;
        if (numConsecUpperCase > 0) score -= numConsecUpperCase * MULT_CONSEC_UPPERCASE_PENALTY;
        if (numConsecLowerCase > 0) score -= numConsecLowerCase * MULT_CONSEC_LOWERCASE_PENALTY;
        if (numConsecNumbers > 0) score -= numConsecNumbers * MULT_CONSEC_NUMBER_PENALTY;
        if (numSeqAlpha > 0) score -= numSeqAlpha * MULT_SEQ_ALPHA_PENALTY;
        if (numSeqNumber > 0) score -= numSeqNumber * MULT_SEQ_NUMBER_PENALTY;
        if (numSeqSymbol > 0) score -= numSeqSymbol * MULT_SEQ_SYMBOL_PENALTY;

        // --- Bonificación por Requisitos ---
        int numRequirements = 0;
        if (password.length >= minPwdLen) numRequirements++;
        if (numUpperCase > 0) numRequirements++;
        if (numLowerCase > 0) numRequirements++;
        if (numNumbers > 0) numRequirements++;
        if (numSymbols > 0) numRequirements++;
        
        int minReqChars = (password.length >= minPwdLen) ? 3 : 4;
        if (numRequirements > minReqChars) 
        {
            score += numRequirements * MULT_REQUIREMENTS_BONUS;
        }

        // --- Normalizar Puntuación Final y Determinar Nivel ---
        score = Math.max(0, Math.min(100, score));

        return score;
    }
    
    public static Level getLevel(int score)
    {
        Level level;
        if(score == 0)
        {
            level = Level.TooShort;
        } 
        else if (score < 20) 
        {
            level = Level.VeryWeak;
        } 
        else if (score < 40) 
        {
            level = Level.Weak;
        } 
        else if (score < 60) 
        {
            level = Level.Good;
        } 
        else if (score < 80) 
        {
            level = Level.Strong;
        } 
        else 
        {
            level = Level.VeryStrong;
        }
        return level;
    }

}
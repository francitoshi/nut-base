/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class PasswordPolicy
{
    private static final Pattern LETTER_PATTERN = Pattern.compile("\\p{L}");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("\\p{Ll}");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("\\p{Lu}");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\p{N}");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{P}");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("\\p{S}");

    public static class PolicyFaultException extends Exception
    {
    }
    public static class EmptyException extends PolicyFaultException
    {
    }
    public static class TooShortException extends PolicyFaultException
    {
    }
    public static class TooLongException extends PolicyFaultException
    {
    }
    public static class NeedLetterException extends PolicyFaultException
    {
    }
    public static class NeedLowerCaseException extends PolicyFaultException
    {
    }
    public static class NeedUpperCaseException extends PolicyFaultException
    {
    }
    public static class NeedNumberException extends PolicyFaultException
    {
    }
    public static class NeedPunctuationException extends PolicyFaultException
    {
    }
    public static class NeedSymbolException extends PolicyFaultException
    {
    }
    public static class WrongPatternException extends PolicyFaultException
    {
    }
    public static class TooPredictableException extends PolicyFaultException
    {
    }
    public enum Need
    {
        Letter, Lowercase, Uppercase, Number, Punctuation,Symbol
    }
    public final int minSize;
    public final int maxSize;
    public final Pattern pattern;
    public final boolean needLetter;
    public final boolean needUppercase;
    public final boolean needLowercase;
    public final boolean needNumber;
    public final boolean needPunctuation;
    public final boolean needSymbol;

    public PasswordPolicy(int minSize, int maxSize, String pattern, Need... needs)
    {
        HashSet<Need> needsSet = new HashSet<>(Arrays.asList(needs));
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.pattern = pattern!=null ? Pattern.compile(pattern) : null;
        this.needLetter = needsSet.contains(Need.Letter);
        this.needUppercase = needsSet.contains(Need.Uppercase);
        this.needLowercase = needsSet.contains(Need.Lowercase);
        this.needNumber = needsSet.contains(Need.Number);
        this.needPunctuation = needsSet.contains(Need.Punctuation);
        this.needSymbol = needsSet.contains(Need.Symbol);
    }
    public boolean verifySafe(String password, String... related) 
    {
        try
        {
            verify(password, related);
            return false;
        }
        catch (PolicyFaultException ex)
        {
            return true;
        }
    }
    public void verify(String password, String... related) throws EmptyException, TooShortException, TooLongException, WrongPatternException, NeedLetterException, NeedLowerCaseException, NeedUpperCaseException, NeedNumberException, NeedPunctuationException, NeedSymbolException, TooPredictableException
    {
        if(password.length()==0 && minSize>0)
        {
            throw new EmptyException();
        }
        if(password.length()<minSize)
        {
            throw new TooShortException();
        }
        if(password.length()>maxSize)
        {
            throw new TooLongException();
        }
        if(pattern!=null && !pattern.matcher(password).matches())
        {
            throw new WrongPatternException();
        }
        if(needLetter && !LETTER_PATTERN.matcher(password).find())
        {
            throw new NeedLetterException();
        }
        if(needLowercase && !LOWERCASE_PATTERN.matcher(password).find())
        {
            throw new NeedLowerCaseException();
        }
        if(needUppercase && !UPPERCASE_PATTERN.matcher(password).find())
        {
            throw new NeedUpperCaseException();
        }
        if(needNumber && !NUMBER_PATTERN.matcher(password).find())
        {
            throw new NeedNumberException();
        }
        if(needPunctuation && !PUNCTUATION_PATTERN.matcher(password).find())
        {
            throw new NeedPunctuationException();
        }
        if(needSymbol && !SYMBOL_PATTERN.matcher(password).find())
        {
            throw new NeedSymbolException();
        }
        if(related.length>0)
        {
            String pending = password.toLowerCase();
            for(String item : related)
            {
                pending = pending.replace(item.toLowerCase(), "");
            }
            if(pending.length()<minSize)
            {
                throw new TooPredictableException();
            }
        }
        if(pattern!=null && !pattern.matcher(password).matches())
        {
            throw new WrongPatternException();
        }
    }

}

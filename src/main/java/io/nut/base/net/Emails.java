/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.net;

import java.util.regex.Pattern;

/**
 * Utility for validating email addresses, with optional support for local
 * domains (without a dot, like "localhost").
 */
public class Emails
{
    // Local part: letters, digits and some symbols allowed by RFC 5322 (simplified and practical version)
    private static final String LOCAL_PART_REGEX = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*";
 
    // "Normal" domain: at least one tag + a period + a TLD of at least 2 characters (e.g., example.com).
    // The TLD maintains the same character set [a-zA-Z0-9-] (not just letters) to avoid rejecting
    // internationalized domains in punycode format (e.g., example.xn--p1ai), which are valid and legitimate.
    private static final String DOMAIN_REGEX = "[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9-]{2,}";
 
    // "local" domain: a single label without a dot (e.g., localhost, mail server, intranet)
    private static final String LOCAL_DOMAIN_REGEX = "[a-zA-Z0-9-]+";
 
    private static final Pattern PATTERN_STANDARD = Pattern.compile("^" + LOCAL_PART_REGEX + "@" + DOMAIN_REGEX + "$");
 
    private static final Pattern PATTERN_WITH_LOCAL_DOMAIN = Pattern.compile("^" + LOCAL_PART_REGEX + "@(?:" + DOMAIN_REGEX + "|" + LOCAL_DOMAIN_REGEX + ")$");
     
    
    /**
     * Validates an email address.
     *
     * @param email the address to validate (can be null)
     * @param allowLocalDomains if true, domains without a period are accepted
     * (e.g., "alice@localhost", "bob@mailserver"); if false, only domains with
     * at least one period are accepted (e.g., "alice@example.com")
     * @return true if the address is valid according to the specified criteria,
     * false otherwise
     */
    public static boolean isValidEmail(String email, boolean allowLocalDomains) 
    {
        if (email == null || email.isEmpty()) 
        {
            return false;
        }
 
        Pattern pattern = allowLocalDomains ? PATTERN_WITH_LOCAL_DOMAIN : PATTERN_STANDARD;
        return pattern.matcher(email).matches();
    }
    public static boolean isValidEmail(String email) 
    {
        return isValidEmail(email, false);
    }
 
    public static String[] parseEmailAddress(String input)
    {
        if(input==null)
        {
            return null;
        }
        
        input = input.trim();
        
        if (input.isEmpty())
        {
            return new String[]{"", ""};
        }

        String name = "";
        String email = "";

        // Search for email among < >
        int angleStart = input.indexOf('<');
        int angleEnd = input.indexOf('>');

        if (angleStart >= 0 && angleEnd > angleStart)
        {
            // Format "Name <email>" or "<email>"
            email = input.substring(angleStart + 1, angleEnd).trim();
            name = input.substring(0, angleStart).trim();
        }
        else
        {
            // Format "email" plain with no angles
            // Verify that an email look like a valid email (it has an @)
            if (input.contains("@"))
            {
                email = input;
                name = "";
            }
        }

        // Minimum validation: the email must contain '@'
        if (!email.contains("@"))
        {
            email = "";
        }

        return new String[]{ name, email };
    }
}

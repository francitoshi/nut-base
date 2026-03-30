/*
 * Emails.java
 *
 * Copyright (c) 2014-2026 francitoshi@gmail.com
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
package io.nut.base.net;

import java.util.regex.Pattern;

/**
 *
 * @author franci
 */
public class Emails
{
    public static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    //public static final String EMAIL_REGEX = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:[A-Z]{2}|com|org|net|edu|gov|mil|biz|info|mobi|name|aero|asia|jobs|museum)\\b";
    public static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);    
    public static boolean isValidEmail(String value)
    {
        return EMAIL_PATTERN.matcher(value).matches();
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

        // Validación mínima: el email debe contener '@'
        if (!email.contains("@"))
        {
            email = "";
        }

        return new String[]{ name, email };
    }
}

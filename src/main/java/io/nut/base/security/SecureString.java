/*
 *  SecureString.java
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

/**
 * Represents a passphrase or other sensitive character sequence that is handled
 * securely.
 * <p>
 * The primary goal of this interface is to provide a safer alternative to using
 * {@link java.lang.String} for sensitive data. Implementations should avoid
 * storing the character data in plaintext within memory. Instead, they should
 * use techniques like encryption or obfuscation.
 * <p>
 * This interface extends {@link AutoCloseable}, making it suitable for use in
 * {@code try-with-resources} statements. This ensures that the {@link #clear()}
 * method is called automatically, securely wiping the sensitive data from
 * memory once it is out of scope.
 * <p>
 * By also extending {@link CharSequence}, implementations can be passed to many
 * existing APIs without the need to first convert them to an insecure
 * {@code String}.
 *
 * @see AutoCloseable
 * @see CharSequence
 */
public interface SecureString extends AutoCloseable, CharSequence
{
    /**
     * Returns a plaintext copy of the passphrase as a primitive {@code char[]}.
     * <p>
     * <b>Security Warning:</b> This method provides direct access to the
     * sensitive data. It is the <strong>critical responsibility of the
     * caller</strong> to securely overwrite this array (e.g., by filling it
     * with zeros) as soon as it is no longer needed. The recommended pattern is
     * a {@code try-finally} block:
     * <pre>{@code
     * char[] passphrase = null;
     * try 
     * {
     *     passphrase = securePass.getChars();
     *     // ... use the passphrase array ...
     * } 
     * finally 
     * {
     *     if (passphrase != null) 
     *     {
     *         Arrays.fill(passphrase, '\0');
     *     }
     * }
     * }
     * </pre>
     *
     * @return a new {@code char[]} containing the plaintext passphrase.
     */
    char[] toCharArray();

}

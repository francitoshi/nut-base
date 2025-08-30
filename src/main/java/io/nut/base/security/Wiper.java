/*
 *  Wiper.java
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

import java.lang.reflect.Field;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * The {@code Wiper} class provides utility methods for securely wiping
 * cryptographic key material from memory. This is crucial for preventing
 * sensitive key data from being exposed through memory dumps or other forensic
 * analysis techniques after it is no longer needed.
 * <p>
 * It attempts to overwrite the byte arrays holding the key material with zeros.
 * While this offers a layer of security, it's important to understand that
 * Java's garbage collection and memory management might still leave traces of
 * the original data in other memory locations. For ultimate security, consider
 * using hardware security modules (HSMs) or secure enclaves.
 * </p>
 */
public abstract class Wiper
{
    
    /**
     * Attempts to securely wipe the key material stored within a
     * {@code SecretKeySpec} object. This method uses reflection to access the
     * private {@code key} field of the {@code SecretKeySpec} and overwrites its
     * contents with zeros.
     *
     * @param key The {@code SecretKeySpec} object whose key material
     * needs to be wiped.
     * @return {@code true} if the key material was successfully wiped,
     * {@code false} otherwise. This can fail if reflection access is denied,
     * the field is not found, or other runtime exceptions occur.
     */
    public static boolean wipeSecretKeySpec(SecretKeySpec key)
    {
        try
        {
            // Get the private 'key' field from SecretKeySpec
            Field keyField = SecretKeySpec.class.getDeclaredField("key");
            // Make the private field accessible
            keyField.setAccessible(true);
            
            // Get the byte array holding the key
            byte[] keyBytes = (byte[]) keyField.get(key);
            // If the key array exists, fill it with zeros
            if (keyBytes != null)
            {
                Arrays.fill(keyBytes, (byte) 0);
            }
            return true;
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex)
        {
            return false;
        }
    }

    /**
     * Attempts to securely wipe the key material stored within a generic
     * {@code SecretKey} object.
     * <p>
     * If the provided {@code SecretKey} is an instance of
     * {@code SecretKeySpec}, it delegates to
     * {@link #wipeSecretKeySpec(SecretKeySpec)} for a more targeted wipe.
     * Otherwise, it uses reflection to iterate through all declared fields of
     * the {@code SecretKey} object's class. If a field is found to be a
     * {@code byte[]} array, it attempts to overwrite its contents with zeros.
     * </p>
     * <p>
     * This method is a best-effort attempt and may not be exhaustive for all
     * {@code SecretKey} implementations, as key material might be stored in
     * different types of fields or in native memory.
     * </p>
     *
     * @param key The {@code SecretKey} object whose key material needs to
     * be wiped.
     * @return {@code true} if at least one {@code byte[]} field was found and
     * wiped, {@code false} otherwise (e.g., no {@code byte[]} fields were found
     * or accessible).
     */
    public static boolean wipeSecretKey(SecretKey key)
    {
        // Handle SecretKeySpec specifically for a more reliable wipe
        if (key instanceof SecretKeySpec)
        {
            if(wipeSecretKeySpec((SecretKeySpec) key))
            {
                return true;
            }
        }
        
        Class<?> clazz = key.getClass();
        // Get all declared fields of the SecretKey's class
        Field[] fields = clazz.getDeclaredFields();
        boolean foundAndWiped = false;

        // Iterate through all fields
        for (Field field : fields)
        {
            // Check if the field is a byte array
            if (field.getType().equals(byte[].class))
            {
                try
                {
                    // Make the private field accessible
                    field.setAccessible(true);
                    // Get the byte array instance
                    byte[] keyArray = (byte[]) field.get(key);

                    // If the array exists, fill it with zeros
                    if (keyArray != null)
                    {
                        Arrays.fill(keyArray, (byte) 0);
                        foundAndWiped = true;
                    }
                }
                catch (IllegalAccessException ex)
                {
                    // Ignore fields that are not accessible even after setAccessible(true)
                    // or other access exceptions. This might happen with final fields or
                    // security manager restrictions.
                }
            }
        }
        return foundAndWiped;
    }
}

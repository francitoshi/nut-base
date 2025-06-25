/*
 *  EncryptedMapWrapperTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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

import io.nut.base.serializer.StringSerializer;
import java.io.File;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 *
 * @author franci
 */
public class EncryptedMapWrapperTest
{
    static final int ROUNDS = 8;
    static final int KEYBITS = 256;
    static final Kripto KRIPTO = Kripto.getInstance(true).setMinimumPbkdf2Rounds(ROUNDS);

    private static final String TEST_KEY = "test_key";
    private static final String API_KEY = "api_key";
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String USER = "username";

    @Test
    public void testMain() throws Exception
    {
        char[] passphrase = "passphrase".toCharArray();
        
        System.out.println("=== HashMap ===");
        Map<String, String> hashMap = new HashMap<>();
        
        exampleMap(hashMap, passphrase, "example1");
        File file = new File("encrypted.db");
        System.out.println("\n=== MapDB ===");
        try(DB db = DBMaker.fileDB(file).make())
        {
            file.deleteOnExit();
            Map<String, String> mapdbMap = (Map<String, String>) db.hashMap("userdata").createOrOpen();
            exampleMap(mapdbMap, passphrase, "example2");
        }        

        demostrationCipher(passphrase);
    }
    private static final StringSerializer STRING_SERIALIZER = new StringSerializer();
    private static void exampleMap(Map<String,String> map, char[] passphrase, String saltSeed) throws InvalidKeySpecException 
    {
        EncryptedMapWrapper<String, String> encryptedMap = new EncryptedMapWrapper<>(KRIPTO, map, passphrase, saltSeed, ROUNDS, KEYBITS, STRING_SERIALIZER, STRING_SERIALIZER);
        
        // Insert data
        encryptedMap.put(USER, "peter123");
        encryptedMap.put(PASSWORD, "mySecretKey456");
        encryptedMap.put(EMAIL, "juan@example.com");
        encryptedMap.put(API_KEY, "sk-1234567890abcdef");
        
        // Read data
        System.out.println("User: " + encryptedMap.get(USER));
        System.out.println("Email: " + encryptedMap.get(EMAIL));
        System.out.println("API Key: " + encryptedMap.get(API_KEY));
        
        // Operations with the Map
        assertTrue(encryptedMap.containsKey(USER));
        assertFalse(encryptedMap.containsKey("telefone"));
        System.out.println("size: " + encryptedMap.size());
        
        // View encrypted data in the underlying HashMap
        System.out.println("\nENCRYPTED data stored in HashMap:");
        map.forEach((k, v) -> {
            String shortK = k.length() > 32 ? k.substring(0, 32) + "..." : k;
            String shortV = v.length() > 32 ? v.substring(0, 32) + "..." : v;
            System.out.println("  " + shortK + " -> " + shortV);
        });
        
        // Delete a key
        String removedValue = encryptedMap.remove(PASSWORD);
        System.out.println("removed value: " + removedValue);
        System.out.println("size after remove: " + encryptedMap.size());
    }
    
    private static void demostrationCipher(char[] passphrase) throws Exception 
    {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        
        EncryptedMapWrapper<String, String> encrypted1 = new EncryptedMapWrapper<>(KRIPTO, map1, passphrase, "salt", ROUNDS, KEYBITS, STRING_SERIALIZER, STRING_SERIALIZER);
        EncryptedMapWrapper<String, String> encrypted2 = new EncryptedMapWrapper<>(KRIPTO, map2, passphrase, "salt", ROUNDS, KEYBITS, STRING_SERIALIZER, STRING_SERIALIZER);
        
        // Insert the same key and value into both maps
        encrypted1.put(TEST_KEY, "test_value");
        encrypted2.put(TEST_KEY, "test_value");
        
        String encryptedKey1 = map1.keySet().iterator().next();
        String encryptedKey2 = map2.keySet().iterator().next();
        
        String encryptedValue1 = map1.values().iterator().next();
        String encryptedValue2 = map2.values().iterator().next();
        
        System.out.println("KEYS (deterministic):");
        System.out.println("  Map1: " + encryptedKey1.substring(0, 32) + "...");
        System.out.println("  Map2: " + encryptedKey2.substring(0, 32) + "...");
        System.out.println("  ¿equals?: " + encryptedKey1.equals(encryptedKey2));
        
        System.out.println("\nVALUES (undeterministic):");
        System.out.println("  Map1: " + encryptedValue1.substring(0, 32) + "...");
        System.out.println("  Map2: " + encryptedValue2.substring(0, 32) + "...");
        System.out.println("  ¿equals?: " + encryptedValue1.equals(encryptedValue2));
        
        System.out.println("\rrecover:");
        System.out.println("  Map1 get('test_key'): " + encrypted1.get(TEST_KEY));
        System.out.println("  Map2 get('test_key'): " + encrypted2.get(TEST_KEY));
    }
}

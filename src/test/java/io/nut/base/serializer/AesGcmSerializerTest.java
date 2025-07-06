/*
 *  AesGcmSerializerTest.java
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
package io.nut.base.serializer;

import io.nut.base.crypto.Derive;
import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.Kripto.SecretKeyAlgorithm;
import io.nut.base.crypto.Kripto.SecretKeyDerivation;
import io.nut.base.util.Strings;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class AesGcmSerializerTest
{
    @Test
    public void testSome() throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        Kripto kripto = Kripto.getInstance().setMinDeriveRounds(8);
        
        char[] passphrase = "passphrase".toCharArray();
        byte[] salt = "salt".getBytes();
        Derive derive = kripto.getDerivePBKDF2WithHmacSHA256();
        SecretKey sk = derive.deriveSecretKey(passphrase, salt, 8, 256, SecretKeyAlgorithm.AES);

        StringSerializer ss = new StringSerializer();
        
        AesGcmSerializer<String> instance = new AesGcmSerializer<>(sk, ss, kripto);

        String s0 = "";
        String r0 = instance.fromBytes(instance.toBytes(s0));
        assertEquals(s0, r0);
        
        String s1 = "Hello World";
        String r1 = instance.fromBytes(instance.toBytes(s1));
        assertEquals(s1, r1);
                
        for(int i=0;i<100;i++)
        {
            String s = Strings.repeat('a', i);
            String r = instance.fromBytes(instance.toBytes(s));
            assertEquals(s, r);
        }
    }
    
}

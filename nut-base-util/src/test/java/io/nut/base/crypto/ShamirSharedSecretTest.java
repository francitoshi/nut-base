/*
 *  ShamirSharedSecretTest.java
 *
 *  Copyright (C) 2018-2026 francitoshi@gmail.com
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
 *
 */
package io.nut.base.crypto;

import io.nut.base.util.Shuffles;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 *
 * @author franci
 */
public class ShamirSharedSecretTest
{
    /**
     * Test of split method, of class ShamirSharedSecret.
     */
    @Test
    public void testSplit()
    {
        byte[] secret = "Hello World!!!".getBytes();
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(2, 2);
            byte[][] result = instance.split(secret);
            assertNotNull(result);
            assertEquals(2, result.length);
        }
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(3, 2);
            byte[][] result = instance.split(secret);
            assertNotNull(result);
            assertEquals(3, result.length);
        }
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(5, 2);
            byte[][] result = instance.split(secret);
            assertNotNull(result);
            assertEquals(5, result.length);
        }
    }

    /**
     * Test of join method, of class ShamirSharedSecret.
     */
    @Test
    public void testJoin()
    {
        byte[] secret = "Hello World!!!".getBytes();
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(3, 2);
            byte[][] result = instance.split(secret);
            assertArrayEquals(secret, instance.join(new byte[][]{result[0],result[1]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[0],result[2]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[1],result[2]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[2],result[1]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[2],result[0]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[1],result[0]}));

            assertArrayEquals(secret, instance.join(new byte[][]{result[2],result[1],result[0]}));
            assertArrayEquals(secret, instance.join(result));
            
            assertArrayEquals(secret, instance.join(new byte[][]{result[1],result[1],result[0]}));
        }
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(5, 3);
            byte[][] result = instance.split(secret);
            byte[] phrase = instance.join(result);
            assertArrayEquals(secret, phrase);
        }
        {
            Shuffles shuffles = new Shuffles();
            
            secret = "Somewhere in la Mancha, in a place whose name I do not care to remember, a gentleman lived not long ago, one of those who has a lance and ancient shield on a shelf and keeps a skinny nag and a greyhound for racing.".getBytes();
            // exaustive test
            for(int p=2;p<8;p++)
            {
                for(int t=2;t<=p;t++)
                {
                    System.out.print(t+"/"+p);
                    ShamirSharedSecret instance = new ShamirSharedSecret(p, t);
                    byte[][] result = instance.split(secret);
                    System.out.println("secret.length="+secret.length);
                    System.out.println("result[0].length="+result[0].length);
                    
                    Shuffles.shuffle(result);
                    
                    // with t sub-keys secret should be known
                    result = Arrays.copyOf(result, t);
                    byte[] phrase = instance.join(result);
                    assertArrayEquals(secret, phrase);
                    System.out.print("✔");
                    
                    // with t-1 sub-keys secret should be unknown
                    result = Arrays.copyOf(result, t-1);
                    phrase = instance.join(result);
                    assertFalse(Arrays.equals(secret, phrase));
                    System.out.println("✕");
                }
            }
        }
        
        
    }
  
    /**
     *
     * Extended testing for {@link ShamirSharedSecret}.
     *
     * The existing test (ShamirSharedSecretTest) covers the basic split/join
     * cycle with a fixed-text secret. These tests add:
     *
     * 1. {@link #testSecrecyBelowThreshold} — statistical secrecy property:
     * with fewer parts than the threshold k, the reconstruction should NOT
     * match the original secret, for every combination of (n, k) and various
     * secret sizes.
     *
     * 2. {@link #testSplitProducesIndependentParts} — the parts produced by
     * split() should all be distinct from each other, have the correct length
     * (secret + 1 byte identifier), and not contain the secret in plaintext.
     */
    // -------------------------------------------------------------------------
    // TEST 1 — Property secrecy: with k-1 parts the original cannot be recovered
    // -------------------------------------------------------------------------
    /**
     * Verifies the fundamental property of Shamir's scheme: with any subset of
     * parts of a size strictly smaller than the threshold {@code k},
     * reconstruction returns bytes distinct from the original secret.
     * <p>
     * All (n, k) configurations are tested, with n between 2 and 7 and k ≤ n,
     * and several secret sizes (1, 16, 32, 128 bytes) generated randomly. For
     * each configuration, all combinations of exactly k-1 parts are checked,
     * not just an ad hoc subset.
     */
    @ParameterizedTest(name = "secretSize={0} bytes")
    @ValueSource(ints = { 1, 16, 32, 128})
    public void testSecrecyBelowThreshold(int secretSize)
    {
        SecureRandom rng = new SecureRandom();
        byte[] secret = new byte[secretSize];
        rng.nextBytes(secret);

        for (int n = 2; n <= 7; n++)
        {
            for (int k = 2; k <= n; k++)
            {
                // Checking the threshold only makes sense if k > 1 (already guaranteed
                // by ShamirScheme.of, but explicitly reminded in the test)
                if (k < 2)
                {
                    continue;
                }

                ShamirSharedSecret sss = new ShamirSharedSecret(n, k);
                byte[][] parts = sss.split(secret);

                // Check all combinations of exactly k-1 parts
                int subsetSize = k - 1;
                int[] indexes = firstIndexes(subsetSize, n);

                do
                {
                    byte[][] subset = selectParts(parts, indexes, subsetSize);
                    byte[] recovered = sss.join(subset);

                    assertFalse(Arrays.equals(secret, recovered),
                            String.format(
                                    "With only %d/%d parts (threshold=%d) the secret was recovered — "
                                    + "n=%d, k=%d, secretSize=%d, indexes=%s",
                                    subsetSize, n, k, n, k, secretSize, Arrays.toString(indexes)
                            )
                    );
                }
                while (nextCombination(indexes, subsetSize, n));
            }
        }
    }

    // -------------------------------------------------------------------------
    // TEST 2 — Las partes son independientes, tienen la longitud correcta
    //          y no contienen el secreto en claro
    // -------------------------------------------------------------------------
    /**
     * Verifica tres propiedades estructurales de las partes producidas por
     * {@link ShamirSharedSecret#split(byte[])}:
     *
     * <ol>
     * <li><b>Longitud correcta:</b> cada parte tiene exactamente
     * {@code secret.length + 1} bytes (1 byte de identificador de share + los
     * bytes del polinomio evaluado).</li>
     * <li><b>Partes distintas:</b> ningún par de partes es idéntico. Si dos
     * partes fuesen iguales el esquema estaría roto o habría un fallo en el
     * generador.</li>
     * <li><b>Secreto no en claro:</b> ninguna parte contiene el secreto
     * original como subsecuencia contigua. Una parte que exponga el secreto
     * directamente anularía la confidencialidad del esquema. Se prueba con el
     * secreto en UTF-8 para que la comprobación sea legible y
     * determinista.</li>
     * </ol>
     *
     * Se parametriza sobre varios pares (n, k) representativos.
     */
    @ParameterizedTest(name = "n={0}, k={1}")
    @CsvSource({ "2,2", "3,2", "3,3", "5,2", "5,3", "5,5", "10,5", "20,10" })
    public void testSplitProducesIndependentParts(int n, int k)
    {
        // Secreto con contenido reconocible para facilitar la depuración si falla
        String secretText = "shamir-secret-sharing-test-2024";
        byte[] secret = secretText.getBytes(StandardCharsets.UTF_8);

        ShamirSharedSecret sss = new ShamirSharedSecret(n, k);
        byte[][] parts = sss.split(secret);

        // --- 1. Número correcto de partes ---
        assertEquals(n, parts.length,
                "split() debe devolver exactamente n=" + n + " partes");

        for (int i = 0; i < parts.length; i++)
        {
            // --- 2. Longitud de cada parte ---
            assertEquals(
                    secret.length + 1,
                    parts[i].length,
                    String.format("La parte %d tiene longitud %d; se esperaba %d (secreto+1)",
                            i, parts[i].length, secret.length + 1)
            );

            // --- 3. Partes distintas entre sí ---
            for (int j = i + 1; j < parts.length; j++)
            {
                assertFalse(
                        Arrays.equals(parts[i], parts[j]),
                        String.format("Las partes %d y %d son idénticas — n=%d, k=%d", i, j, n, k)
                );
            }

            // --- 4. El secreto no aparece en claro dentro de ninguna parte ---
            assertFalse(
                    containsSubarray(parts[i], secret),
                    String.format("La parte %d contiene el secreto en claro — n=%d, k=%d", i, n, k)
            );
        }

        // --- 5. Verificación de completitud: con k partes cualesquiera se recupera ---
        //   (complementario a testSecrecyBelowThreshold; asegura que el test
        //    de estructura no rompe la reconstrucción)
        byte[][] minimalSubset = Arrays.copyOf(parts, k);
        assertArrayEquals(
                secret,
                sss.join(minimalSubset),
                "Con exactamente k partes debe recuperarse el secreto original"
        );
    }

    // =========================================================================
    // Utilidades privadas
    // =========================================================================
    /**
     * Devuelve los primeros {@code size} índices en orden ascendente: [0, 1,
     * ..., size-1].
     */
    private static int[] firstIndexes(int size, int n)
    {
        int[] idx = new int[size];
        for (int i = 0; i < size; i++)
        {
            idx[i] = i;
        }
        return idx;
    }

    /**
     * Avanza {@code indices} a la siguiente combinación lexicográfica de
     * {@code size} elementos tomados de [0, n).
     *
     * @return {@code true} si existe una combinación siguiente; {@code false}
     * si ya se agotaron todas.
     */
    private static boolean nextCombination(int[] indices, int size, int n)
    {
        int i = size - 1;
        while (i >= 0 && indices[i] == n - size + i)
        {
            i--;
        }
        if (i < 0)
        {
            return false;
        }

        indices[i]++;
        for (int j = i + 1; j < size; j++)
        {
            indices[j] = indices[j - 1] + 1;
        }
        return true;
    }

    /**
     * Construye un array de partes seleccionando las posiciones indicadas por
     * {@code indices}.
     */
    private static byte[][] selectParts(byte[][] parts, int[] indices, int count)
    {
        byte[][] subset = new byte[count][];
        for (int i = 0; i < count; i++)
        {
            subset[i] = parts[indices[i]];
        }
        return subset;
    }

    /**
     * Devuelve {@code true} si {@code haystack} contiene {@code needle} como
     * subsecuencia contigua de bytes.
     */
    private static boolean containsSubarray(byte[] haystack, byte[] needle)
    {
        if (needle.length == 0)
        {
            return true;
        }
        if (haystack.length < needle.length)
        {
            return false;
        }

        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++)
        {
            for (int j = 0; j < needle.length; j++)
            {
                if (haystack[i + j] != needle[j])
                {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}

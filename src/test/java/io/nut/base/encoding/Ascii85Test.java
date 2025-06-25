package io.nut.base.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

/**
 *
 * @author franci
 */
public class Ascii85Test
{

    static final byte[] EMPTY_BYTES = new byte[0];
    static final char[] EMPTY_CHARS = new char[0];

    @Test
    public void testMisc()
    {
        assertNull(Ascii85.encode(null));
        assertNull(Ascii85.decode(null));

        assertArrayEquals(EMPTY_CHARS, Ascii85.encode(EMPTY_BYTES));
        assertArrayEquals(EMPTY_BYTES, Ascii85.decode(EMPTY_CHARS));

        Random random = new Random(0);
        
        //test random data, 0s or i
        for (int size = 0; size < 100; size++)
        {
            byte[] z = new byte[size];
            assertArrayEquals(z, Ascii85.decode(Ascii85.encode(z)));
            
            for (int i = 0; i < 256; i++)
            {
                byte[] b = new byte[i];
                random.nextBytes(b);
                assertArrayEquals(b, Ascii85.decode(Ascii85.encode(b)));
                
                Arrays.fill(z,(byte)i);
                assertArrayEquals(z, Ascii85.decode(Ascii85.encode(z)));
            }
        }
    }

    static final String TXT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
    static final String RES="9jqo^BlbD-BleB1DJ+*+F(f,q/0JhKF<GL>Cj@.4Gp$d7F!,L7@<6@)/0JDEF<G%<+EV:2F!,O<" +
                            "DJ+*.@<*K0@<6L(Df-\\0Ec5e;DffZ(EZee.Bl.9pF\"AGXBPCsi+DGm>@3BB/F*&OCAfu2/AKYi(" +
                            "DIb:@FD,*)+C]U=@3BN#EcYf8ATD3s@q?d$AftVqCh[NqF<G:8+EV:.+Cf>-FD5W8ARlolDIal(" +
                            "DId<j@<?3r@:F%a+D58'ATD4$Bl@l3De:,-DJs`8ARoFb/0JMK@qB4^F!,R<AKZ&-DfTqBG%G>u" +
                            "D.RTpAKYo'+CT/5+Cei#DII?(E,9)oF*2M7/c";
    
    // --- Pruebas de Funcionalidad y Casos Típicos ---
    @Test
    @DisplayName("Debe codificar y decodificar correctamente un texto estándar")
    void encodeAndDecode_shouldWorkForStandardText()
    {
        byte[] originalData = TXT.getBytes(StandardCharsets.UTF_8);
        char[] expectedEncoded = RES.toCharArray();

        char[] actualEncoded = Ascii85.encode(originalData);
        assertArrayEquals(expectedEncoded, actualEncoded, "La codificación no coincide con el valor esperado.");

        byte[] decodedData = Ascii85.decode(actualEncoded);
        assertArrayEquals(originalData, decodedData, "La decodificación no restauró los datos originales.");
    }

    @Test
    @DisplayName("Debe manejar correctamente el padding (entrada no múltiplo de 4)")
    void encodeAndDecode_shouldHandlePaddingCorrectly()
    {
        // "Man" tiene 3 bytes, requiere padding. Salida esperada de 4 chars.
        byte[] originalData = "Man".getBytes(StandardCharsets.UTF_8);
        char[] expectedEncoded = "9jqo".toCharArray();

        char[] actualEncoded = Ascii85.encode(originalData);
        assertArrayEquals(expectedEncoded, actualEncoded, "La codificación con padding es incorrecta.");

        byte[] decodedData = Ascii85.decode(actualEncoded);
        assertArrayEquals(originalData, decodedData, "La decodificación con padding falló.");
    }

    @Test
    @DisplayName("Debe usar la compresión 'z' para bloques de cuatro ceros")
    void test_shouldUseZCompression()
    {
        byte[] originalData ={ 0, 0, 0, 0, 65, 66, 65, 66, 0, 0, 0, 0 }; // Ceros, "ABAB", Ceros

        char[] actualEncoded = Ascii85.encode(originalData);
        assertEquals(7, actualEncoded.length, "La compresión 'z' no se aplicó correctamente.");

        byte[] decodedData = Ascii85.decode(actualEncoded);
        assertArrayEquals(originalData, decodedData, "La decodificación con 'z' falló.");
    }

    @Test
    @DisplayName("Debe decodificar '!!!!!' como cuatro bytes cero")
    void decode_shouldTreatAllExclamationAsZeroBlock()
    {
        char[] allExclamations = "!!!!!".toCharArray();
        byte[] expected = { 0, 0, 0, 0 };
        byte[] actual = Ascii85.decode(allExclamations);
        assertArrayEquals(expected, actual, "La secuencia '!!!!!' no se decodificó como cuatro ceros.");
    }

    // --- Pruebas de Robustez (ser permisivo con la entrada) ---
    @Test
    @DisplayName("decode() debe ignorar espacios en blanco en la entrada")
    void decode_shouldBePermissiveAndIgnoreWhitespace()
    {
        String hello_world = "hello world!!";
        
        byte[] expectedData = hello_world.getBytes(StandardCharsets.UTF_8);
        
        char[] clean = Ascii85.encode(expectedData);
        
        char[] dirtyData = new String(clean).replaceAll("7", " 7 ").replaceAll("B", " B ").replaceAll("T", " T ").toCharArray();

        byte[] decodedData = Ascii85.decode(dirtyData);
        assertArrayEquals(expectedData, decodedData, "La decodificación falló al no ignorar los espacios en blanco.");
    }

    @Test
    @DisplayName("decode() debe devolver un array vacío si la entrada solo contiene espacios")
    void decode_shouldReturnEmptyArray_forWhitespaceOnlyInput()
    {
        char[] whitespaceInput = " \t\n\r ".toCharArray();
        byte[] result = Ascii85.decode(whitespaceInput);
        assertNotNull(result);
        assertEquals(0, result.length, "La decodificación de solo espacios debería ser un array vacío.");
    }

    // --- Pruebas de Casos de Error ---
    @Test
    @DisplayName("decode() debe lanzar IllegalArgumentException para caracteres inválidos")
    void decode_shouldThrowException_forInvalidCharacters()
    {
        // El guion bajo '¿' no es un carácter válido en Ascii85
        char[] badData = "9j¿qo".toCharArray();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> Ascii85.decode(badData));

        assertTrue(exception.getMessage().contains("Invalid character for Ascii85"), "The error message is not as expected.");
    }

    @Test
    @DisplayName("decode() debe lanzar IllegalArgumentException para un bloque final de longitud inválida")
    void decode_shouldThrowException_forInvalidFinalBlockLength()
    {
        // Un bloque final no puede tener 1 solo carácter. Mínimo 2.
        char[] badData = "87cURD]j7BEbo80A".toCharArray();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> { Ascii85.decode(badData); });

        assertTrue(exception.getMessage().contains("Invalid final Ascii85 block"), "El mensaje de error no es el esperado.");
    }
}

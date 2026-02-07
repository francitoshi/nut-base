/*
 * MarkableAudioInputStreamTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class MarkableAudioInputStreamTest
{

    /**
     * Test of getMarkable method, of class MarkableAudioInputStream.
     */
    @Test
    public void testGetMarkable() throws IOException, Exception
    {
        AudioInputStream testStream = createTestAudioStream();

        // Obtener versión markable
        AudioInputStream markable = MarkableAudioInputStream.getMarkable(testStream);

        assertEquals(testStream.getFormat(), markable.getFormat());
        assertTrue(markable.markSupported());

        // Probar con diferentes tamaños de lectura
        for (int readSize = 1; readSize <= 1000; readSize++)
        {
            testMarkResetWithSize(markable, readSize);
        }

        markable.close();
    }

    /**
     * Prueba mark/reset con un tamaño de lectura específico
     */
    private static void testMarkResetWithSize(AudioInputStream stream, int readSize) throws Exception
    {
        // Marcar posición actual
        stream.mark(readSize * 3); // Marcar con límite generoso

        // Primera lectura
        byte[] firstRead = new byte[readSize];
        int bytesRead1 = stream.read(firstRead, 0, readSize);

        if(bytesRead1 <= 0)
        {
            // Reiniciar el stream para continuar las pruebas
            return;
        }

        // Resetear a la marca
        stream.reset();

        // Segunda lectura (debería obtener los mismos datos)
        byte[] secondRead = new byte[readSize];
        int bytesRead2 = stream.read(secondRead, 0, readSize);

        // Verificar que se leyó la misma cantidad
        assertEquals(bytesRead1, bytesRead2, "bytesRead diferentes! first=" + bytesRead1 + ", second=" + bytesRead2);
        

        // Verificar que los datos son idénticos
        assertArrayEquals(Arrays.copyOf(firstRead, bytesRead1), Arrays.copyOf(secondRead, bytesRead2));

        // Avanzar para la siguiente prueba (leer los mismos bytes una vez más)
        stream.reset();
        stream.read(new byte[readSize], 0, readSize);
    }

    /**
     * Crea un AudioInputStream de prueba con datos conocidos
     */
    private static AudioInputStream createTestAudioStream()
    {
        // Crear datos de audio de prueba (patrón repetitivo)
        int dataSize = 10000; // 10KB de datos
        byte[] audioData = new byte[dataSize];

        // Llenar con un patrón predecible
        for (int i = 0; i < dataSize; i++)
        {
            audioData[i] = (byte) (i % 256);
        }

        // Crear formato de audio (16-bit, 44.1kHz, stereo)
        AudioFormat format = Audio.PCM_CD_STEREO;

        // Crear el stream
        ByteArrayInputStream byteStream = new ByteArrayInputStream(audioData);
        return new AudioInputStream(byteStream, format, audioData.length / format.getFrameSize());
    }
}

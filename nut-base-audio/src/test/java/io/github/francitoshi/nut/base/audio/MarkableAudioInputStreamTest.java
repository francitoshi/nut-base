/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio;

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
        AudioInputStream markable = Audio.getMarkable(testStream);

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

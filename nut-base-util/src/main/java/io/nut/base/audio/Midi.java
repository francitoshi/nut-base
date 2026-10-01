/*
 * Midi.java
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

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

public class Midi
{

    public static void main(String[] args)
    {
//        String rutaArchivo = "/home/franci/Downloads/Mision_Imposible.mid"; // Reemplaza con la ruta real de tu archivo MIDI
        String rutaArchivo = "/home/franci/Downloads/La_pantera_rosa.mid"; // Reemplaza con la ruta real de tu archivo MIDI

        try
        {
            // Paso 1: Obtener el Sequencer
            Sequencer sequencer = MidiSystem.getSequencer();

            // Paso 2: Abrir el Sequencer
            sequencer.open();

            // Paso 3: Cargar la secuencia desde el archivo
            Sequence sequence = MidiSystem.getSequence(new File(rutaArchivo));

            // Paso 4: Asignar la secuencia
            sequencer.setSequence(sequence);

            // Paso 5: Iniciar la reproducción
            sequencer.start();
            
            // Paso 6: Esperar a que termine (opcional)
            while (sequencer.isRunning())
            {
                Thread.sleep(1000); // Espera 1 segundo antes de verificar de nuevo
            }

            // Paso 7: Cerrar el Sequencer
            sequencer.close();

            System.out.println("Reproducción completada.");
        }
        catch (MidiUnavailableException | InvalidMidiDataException | IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}

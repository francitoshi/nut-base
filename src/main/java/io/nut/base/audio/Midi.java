package io.nut.base.audio;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author franci
 */
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

package io.nut.base.util.concurrent.channel;

/**
 * Define la política de reacción del canal cuando un hilo sufre
 * una interrupción externa durante una operación bloqueante.
 */
public enum InterruptionPolicy
{
    /**
     * Reintenta la operación de forma ininterrumpible, preservando
     * el estado de interrupción del hilo al finalizar.
     */
    IGNORE,

    /**
     * Aborta la operación inmediatamente lanzando una {@link ChannelInterruptedException}.
     */
    THROW_EXCEPTION,

    /**
     * Cierra el canal si este implementa {@link ChannelCloser}, notificando el fin
     * de datos al resto de participantes, y lanza {@link ChannelInterruptedException}.
     */
    CLOSE_CHANNEL
}
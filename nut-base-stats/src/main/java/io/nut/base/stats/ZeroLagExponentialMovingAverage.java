/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.util.LinkedList;
import java.util.Queue;

public class ZeroLagExponentialMovingAverage extends MovingAverage
{

    private final ExponentialMovingAverage ema;
    private final int lag;
    // Usamos una cola para mantener el historial de precios necesario para el lag
    private final Queue<Double> history;
    private double zlema;

    public ZeroLagExponentialMovingAverage(int period)
    {
        // Un ZLEMA parte del mismo periodo que su EMA interno
        super(period);
        this.ema = new ExponentialMovingAverage(period);

        // Fórmula del Lag según definición estándar de ZLEMA
        this.lag = (period - 1) / 2;

        this.history = new LinkedList<>();
    }

    @Override
    public double next(double value)
    {
        // 1. Gestionar el historial para encontrar el valor retardado
        history.add(value);

        double olderValue = value; // Por defecto, si no llenamos el lag, usamos el actual

        // Necesitamos el valor de hace 'lag' periodos.
        // Si el historial supera el tamaño del lag, el elemento que sale
        // es exactamente el que ocurrió hace 'lag' pasos.
        if (history.size() > lag)
        {
            olderValue = history.poll();
        }

        // 2. Calcular el dato "des-retardado"
        // Data = 2 * Actual - (Valor de hace Lag periodos)
        double adjustedData = (2.0 * value) - olderValue;

        // 3. Aplicar EMA al dato ajustado
        return zlema = ema.next(adjustedData);
    }

    @Override
    public double average()
    {
        return zlema;
    }
}

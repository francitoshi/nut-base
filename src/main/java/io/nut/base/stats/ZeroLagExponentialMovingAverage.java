/*
 *  ZeroLagExponentialMovingAverage.java
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
        // El EMA interno usa el mismo periodo que el ZLEMA
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

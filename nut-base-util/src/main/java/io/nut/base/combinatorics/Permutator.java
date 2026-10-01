/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.combinatorics;

import io.nut.base.concurrent.Generator;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Permutator<E> extends Generator<E[]>
{

    private final E[] values;
    private final int k;
    private final E[] empty;

    public Permutator(E[] values, int k, int capacity)
    {
        super(capacity);
        this.values = values.clone();
        this.k = k;
        if (k > values.length || k < 0)
        {
            throw new InvalidParameterException("invalid value for k="+k);
        }
        this.empty = Arrays.copyOf(values, 0);
    }

    public Permutator(E[] values, int k)
    {
        this(values, k, 0);
    }
    
    public Permutator(E[] values)
    {
        this(values, values.length, 0);
    }

    @Override
    public void run()
    {
        if(k==values.length)
        {
            permuteFull(values, k);
        }
        else
        {
            permuteK(values, new ArrayList<>(), new boolean[values.length], k);
        }
    }

    // Implementación del algoritmo de Heap
    private void permuteFull(E[] array, int size)
    {
        // Caso base: si size es 1, imprimimos la permutación
        if (size == 1)
        {
            this.yield(array.clone());
            return;
        }

        for (int i = 0; i < size; i++)
        {
            permuteFull(array, size - 1);

            if (size % 2 == 1)
            {
                // Si size es impar, intercambiamos el primer y último elemento
                swap(array, 0, size - 1);
            }
            else
            {
                // Si size es par, intercambiamos el i-ésimo con el último
                swap(array, i, size - 1);
            }
        }
    }

    private static <T> void swap(T[] array, int i, int j)
    {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private void permuteK(E[] array, List<E> current, boolean[] used, int k) 
    {
        if (current.size() == k) 
        {
            this.yield(current.toArray(this.empty));
            return;
        }

        for (int i = 0; i < array.length; i++)
        {
            if (!used[i])
            {
                used[i] = true;           // Marcamos como usado
                current.add(array[i]); // Añadimos al resultado actual
                permuteK(array, current, used, k); // Recursión
                current.remove(current.size() - 1); // Backtracking
                used[i] = false;          // Desmarcamos
            }
        }
    }
    

}

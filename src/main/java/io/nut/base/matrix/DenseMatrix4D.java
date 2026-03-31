package io.nut.base.matrix;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;


public class DenseMatrix4D<E> implements Matrix<E>
{
    private volatile E[][][][] items;
    private volatile int count;

    public DenseMatrix4D(E[][][][] items)
    {
        this.items = items;
        this.count = 0;
    }

    private void checkDims(int[] indices)
    {
        if (indices.length != 4)
        {
            throw new IllegalArgumentException(String.format("Expected 4 indices, but received %d", indices.length));
        }
    }

    public E get(int x, int y, int z, int w)
    {
        if(this.items.length<=x)
        {
            return null;
        }
        if(this.items[x].length<=y)
        {
            return null;
        }
        if(this.items[x][y].length<=z)
        {
            return null;
        }
        if(this.items[x][y][z].length<=w)
        {
            return null;
        }
        return (E) items[x][y][z][w];
    }
    
    public E set(E e, int x, int y, int z, int w)
    {
        if(items.length<=x)
        {
            items = Arrays.copyOf(items, x);
        }
        if(items[x].length<=y)
        {
            items[x] = Arrays.copyOf(items[x], y);
        }
        if(items[x][y].length<=z)
        {
            items[x][y] = Arrays.copyOf(items[x][y], z);
        }
        if(this.items[x][y][z].length<=w)
        {
            items[x][y][z] = Arrays.copyOf(items[x][y][z], w);
        }
        
        E prev = items[x][y][z][w];
        items[x][y][z][w] = e;
        
        return prev;
    }

    public E remove(int x, int y, int z, int w)
    {
        if(this.items.length<=x)
        {
            return null;
        }
        if(this.items[x].length<=y)
        {
            return null;
        }
        if(this.items[x][y].length<=z)
        {
            return null;
        }
        if(this.items[x][y][z].length<=w)
        {
            return null;
        }

        E prev = items[x][y][z][w];
        items[x][y][z][w] = null;
        
        return prev;
    }

    @Override
    public E set(E e, int... indices)
    {
        checkDims(indices);
        return set(e, indices[0], indices[1], indices[2], indices[3]);
    }

    @Override
    public E get(int... indices)
    {
        checkDims(indices);
        return get(indices[0], indices[1], indices[2], indices[3]);
    }

    @Override
    public E remove(int... indices)
    {
        checkDims(indices);
        return remove(indices[0], indices[1], indices[2], indices[3]);
    }

    @Override
    public int count()
    {
        return count;
    }

    @Override
    public int dimensions()
    {
        return 4;
    }

    @Override
    public void forEach(BiConsumer<int[], E> action)
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        E value = get(x, y, z, w);
                        if (value != null)
                        {
                            action.accept(new int[]{x, y, z, w}, value);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator)
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        E value = get(x, y, z, w);
                        if (value != null)
                        {
                            set(operator.apply(value));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean anyMatch(Predicate<E> predicate)
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        E value = get(x, y, z, w);
                        if (value != null && predicate.test(value))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean allMatch(Predicate<E> predicate)
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        E value = get(x, y, z, w);
                        if (value != null && !predicate.test(value))
                        {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Optional<E> findFirst(Predicate<E> predicate)
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        E value = get(x, y, z, w);
                        if (value != null && predicate.test(value))
                        {
                            return Optional.of(value);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void removeIf(Predicate<E> predicate)
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        E value = get(x, y, z, w);
                        if (value != null && predicate.test(value))
                        {
                            remove(x, y, z, w);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void clear()
    {
        for (int x = 0; x < items.length; x++)
        {
            for (int y = 0; y < items[x].length; y++)
            {
                for (int z = 0; z < items[x][y].length; z++)
                {
                    for (int w = 0; w < items[x][y][z].length; w++)
                    {
                        remove(x,y, z, w);
                    }
                }
            }
        }
        count = 0;
    }
}

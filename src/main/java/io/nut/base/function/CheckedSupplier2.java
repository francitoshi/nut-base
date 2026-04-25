package io.nut.base.function;

@FunctionalInterface
public interface CheckedSupplier2<T, E1 extends Exception, E2 extends Exception>
{
    T get() throws E1, E2;
}

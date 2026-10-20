package io.nut.base.commons.serializer;

public interface Serializer<T>
{
    byte[] serialize(T object);

    T deserialize(byte[] data);
}

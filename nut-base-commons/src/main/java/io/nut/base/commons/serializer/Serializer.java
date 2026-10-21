/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.serializer;

public interface Serializer<T>
{
    byte[] serialize(T object);

    T deserialize(byte[] data);

    /**
     * Marker for the serializers able to serialize any object, using the
     * serializer registered for its concrete type in a registry. It is used to
     * detect the serializers that would recurse endlessly.
     */
    interface ObjectSerializer extends Serializer<Object>
    {
    }
}

/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link Serializer}s, indexed by the handled type and by a unique
 * numeric id.
 * <p>
 * Every registration stores an {@link Item} holding the type, the id and the
 * serializer, and it is reachable from two maps: one keyed by the class and one
 * keyed by the id. Both keys are unique, so the same type cannot be registered
 * twice and the same id cannot be assigned to two different types.
 * <p>
 * The numeric id is meant to be a stable, compact way of referring to a type
 * on the wire, without exposing class names.
 */
public class SerializerRegistry
{

    private static final SerializerRegistry DEFAULT = new SerializerRegistry();

    private final Map<Class<?>, Item<?>> byType = new ConcurrentHashMap<>();

    private final Map<Integer, Item<?>> byId = new ConcurrentHashMap<>();

    /**
     * Shared instance for those who do not want to inject their own.
     */
    public static SerializerRegistry getDefault()
    {
        return DEFAULT;
    }

    /**
     * Registers a serializer for the given type with the given id.
     *
     * @param type       The type handled by the serializer.
     * @param id         The unique id of the registration.
     * @param serializer The serializer to register.
     * @throws NullPointerException     If the type or the serializer is null.
     * @throws IllegalArgumentException If the type or the id is already registered.
     */
    public <T> void register(Class<T> type, int id, Serializer<T> serializer)
    {
        if (type == null || serializer == null)
        {
            throw new NullPointerException("type and serializer must not be null");
        }
        synchronized (this)
        {
            Item<?> byTypeItem = byType.get(type);
            if (byTypeItem != null)
            {
                throw new IllegalArgumentException("A serializer is already registered for " + type.getName() + " with id " + byTypeItem.getId());
            }
            Item<?> byIdItem = byId.get(id);
            if (byIdItem != null)
            {
                throw new IllegalArgumentException("The id " + id + " is already registered for " + byIdItem.getType().getName());
            }
            Item<T> item = new Item<>(type, id, serializer);
            byType.put(type, item);
            byId.put(id, item);
        }
    }

    /**
     * Registers only if neither the type nor the id were registered yet.
     *
     * @param type       The type handled by the serializer.
     * @param id         The unique id of the registration.
     * @param serializer The serializer to register.
     * @return True if it has been registered.
     * @throws NullPointerException If the type or the serializer is null.
     */
    public <T> boolean registerIfAbsent(Class<T> type, int id, Serializer<T> serializer)
    {
        if (type == null || serializer == null)
        {
            throw new NullPointerException("type and serializer must not be null");
        }
        synchronized (this)
        {
            if (byType.containsKey(type) || byId.containsKey(id))
            {
                return false;
            }
            Item<T> item = new Item<>(type, id, serializer);
            byType.put(type, item);
            byId.put(id, item);
            return true;
        }
    }

    /**
     * Removes the registration of the given type, both from the type map and
     * from the id map.
     *
     * @param type The type to unregister.
     * @return The removed item, or null if the type was not registered.
     */
    public Item<?> unregister(Class<?> type)
    {
        if (type == null)
        {
            return null;
        }
        synchronized (this)
        {
            Item<?> item = byType.remove(type);
            if (item != null)
            {
                byId.remove(item.getId(), item);
            }
            return item;
        }
    }

    /**
     * Removes the registration with the given id, both from the id map and
     * from the type map.
     *
     * @param id The id to unregister.
     * @return The removed item, or null if the id was not registered.
     */
    public Item<?> unregister(int id)
    {
        synchronized (this)
        {
            Item<?> item = byId.remove(id);
            if (item != null)
            {
                byType.remove(item.getType(), item);
            }
            return item;
        }
    }

    /**
     * Checks whether the given type has a registered serializer.
     *
     * @param type The type to check.
     * @return True if the type is registered.
     */
    public boolean isRegistered(Class<?> type)
    {
        return byType.containsKey(type);
    }

    /**
     * Checks whether the given id has a registered serializer.
     *
     * @param id The id to check.
     * @return True if the id is registered.
     */
    public boolean isRegistered(int id)
    {
        return byId.containsKey(id);
    }

    /**
     * Exact lookup by type.
     *
     * @param type The type to look for.
     * @return The registered serializer.
     * @throws IllegalArgumentException If the type is not registered.
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<T> get(Class<T> type)
    {
        Item<?> item = byType.get(type);
        if (item == null)
        {
            throw new IllegalArgumentException("No serializer registered for " + type.getName());
        }
        return (Serializer<T>) item.getSerializer();
    }

    /**
     * Exact lookup by id.
     *
     * @param id The id to look for.
     * @return The registered serializer.
     * @throws IllegalArgumentException If the id is not registered.
     */
    public Serializer<?> getById(int id)
    {
        Item<?> item = byId.get(id);
        if (item == null)
        {
            throw new IllegalArgumentException("No serializer registered with id " + id);
        }
        return item.getSerializer();
    }

    /**
     * Returns the registration of the given type.
     *
     * @param type The type to look for.
     * @return The registered item.
     * @throws IllegalArgumentException If the type is not registered.
     */
    @SuppressWarnings("unchecked")
    public <T> Item<T> getItem(Class<T> type)
    {
        Item<?> item = byType.get(type);
        if (item == null)
        {
            throw new IllegalArgumentException("No serializer registered for " + type.getName());
        }
        return (Item<T>) item;
    }

    /**
     * Returns the registration with the given id.
     *
     * @param id The id to look for.
     * @return The registered item.
     * @throws IllegalArgumentException If the id is not registered.
     */
    public Item<?> getItemById(int id)
    {
        Item<?> item = byId.get(id);
        if (item == null)
        {
            throw new IllegalArgumentException("No serializer registered with id " + id);
        }
        return item;
    }

    /**
     * Returns the id registered for the given type.
     *
     * @param type The type to look for.
     * @return The id of the registration.
     * @throws IllegalArgumentException If the type is not registered.
     */
    public int getId(Class<?> type)
    {
        return getItem(type).getId();
    }

    /**
     * Looks also for superclasses and interfaces. Useful to serialize an object
     * whose concrete type is not registered.
     *
     * @param type The type to look for.
     * @return The serializer registered for the type or for one of its supertypes.
     * @throws IllegalArgumentException If no serializer is found.
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<? super T> getForInstanceOf(Class<T> type)
    {
        Item<?> item = find(type);
        if (item == null)
        {
            throw new IllegalArgumentException("No serializer for " + type.getName() + " nor for its supertypes");
        }
        return (Serializer<? super T>) item.getSerializer();
    }

    /**
     * Returns the registration of the given type or of one of its supertypes.
     *
     * @param type The type to look for.
     * @return The registered item, or null if none is found.
     */
    public Item<?> findItemForInstanceOf(Class<?> type)
    {
        return find(type);
    }

    /**
     * Returns all the registered items.
     *
     * @return A snapshot of the registered items.
     */
    public Collection<Item<?>> items()
    {
        return Collections.unmodifiableCollection(new ArrayList<>(byType.values()));
    }

    /**
     * Returns the registered types.
     *
     * @return A snapshot of the registered types.
     */
    public Set<Class<?>> types()
    {
        return Collections.unmodifiableSet(new HashSet<>(byType.keySet()));
    }

    /**
     * Returns the registered ids.
     *
     * @return A snapshot of the registered ids.
     */
    public Set<Integer> ids()
    {
        return Collections.unmodifiableSet(new HashSet<>(byId.keySet()));
    }

    /**
     * Returns the number of registered serializers.
     *
     * @return The number of registrations.
     */
    public int size()
    {
        return byType.size();
    }

    /**
     * Removes every registration.
     */
    public void clear()
    {
        synchronized (this)
        {
            byType.clear();
            byId.clear();
        }
    }

    private Item<?> find(Class<?> type)
    {
        if (type == null)
        {
            return null;
        }
        Item<?> item = byType.get(type);
        if (item != null)
        {
            return item;
        }
        for (Class<?> iface : type.getInterfaces())
        {
            item = find(iface);
            if (item != null)
            {
                return item;
            }
        }
        return find(type.getSuperclass());
    }

    /**
     * A registration: the handled type, its unique id and its serializer.
     *
     * @param <T> The handled type.
     */
    private static final class Item<T>
    {

        private final Class<T> type;

        private final int id;

        private final Serializer<T> serializer;

        public Item(Class<T> type, int id, Serializer<T> serializer)
        {
            if (type == null || serializer == null)
            {
                throw new NullPointerException("type and serializer must not be null");
            }
            this.type = type;
            this.id = id;
            this.serializer = serializer;
        }

        /**
         * @return The handled type.
         */
        public Class<T> getType()
        {
            return type;
        }

        /**
         * @return The unique id of the registration.
         */
        public int getId()
        {
            return id;
        }

        /**
         * @return The serializer.
         */
        public Serializer<T> getSerializer()
        {
            return serializer;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof Item))
            {
                return false;
            }
            Item<?> other = (Item<?>) o;
            return id == other.id && type.equals(other.type);
        }

        @Override
        public int hashCode()
        {
            return 31 * type.hashCode() + id;
        }

        @Override
        public String toString()
        {
            return "Item[type=" + type.getName() + ", id=" + id + "]";
        }
    }
}

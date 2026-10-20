package io.nut.base.commons.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerializerRegistry
{

    private static final SerializerRegistry DEFAULT = new SerializerRegistry();

    private final Map<Class<?>, Serializer<?>> registry = new ConcurrentHashMap<>();

    /**
     * Instancia compartida por defecto, para quien no quiera inyectar la suya.
     */
    public static SerializerRegistry getDefault()
    {
        return DEFAULT;
    }

    public <T> void register(Class<T> type, Serializer<T> serializer)
    {
        if (type == null || serializer == null)
        {
            throw new NullPointerException();
        }
        registry.put(type, serializer);
    }

    /**
     * Registra solo si no había ninguno. Devuelve true si lo ha registrado.
     */
    public <T> boolean registerIfAbsent(Class<T> type, Serializer<T> serializer)
    {
        return registry.putIfAbsent(type, serializer) == null;
    }

    public void unregister(Class<?> type)
    {
        registry.remove(type);
    }

    public boolean isRegistered(Class<?> type)
    {
        return registry.containsKey(type);
    }

    /**
     * Búsqueda exacta por tipo.
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<T> get(Class<T> type)
    {
        Serializer<?> s = registry.get(type);
        if (s == null)
        {
            throw new IllegalArgumentException("No hay serializador registrado para " + type.getName());
        }
        return (Serializer<T>) s;
    }

    /**
     * Busca también por superclases e interfaces. Útil para serializar a partir
     * de un objeto cuyo tipo concreto no está registrado.
     */
    @SuppressWarnings("unchecked")
    public <T> Serializer<? super T> getForInstanceOf(Class<T> type)
    {
        Serializer<?> s = find(type);
        if (s == null)
        {
            throw new IllegalArgumentException("No hay serializador para " + type.getName() + " ni sus supertipos");
        }
        return (Serializer<? super T>) s;
    }

    private Serializer<?> find(Class<?> type)
    {
        if (type == null)
        {
            return null;
        }
        Serializer<?> s = registry.get(type);
        if (s != null)
        {
            return s;
        }
        for (Class<?> iface : type.getInterfaces())
        {
            s = find(iface);
            if (s != null)
            {
                return s;
            }
        }
        return find(type.getSuperclass());
    }
}

package io.nut.base.security;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.util.Arrays;

public final class SecureSecretKey implements SecretKey
{

    private byte[] keyMaterial;
    private final String algorithm;
    private volatile boolean destroyed = false;

    public SecureSecretKey(byte[] keyMaterial, String algorithm)
    {
        if (keyMaterial == null || keyMaterial.length == 0)
        {
            throw new IllegalArgumentException("Key material cannot be null or empty");
        }
        this.keyMaterial = keyMaterial.clone(); // copia defensiva
        this.algorithm = algorithm;
    }

    @Override
    public String getAlgorithm()
    {
        checkDestroyed();
        return algorithm;
    }

    @Override
    public String getFormat()
    {
        checkDestroyed();
        return "RAW";
    }

    @Override
    public byte[] getEncoded()
    {
        checkDestroyed();
        return keyMaterial.clone(); // nunca devolver referencia directa
    }

    @Override
    public void destroy() throws DestroyFailedException
    {
        if (!destroyed)
        {
            try
            {
                Arrays.fill(this.keyMaterial, (byte) 0);
                this.keyMaterial = null;
                destroyed = true;
            }
            catch (Exception e)
            {
                throw new DestroyFailedException("Failed to destroy key: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isDestroyed()
    {
        return destroyed;
    }

    private void checkDestroyed()
    {
        if (destroyed)
        {
            throw new IllegalStateException("Key has been destroyed");
        }
    }
}

package io.nut.base.crypto.bips;

public class ProtocolException extends RuntimeException
{

    public ProtocolException()
    {
    }

    public ProtocolException(String message)
    {
        super(message);
    }

    public ProtocolException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ProtocolException(Throwable cause)
    {
        super(cause);
    }

    public ProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

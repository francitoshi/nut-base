package io.nut.base.util.concurrent.channel;

/**
 * Unchecked exception indicating that a blocking operation on the channel
 * was externally interrupted.
 */
public class ChannelInterruptedException extends RuntimeException 
{
    private static final long serialVersionUID = 1L;

    public ChannelInterruptedException(String message, InterruptedException cause) 
    {
        super(message, cause);
    }

    public ChannelInterruptedException(InterruptedException cause) 
    {
        super(cause != null ? cause.getMessage() : null, cause);
    }

    @Override
    public synchronized InterruptedException getCause() 
    {
        return (InterruptedException) super.getCause();
    }
}
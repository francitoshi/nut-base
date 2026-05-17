package io.nut.base.util.concurrent.hive;

public abstract class InOutBee<I,O> extends Bee<I>
{
    protected volatile Bee<O> out;

    public InOutBee(int threads, Hive hive, int queueSize)
    {
        super(threads, hive, queueSize);
    }

    public InOutBee(int threads, Hive hive)
    {
        super(threads, hive);
    }

    public InOutBee(int threads)
    {
        super(threads);
    }

    public InOutBee()
    {
    }    

    public InOutBee<I,O> setOut(Bee<O> out)
    {
        this.out = out;
        return this;
    }
    
    public final boolean sendOut(O o)
    {
        Bee<O> target = this.out;
        if (target == null)
        {
            throw new IllegalStateException("out Bee not set");
        }
        return this.out.send(o);
    }
    
}

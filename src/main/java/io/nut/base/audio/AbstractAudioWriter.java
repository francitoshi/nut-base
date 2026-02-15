package io.nut.base.audio;

import java.io.IOException;

public abstract class AbstractAudioWriter implements AudioWriter
{
    @Override
    public final int write(byte[] bytes, int off, int len, int count) throws IOException
    {
        int writeCount = 0;
        if(len>0)
        {
            while(count>0)
            {
                int n = Math.min(len, count);
                int w = this.write(bytes, off, n);
                if(w<0)
                {
                    return w;
                }
                writeCount += w;
                count -= w;
            }
        }
        return writeCount;
    }
   
}

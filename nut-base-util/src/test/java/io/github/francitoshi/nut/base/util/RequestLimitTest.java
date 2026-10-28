/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class RequestLimitTest
{
    /**
     * Test of update method, of class RequestLimit.
     */
    @Test
    public void testUpdate() throws InterruptedException
    {
        RequestLimit instance = new RequestLimit(10,10000);
        for(int i=0;i<50;)
        {
            for(int j=0;j<5;j++,i++)
            {
                instance.update(0.1);
                System.out.println(""+i+" "+new Date());
            }
            Thread.sleep(400);
        }
    }
    
}

/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class PathWalkerTest
{

    /**
     * Test of of method, of class PathWalker.
     */
    @Test
    public void testOf_PathWalkerFileConsumer_BiPredicate() throws IOException
    {
        PathWalker.FileConsumer consumer = null;
        PathWalker result = PathWalker.of((x,y)-> System.out.println(x));
        result.walk(new File(".").toPath());
        
    }

}

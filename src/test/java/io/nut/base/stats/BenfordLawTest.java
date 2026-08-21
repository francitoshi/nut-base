/*
 * Copyright (C) 2020-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BenfordLawTest
{

    /**
     * Test of probability method, of class BenfordLaw.
     */
    @Test
    public void testBenford()
    {
        double delta = 0.000001;
        assertEquals(0.30103,    BenfordLaw.probability(1, 10), delta);
        assertEquals(0.176091,   BenfordLaw.probability(2, 10), delta);
        assertEquals(0.124939,   BenfordLaw.probability(3, 10), delta);
        assertEquals(0.09691,    BenfordLaw.probability(4, 10), delta);
        assertEquals(0.0791812,  BenfordLaw.probability(5, 10), delta);
        assertEquals(0.0669468,  BenfordLaw.probability(6, 10), delta);
        assertEquals(0.0579919,  BenfordLaw.probability(7, 10), delta);
        assertEquals(0.0511525,  BenfordLaw.probability(8, 10), delta);
        assertEquals(0.0457575,  BenfordLaw.probability(9, 10), delta);
    }

}

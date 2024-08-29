/*
 *  BeeTest.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BeeTest
{
    /**
     * Test of setHive method, of class Bee.
     */
    @Test
    public void testSetHive()
    {
        Bee<String> instance = new Bee<String>()
        {
            @Override
            protected void receive(String m)
            {
                System.out.println(m);
            }
        };
        instance.send("hello");

        Hive hive = new Hive();
        instance.setHive(hive);

        instance.send("world");
        
        instance.shutdown();
    }

    
}

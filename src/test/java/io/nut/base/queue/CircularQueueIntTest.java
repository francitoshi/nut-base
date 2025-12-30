/*
 *  CircularQueueIntTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.queue;

import io.nut.base.profile.Profiler;
import io.nut.base.time.JavaTime;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// Claude Sonnet 4.5
class CircularQueueIntTest
{

    private CircularQueueInt queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueInt(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push(100);
        queue.push(200);
        assertEquals(100, queue.pop());
        assertEquals(200, queue.pop());
        assertEquals(0, queue.pop());
    }

    @Test
    void testSum()
    {
        queue.push(5);
        queue.push(10);
        queue.push(15);
        assertEquals(30L, queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(10);
        queue.push(20);
        queue.push(30);
        assertEquals(20.0, queue.average(), 0.001);
    }

    @Test
    void testMinMax()
    {
        queue.push(25);
        queue.push(10);
        queue.push(40);
        assertEquals(10, queue.min());
        assertEquals(40, queue.max());
    }
    @Test
    void testThroughput()
    {
        final int num = 1024*16;
        CircularQueueInt q1 = new CircularQueueInt(num);
        Queue<Integer> q2 = new ArrayDeque<>(num);
        Queue<Integer> q3 = new LinkedBlockingQueue<>(num);
        Queue<Integer> q4 = new LinkedList<>();
        Profiler profiler = new Profiler(JavaTime.Resolution.MS);
        Profiler.Task t1 = profiler.getTask("q1");
        Profiler.Task t2 = profiler.getTask("q2");
        Profiler.Task t3 = profiler.getTask("q3");
        Profiler.Task t4 = profiler.getTask("q4");
        
        for(int i=0;i<num;i++)
        {
            t1.start();
            for(int j=0;j<i;j++)
            {
                q1.push(num);
            }
            t1.stop().count();
            
            t2.start();
            for(int j=0;j<i;j++)
            {
                while(q2.size()>=num) q2.poll();
                q2.add(num);
            }
            t2.stop().count();
            
            t3.start();
            for(int j=0;j<i;j++) 
            {
                while(q3.size()>=num) q3.poll();
                q3.add(num);
            }
            t3.stop().count();
            
            t4.start();
            for(int j=0;j<i;j++) 
            {
                while(q4.size()>=num) q4.poll();
                q4.add(num);
            }
            t4.stop().count();
        }
        
        assertEquals(num, q1.size());
        assertEquals(num, q2.size());
        assertEquals(num, q3.size());
        assertEquals(num, q4.size());

        profiler.print();
    }
}

/*
 *  InOutBeeTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
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

import io.nut.base.util.As;
import io.nut.base.util.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class InOutBeeTest
{
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Collects all messages it receives into a thread-safe list. */
    private static <T> Bee<T> sink(List<T> collector)
    {
        return Bee.bee(item -> collector.add(item));
    }

    /** Collects all messages it receives into a thread-safe list, backed by a Hive. */
    private static <T> Bee<T> sink(Hive hive, List<T> collector)
    {
        return Bee.bee(1, hive, item -> collector.add(item));
    }

    // -----------------------------------------------------------------------
    // InOutBee tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("InOutBee")
    class InOutBeeTests
    {
        @Test
        @DisplayName("setOut returns this instance for fluent chaining")
        void setOutReturnsSelf()
        {
            List<String> out = new ArrayList<>();
            InOutBee<String, String> bee = new InOutBee<String, String>()
            {
                @Override
                protected void receive(String s)
                {
                    sendOut(s.toUpperCase());
                }
            };

            Bee<String> downstream = sink(out);
            InOutBee<String, String> returned = bee.setOut(downstream);

            assertSame(bee, returned, "setOut must return 'this' for chaining");
        }

        @Test
        @DisplayName("sendOut forwards message to downstream Bee")
        void sendOutForwardsToDownstream()
        {
            List<String> received = new ArrayList<>();

            InOutBee<String, String> bee = new InOutBee<String, String>()
            {
                @Override
                protected void receive(String s)
                {
                    sendOut(s + "!");
                }
            };
            bee.setOut(sink(received));

            bee.send("hello");
            bee.shutdown(false);
            bee.awaitTermination(2000);

            assertEquals(As.list("hello!"), received);
        }


        @Test
        @DisplayName("multiple messages are all forwarded downstream (synchronous)")
        void multipleMessagesForwardedSync()
        {
            List<Integer> received = new ArrayList<>();

            InOutBee<String, Integer> bee = new InOutBee<String, Integer>()
            {
                @Override
                protected void receive(String s)
                {
                    sendOut(s.length());
                }
            };
            bee.setOut(sink(received));

            bee.send("a");
            bee.send("bb");
            bee.send("ccc");
            bee.shutdown();
            bee.awaitTermination(2000);

            assertEquals(As.list(1, 2, 3), received);
        }

        @Test
        @DisplayName("send returns false after shutdown")
        void sendReturnsFalseAfterShutdown()
        {
            List<String> received = new ArrayList<>();
            InOutBee<String, String> bee = new InOutBee<String, String>()
            {
                @Override
                protected void receive(String s)
                {
                    sendOut(s);
                }
            };
            bee.setOut(sink(received));
            bee.shutdown();
            bee.awaitTermination(2000);

            boolean result = bee.send("late");
            assertFalse(result, "send must return false once the Bee is shut down");
        }

        @Test
        @DisplayName("downstream Bee can be reassigned with setOut")
        void setOutCanReassignDownstream()
        {
            List<String> first  = new ArrayList<>();
            List<String> second = new ArrayList<>();

            InOutBee<String, String> bee = new InOutBee<String, String>()
            {
                @Override
                protected void receive(String s)
                {
                    sendOut(s);
                }
            };

            bee.setOut(sink(first));
            bee.send("one");

            bee.setOut(sink(second));
            bee.send("two");

            bee.shutdown();
            bee.awaitTermination(2000);

            assertEquals(As.list("one"), first);
            assertEquals(As.list("two"), second);
        }

        @Test
        @DisplayName("async: all messages forwarded downstream via Hive")
        void asyncMessagesForwarded() throws InterruptedException
        {
            Hive hive = new Hive(4);
            List<Integer> received = new CopyOnWriteArrayList<>();

            InOutBee<String, Integer> bee = new InOutBee<String, Integer>(2, hive)
            {
                @Override
                protected void receive(String s)
                {
                    sendOut(s.length());
                }
            };
            Bee<Integer> downstream = sink(hive, received);
            bee.setOut(downstream);

            int count = 50;
            for (int i = 0; i < count; i++)
            {
                bee.send(Strings.repeat("x",i + 1));
            }

            Bee.shutdownAndAwaitTermination(bee, downstream);

            assertEquals(count, received.size());
            // spot-check a couple of values
            assertTrue(received.contains(1));
            assertTrue(received.contains(count));
        }
    }
}

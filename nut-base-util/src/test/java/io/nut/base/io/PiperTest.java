/*
 *  PiperTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.io;

import io.nut.base.util.BytesFilter;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Unit tests for the {@link Piper} class. These tests use in-memory streams to
 * simulate client/server communication and verify the data transfer and
 * filtering logic.
 */
class PiperTest
{

    private final String CLIENT_DATA = "Hello, this is the client speaking!";
    private final String SERVER_DATA = "And this is the server responding.";


    /**
     * Tests the basic bidirectional data transfer without any filters. Verifies
     * that data sent by the client is received by the server, and vice versa.
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS) // Fails the test if it hangs
    void testBidirectionalPipe_noFilter_transfersDataCorrectly() throws InterruptedException
    {
        // Data flow: clientInputStream -> serverOutputStream
        ByteArrayInputStream clientInputStream = new ByteArrayInputStream(CLIENT_DATA.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream serverOutputStream = new ByteArrayOutputStream();

        // Data flow: serverInputStream -> clientOutputStream
        ByteArrayInputStream serverInputStream = new ByteArrayInputStream(SERVER_DATA.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();

        // Arrange
        AtomicInteger activePipers = new AtomicInteger(0);
        Piper piper = new Piper(clientInputStream, clientOutputStream, serverInputStream, serverOutputStream, true, activePipers);

        // Act
        piper.start();
        piper.join();

        // Assert
        // Verify that the server's output stream received exactly what the client sent
        assertArrayEquals(CLIENT_DATA.getBytes(StandardCharsets.UTF_8), serverOutputStream.toByteArray());

        // Verify that the client's output stream received exactly what the server sent
        assertArrayEquals(SERVER_DATA.getBytes(StandardCharsets.UTF_8), clientOutputStream.toByteArray());
    }

    /**
     * Tests the data transfer when directional filters are applied. Verifies
     * that the data is transformed according to the filter logic in both
     * directions.
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testBidirectionalPipe_withFilters_transformsDataCorrectly() throws InterruptedException
    {
        // Data flow: clientInputStream -> serverOutputStream
        ByteArrayInputStream clientInputStream = new ByteArrayInputStream(CLIENT_DATA.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream serverOutputStream = new ByteArrayOutputStream();

        // Data flow: serverInputStream -> clientOutputStream
        ByteArrayInputStream serverInputStream = new ByteArrayInputStream(SERVER_DATA.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream clientOutputStream = new ByteArrayOutputStream();

        // Arrange
        AtomicInteger activePipers = new AtomicInteger(0);

        // A simple filter that converts byte data to an uppercase string and back to bytes
        BytesFilter toUpperCaseFilter = (bytes) -> new String(bytes, StandardCharsets.UTF_8)
                .toUpperCase()
                .getBytes(StandardCharsets.UTF_8);

        // A filter that reverses the bytes in the array
        BytesFilter reverseBytesFilter = (bytes) ->
        {
            return new String(bytes).toUpperCase().getBytes();
        };

        // Create the piper with different filters for each direction
        Piper piper = new Piper(
                clientInputStream, clientOutputStream,
                serverInputStream, serverOutputStream,
                true, activePipers,
                toUpperCaseFilter, // client-to-server filter
                reverseBytesFilter // server-to-client filter
        );

        // Act
        piper.start();
        piper.join();

        // Assert
        // Verify client-to-server data was converted to uppercase
        byte[] expectedServerOutput = CLIENT_DATA.toUpperCase().getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expectedServerOutput, serverOutputStream.toByteArray());

        // Verify server-to-client data was reversed
        byte[] expectedClientOutput = reverseBytesFilter.filter(SERVER_DATA.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(expectedClientOutput, clientOutputStream.toByteArray());
    }

}

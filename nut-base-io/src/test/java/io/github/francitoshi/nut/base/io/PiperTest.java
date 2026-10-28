/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import io.github.francitoshi.nut.base.commons.io.BytesFilter;
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

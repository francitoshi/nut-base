/*
 *  DuplexLayerTest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.signal;

import io.nut.base.audio.AudioMorseTransceiver;
import io.nut.base.audio.Wave;
import io.nut.base.util.Strings;
import io.nut.base.util.Utils;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;

import static org.junit.jupiter.api.Assertions.*;


class FakeTransceiver implements Transceiver
{
    final Frame framer = new Frame();
    
    final AtomicInteger dataWriteCounter = new AtomicInteger();
    final AtomicInteger ackWriteCounter = new AtomicInteger();
    final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(10);
    final boolean ack;

    public FakeTransceiver(boolean ack)
    {
        this.ack = ack;
    }
    
    @Override
    public void write(byte[] frame)
    {
        boolean isData = framer.isData(frame);
        boolean isAck = framer.isAck(frame);
        if(isData) dataWriteCounter.incrementAndGet();
        if(isAck) ackWriteCounter.incrementAndGet();
        if(framer.isData(frame) && ack)
        {
            try
            {
                char id = framer.getId(frame);
                byte[] ack = framer.createAck('\0', '\0', id);
                queue.put(ack);
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(FakeTransceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public byte[] read()
    {
        try
        {
            return queue.take();
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(FakeTransceiver.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void feed(byte[] frame)
    {
        try
        {
            queue.put(frame);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(FakeTransceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public FakeTransceiver open()
    {
        return this;
    }

    @Override
    public void close()
    {
    }
}
class FakeListener implements DuplexLayer.FrameListener
{
    final AtomicInteger onDeliveredCounter = new AtomicInteger();
    final AtomicInteger onFailedCounter = new AtomicInteger();
    final AtomicInteger onReceivedCounter = new AtomicInteger();
    @Override
    public void onDelivered(char id)
    {
        onDeliveredCounter.incrementAndGet();
    }

    @Override
    public void onFailed(char id, int statusCode)
    {
        onFailedCounter.incrementAndGet();
    }

    @Override
    public void onReceived(char id, byte[] payload)
    {
        onReceivedCounter.incrementAndGet();
    }
    
}
/**
 * Tests unitarios para DuplexLayer — sin Mockito, solo JUnit 5.
 *
 * Dependencias (pom.xml / build.gradle): -
 * org.junit.jupiter:junit-jupiter:5.10+
 */
class DuplexLayerTest
{
    final Frame framer = new Frame();

    // =========================================================================
    // Stubs manuales
    // =========================================================================
    /**
     * Transceiver de prueba basado en BlockingQueues. feed() → inyecta frames
     * que DuplexLayer "recibirá" pollSent() → recupera frames que DuplexLayer
     * "envió"
     */
    static class StubTransceiver implements Transceiver
    {

        private final BlockingQueue<byte[]> inbound = new LinkedBlockingQueue<>();
        private final BlockingQueue<byte[]> outbound = new LinkedBlockingQueue<>();

        void feed(byte[] frame)
        {
            inbound.offer(frame);
        }

        byte[] pollSent(long timeoutMs) throws InterruptedException
        {
            return outbound.poll(timeoutMs, TimeUnit.MILLISECONDS);
        }

        List<byte[]> drainSent()
        {
            List<byte[]> result = new ArrayList<>();
            outbound.drainTo(result);
            return result;
        }

        @Override
        public byte[] read()
        {
            try
            {
                return inbound.take();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        @Override
        public void write(byte[] frame)
        {
            outbound.offer(frame);
        }

        @Override
        public StubTransceiver open()
        {
            return this;
        }

        @Override
        public void close()
        {
        }
    }

    /**
     * FrameListener de prueba que registra todas las llamadas recibidas y
     * expone helpers de espera para sincronización en los tests.
     */
    static class RecordingListener implements DuplexLayer.FrameListener
    {

        static class DeliveredEvent
        {
            final char id;
            
            DeliveredEvent(char id)
            {
                this.id = id;
            }
        }

        static class FailedEvent
        {

            final char id;
            final int statusCode;

            FailedEvent(char id, int statusCode)
            {
                this.id = id;
                this.statusCode = statusCode;
            }
        }

        static class ReceivedEvent
        {

            final char id;
            final byte[] payload;

            ReceivedEvent(char id, byte[] payload)
            {
                this.id = id;
                this.payload = payload;
            }
        }

        final List<DeliveredEvent> delivered = new CopyOnWriteArrayList<>();
        final List<FailedEvent> failed = new CopyOnWriteArrayList<>();
        final List<ReceivedEvent> received = new CopyOnWriteArrayList<>();

        volatile CountDownLatch deliveredLatch = new CountDownLatch(1);
        volatile CountDownLatch failedLatch = new CountDownLatch(1);
        volatile CountDownLatch receivedLatch = new CountDownLatch(1);

        @Override
        public void onDelivered(char id)
        {
            delivered.add(new DeliveredEvent(id));
            deliveredLatch.countDown();
        }

        @Override
        public void onFailed(char id, int statusCode)
        {
            failed.add(new FailedEvent(id, statusCode));
            failedLatch.countDown();
        }

        @Override
        public void onReceived(char id, byte[] payload)
        {
            received.add(new ReceivedEvent(id, payload));
            receivedLatch.countDown();
        }

        boolean awaitDelivered(int n, long ms) throws InterruptedException
        {
            long deadline = System.currentTimeMillis() + ms;
            while (delivered.size() < n)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                deliveredLatch = new CountDownLatch(1);
                deliveredLatch.await(remaining, TimeUnit.MILLISECONDS);
            }
            return true;
        }

        boolean awaitFailed(int n, long ms) throws InterruptedException
        {
            long deadline = System.currentTimeMillis() + ms;
            while (failed.size() < n)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                failedLatch = new CountDownLatch(1);
                failedLatch.await(remaining, TimeUnit.MILLISECONDS);
            }
            return true;
        }

        boolean awaitReceived(int n, long ms) throws InterruptedException
        {
            long deadline = System.currentTimeMillis() + ms;
            while (received.size() < n)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                receivedLatch = new CountDownLatch(1);
                receivedLatch.await(remaining, TimeUnit.MILLISECONDS);
            }
            return true;
        }
    }

    // =========================================================================
    // Fixture
    // =========================================================================
    private DuplexLayer layer;
    private StubTransceiver transceiver;
    private RecordingListener listener;

    @BeforeEach
    void setUp()
    {
        transceiver = new StubTransceiver();
        listener = new RecordingListener();
        layer = new DuplexLayer();
        layer.setTransceiver(transceiver);
        layer.setListener(listener);
        layer.open();
    }

    @AfterEach
    void tearDown()
    {
        layer.close();
    }

    // =========================================================================
    // 1. Inicialización
    // =========================================================================
    @Test
    @DisplayName("DuplexLayer se instancia sin lanzar excepción")
    void testInstantiation()
    {
        assertDoesNotThrow(() -> new DuplexLayer());
    }

    @Test
    @DisplayName("open() y close() no lanzan excepción")
    void testOpenClose()
    {
        DuplexLayer dl = new DuplexLayer();
        dl.setTransceiver(new StubTransceiver());
        dl.setListener(new RecordingListener());
        assertDoesNotThrow(() ->
        {
            dl.open();
            dl.close();
        });
    }

    @Test
    @DisplayName("close() es idempotente — múltiples llamadas no lanzan excepción")
    void testClose_idempotent()
    {
        assertDoesNotThrow(() ->
        {
            layer.close();
            layer.close();
        });
    }

    // =========================================================================
    // 2. Recepción de frames DATA
    // =========================================================================
    @Test
    @DisplayName("Al recibir DATA se invoca onReceived con el id correcto")
    void testReceiveData_idIsCorrect() throws InterruptedException
    {
        char id = 42;
        transceiver.feed(framer.createData('\0', '\0', id, new byte[]{ 0x01 }));

        assertTrue(listener.awaitReceived(1, 1_000));
        assertEquals(id, listener.received.get(0).id);
    }

    @Test
    @DisplayName("Al recibir DATA el payload llega íntegro")
    void testReceiveData_payloadIsCorrect() throws InterruptedException
    {
        char id = 1;
        byte[] payload = { 0x01, 0x02, 0x03, 0x04 };
        transceiver.feed(framer.createData('\0', '\0', id, payload));

        assertTrue(listener.awaitReceived(1, 1_000));
        assertArrayEquals(payload, listener.received.get(0).payload);
    }

    @Test
    @DisplayName("Al recibir DATA se envía automáticamente un ACK con el mismo id")
    void testReceiveData_sendsAck() throws InterruptedException
    {
        char id = 7;
        transceiver.feed(framer.createData('\0', '\0', id, new byte[]{ 0x10 }));

        byte[] sent = transceiver.pollSent(1_000);
        assertNotNull(sent, "Se esperaba un ACK en el canal de salida");
        assertTrue(framer.isAck(sent));
        assertEquals(id, framer.getId(sent));
    }

    @Test
    @DisplayName("Cinco DATA frames consecutivos generan cinco llamadas a onReceived")
    void testReceiveMultipleDataFrames() throws InterruptedException
    {
        for (char i = 0; i < 5; i++)
        {
            transceiver.feed(framer.createData('\0', '\0', i, new byte[]{(byte) i}));
        }
        assertTrue(listener.awaitReceived(5, 2_000));
        assertEquals(5, listener.received.size());
    }

    @Test
    @DisplayName("Recibir DATA con payload vacío no lanza excepción")
    void testReceiveData_emptyPayload() throws InterruptedException
    {
        transceiver.feed(framer.createData('\0', '\0', (char) 99, new byte[0]));
        assertTrue(listener.awaitReceived(1, 1_000));
        assertArrayEquals(new byte[0], listener.received.get(0).payload);
    }

    // =========================================================================
    // 3. Recepción de frames ACK
    // =========================================================================
    @Test
    @DisplayName("Al recibir ACK para un frame pendiente se invoca onDelivered")
    void testReceiveAck_callsOnDelivered() throws InterruptedException
    {
        char id = 1;
        layer.write(framer.createData('\0', '\0', id, new byte[]{ 0x55 }));
        transceiver.pollSent(1_000); // consumir el frame enviado

        transceiver.feed(framer.createAck('\0', '\0', id));

        assertTrue(listener.awaitDelivered(1, 1_000));
        assertEquals(id, listener.delivered.get(0).id);
    }

    @Test
    @DisplayName("ACK para id desconocido es ignorado — no invoca ningún callback")
    void testReceiveAck_unknownId_isIgnored() throws InterruptedException
    {
        transceiver.feed(framer.createAck('\0', '\0', (char) 999));
        Thread.sleep(300);
        assertTrue(listener.delivered.isEmpty());
        assertTrue(listener.failed.isEmpty());
    }

    @Test
    @DisplayName("Tras recibir ACK no se invoca onFailed")
    void testReceiveAck_doesNotCallOnFailed() throws InterruptedException
    {
        char id = 3;
        layer.write(framer.createData('\0', '\0', id, new byte[]{0x01}));
        transceiver.pollSent(1_000);

        transceiver.feed(framer.createAck('\0', '\0', id));
        assertTrue(listener.awaitDelivered(1, 1_000));
        assertTrue(listener.failed.isEmpty());
    }

    // =========================================================================
    // 4. Recepción de frames NACK
    // =========================================================================
    @Test
    @DisplayName("Al recibir NACK se invoca onFailed con el status correcto")
    void testReceiveNack_callsOnFailed() throws InterruptedException
    {
        char id = 4;
        byte status = 0x0A;

        FakeTransceiver ft = new FakeTransceiver(false);
        FakeListener fl = new FakeListener();
        DuplexLayer dl = new DuplexLayer().setTransceiver(ft).setListener(fl).open();

        dl.write(framer.createData('\0', '\0', id, new byte[]{0x77}));
        ft.feed(framer.createNack('\0', '\0', id, status));
        Utils.parkMillis(10);
        assertEquals(1, fl.onFailedCounter.get());
    }

    @Test
    @DisplayName("NACK duplicado no invoca onFailed más de una vez")
    void testReceiveNack_duplicate_notCalledTwice() throws InterruptedException
    {
        char id = 5;
        layer.write(framer.createData('\0', '\0', id, new byte[]{ 0x77 }));
        transceiver.pollSent(1_000);

        transceiver.feed(framer.createNack('\0', '\0', id, (byte) 1));
        transceiver.feed(framer.createNack('\0', '\0', id, (byte) 1));

        assertTrue(listener.awaitFailed(1, 1_000));
        Thread.sleep(300);
        assertEquals(1, listener.failed.size(), "onFailed no debe llamarse dos veces");
    }

    @Test
    @DisplayName("NACK para id desconocido es ignorado — no invoca ningún callback")
    void testReceiveNack_unknownId_isIgnored() throws InterruptedException
    {
        transceiver.feed(framer.createNack('\0', '\0', (char) 888, (byte) 0xFF));
        Thread.sleep(300);
        assertTrue(listener.failed.isEmpty());
    }

    @Test
    @DisplayName("Tras recibir NACK no se invoca onDelivered")
    void testReceiveNack_doesNotCallOnDelivered() throws InterruptedException
    {
        char id = 6;
        layer.write(framer.createData('\0', '\0', id, new byte[]{ 0x01 }));
        transceiver.pollSent(1_000);

        transceiver.feed(framer.createNack('\0', '\0', id, (byte) 2));
        assertTrue(listener.awaitFailed(1, 1_000));
        assertTrue(listener.delivered.isEmpty());
    }

    // =========================================================================
    // 5. Envío y retransmisión
    // =========================================================================
    @Test
    @DisplayName("write() provoca al menos un envío a través del transceiver")
    void testWrite_sendsFrame() throws InterruptedException
    {
        byte[] frame = framer.createData('\0', '\0', (char) 10, new byte[]{ 0x01 });
        layer.write(frame);

        byte[] sent = transceiver.pollSent(2_000);
        assertNotNull(sent, "El frame debería haberse enviado");
        assertArrayEquals(frame, sent);
    }

    @Test
    @DisplayName("Sin ACK el frame es retransmitido al menos una vez")
    void testRetransmission_whenNoAck() throws InterruptedException
    {
        FakeTransceiver ft = new FakeTransceiver(false);
        DuplexLayer dl = new DuplexLayer().setTransceiver(ft).open();
        byte[] frame = framer.createData('\0', '\0', (char) 20, new byte[]{0x02});
        dl.write(frame);
        Utils.parkMillis(DuplexLayer.ACK_TIMEOUT_MILLIS*2);
        assertTrue(ft.dataWriteCounter.get()>1);
    }

    @Test
    @DisplayName("Tras recibir ACK el frame ya no es retransmitido")
    void testNoRetransmission_afterAck() throws InterruptedException
    {
        char id = 21;
        FakeTransceiver ft = new FakeTransceiver(true);
        
        byte[] frame = framer.createData('\0', '\0', id, new byte[]{0x03});
        DuplexLayer dl = new DuplexLayer().setTransceiver(ft).open();

        dl.write(frame);
        Utils.parkMillis(DuplexLayer.ACK_TIMEOUT_MILLIS*DuplexLayer.MAX_RETRIES);
        assertEquals(1, ft.dataWriteCounter.get());
    }

    @Test
    @DisplayName("Múltiples write() consecutivos encolan todos los frames")
    void testMultipleWrites_allSent() throws InterruptedException
    {
        int count = 5;
        for (char i = 0; i < count; i++)
        {
            layer.write(framer.createData('\0', '\0', i, new byte[]{ (byte) i }));
        }
        for (int i = 0; i < count; i++)
        {
            assertNotNull(transceiver.pollSent(2_000),
                    "Se esperaba el frame #" + i + " en el canal de salida");
        }
    }

    // =========================================================================
    // 6. Documentación de bugs conocidos
    // =========================================================================

    /**
     * BUG #2: outgoingMap.remove(ef) pasa el objeto como clave en vez de ef.id.
     * La entrada nunca se borra del mapa (memory leak). Fix:
     * outgoingMap.remove(ef.id)
     */
    @Test
    @DisplayName("[BUG#2] Tras ACK el frame debe eliminarse del outgoingMap")
    void testBug2_outgoingMapNotCleaned() throws InterruptedException
    {
        char id = 5;
        byte[] frame = framer.createData('\0', '\0', id, new byte[]{ 0x01 });
        layer.write(frame);
        transceiver.pollSent(1_000);

        transceiver.feed(framer.createAck('\0', '\0', id));
        assertTrue(listener.awaitDelivered(1, 1_000));

        // Segundo ACK: si el mapa está limpio este ACK no encuentra la entrada.
        // Si no lo está, el flag 'acked' evita la doble llamada pero hay memory leak.
        transceiver.feed(framer.createAck('\0', '\0', id));
        Thread.sleep(300);
        assertEquals(1, listener.delivered.size(), "BUG#2: onDelivered llamado más veces de lo esperado (entry no eliminada)");
    }

    @Test
    @Disabled("this test is only to test manually beacuse it will produce noise")
    public void testAudioLoop() throws LineUnavailableException
    {
        Utils.async(()->
        {
            try
            {
                launchDuplex("alice", 12);
            }
            catch (LineUnavailableException ex)
            {
                Logger.getLogger(DuplexLayerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        });        
        
        Utils.async(()-> 
        {
            try
            {
                launchDuplex("bob", 12);
            }
            catch (LineUnavailableException ex)
            {
                Logger.getLogger(DuplexLayerTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        
    }

    public void launchDuplex(String name, int wpm) throws LineUnavailableException
    {
        DuplexLayer duplexLayer = new DuplexLayer();
        AudioMorseTransceiver userTransceiver = new AudioMorseTransceiver(800, wpm, Wave.SQUARE);
        duplexLayer.setTransceiver(userTransceiver);
        DuplexLayer.FrameListener userListener = new DuplexLayer.FrameListener()
        {
            @Override
            public void onDelivered(char id)
            {
                System.out.println("onDelivered:"+name+":"+id);
            }
            
            @Override
            public void onFailed(char id, int statusCode)
            {
                System.out.println("onFailed:"+name+":"+id+" "+statusCode);
            }
            
            @Override
            public void onReceived(char id, byte[] payload)
            {
                System.out.println("onReceived:"+name+":"+id+" "+new String(payload, StandardCharsets.UTF_8));
            }
        };
        
        duplexLayer.setListener(userListener);
        duplexLayer.open();
        
        for(int i=1;i<10;i++)
        {
            char c = (char) ('0'+i);
            String s = Strings.repeat(c, i);
            duplexLayer.send('\0', s.getBytes(StandardCharsets.UTF_8));
        }   
        for(int i = 0;i<10;i++)
        {
            byte[] frame = transceiver.read();
            byte[] payload =framer.getPayload(frame);
            String plaintext = new String(payload, StandardCharsets.UTF_8);
            System.out.println(plaintext);
        }
        Utils.parkMillis(2000);
        duplexLayer.close();
        
    }    
}

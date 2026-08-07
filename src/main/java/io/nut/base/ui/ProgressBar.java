
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ProgressBar ------------ Emula las capacidades principales de la librería Go
 * "schollz/progressbar": - Barra de progreso configurable (ancho, caracteres de
 * relleno/vacío/cabeza) - Descripción / etiqueta - Porcentaje completado -
 * Velocidad (iteraciones por segundo) - ETA (tiempo restante estimado) - Tiempo
 * transcurrido - Colores ANSI opcionales - Modo "spinner" cuando el total es
 * desconocido (max <= 0) - Thread-safe (Add/Set pueden llamarse desde varios
 * hilos) - Limpieza de la barra al terminar (opcional)
 *
 * Uso básico:
 *
 * ProgressBar bar = new ProgressBar.Builder() .setMax(100)
 * .setDescription("Descargando") .build();
 *
 * for (int i = 0; i < 100; i++) { bar.add(1); Thread.sleep(50); } bar.finish();
 */
public class ProgressBar implements AutoCloseable
{

    // ---- Configuración ----
    private final long max;                 // total de iteraciones (<=0 => modo spinner)
    private final int width;                // ancho de la barra en caracteres
    private final String description;       // texto descriptivo a la izquierda
    private final char fillChar;             // carácter de progreso completado
    private final char emptyChar;            // carácter de progreso pendiente
    private final char headChar;             // carácter "cabeza" de la barra
    private final boolean showBytes;         // formatear como bytes (KB/MB/GB) en vez de unidades
    private final boolean useColors;         // colores ANSI
    private final boolean clearOnFinish;     // borra la línea al llamar finish()
    private final long throttleMillis;       // frecuencia mínima de redibujado

    // ---- Estado ----
    private final AtomicLong current = new AtomicLong(0);
    private final long startNanos = System.nanoTime();
    private final ReentrantLock renderLock = new ReentrantLock();
    private volatile long lastRenderNanos = 0;
    private volatile boolean finished = false;

    private static final String[] SPINNER_FRAMES =
    {
        "|", "/", "-", "\\"
    };
    private int spinnerIndex = 0;

    // Códigos ANSI
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private ProgressBar(Builder b)
    {
        this.max = b.max;
        this.width = b.width;
        this.description = b.description;
        this.fillChar = b.fillChar;
        this.emptyChar = b.emptyChar;
        this.headChar = b.headChar;
        this.showBytes = b.showBytes;
        this.useColors = b.useColors;
        this.clearOnFinish = b.clearOnFinish;
        this.throttleMillis = b.throttleMillis;
    }

    // ---------------- API pública ----------------
    /**
     * Incrementa el progreso en {@code n} y redibuja si corresponde.
     */
    public void add(long n)
    {
        if (finished)
        {
            return;
        }
        long value = current.addAndGet(n);
        if (max > 0 && value > max)
        {
            current.set(max);
        }
        maybeRender();
    }

    /**
     * Fija el progreso a un valor absoluto.
     */
    public void set(long value)
    {
        if (finished)
        {
            return;
        }
        current.set(max > 0 ? Math.min(value, max) : value);
        maybeRender();
    }

    /**
     * Marca la barra como completada al 100% y hace el render final.
     */
    public void finish()
    {
        if (finished)
        {
            return;
        }
        if (max > 0)
        {
            current.set(max);
        }
        finished = true;
        render(true);
        if (clearOnFinish)
        {
            clearLine();
        }
        else
        {
            System.out.println();
        }
    }

    /**
     * Devuelve el progreso actual.
     */
    public long getCurrent()
    {
        return current.get();
    }

    /**
     * Devuelve el porcentaje (0-100), o -1 en modo spinner.
     */
    public double getPercent()
    {
        if (max <= 0)
        {
            return -1;
        }
        return (current.get() * 100.0) / max;
    }

    @Override
    public void close()
    {
        finish();
    }

    // ---------------- Lógica interna ----------------
    private void maybeRender()
    {
        long now = System.nanoTime();
        long elapsedMs = (now - lastRenderNanos) / 1_000_000;
        if (elapsedMs >= throttleMillis || current.get() == max)
        {
            render(false);
        }
    }

    private void render(boolean force)
    {
        if (!renderLock.tryLock())
        {
            return;
        }
        try
        {
            lastRenderNanos = System.nanoTime();
            StringBuilder sb = new StringBuilder();
            sb.append('\r');

            if (description != null && !description.isEmpty())
            {
                sb.append(description).append(' ');
            }

            long cur = current.get();
            double elapsedSec = (System.nanoTime() - startNanos) / 1_000_000_000.0;
            double rate = elapsedSec > 0 ? cur / elapsedSec : 0;

            if (max <= 0)
            {
                // Modo spinner: no conocemos el total
                sb.append(spinnerFrame()).append(' ');
                sb.append(formatCount(cur)).append(' ');
                sb.append(colorize(ANSI_CYAN, formatRate(rate)));
            }
            else
            {
                double percent = (cur * 100.0) / max;
                int filled = (int) Math.round(width * (percent / 100.0));
                filled = Math.min(filled, width);

                sb.append('[');
                sb.append(colorize(ANSI_GREEN, repeat(fillChar, Math.max(0, filled - 1))));
                if (filled > 0 && filled < width)
                {
                    sb.append(colorize(ANSI_GREEN, String.valueOf(headChar)));
                }
                else if (filled > 0)
                {
                    sb.append(colorize(ANSI_GREEN, String.valueOf(fillChar)));
                }
                sb.append(repeat(emptyChar, width - filled));
                sb.append(']');

                sb.append(String.format(" %5.1f%%", percent));
                sb.append(" (").append(formatCount(cur)).append("/").append(formatCount(max)).append(")");
                sb.append(' ').append(colorize(ANSI_CYAN, formatRate(rate)));

                String eta = formatEta(cur, rate);
                if (eta != null)
                {
                    sb.append(' ').append(colorize(ANSI_YELLOW, "ETA " + eta));
                }
            }

            sb.append(" [" + formatDuration(elapsedSec) + "]");

            System.out.print(sb);
            System.out.flush();
        }
        finally
        {
            renderLock.unlock();
        }
    }

    private String spinnerFrame()
    {
        String frame = SPINNER_FRAMES[spinnerIndex % SPINNER_FRAMES.length];
        spinnerIndex++;
        return frame;
    }

    private String formatCount(long v)
    {
        if (!showBytes)
        {
            return String.valueOf(v);
        }
        return formatBytes(v);
    }

    private static String formatBytes(long bytes)
    {
        String[] units =
        {
            "B", "KB", "MB", "GB", "TB"
        };
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1)
        {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.1f%s", value, units[unitIndex]);
    }

    private String formatRate(double rate)
    {
        if (showBytes)
        {
            return formatBytes((long) rate) + "/s";
        }
        return String.format("%.1f it/s", rate);
    }

    private String formatEta(long cur, double rate)
    {
        if (rate <= 0 || max <= 0)
        {
            return null;
        }
        long remaining = max - cur;
        if (remaining <= 0)
        {
            return "0s";
        }
        double secs = remaining / rate;
        return formatDuration(secs);
    }

    private static String formatDuration(double seconds)
    {
        long total = (long) seconds;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (h > 0)
        {
            return String.format("%dh%02dm%02ds", h, m, s);
        }
        if (m > 0)
        {
            return String.format("%dm%02ds", m, s);
        }
        return String.format("%ds", s);
    }

    private static String repeat(char c, int times)
    {
        if (times <= 0)
        {
            return "";
        }
        char[] arr = new char[times];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }

    private String colorize(String color, String text)
    {
        if (!useColors)
        {
            return text;
        }
        return color + text + ANSI_RESET;
    }

    private static void clearLine()
    {
        System.out.print("\r\u001B[2K");
        System.out.flush();
    }

    // ---------------- Builder ----------------
    public static class Builder
    {

        private long max = 100;
        private int width = 40;
        private String description = "";
        private char fillChar = '=';
        private char emptyChar = '-';
        private char headChar = '>';
        private boolean showBytes = false;
        private boolean useColors = true;
        private boolean clearOnFinish = false;
        private long throttleMillis = 50; // evita parpadeo excesivo

        public Builder setMax(long max)
        {
            this.max = max;
            return this;
        }

        public Builder setWidth(int width)
        {
            this.width = width;
            return this;
        }

        public Builder setDescription(String description)
        {
            this.description = description;
            return this;
        }

        public Builder setFillChar(char fillChar)
        {
            this.fillChar = fillChar;
            return this;
        }

        public Builder setEmptyChar(char emptyChar)
        {
            this.emptyChar = emptyChar;
            return this;
        }

        public Builder setHeadChar(char headChar)
        {
            this.headChar = headChar;
            return this;
        }

        public Builder showAsBytes(boolean showBytes)
        {
            this.showBytes = showBytes;
            return this;
        }

        public Builder useColors(boolean useColors)
        {
            this.useColors = useColors;
            return this;
        }

        public Builder clearOnFinish(boolean clearOnFinish)
        {
            this.clearOnFinish = clearOnFinish;
            return this;
        }

        public Builder setThrottleMillis(long ms)
        {
            this.throttleMillis = ms;
            return this;
        }

        public ProgressBar build()
        {
            return new ProgressBar(this);
        }
    }

    // ---------------- Demo ----------------
    public static void main(String[] args) throws InterruptedException
    {
        System.out.println("Demo 1: barra normal con total conocido");
        ProgressBar bar = new ProgressBar.Builder()
                .setMax(50)
                .setDescription("Descargando")
                .setWidth(60)
                .build();

        for (int i = 0; i < 50; i++)
        {
            bar.add(1);
            Thread.sleep(50);
        }
        bar.finish();

        System.out.println("\nDemo 2: modo bytes");
        ProgressBar byteBar = new ProgressBar.Builder()
                .setMax(10_000_000)
                .setDescription("Subiendo archivo")
                .showAsBytes(true)
                .build();

        for (int i = 0; i < 100; i++)
        {
            byteBar.add(100_000);
            Thread.sleep(10);
        }
        byteBar.finish();

        System.out.println("\nDemo 3: modo spinner (total desconocido)");
        ProgressBar spinner = new ProgressBar.Builder()
                .setMax(-1)
                .setDescription("Procesando")
                .build();

        for (int i = 0; i < 40; i++)
        {
            spinner.add(1);
            Thread.sleep(10);
        }
        spinner.finish();
    }
}

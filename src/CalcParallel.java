import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Parallel drop-in replacement for Calc.run().
 *
 * The width axis is split into equal stripes — one per thread. Each thread
 * processes its own stripe with a private WindowPosition and Random, then
 * writes results directly into the shared output Grid.
 *
 * Thread safety rationale:
 *   - Reads: input Grids are not modified during the phase; concurrent reads are safe.
 *   - Writes: Array2DInt stores are int-writes (JLS §17.7 guarantees atomicity for
 *     reads/writes of ints). CA double-buffering ensures at most one vehicle writes
 *     to any given output cell (Nagel-Schreckenberg prevents collisions), so no two
 *     threads ever write to the same cell.
 *   - Visibility: Future.get() provides the happens-before between worker writes
 *     and subsequent main-thread reads.
 *
 * Usage:
 *   CalcParallel cp = new CalcParallel(nThreads);
 *   cp.run(tca, "moveAndLightsPhase", inputs, outputs, ws, ws, rng);
 *   // reuse cp across many steps
 *   cp.shutdown(); // call once when done
 */
public class CalcParallel {

    private final ExecutorService pool;
    private final int threads;

    public CalcParallel(int threads) {
        this.threads = threads;
        this.pool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "calc-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public int threads() { return threads; }

    public void shutdown() { pool.shutdownNow(); }

    /**
     * Parallel execution of a single convolution phase.
     * Semantics match Calc.run() exactly, except ordering of random calls
     * across stripe boundaries differs (simulation remains statistically valid).
     */
    public void run(Object udfHost, String methodName,
                    List<? extends Grid> inputs,
                    List<? extends Grid> outputs,
                    int xWindowSize, int yWindowSize,
                    Random masterRandom) throws Exception {

        Grid primary = inputs.get(0);
        int w = primary.width();
        int h = primary.height();

        // Clear all outputs before parallel execution
        for (Grid o : outputs)
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++)
                    o.set(x, y, null);

        Method method = udfHost.getClass().getMethod(methodName, ConvolveWindows.class);
        method.setAccessible(true);

        Grid[] inArrays = inputs.toArray(new Grid[0]);
        int nStripes = Math.min(threads, w);
        int stripe   = (w + nStripes - 1) / nStripes;

        // Generate per-stripe seeds from master before submitting (deterministic order)
        long[] seeds = new long[nStripes];
        for (int t = 0; t < nStripes; t++) seeds[t] = masterRandom.nextLong();

        List<Future<?>> futures = new ArrayList<>(nStripes);
        for (int t = 0; t < nStripes; t++) {
            final int xS  = t * stripe;
            final int xE  = Math.min(xS + stripe, w);
            final Random rng = new Random(seeds[t]);

            futures.add(pool.submit(() -> {
                try {
                    WindowPosition pos = new WindowPosition(xWindowSize, yWindowSize, w, h, rng);

                    InputConvolveWindow[]  inWins  = new InputConvolveWindow [inputs.size()];
                    OutputConvolveWindow[] outWins = new OutputConvolveWindow[outputs.size()];
                    for (int i = 0; i < inputs.size();  i++) inWins[i]  = new InputWindow(inputs.get(i),   pos);
                    for (int i = 0; i < outputs.size(); i++) outWins[i] = new OutputWindow(outputs.get(i), pos);
                    ConvolveWindows windows = new WinImpl(inWins, outWins, pos);

                    for (int x = xS; x < xE; x++) {
                        for (int y = 0; y < h; y++) {
                            boolean skip = false;
                            for (Grid a : inArrays)
                                if (a.get(x, y) == null) { skip = true; break; }
                            if (skip) continue;
                            pos.setPosition(x, y);
                            method.invoke(udfHost, windows);
                        }
                    }
                } catch (InvocationTargetException e) {
                    Throwable c = e.getCause();
                    throw (c instanceof RuntimeException) ? (RuntimeException) c
                                                         : new RuntimeException("UDF failed", c);
                }
                return null;
            }));
        }

        // Wait and propagate any UDF exceptions
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof Exception) throw (Exception) c;
                throw new RuntimeException(c);
            }
        }
    }

    // ConvolveWindows implementation — one instance per thread stripe
    private static final class WinImpl implements ConvolveWindows {
        private final InputConvolveWindow[]  in;
        private final OutputConvolveWindow[] out;
        private final WindowPosition         pos;

        WinImpl(InputConvolveWindow[] in, OutputConvolveWindow[] out, WindowPosition pos) {
            this.in = in; this.out = out; this.pos = pos;
        }

        @Override public InputConvolveWindow  input(int i)  { return in[i];  }
        @Override public OutputConvolveWindow output(int i) { return out[i]; }
        @Override public void move(int x, int y)            { pos.setPosition(x, y); }
    }
}

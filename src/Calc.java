import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public class Calc {

    public static final int N_CPU = 8;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(N_CPU, r -> {
        Thread t = new Thread(r, "calc-worker");
        t.setDaemon(true);
        return t;
    });

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(POOL::shutdownNow, "calc-pool-shutdown"));
    }

    public static void run(
            Object udfHost,
            String methodName,
            List<? extends Grid> inputs,
            List<? extends Grid> outputs,
            int xWindowSize,
            int yWindowSize,
            Random masterRandom) throws Exception {

        Grid primary = inputs.get(0);
        int w = primary.width();
        int h = primary.height();

        for (Grid o : outputs)
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++)
                    o.set(x, y, null);

        Method method = udfHost.getClass().getMethod(methodName, ConvolveWindows.class);
        method.setAccessible(true);

        Grid[] inArrays = inputs.toArray(new Grid[0]);
        int nStripes = Math.min(N_CPU, w);
        int stripe   = (w + nStripes - 1) / nStripes;

        long[] seeds = new long[nStripes];
        for (int t = 0; t < nStripes; t++) seeds[t] = masterRandom.nextLong();

        List<Future<?>> futures = new ArrayList<>(nStripes);
        for (int t = 0; t < nStripes; t++) {
            final int xS  = t * stripe;
            final int xE  = Math.min(xS + stripe, w);
            final Random rng = new Random(seeds[t]);

            futures.add(POOL.submit(() -> {
                try {
                    WindowPosition pos = new WindowPosition(xWindowSize, yWindowSize, w, h, rng);

                    InputConvolveWindow[]  inWins  = new InputConvolveWindow [inputs.size()];
                    OutputConvolveWindow[] outWins = new OutputConvolveWindow[outputs.size()];
                    for (int i = 0; i < inputs.size();  i++) inWins[i]  = new InputWindow(inputs.get(i),   pos);
                    for (int i = 0; i < outputs.size(); i++) outWins[i] = new OutputWindow(outputs.get(i), pos);
                    ConvolveWindows windows = new WindowsImpl(inWins, outWins, pos);

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

    private static final class WindowsImpl implements ConvolveWindows {
        private final InputConvolveWindow[]  in;
        private final OutputConvolveWindow[] out;
        private final WindowPosition         pos;

        WindowsImpl(InputConvolveWindow[] in, OutputConvolveWindow[] out, WindowPosition pos) {
            this.in = in; this.out = out; this.pos = pos;
        }

        @Override public InputConvolveWindow  input(int i)  { return in[i];  }
        @Override public OutputConvolveWindow output(int i) { return out[i]; }
        @Override public void move(int x, int y)            { pos.setPosition(x, y); }
    }
}

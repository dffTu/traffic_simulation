import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Benchmark {

    private static final int WARMUP  = 20;
    private static final int MEASURE = 50;
    private static final int RUNS    = 5;
    private static final int WS      = 11;
    private static final long SEED   = 42L;

    enum Strategy {
        BASELINE,   // TCA  + Array2D   (4 phases)
        INT,        // TCA  + Array2DInt (4 phases)
        V2,         // TCAv2+ Array2DInt (5 phases: computeGap + move + turns + lights)
        V3          // TCAv3+ Array2DInt (3 phases: moveAndLights + turns)
    }

    public static void main(String[] args) throws Exception {
        int[][] sizes = {
            {72, 144}, {144, 288}, {256, 512},
            {256, 1024}, {256, 2048}, {256, 4096}, {1000, 1000}
        };

        System.out.println("JVM warmup ...");
        measureMs(72, 144, WARMUP, MEASURE, Strategy.BASELINE);
        measureMs(72, 144, WARMUP, MEASURE, Strategy.V3);
        System.out.println("done\n");

        String header = String.format("%-14s  %-20s  %-20s  %-20s  %-20s  %s  %s  %s",
                "Grid",
                "TCA+Array2D (base)",
                "TCA+Array2DInt",
                "TCAv2+Array2DInt",
                "TCAv3+Array2DInt",
                "SpeedupInt", "SpeedupV2", "SpeedupV3");
        String sep = "-".repeat(130);
        System.out.println(header);
        System.out.println(sep);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        new File("results").mkdirs();
        String basePath = "results/benchmark_" + timestamp;

        try (PrintWriter txt = new PrintWriter(basePath + ".txt");
             PrintWriter csv = new PrintWriter(basePath + ".csv")) {

            txt.println("Benchmark: TCA vs Array2DInt vs TCAv2 vs TCAv3");
            txt.println("warmup=" + WARMUP + "  measure=" + MEASURE
                    + "  runs=" + RUNS + "  seed=" + SEED);
            txt.println("date: " + timestamp);
            txt.println();
            txt.println(header);
            txt.println(sep);

            csv.println("grid,width,height,cells,"
                    + "baseline_ms,baseline_sps,"
                    + "int_ms,int_sps,"
                    + "v2_ms,v2_sps,"
                    + "v3_ms,v3_sps,"
                    + "speedup_int,speedup_v2,speedup_v3");

            for (int[] sz : sizes) {
                int w = sz[0], h = sz[1];
                long[] msBase = new long[RUNS];
                long[] msInt  = new long[RUNS];
                long[] msV2   = new long[RUNS];
                long[] msV3   = new long[RUNS];
                for (int r = 0; r < RUNS; r++) {
                    msBase[r] = measureMs(w, h, 0, MEASURE, Strategy.BASELINE);
                    msInt[r]  = measureMs(w, h, 0, MEASURE, Strategy.INT);
                    msV2[r]   = measureMs(w, h, 0, MEASURE, Strategy.V2);
                    msV3[r]   = measureMs(w, h, 0, MEASURE, Strategy.V3);
                }
                long mBase = median(msBase), mInt = median(msInt),
                     mV2   = median(msV2),   mV3  = median(msV3);
                double sBase = sps(mBase), sInt = sps(mInt),
                       sV2   = sps(mV2),   sV3  = sps(mV3);
                double supInt = sInt / sBase, supV2 = sV2 / sBase, supV3 = sV3 / sBase;

                String line = String.format(
                        "%-14s  %5d ms (%6.0f s/s)   %5d ms (%6.0f s/s)   %5d ms (%6.0f s/s)   %5d ms (%6.0f s/s)   %.2fx       %.2fx       %.2fx",
                        w + "x" + h,
                        mBase, sBase, mInt, sInt, mV2, sV2, mV3, sV3,
                        supInt, supV2, supV3);
                System.out.println(line);
                txt.println(line);

                csv.printf(Locale.US,
                        "%dx%d,%d,%d,%d,%.0f,%d,%.0f,%d,%.0f,%d,%.0f,%.4f,%.4f,%.4f%n",
                        w, h, w, h,
                        mBase, sBase, mInt, sInt, mV2, sV2, mV3, sV3,
                        supInt, supV2, supV3);
            }

            System.out.printf("%nSaved: %s.txt  %s.csv%n", basePath, basePath);
            txt.printf("%nwarmup=%d  measure=%d  runs=%d  seed=%d%n",
                    WARMUP, MEASURE, RUNS, SEED);
        }
    }

    static long measureMs(int w, int h, int warmup, int measure, Strategy s) throws Exception {
        GridFactory factory = (s == Strategy.BASELINE) ? Array2D::new : Array2DInt::new;
        TCA tca = switch (s) {
            case V2 -> new TCAv2();
            case V3 -> new TCAv3();
            default -> new TCA();
        };

        Random rng = new Random(SEED);
        Grid driver = filled(factory, w, h, 0.0);
        Grid lane   = factory.create(w, h);
        run(new MapGen(), "generate", list(driver), list(lane), 0, rng);

        Grid pos    = factory.create(w, h);
        run(tca, "putVehicle", list(lane), list(pos), 0, rng);

        Grid length = factory.create(w, h);
        Grid speed  = factory.create(w, h);
        run(tca, "setLength", list(pos), list(length), 3, rng);
        run(tca, "setSpeed",  list(pos), list(speed),  0, rng);

        Grid temperature = filled(factory, w, h, 20.0);
        Grid gap = (s == Strategy.V2) ? factory.create(w, h) : null;

        Grid[] spd = {speed,  factory.create(w, h)};
        Grid[] len = {length, factory.create(w, h)};

        for (int i = 0; i < warmup; i++) step(tca, lane, spd, len, temperature, gap, s, rng);

        long t0 = System.nanoTime();
        for (int i = 0; i < measure; i++) step(tca, lane, spd, len, temperature, gap, s, rng);
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static void step(TCA tca, Grid lane, Grid[] spd, Grid[] len,
                              Grid temp, Grid gap, Strategy s, Random rng) throws Exception {
        if (s == Strategy.V3) {
            // 3 phases: moveAndLights → turnLeft → turnRight
            run(tca, "moveAndLightsPhase", list(lane, spd[0], len[0], temp), list(spd[1], len[1]), WS, rng);
            swap(spd); swap(len);
            run(tca, "turnLeftPhase",  list(lane, spd[0], len[0]), list(spd[1], len[1]), WS, rng);
            swap(spd); swap(len);
            run(tca, "turnRightPhase", list(lane, spd[0], len[0]), list(spd[1], len[1]), WS, rng);
            swap(spd); swap(len);
        } else {
            // 4 phases baseline/int; 5 phases V2 (extra computeGap)
            if (gap != null) {
                run(tca, "computeGap", list(spd[0], lane, len[0]), list(gap), WS, rng);
                run(tca, "moveForwardPhase", list(lane, spd[0], len[0], temp, gap), list(spd[1], len[1]), WS, rng);
            } else {
                run(tca, "moveForwardPhase", list(lane, spd[0], len[0], temp), list(spd[1], len[1]), WS, rng);
            }
            swap(spd); swap(len);
            run(tca, "turnLeftPhase",  list(lane, spd[0], len[0]), list(spd[1], len[1]), WS, rng);
            swap(spd); swap(len);
            run(tca, "turnRightPhase", list(lane, spd[0], len[0]), list(spd[1], len[1]), WS, rng);
            swap(spd); swap(len);
            run(tca, "lightsPhase",    list(lane, spd[0], len[0]), list(spd[1], len[1]), WS, rng);
            swap(spd); swap(len);
        }
    }

    private static void run(Object host, String method,
                             List<? extends Grid> in, List<? extends Grid> out,
                             int ws, Random rng) throws Exception {
        Calc.run(host, method, in, out, ws, ws, rng);
    }

    private static Grid filled(GridFactory f, int w, int h, double v) {
        Grid g = f.create(w, h);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                g.set(x, y, v);
        return g;
    }

    @SafeVarargs
    private static <T extends Grid> List<T> list(T... grids) {
        return Arrays.asList(grids);
    }

    private static void swap(Grid[] a) { Grid t = a[0]; a[0] = a[1]; a[1] = t; }

    private static double sps(long ms) { return MEASURE * 1000.0 / ms; }

    private static long median(long[] a) {
        long[] s = a.clone(); Arrays.sort(s); return s[s.length / 2];
    }
}

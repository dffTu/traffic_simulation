import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws Exception {
        int width   = args.length >= 1 ? Integer.parseInt(args[0]) : 144;
        int height  = args.length >= 2 ? Integer.parseInt(args[1]) : 288;
        int steps   = args.length >= 3 ? Integer.parseInt(args[2]) : 100;
        long seed   = args.length >= 4 ? Long.parseLong(args[3])   : 0L;
        int scale   = args.length >= 5 ? Integer.parseInt(args[4]) : 4;
        String grid = args.length >= 6 ? args[5].toLowerCase()     : "int";
        String ver  = args.length >= 7 ? args[6].toLowerCase()     : "v3";
        int threads = args.length >= 8 ? Integer.parseInt(args[7]) : 1;

        GridFactory factory;
        switch (grid) {
            case "int":    factory = Array2DInt::new; break;
            case "double": factory = Array2D::new;    break;
            default:
                System.err.println("grid: double | int");
                System.exit(1); return;
        }

        TCA tca;
        switch (ver) {
            case "v3":  tca = new TCAv3(); break;
            case "v2":  tca = new TCAv2(); break;
            case "tca": tca = new TCA();   break;
            default:
                System.err.println("tca: tca | v2 | v3");
                System.exit(1); return;
        }

        System.out.printf("grid %dx%d  steps=%d  seed=%d  scale=%d  grid=%s  tca=%s  threads=%d%n%n",
                width, height, steps, seed, scale, grid, ver, threads);

        parallel = threads > 1 ? new CalcParallel(threads) : null;

        Random rng = new Random(seed);
        final int WS = 11;
        long t0 = System.nanoTime();

        // ── Init ─────────────────────────────────────────────────────────
        Grid driver = fill(factory, width, height, 0.0);
        Grid lane   = factory.create(width, height);
        calc(new MapGen(), "generate", rng, 0, in(driver), out(lane));

        Grid pos = factory.create(width, height);
        calc(tca, "putVehicle", rng, 0, in(lane), out(pos));

        Grid length = factory.create(width, height);
        Grid speed  = factory.create(width, height);
        calc(tca, "setLength", rng, 3, in(pos), out(length));
        calc(tca, "setSpeed",  rng, 0, in(pos), out(speed));

        Grid temperature = fill(factory, width, height, 20.0);
        Grid gap = (tca instanceof TCAv2) ? factory.create(width, height) : null;

        long tInit = System.nanoTime();
        System.out.printf("init done in %d ms%n", (tInit - t0) / 1_000_000);
        printStats("step 0", speed, length, lane);
        if (scale > 0) saveFrame(lane, speed, length, scale, 0);

        // ── Simulation loop ───────────────────────────────────────────────
        Grid[] spd = {speed,  factory.create(width, height)};
        Grid[] len = {length, factory.create(width, height)};

        for (int step = 1; step <= steps; step++) {

            if (tca instanceof TCAv3) {
                // 3 фазы: moveAndLights → turnLeft → turnRight
                calc(tca, "moveAndLightsPhase", rng, WS,
                        in(lane, spd[0], len[0], temperature), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "turnLeftPhase",  rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "turnRightPhase", rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

            } else if (tca instanceof TCAv2) {
                // 5 фаз: computeGap → moveForward → turnLeft → turnRight → lights
                calc(tca, "computeGap", rng, WS, in(spd[0], lane, len[0]), out(gap));

                calc(tca, "moveForwardPhase", rng, WS,
                        in(lane, spd[0], len[0], temperature, gap), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "turnLeftPhase",  rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "turnRightPhase", rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "lightsPhase",    rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

            } else {
                // 4 фазы: оригинальный TCA
                calc(tca, "moveForwardPhase", rng, WS,
                        in(lane, spd[0], len[0], temperature), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "turnLeftPhase",  rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "turnRightPhase", rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);

                calc(tca, "lightsPhase",    rng, WS, in(lane, spd[0], len[0]), out(spd[1], len[1]));
                swap(spd); swap(len);
            }

            if (step == 1 || step % 10 == 0 || step == steps)
                printStats("step " + step, spd[0], len[0], lane);
            if (scale > 0)
                saveFrame(lane, spd[0], len[0], scale, step);
        }

        long tEnd = System.nanoTime();
        System.out.printf("%nsimulation done in %d ms (%.1f steps/sec)%n",
                (tEnd - tInit) / 1_000_000,
                steps * 1000.0 / ((tEnd - tInit) / 1_000_000.0));

        if (parallel != null) parallel.shutdown();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static CalcParallel parallel;

    private static void calc(Object host, String method, Random rng, int ws,
                              List<Grid> inputs, List<Grid> outputs) throws Exception {
        if (parallel != null)
            parallel.run(host, method, inputs, outputs, ws, ws, rng);
        else
            Calc.run(host, method, inputs, outputs, ws, ws, rng);
    }

    @SafeVarargs
    private static <T extends Grid> List<T> in(T... grids)  { return Arrays.asList(grids); }

    @SafeVarargs
    private static <T extends Grid> List<T> out(T... grids) { return Arrays.asList(grids); }

    private static void swap(Grid[] a) { Grid t = a[0]; a[0] = a[1]; a[1] = t; }

    private static Grid fill(GridFactory f, int w, int h, double v) {
        Grid a = f.create(w, h);
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                a.set(x, y, v);
        return a;
    }

    private static void saveFrame(Grid lane, Grid speed, Grid length, int scale, int step)
            throws Exception {
        Viz.save(Viz.render(lane, speed, length, scale),
                String.format("frames/step_%04d.png", step));
    }

    private static void printStats(String label, Grid speed, Grid length, Grid lane) {
        int vehicles = 0, stopped = 0, lights = 0;
        double speedSum = 0;
        for (int x = 0; x < speed.width(); x++) {
            for (int y = 0; y < speed.height(); y++) {
                Double v   = speed.get(x, y);
                Double l   = lane.get(x, y);
                Double len = length.get(x, y);
                if (v == null || l == null) continue;
                if (l.intValue() == TCA.LANE_TRAFFIC_LIGHTS) { lights++; continue; }
                if (len == null || len.intValue() <= 0) continue;
                vehicles++;
                int sv = v.intValue();
                if (sv >= 100) sv %= 100;
                if (sv == 0) stopped++;
                speedSum += sv;
            }
        }
        System.out.printf("%-10s  vehicles=%5d  avgSpeed=%.2f  stopped=%d  lights=%d%n",
                label, vehicles, vehicles > 0 ? speedSum / vehicles : 0.0, stopped, lights);
    }
}

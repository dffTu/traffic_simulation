public class TCAv4 extends TCAv3 {

    private short[][] weTL;
    private short[][] snTL;

    public void init(Grid lane) {
        int w = lane.width(), h = lane.height();
        weTL = new short[w][h];
        snTL = new short[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                weTL[x][y] = scanWE(lane, x, y, w, h);
                snTL[x][y] = scanSN(lane, x, y, w, h);
            }
        }
    }

    private static short scanWE(Grid lane, int ax, int ay, int w, int h) {
        for (int xOff = 1; xOff <= MAX_VEHICLE_VISIBILITY; xOff++) {
            for (int yOff = -CROSSING_WIDTH; yOff <= CROSSING_WIDTH; yOff++) {
                int nx = ax + xOff, ny = ay + yOff;
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                Double v = lane.get(nx, ny);
                if (v != null && v.intValue() == LANE_TRAFFIC_LIGHTS)
                    return (short) ((xOff << 3) | (yOff + 4));
            }
        }
        return 0;
    }

    private static short scanSN(Grid lane, int ax, int ay, int w, int h) {
        for (int xOff = 1; xOff <= MAX_VEHICLE_VISIBILITY; xOff++) {
            for (int yOff = -CROSSING_WIDTH; yOff <= CROSSING_WIDTH; yOff++) {
                int nx = ax + yOff, ny = ay - xOff;
                if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                Double v = lane.get(nx, ny);
                if (v != null && v.intValue() == LANE_TRAFFIC_LIGHTS)
                    return (short) ((xOff << 3) | (yOff + 4));
            }
        }
        return 0;
    }

    @Override
    public int[] findTrafficLights(InputConvolveWindow lane, boolean iAmSNVehicle) {
        if (weTL == null) return super.findTrafficLights(lane, iAmSNVehicle);
        short v = iAmSNVehicle ? snTL[lane.getArrayX()][lane.getArrayY()]
                               :  weTL[lane.getArrayX()][lane.getArrayY()];
        if (v == 0) return null;
        return new int[]{ v >> 3, (v & 7) - 4 };
    }
}

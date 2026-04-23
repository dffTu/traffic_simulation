public class MapGen {
    public static final int BW = 15;
    public static final int NLANES = 3;
    public static final int PERIOD = BW + NLANES;

    public void generate(ConvolveWindows w) {
        int localX = Math.floorMod(w.input(0).getArrayX(), PERIOD);
        int localY = Math.floorMod(w.input(0).getArrayY(), PERIOD);

        Double result = null;
        if (localX == BW - 1 && localY == 0) {
            result = (double) TCA.LANE_TRAFFIC_LIGHTS;
        } else if (localX < BW && localY < BW) {
            result = null;
        } else if (localX >= BW && localY >= BW) {
            result = (double) TCA.LANE_ROAD_CROSSING;
        } else if (localX >= BW) {
            result = (double) TCA.LANE_SOUTH_NORTH;
        } else if (localY >= BW) {
            result = (double) TCA.LANE_WEST_EAST;
        }
        w.output(0).set(result, 0, 0);
    }
}

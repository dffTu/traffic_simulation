public class TCAv2 extends TCA {
    public void computeGap(ConvolveWindows w) {
        Double speedVal = w.input(0).get(0, 0);
        Double laneVal  = w.input(1).get(0, 0);
        Double lenVal   = w.input(2).get(0, 0);
        if (speedVal == null || laneVal == null || lenVal == null) return;

        int laneType = laneVal.intValue();
        int mySpeed  = speedVal.intValue();
        int myLength = lenVal.intValue();

        if (laneType == LANE_TRAFFIC_LIGHTS) {
            w.output(0).set(-1.0, 0, 0);
            return;
        }

        ConvolveWindow.Degrees degrees = ConvolveWindow.Degrees._0;
        switch (laneType) {
            case LANE_SOUTH_NORTH:
                degrees = ConvolveWindow.Degrees._90;
                break;
            case LANE_ROAD_CROSSING:
                if (mySpeed >= 100) degrees = ConvolveWindow.Degrees._90;
                break;
        }
        InputConvolveWindow speedWin = w.input(0).rotate(degrees);

        int dist = myLength;
        while (dist <= MAX_VEHICLE_VISIBILITY) {
            if (speedWin.get(dist, 0) != null) break;
            dist++;
        }
        w.output(0).set((double) (dist - myLength), 0, 0);
    }

    @Override
    public void moveForwardPhase(ConvolveWindows windows) {
        InputConvolveWindow lane        = windows.input(0);
        InputConvolveWindow speed       = windows.input(1);
        InputConvolveWindow length      = windows.input(2);
        InputConvolveWindow temperature = windows.input(3);
        InputConvolveWindow gap         = windows.input(4);

        OutputConvolveWindow outSpeed  = windows.output(0);
        OutputConvolveWindow outLength = windows.output(1);

        int laneType = lane.get(0, 0).intValue();
        int myLength = length.get(0, 0).intValue();
        int mySpeed  = speed.get(0, 0).intValue();
        double temp  = temperature.get(0, 0);
        boolean iAmSNVehicle = false;
        boolean atARoadCrossing = false;
        ConvolveWindow.Degrees degrees = ConvolveWindow.Degrees._0;

        switch (laneType) {
            case LANE_TRAFFIC_LIGHTS:
                outSpeed.set((double) mySpeed, 0, 0);
                outLength.set((double) myLength, 0, 0);
                return;
            case LANE_ROAD_CROSSING:
                if (mySpeed >= 100) {
                    iAmSNVehicle = true;
                    degrees = ConvolveWindow.Degrees._90;
                    mySpeed %= 10;
                }
                atARoadCrossing = true;
                break;
            case LANE_WEST_EAST:
                break;
            case LANE_SOUTH_NORTH:
                degrees = ConvolveWindow.Degrees._90;
                iAmSNVehicle = true;
                break;
        }

        lane      = lane.rotate(degrees);
        speed     = speed.rotate(degrees);
        outSpeed  = outSpeed.rotate(degrees);
        outLength = outLength.rotate(degrees);

        int distToNearestVehicle = gap.get(0, 0).intValue();

        int newSpeed = Math.min(mySpeed + 1, MAX_VEHICLE_SPEED - myLength + 1);
        newSpeed = Math.min(newSpeed, distToNearestVehicle);

        if (speed.random().nextDouble() < 0.1) newSpeed = Math.max(0, newSpeed - 1);

        if (temp < 10 && temperature.random().nextDouble() < 0.2) newSpeed = Math.max(0, newSpeed - 1);

        if (!atARoadCrossing) {
            int[] lights = findTrafficLights(lane, iAmSNVehicle);
            if (lights != null) {
                int xL = lights[0];
                int lState = getLightsState(speed.get(xL, lights[1]).intValue(), iAmSNVehicle);
                if (lState == LIGHTS_RED || lState == LIGHTS_YELLOW) {
                    newSpeed = Math.min(Math.max(xL - myLength, 0), newSpeed);
                }
            }
        }

        int speedValue = newSpeed;
        int x = newSpeed;
        Double destinationLaneCellD = lane.get(newSpeed, 0);
        if (destinationLaneCellD != null) {
            if (destinationLaneCellD.intValue() == LANE_ROAD_CROSSING && iAmSNVehicle) {
                speedValue = 100 + newSpeed;
            }
        }
        outSpeed.set((double) speedValue, x, 0);
        outLength.set((double) myLength, x, 0);
    }
}

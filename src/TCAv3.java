public class TCAv3 extends TCA {

    public void moveAndLightsPhase(ConvolveWindows windows) {
        InputConvolveWindow lane        = windows.input(0);
        InputConvolveWindow speed       = windows.input(1);
        InputConvolveWindow length      = windows.input(2);
        InputConvolveWindow temperature = windows.input(3);

        OutputConvolveWindow outSpeed  = windows.output(0);
        OutputConvolveWindow outLength = windows.output(1);

        int laneType = lane.get(0, 0).intValue();
        int mySpeed  = speed.get(0, 0).intValue();
        int myLength = length.get(0, 0).intValue();

        if (laneType == LANE_TRAFFIC_LIGHTS) {
            int state = mySpeed;

            if (state == 201 || state == 202) {
                state = (state == 201) ? LIGHTS_TICKS : 100 + LIGHTS_TICKS;
                outSpeed.set((double) state, 0, 0);
                outLength.set(0.0, 0, 0);
                return;
            }

            boolean greenForSN = (state >= 100);
            if (greenForSN) state %= 100;

            if (state == 0) {
                state = greenForSN ? 201 : 202;
            } else {
                state--;
                if (greenForSN) state += 100;
            }

            outSpeed.set((double) state, 0, 0);
            outLength.set(0.0, 0, 0);
            return;
        }

        double temp = temperature.get(0, 0);
        boolean iAmSNVehicle = false;
        boolean atARoadCrossing = false;
        ConvolveWindow.Degrees degrees = ConvolveWindow.Degrees._0;

        switch (laneType) {
            case LANE_ROAD_CROSSING:
                if (mySpeed >= 100) {
                    iAmSNVehicle = true;
                    degrees = ConvolveWindow.Degrees._90;
                    mySpeed %= 10;
                }
                atARoadCrossing = true;
                break;
            case LANE_SOUTH_NORTH:
                degrees = ConvolveWindow.Degrees._90;
                iAmSNVehicle = true;
                break;
            case LANE_WEST_EAST:
            default:
                break;
        }

        lane      = lane.rotate(degrees);
        speed     = speed.rotate(degrees);
        outSpeed  = outSpeed.rotate(degrees);
        outLength = outLength.rotate(degrees);

        int distToNearestVehicle = myLength;
        while (distToNearestVehicle <= MAX_VEHICLE_VISIBILITY) {
            if (speed.get(distToNearestVehicle, 0) != null) break;
            distToNearestVehicle++;
        }
        distToNearestVehicle -= myLength;

        int newSpeed = Math.min(mySpeed + 1, MAX_VEHICLE_SPEED - myLength + 1);
        newSpeed = Math.min(newSpeed, distToNearestVehicle);

        if (speed.random().nextDouble() < 0.1) newSpeed = Math.max(0, newSpeed - 1);

        if (temp < 10 && temperature.random().nextDouble() < 0.2)
            newSpeed = Math.max(0, newSpeed - 1);

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
        Double dest = lane.get(newSpeed, 0);
        if (dest != null && dest.intValue() == LANE_ROAD_CROSSING && iAmSNVehicle) {
            speedValue = 100 + newSpeed;
        }
        outSpeed.set((double) speedValue, x, 0);
        outLength.set((double) myLength, x, 0);
    }
}

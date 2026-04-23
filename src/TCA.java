/*
 * Copyright (c) 2021.
 * Ramon Antonio Rodriges Zalipynis (Architect & Developer), rodriges AT gis DOT land
 */

/**
 * @author (c) R.A. Rodriges Zalipynis (Architect  & Developer), rodriges AT wikience DOT org
 */
public class TCA {
    public static final int MAX_VEHICLE_SPEED = 3;
    public static final int MAX_VEHICLE_LENGTH = 3;
    public static final int MAX_VEHICLE_VISIBILITY = 5;

    public static final int LANE_IMPASSIBLE = -1;
    public static final int LANE_WEST_EAST = 0;
    public static final int LANE_SOUTH_NORTH = 1;
    public static final int LANE_ROAD_CROSSING = 2;
    public static final int LANE_TRAFFIC_LIGHTS = 3;

    public static final int LIGHTS_RED = 0;
    public static final int LIGHTS_YELLOW = 1;
    public static final int LIGHTS_GREEN = 2;

    public static final int CROSSING_WIDTH = 3;

    public static final int TURN_LEFT = 1;
    public static final int TURN_RIGHT = -1;

    public static final int LIGHTS_TICKS = 5;

    public void putVehicle(ConvolveWindows w) {
        double laned = w.input(0).get(0, 0);
        int lane = (int) laned;
        boolean is = w.input(0).random().nextDouble() > 0.85;

        double result = Double.MAX_VALUE;
        if (lane == LANE_TRAFFIC_LIGHTS) result = 4.;
        if (lane == LANE_ROAD_CROSSING) result = 2.;
        if (lane == LANE_SOUTH_NORTH && is) result = 1.;
        if (lane == LANE_SOUTH_NORTH && !is) result = -1.;
        if (lane == LANE_WEST_EAST && is) result = 0.;
        if (lane == LANE_WEST_EAST && !is) result = -2.;

        w.output(0).set(result, 0, 0);
    }

    public void setLength(ConvolveWindows w) {
        InputConvolveWindow win = w.input(0);
        double val = win.get(0, 0);
        if (val != 1 && val != 0) {
            if (val == 4) {
                // traffic lights
                w.output(0).set((double) 0, 0, 0);
            } else {
                w.output(0).set(null, 0, 0);
            }
            return; // no vehicle
        }
        // a south-north moving vehicle, just rotate the window
        if (val == 1) win = win.rotate(ConvolveWindow.Degrees._90);
        int cellsToClosestVehicle = 1;
        for (int i = 1; i < MAX_VEHICLE_LENGTH; i++) {
            Double next = win.get(i, 0);
            if (next == null || next >= 0) break;
            cellsToClosestVehicle++;
        }
        int vehicleLength = win.random().nextInt(cellsToClosestVehicle) + 1;
        w.output(0).set((double) (vehicleLength), 0, 0);
    }

    public void setSpeed(ConvolveWindows w) {
        double val = w.input(0).get(0, 0);
        Double output = null;
        if (val == 1 || val == 0) {
            output = (double) w.input(0).random().nextInt(4);
        } else {
            if (val == 4) {
                // traffic lights
                output = (double) (200 + w.output(0).random().nextInt(2) + 1);
            }
        }
        w.output(0).set(output, 0, 0);
    }

    public boolean isLeftTurnNeeded(InputConvolveWindow speed, int myLength) {
        // != null: convolution is not run for NoData (0,0) cells
        int mySpeed = speed.get(0, 0).intValue();
        int myFrontBumper = myLength - 1;

        for (int x = myFrontBumper; x <= MAX_VEHICLE_VISIBILITY; x++) {
            Double frontSpeed = speed.get(x, 0);
            if (frontSpeed != null) {
                if (frontSpeed < mySpeed)
                    return speed.random().nextDouble() > 0.4;
                else return false;
            }
        }

        // we can still turn left with some probability even if there is no
        // slow-moving vehicle in front of us
        return speed.random().nextDouble() > 0.8;
    }

    public boolean isRightTurnNeeded(InputConvolveWindow speed) {
        return speed.random().nextDouble() > 0.8;
    }

    public boolean isLaneTurnPossible(InputConvolveWindow speed, InputConvolveWindow length, int turnType) {
        // border is on the left/right, we are at the leftmost/rightmost lane
        if (speed.get(0, turnType) == null) return false;

        for (int x = 0; x <= MAX_VEHICLE_VISIBILITY; x++) {
            Double backSpeed = speed.get(-x, turnType);
            Double backLength = length.get(-x, turnType);
            if (backSpeed != null) {
                // x = vehicle's rear bumper, lets calculate the front bumper
                int xFrontBumper_plus_one = x - backLength.intValue();
                if (xFrontBumper_plus_one <= 0)
                    return false; // a vehicle is just near us
                if (xFrontBumper_plus_one - backSpeed.intValue() <= 0)
                    return false; // a too fast-moving vehicle
            }
        }

        return true;
    }

    public void turnLeftPhase(ConvolveWindows windows) {
        turnPhase(windows, TURN_LEFT);
    }

    public void turnRightPhase(ConvolveWindows windows) {
        turnPhase(windows, TURN_RIGHT);
    }

    private void turnPhase(ConvolveWindows windows, int turnType) {
        InputConvolveWindow lane = windows.input(0);
        InputConvolveWindow speed = windows.input(1);
        InputConvolveWindow length = windows.input(2);

        OutputConvolveWindow outSpeed = windows.output(0);
        OutputConvolveWindow outLength = windows.output(1);

        boolean notOnCrossing = true;

        // never == null
        int laneType = lane.get(0, 0).intValue();
        int myLength = length.get(0, 0).intValue();
        int mySpeed = speed.get(0, 0).intValue();
        int codedSpeed = mySpeed;
        ConvolveWindow.Degrees degrees = ConvolveWindow.Degrees._0;
        boolean iAmSNVehicle = false;
        switch (laneType) {
            case LANE_TRAFFIC_LIGHTS:
                outSpeed.set((double) mySpeed, 0, 0);
                outLength.set((double) myLength, 0, 0);
                return;
            case LANE_ROAD_CROSSING:
                notOnCrossing = false;
                if (mySpeed >= 100) {
                    iAmSNVehicle = true;
                    degrees = ConvolveWindow.Degrees._90;
                    mySpeed %= 10;
                }
                break;
            case LANE_WEST_EAST:
                break;
            case LANE_SOUTH_NORTH:
                degrees = ConvolveWindow.Degrees._90;
                iAmSNVehicle = true;
                break;
        }

        lane = lane.rotate(degrees);
        speed = speed.rotate(degrees);
        length = length.rotate(degrees);
        outSpeed = outSpeed.rotate(degrees);
        outLength = outLength.rotate(degrees);

        if (notOnCrossing) {
            int y = 0;
            boolean isTurnNeeded = false;
            boolean isTurnPossible = false;
            switch (turnType) {
                case TURN_LEFT:
                    isTurnNeeded = isLeftTurnNeeded(speed, myLength);
                    isTurnPossible = isLaneTurnPossible(speed, length, turnType);
                    break;
                case TURN_RIGHT:
                    isTurnNeeded = isRightTurnNeeded(speed);
                    isTurnPossible = isLaneTurnPossible(speed, length, turnType);
                    break;
            }

            if (isTurnNeeded && isTurnPossible) {
                y = turnType;
            }

            outSpeed.set((double) mySpeed, 0, y);
            outLength.set((double) myLength, 0, y);
        } else {
            boolean isTurnPossible = true;

            // it is not our turn to turn
            if ((turnType == TURN_LEFT && iAmSNVehicle) ||
                    (turnType == TURN_RIGHT && !iAmSNVehicle)) {
                isTurnPossible = false;
            }

            // a vehicle may not be allowed to turn as it is not on the outer lane
            Double v = lane.get(0, turnType);
            if (v == null) {
                isTurnPossible = false;
            } else {
                if (v.intValue() == LANE_ROAD_CROSSING) {
                    isTurnPossible = false;
                }
            }

            if (!isTurnPossible) {
                outSpeed.set((double) codedSpeed, 0, 0);
                outLength.set((double) myLength, 0, 0);
                return;
            }

            boolean isTurnNeeded = windows.input(0).random().nextDouble() > 0.8;
            isTurnPossible = isCrossingTurnPossible(length, turnType);

            double theSpeed;
            int y;
            if (isTurnNeeded && isTurnPossible) {
                y = turnType;
                theSpeed = mySpeed;
            } else {
                y = 0;
                theSpeed = codedSpeed;
            }
            outSpeed.set(theSpeed, 0, y);
            outLength.set((double) myLength, 0, y);
        }
    }

    private boolean isCrossingTurnPossible(InputConvolveWindow length, int turnType) {
        // check whether there is sufficient space
        int myLength = length.get(0, 0).intValue();
        int y = turnType;
        Double len = length.get(0, y);
        while (len == null && myLength > 0) {
            myLength--;
            y += turnType;
            len = length.get(0, y);
        }
        return myLength == 0;
    }

    public int[] findTrafficLights(InputConvolveWindow lane, boolean iAmSNVehicle) {
        int bestX = Integer.MAX_VALUE;
        int bestY = 0;

        for (int x = 1; x <= MAX_VEHICLE_VISIBILITY; x++) {
            for (int y = -CROSSING_WIDTH; y <= CROSSING_WIDTH; y++) {
                Double cell = lane.get(x, y);
                if (cell != null && cell.intValue() == LANE_TRAFFIC_LIGHTS) {
                    if (x < bestX || (x == bestX && Math.abs(y) < Math.abs(bestY))) {
                        bestX = x;
                        bestY = y;
                    }
                }
            }
            if (bestX != Integer.MAX_VALUE) {
                return new int[]{bestX, bestY};
            }
        }

        return null;
    }

    public int getLightsState(int value, boolean iAmSNVehicle) {
        // xx < 100 ticks for green for WE
        // xx > 100 ticks for green for SE
        // x = 201 - yellow, next green is for WE
        // x = 202 - yellow, next green is for SN
        // initially 201 or 202 randomly in speed, length is always 0

        if (value == 201 || value == 202) {
            return LIGHTS_YELLOW;
        }

        if (value < 100) {
            if (iAmSNVehicle) {
                return LIGHTS_RED;
            } else {
                return LIGHTS_GREEN;
            }
        } else {
            if (iAmSNVehicle) {
                return LIGHTS_GREEN;
            } else {
                return LIGHTS_RED;
            }
        }
    }

    public void lightsPhase(ConvolveWindows windows) {
        // xx < 100 ticks for green for WE
        // xx > 100 ticks for green for SE
        // x = 201 - yellow, next green is for WE
        // x = 202 - yellow, next green is for SN
        // initially 201 or 202 randomly in speed, length is always 0

        InputConvolveWindow lane = windows.input(0);
        InputConvolveWindow speed = windows.input(1);
        InputConvolveWindow length = windows.input(2);
        int state = speed.get(0, 0).intValue();
        int aLength = length.get(0, 0).intValue();

        OutputConvolveWindow outSpeed = windows.output(0);
        OutputConvolveWindow outLength = windows.output(1);

        if (lane.get(0, 0).intValue() != LANE_TRAFFIC_LIGHTS) {
            outSpeed.set((double) state, 0, 0);
            outLength.set((double) aLength, 0, 0);
            return;
        }

        if (state == 201 || state == 202) {
            // check the road crossing
            boolean areVehiclesOnCrossing = false;
            for (int x = 1; x <= CROSSING_WIDTH; x++) {
                for (int y = -1; y >= -CROSSING_WIDTH; y--) {
                    if (speed.get(x, y) != null) {
                        areVehiclesOnCrossing = true;
                        break;
                    }
                }
                if (areVehiclesOnCrossing) break;
            }
            if (!areVehiclesOnCrossing) {
                // switch the light
                state = state == 201 ? LIGHTS_TICKS : 100 + LIGHTS_TICKS;
            }
            outSpeed.set((double) state, 0, 0);
            outLength.set((double) 0, 0, 0);
            return;
        }

        boolean greenForSN = false;
        if (state >= 100) {
            greenForSN = true;
            state = state % 100;
        }

        if (state == 0) {
            // switch the lights
            state = greenForSN ? 201 : 202;
        } else {
            state--;
            if (greenForSN) state += 100;
        }

        outSpeed.set((double) state, 0, 0);
        outLength.set((double) 0, 0, 0);
    }

    public void moveForwardPhase(ConvolveWindows windows) {
        InputConvolveWindow lane = windows.input(0);
        InputConvolveWindow speed = windows.input(1);
        InputConvolveWindow length = windows.input(2);
        InputConvolveWindow temperature = windows.input(3);

        OutputConvolveWindow outSpeed = windows.output(0);
        OutputConvolveWindow outLength = windows.output(1);

        // never == null
        int laneType = lane.get(0, 0).intValue();
        int myLength = length.get(0, 0).intValue();
        int mySpeed = speed.get(0, 0).intValue();
        double temp = temperature.get(0, 0);
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

        lane = lane.rotate(degrees);
        speed = speed.rotate(degrees);
        outSpeed = outSpeed.rotate(degrees);
        outLength = outLength.rotate(degrees);

        // cannot be less than vehicle's length: (0, 0) is vehicle's rear bumper
        int distToNearestVehicle = myLength;
        while (distToNearestVehicle <= MAX_VEHICLE_VISIBILITY) {
            if (speed.get(distToNearestVehicle, 0) != null) break;
            distToNearestVehicle++;
        }

        distToNearestVehicle -= myLength;

        // 1, 2: acceleration & braking
        int newSpeed = Math.min(mySpeed + 1, MAX_VEHICLE_SPEED - myLength + 1);
        newSpeed = Math.min(newSpeed, distToNearestVehicle);

        // 3 randomization
        if (speed.random().nextDouble() < 0.1) newSpeed = Math.max(0, newSpeed - 1);

        // temperature
        if (temp < 10 && temperature.random().nextDouble() < 0.2) newSpeed = Math.max(0, newSpeed - 1);

        // 4 account for traffic lights
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

        // 5 vehicle movement
        int speedValue = newSpeed;
        int x = newSpeed;
        Double destinationLaneCellD = lane.get(newSpeed, 0);
        if (destinationLaneCellD != null) {
            int destinationLaneCell = destinationLaneCellD.intValue();
            // vehicle is at the road crossing and we need to save its moving direction
            if (destinationLaneCell == LANE_ROAD_CROSSING) {
                if (iAmSNVehicle) {
                    speedValue = 100 + newSpeed;
                }
            }
        }
        outSpeed.set((double) speedValue, x, 0);
        outLength.set((double) myLength, x, 0);
    }
}

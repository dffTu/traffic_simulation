import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Viz {

    private static final Color IMPASSABLE = Color.WHITE;
    private static final Color LANE_WE    = new Color(225, 245, 225);
    private static final Color LANE_SN    = new Color(235, 225, 250);
    private static final Color CROSSING   = new Color(220, 245, 245);
    private static final Color LIGHTS     = new Color(255, 210, 210);
    private static final Color LIGHT_BASE = new Color(250, 235, 235);
    private static final Color LIGHT_WE = new Color(60, 140, 70);
    private static final Color LIGHT_YELLOW = new Color(245, 200, 60);
    private static final Color LIGHT_SN = new Color(120, 80, 190);

    private static final Color SPEED_0    = new Color( 40,  40,  40);   // stopped: dark gray
    private static final Color SPEED_1    = new Color(  0,  90, 230);   // slow:    saturated blue
    private static final Color SPEED_2    = new Color(200,   0, 220);   // medium:  magenta
    private static final Color SPEED_3    = new Color(255, 120,   0);   // fast:    orange

    public static BufferedImage render(Grid lane, Grid speed, Grid length, int scale) {
        int w = lane.width();
        int h = lane.height();
        BufferedImage img = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Double laneValue = lane.get(x, y);
                Double speedValue = speed.get(x, y);
                if (laneValue != null && laneValue.intValue() == TCA.LANE_TRAFFIC_LIGHTS) {
                    drawTrafficLight(g, x, y, scale, speedValue);
                    continue;
                }
                g.setColor(backgroundColor(laneValue));
                g.fillRect(x * scale, y * scale, scale, scale);
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Double s = speed.get(x, y);
                Double len = length.get(x, y);
                Double l = lane.get(x, y);
                if (s == null || len == null || l == null) continue;
                int laneType = l.intValue();
                if (laneType == TCA.LANE_TRAFFIC_LIGHTS) continue;
                int lenVal = len.intValue();
                if (lenVal <= 0) continue;
                int speedVal = s.intValue();

                int dx = directionDx(laneType, speedVal);
                int dy = directionDy(laneType, speedVal);

                g.setColor(speedColor(speedVal));
                for (int i = 0; i < lenVal; i++) {
                    int cx = x + dx * i;
                    int cy = y + dy * i;
                    if (cx < 0 || cx >= w || cy < 0 || cy >= h) break;
                    g.fillRect(cx * scale, cy * scale, scale, scale);
                }
            }
        }

        g.dispose();
        return img;
    }

    public static void save(BufferedImage img, String path) throws IOException {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        ImageIO.write(img, "png", f);
    }

    private static Color backgroundColor(Double v) {
        if (v == null) return IMPASSABLE;
        switch (v.intValue()) {
            case TCA.LANE_WEST_EAST:      return LANE_WE;
            case TCA.LANE_SOUTH_NORTH:    return LANE_SN;
            case TCA.LANE_ROAD_CROSSING:  return CROSSING;
            case TCA.LANE_TRAFFIC_LIGHTS: return LIGHTS;
        }
        return IMPASSABLE;
    }

    private static void drawTrafficLight(Graphics2D g, int x, int y, int scale, Double speedValue) {
        int left = x * scale;
        int top = y * scale;
        Color color = LIGHT_BASE;
        if (speedValue != null) {
            int state = speedValue.intValue();
            if (state == 201 || state == 202) {
                color = LIGHT_YELLOW;
            } else if (state >= 100) {
                color = LIGHT_SN;
            } else {
                color = LIGHT_WE;
            }
        }
        g.setColor(color);
        g.fillRect(left, top, scale, scale);
    }

    private static Color speedColor(int speed) {
        if (speed >= 100) speed %= 100;
        switch (speed) {
            case 0: return SPEED_0;
            case 1: return SPEED_1;
            case 2: return SPEED_2;
            case 3: return SPEED_3;
        }
        return Color.BLACK;
    }

    private static int directionDx(int laneType, int speed) {
        if (laneType == TCA.LANE_WEST_EAST) return 1;
        if (laneType == TCA.LANE_SOUTH_NORTH) return 0;
        if (laneType == TCA.LANE_ROAD_CROSSING) return speed >= 100 ? 0 : 1;
        return 0;
    }

    private static int directionDy(int laneType, int speed) {
        if (laneType == TCA.LANE_WEST_EAST) return 0;
        if (laneType == TCA.LANE_SOUTH_NORTH) return -1;
        if (laneType == TCA.LANE_ROAD_CROSSING) return speed >= 100 ? -1 : 0;
        return 0;
    }
}

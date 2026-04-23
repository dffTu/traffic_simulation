final class Rotation {
    private Rotation() {}

    static int rotX(int dx, int dy, ConvolveWindow.Degrees deg) {
        switch (deg) {
            case _0:   return dx;
            case _90:  return dy;
            case _180: return -dx;
            case _270: return -dy;
        }
        throw new IllegalStateException();
    }

    static int rotY(int dx, int dy, ConvolveWindow.Degrees deg) {
        switch (deg) {
            case _0:   return dy;
            case _90:  return -dx;
            case _180: return -dy;
            case _270: return dx;
        }
        throw new IllegalStateException();
    }

    private static final ConvolveWindow.Degrees[] ALL = {
        ConvolveWindow.Degrees._0,
        ConvolveWindow.Degrees._90,
        ConvolveWindow.Degrees._180,
        ConvolveWindow.Degrees._270
    };

    static ConvolveWindow.Degrees compose(ConvolveWindow.Degrees a, ConvolveWindow.Degrees b) {
        return ALL[(ord(a) + ord(b)) % 4];
    }

    private static int ord(ConvolveWindow.Degrees d) {
        switch (d) {
            case _0:   return 0;
            case _90:  return 1;
            case _180: return 2;
            case _270: return 3;
        }
        throw new IllegalStateException();
    }
}

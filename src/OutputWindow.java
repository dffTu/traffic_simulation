import java.util.Random;

class OutputWindow implements OutputConvolveWindow {
    private final Grid array;
    private final WindowPosition pos;
    private final ConvolveWindow.Degrees rotation;

    OutputWindow(Grid array, WindowPosition pos) {
        this(array, pos, ConvolveWindow.Degrees._0);
    }

    private OutputWindow(Grid array, WindowPosition pos, ConvolveWindow.Degrees rotation) {
        this.array = array;
        this.pos = pos;
        this.rotation = rotation;
    }

    @Override
    public boolean set(Double value, int x, int y) {
        int adx = Rotation.rotX(x, y, rotation);
        int ady = Rotation.rotY(x, y, rotation);
        return array.set(pos.arrayX + adx, pos.arrayY + ady, value);
    }

    @Override
    public OutputConvolveWindow rotate(Degrees degrees) {
        return new OutputWindow(array, pos, Rotation.compose(rotation, degrees));
    }

    @Override public Random random()       { return pos.random; }
    @Override public int getArrayX()       { return pos.arrayX; }
    @Override public int getArrayY()       { return pos.arrayY; }
    @Override public int xWindowSize()     { return pos.xWindowSize; }
    @Override public int yWindowSize()     { return pos.yWindowSize; }
    @Override public int xSubarraySize()   { return pos.xSubarraySize; }
    @Override public int ySubarraySize()   { return pos.ySubarraySize; }

    @Override
    public void move(int currentX, int currentY) {
        pos.setPosition(currentX, currentY);
    }
}

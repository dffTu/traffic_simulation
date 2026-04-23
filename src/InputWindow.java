import java.util.Random;

class InputWindow implements InputConvolveWindow {
    private final Grid array;
    private final WindowPosition pos;
    private final ConvolveWindow.Degrees rotation;

    InputWindow(Grid array, WindowPosition pos) {
        this(array, pos, ConvolveWindow.Degrees._0);
    }

    private InputWindow(Grid array, WindowPosition pos, ConvolveWindow.Degrees rotation) {
        this.array = array;
        this.pos = pos;
        this.rotation = rotation;
    }

    @Override
    public Double get(int x, int y) {
        int adx = Rotation.rotX(x, y, rotation);
        int ady = Rotation.rotY(x, y, rotation);
        return array.get(pos.arrayX + adx, pos.arrayY + ady);
    }

    @Override
    public InputConvolveWindow rotate(Degrees degrees) {
        return new InputWindow(array, pos, Rotation.compose(rotation, degrees));
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

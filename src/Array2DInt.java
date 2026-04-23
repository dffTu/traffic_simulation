import java.util.Arrays;

public class Array2DInt implements Grid {

    static final int NULL_VALUE = Integer.MIN_VALUE;

    private final int width;
    private final int height;
    private final int[] cells;

    public Array2DInt(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.cells = new int[width * height];
        Arrays.fill(cells, NULL_VALUE);
    }

    @Override public int width()  { return width; }
    @Override public int height() { return height; }

    @Override
    public Double get(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        int v = cells[x * height + y];
        return v == NULL_VALUE ? null : (double) v;
    }

    @Override
    public boolean set(int x, int y, Double value) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        cells[x * height + y] = (value == null) ? NULL_VALUE : (int) value.doubleValue();
        return true;
    }
}

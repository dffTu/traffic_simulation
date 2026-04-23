public class Array2D implements Grid {
    private final int width;
    private final int height;
    private final Double[][] cells;

    public Array2D(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.cells = new Double[width][height];
    }

    public int width()  { return width; }
    public int height() { return height; }

    public Double get(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return cells[x][y];
    }

    public boolean set(int x, int y, Double value) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        cells[x][y] = value;
        return true;
    }
}

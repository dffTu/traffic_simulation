public interface Grid {
    int width();
    int height();
    Double get(int x, int y);
    boolean set(int x, int y, Double value);
}

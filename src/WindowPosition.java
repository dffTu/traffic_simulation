import java.util.Random;

class WindowPosition {
    int arrayX;
    int arrayY;

    final int xWindowSize;
    final int yWindowSize;
    final int xSubarraySize;
    final int ySubarraySize;
    final Random random;

    WindowPosition(int xWindowSize, int yWindowSize,
                   int xSubarraySize, int ySubarraySize,
                   Random random) {
        this.xWindowSize = xWindowSize;
        this.yWindowSize = yWindowSize;
        this.xSubarraySize = xSubarraySize;
        this.ySubarraySize = ySubarraySize;
        this.random = random;
    }

    void setPosition(int x, int y) {
        this.arrayX = x;
        this.arrayY = y;
    }
}

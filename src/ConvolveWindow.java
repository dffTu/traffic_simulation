import java.util.Random;

public interface ConvolveWindow<T> {
    enum Degrees { _0, _90, _180, _270 }

    int getArrayX();
    int getArrayY();

    int xWindowSize();
    int yWindowSize();

    int xSubarraySize();
    int ySubarraySize();

    T rotate(Degrees degrees);

    Random random();

    void move(int currentX, int currentY);
}

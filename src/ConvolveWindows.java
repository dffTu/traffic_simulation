public interface ConvolveWindows {
    InputConvolveWindow input(int index);
    OutputConvolveWindow output(int index);
    void move(int currentX, int currentY);
}

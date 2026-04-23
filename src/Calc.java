import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

public class Calc {

    public static void run(
            Object udfHost,
            String methodName,
            List<? extends Grid> inputs,
            List<? extends Grid> outputs,
            int xWindowSize,
            int yWindowSize,
            Random random) throws Exception {

        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("at least one input is required");
        }
        if (outputs == null || outputs.isEmpty()) {
            throw new IllegalArgumentException("at least one output is required");
        }

        Grid primary = inputs.get(0);
        int w = primary.width();
        int h = primary.height();

        for (Grid a : inputs) {
            if (a.width() != w || a.height() != h) {
                throw new IllegalArgumentException("all inputs must share the same shape");
            }
        }
        for (Grid a : outputs) {
            if (a.width() != w || a.height() != h) {
                throw new IllegalArgumentException("all outputs must share the shape of inputs");
            }
            for (Grid in : inputs) {
                if (a == in) {
                    throw new IllegalArgumentException("input and output must be different arrays");
                }
            }
        }

        for (Grid o : outputs) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    o.set(x, y, null);
                }
            }
        }

        Method method = udfHost.getClass().getMethod(methodName, ConvolveWindows.class);
        method.setAccessible(true);

        WindowPosition pos = new WindowPosition(xWindowSize, yWindowSize, w, h, random);
        InputConvolveWindow[] inWins = new InputConvolveWindow[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            inWins[i] = new InputWindow(inputs.get(i), pos);
        }
        OutputConvolveWindow[] outWins = new OutputConvolveWindow[outputs.size()];
        for (int i = 0; i < outputs.size(); i++) {
            outWins[i] = new OutputWindow(outputs.get(i), pos);
        }
        ConvolveWindows windows = new WindowsImpl(inWins, outWins, pos);

        final int nInputs = inputs.size();
        Grid[] inArrays = inputs.toArray(new Grid[0]);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                boolean skip = false;
                for (int i = 0; i < nInputs; i++) {
                    if (inArrays[i].get(x, y) == null) { skip = true; break; }
                }
                if (skip) continue;

                pos.setPosition(x, y);
                try {
                    method.invoke(udfHost, windows);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    if (cause instanceof Error) throw (Error) cause;
                    throw new RuntimeException("UDF " + methodName + " failed at (" + x + "," + y + ")", cause);
                }
            }
        }
    }

    private static final class WindowsImpl implements ConvolveWindows {
        private final InputConvolveWindow[] inputs;
        private final OutputConvolveWindow[] outputs;
        private final WindowPosition pos;

        WindowsImpl(InputConvolveWindow[] inputs, OutputConvolveWindow[] outputs, WindowPosition pos) {
            this.inputs = inputs;
            this.outputs = outputs;
            this.pos = pos;
        }

        @Override public InputConvolveWindow input(int index)   { return inputs[index]; }
        @Override public OutputConvolveWindow output(int index) { return outputs[index]; }
        @Override public void move(int currentX, int currentY)  { pos.setPosition(currentX, currentY); }
    }
}

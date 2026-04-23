import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Viewer {

    private static int frameIndex = 0;
    private static ImageIcon[] frames;
    private static JFrame window;
    private static JLabel canvas;
    private static Timer timer;
    private static int delayMs;
    private static boolean playing = true;

    public static void main(String[] args) throws Exception {
        String dir = args.length >= 1 ? args[0] : "frames";
        delayMs   = args.length >= 2 ? Integer.parseInt(args[1]) : 120;

        File[] files = new File(dir).listFiles((d, n) ->
                n.endsWith(".png") && n.startsWith("step_"));
        if (files == null || files.length == 0) {
            System.err.println("no frames found in '" + dir + "'. run Main first.");
            System.exit(1);
        }
        Arrays.sort(files);
        System.out.println("loading " + files.length + " frames from " + dir + "...");

        frames = new ImageIcon[files.length];
        for (int i = 0; i < files.length; i++) {
            frames[i] = new ImageIcon(ImageIO.read(files[i]));
        }

        final int maxScreenH = 900;
        int w0 = frames[0].getIconWidth();
        int h0 = frames[0].getIconHeight();
        if (h0 > maxScreenH) {
            double k = maxScreenH / (double) h0;
            w0 = (int) (w0 * k);
            h0 = maxScreenH;
            for (int i = 0; i < frames.length; i++) {
                Image scaled = frames[i].getImage().getScaledInstance(w0, h0, Image.SCALE_FAST);
                frames[i] = new ImageIcon(scaled);
            }
            System.out.println("scaled to fit: " + w0 + "x" + h0);
        }
        final int imgW = w0;
        final int imgH = h0;
        SwingUtilities.invokeLater(() -> buildUI(imgW, imgH));
    }

    private static void buildUI(int w, int h) {
        window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.getContentPane().setBackground(Color.BLACK);
        window.getContentPane().setLayout(new BorderLayout());

        canvas = new JLabel(frames[0]);
        canvas.setPreferredSize(new Dimension(w, h));
        window.getContentPane().add(canvas, BorderLayout.CENTER);

        window.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:  togglePlay();                       break;
                    case KeyEvent.VK_RIGHT:  pause(); step(+1);                  break;
                    case KeyEvent.VK_LEFT:   pause(); step(-1);                  break;
                    case KeyEvent.VK_UP:     setDelay(Math.max(20, delayMs - 20)); break;
                    case KeyEvent.VK_DOWN:   setDelay(Math.min(2000, delayMs + 40)); break;
                    case KeyEvent.VK_R:      frameIndex = 0; render();          break;
                    case KeyEvent.VK_Q:
                    case KeyEvent.VK_ESCAPE: window.dispose(); System.exit(0);   break;
                }
            }
        });
        window.setFocusable(true);

        timer = new Timer(delayMs, e -> {
            if (playing) step(+1);
        });
        timer.start();

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        window.requestFocusInWindow();
        render();
    }

    private static void togglePlay() {
        playing = !playing;
        render();
    }

    private static void pause() {
        playing = false;
    }

    private static void setDelay(int d) {
        delayMs = d;
        timer.setDelay(d);
        render();
    }

    private static void step(int dir) {
        frameIndex = (frameIndex + dir + frames.length) % frames.length;
        render();
    }

    private static void render() {
        canvas.setIcon(frames[frameIndex]);
        window.setTitle(String.format(
                "traffic sim — step %d/%d — %s — %d ms/frame   [Space play  ←→ step  ↑↓ speed  R reset  Q quit]",
                frameIndex, frames.length - 1, playing ? "playing" : "paused", delayMs));
    }
}

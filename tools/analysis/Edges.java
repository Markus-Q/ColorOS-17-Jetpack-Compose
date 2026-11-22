import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
/** Prints the runs of pixels along a row or column that differ from a background color. args: png axis(x|y) fixed x0 x1 bgR bgG bgB thr */
public class Edges {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        boolean row = a[1].equals("y");
        int fixed = Integer.parseInt(a[2]), s = Integer.parseInt(a[3]), e = Integer.parseInt(a[4]);
        int br = Integer.parseInt(a[5]), bg = Integer.parseInt(a[6]), bb = Integer.parseInt(a[7]), thr = Integer.parseInt(a[8]);
        boolean in = false; int start = 0; int pr = 0, pg = 0, pb = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i <= e; i++) {
            int rgb = row ? im.getRGB(i, fixed) : im.getRGB(fixed, i);
            int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
            boolean diff = Math.max(Math.abs(r - br), Math.max(Math.abs(g - bg), Math.abs(b - bb))) >= thr;
            if (diff && !in) { in = true; start = i; }
            if (!diff && in) { in = false; sb.append(String.format("[%d..%d] ", start, i - 1)); }
        }
        if (in) sb.append(String.format("[%d..%d] ", start, e));
        System.out.println(sb);
    }
}

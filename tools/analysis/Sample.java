import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Samples pixels of a screenshot.
 * usage: java Sample.java file.png x y [x y ...]     -> r,g,b per point (hex too)
 *        java Sample.java file.png box x0 y0 x1 y1   -> average color of the box and the min/max luminance pixel
 */
public class Sample {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        if (a[1].equals("box")) {
            int x0 = Integer.parseInt(a[2]), y0 = Integer.parseInt(a[3]), x1 = Integer.parseInt(a[4]), y1 = Integer.parseInt(a[5]);
            double sr = 0, sg = 0, sb = 0;
            int n = 0, lmin = 1 << 30, lmax = -1, pmin = 0, pmax = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int p = im.getRGB(x, y);
                    int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                    sr += r; sg += g; sb += b; n++;
                    int l = r * 30 + g * 59 + b * 11;
                    if (l < lmin) { lmin = l; pmin = p; }
                    if (l > lmax) { lmax = l; pmax = p; }
                }
            }
            System.out.printf("avg %.1f,%.1f,%.1f  min %s  max %s  n=%d%n", sr / n, sg / n, sb / n, hex(pmin), hex(pmax), n);
            return;
        }
        for (int i = 1; i + 1 < a.length; i += 2) {
            int x = Integer.parseInt(a[i]), y = Integer.parseInt(a[i + 1]);
            int p = im.getRGB(x, y);
            System.out.printf("(%d,%d) %d,%d,%d  %s%n", x, y, (p >> 16) & 255, (p >> 8) & 255, p & 255, hex(p));
        }
    }

    static String hex(int p) {
        return String.format("#%02X%02X%02X", (p >> 16) & 255, (p >> 8) & 255, p & 255);
    }
}

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Ink bounding box of text or an icon inside a region of a screenshot.
 * usage: java Ink.java file.png x0 y0 x1 y1 [thr=48]
 * The background is the color of the region's top-left pixel; "ink" is every pixel whose max channel distance to the
 * background exceeds thr. Prints left/top/right/bottom (exclusive right/bottom, sub-pixel free), width, height and the
 * summed coverage (in pixels, weighted by distance / full contrast) which is proportional to stroke weight times length.
 */
public class Ink {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        int x0 = Integer.parseInt(a[1]), y0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), y1 = Integer.parseInt(a[4]);
        int thr = a.length > 5 ? Integer.parseInt(a[5]) : 48;
        int bg = im.getRGB(x0, y0);
        int br = (bg >> 16) & 255, bgg = (bg >> 8) & 255, bb = bg & 255;
        int l = 1 << 30, t = 1 << 30, r = -1, b = -1;
        double cov = 0;
        int maxd = 1;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = im.getRGB(x, y);
            int d = Math.max(Math.abs(((p >> 16) & 255) - br), Math.max(Math.abs(((p >> 8) & 255) - bgg), Math.abs((p & 255) - bb)));
            if (d > maxd) maxd = d;
        }
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = im.getRGB(x, y);
            int d = Math.max(Math.abs(((p >> 16) & 255) - br), Math.max(Math.abs(((p >> 8) & 255) - bgg), Math.abs((p & 255) - bb)));
            cov += d / (double) maxd;
            if (d > thr) {
                if (x < l) l = x;
                if (x > r) r = x;
                if (y < t) t = y;
                if (y > b) b = y;
            }
        }
        if (r < 0) {
            System.out.println("no ink");
            return;
        }
        System.out.printf("ink x=%d..%d y=%d..%d  w=%d h=%d  coverage=%.0f  maxContrast=%d%n", l, r, t, b, r - l + 1, b - t + 1, cov, maxd);
    }
}

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.*;

/**
 * Samples (outside, inside) colour pairs along the straight edges of a glass panel and fits per channel
 * inside = gain * outside + offset, plus a luminance/saturation view of the mapping.
 * args: B.png left top right bottom gap size
 *   gap  distance from the edge to the sampled patches (px), size the patch thickness (px)
 */
public class EdgePairs {
    static BufferedImage im;

    static double[] avg(int x0, int y0, int x1, int y1) {
        double r = 0, g = 0, b = 0; int n = 0;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = im.getRGB(x, y);
            r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
        }
        return new double[]{r / n, g / n, b / n};
    }

    public static void main(String[] a) throws Exception {
        im = ImageIO.read(new File(a[0]));
        int L = Integer.parseInt(a[1]), T = Integer.parseInt(a[2]), R = Integer.parseInt(a[3]), B = Integer.parseInt(a[4]);
        int gap = Integer.parseInt(a[5]), sz = Integer.parseInt(a[6]);
        int seg = 40;
        List<double[]> pairs = new ArrayList<>();
        // left & right edges
        for (int y = T + 100; y < B - 100; y += seg) {
            double[] o = avg(L - gap - sz, y, L - gap, y + seg), i = avg(L + gap, y, L + gap + sz, y + seg);
            pairs.add(new double[]{o[0], o[1], o[2], i[0], i[1], i[2]});
            o = avg(R + gap, y, R + gap + sz, y + seg); i = avg(R - gap - sz, y, R - gap, y + seg);
            pairs.add(new double[]{o[0], o[1], o[2], i[0], i[1], i[2]});
        }
        // top & bottom edges
        for (int x = L + 100; x < R - 100; x += seg) {
            double[] o = avg(x, T - gap - sz, x + seg, T - gap), i = avg(x, T + gap, x + seg, T + gap + sz);
            pairs.add(new double[]{o[0], o[1], o[2], i[0], i[1], i[2]});
        }
        for (double[] p : pairs) System.out.printf("out %5.1f %5.1f %5.1f   in %5.1f %5.1f %5.1f%n", p[0], p[1], p[2], p[3], p[4], p[5]);
        for (int c = 0; c < 3; c++) {
            double sx = 0, sy = 0, sxx = 0, sxy = 0; int n = pairs.size();
            for (double[] p : pairs) { sx += p[c]; sy += p[3 + c]; sxx += p[c] * p[c]; sxy += p[c] * p[3 + c]; }
            double gain = (n * sxy - sx * sy) / (n * sxx - sx * sx), off = (sy - gain * sx) / n;
            double se = 0;
            for (double[] p : pairs) { double e = gain * p[c] + off - p[3 + c]; se += e * e; }
            System.out.printf("channel %d: in = %.3f * out + %.2f   (rmse %.2f, n=%d)%n", c, gain, off, Math.sqrt(se / n), n);
        }
    }
}

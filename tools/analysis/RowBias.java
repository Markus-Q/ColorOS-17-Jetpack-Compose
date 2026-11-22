import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Mean (real - lab) per row band, over x in [x0,x1), skipping bright real pixels (text). args: lab real x0 x1 y0 y1 step thr */
public class RowBias {
    public static void main(String[] a) throws Exception {
        BufferedImage lab = ImageIO.read(new File(a[0])), real = ImageIO.read(new File(a[1]));
        int x0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), y0 = Integer.parseInt(a[4]), y1 = Integer.parseInt(a[5]), step = Integer.parseInt(a[6]);
        double thr = Double.parseDouble(a[7]);
        for (int y = y0; y < y1; y += step) {
            double s = 0, sr = 0, sl = 0; int n = 0;
            for (int yy = y; yy < Math.min(y + step, y1); yy++) for (int x = x0; x < x1; x++) {
                int p = real.getRGB(x, yy), q = lab.getRGB(x, yy);
                if (Math.max(Math.max((p >> 16) & 255, (p >> 8) & 255), p & 255) > thr) continue;
                double lr = (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3.0, ll = (((q >> 16) & 255) + ((q >> 8) & 255) + (q & 255)) / 3.0;
                s += lr - ll; sr += lr; sl += ll; n++;
            }
            if (n > 50) System.out.printf("y %4d  real %.2f lab %.2f  diff %+.2f  (n=%d)%n", y, sr / n, sl / n, s / n, n);
        }
    }
}

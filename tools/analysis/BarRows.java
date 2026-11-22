import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Per row statistics of the real bar (S1) versus its backdrop (BG) for x in [x0,x1): mean, std of both, and the std of the difference. args: S1 BG x0 x1 y0 y1 step */
public class BarRows {
    static double g(int p) { return (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3.0; }
    public static void main(String[] a) throws Exception {
        BufferedImage s1 = ImageIO.read(new File(a[0])), bg = ImageIO.read(new File(a[1]));
        int x0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]), y0 = Integer.parseInt(a[4]), y1 = Integer.parseInt(a[5]), st = Integer.parseInt(a[6]);
        System.out.println("  y    real mean/std     bg mean/std");
        for (int y = y0; y < y1; y += st) {
            double sm = 0, sq = 0, bm = 0, bq = 0; int n = 0;
            for (int yy = y; yy < y + st; yy++) for (int x = x0; x < x1; x++) { double r = g(s1.getRGB(x, yy)), b = g(bg.getRGB(x, yy)); sm += r; sq += r * r; bm += b; bq += b * b; n++; }
            sm /= n; bm /= n;
            System.out.printf("%4d   %6.2f %6.2f     %6.2f %6.2f%n", y, sm, Math.sqrt(Math.max(0, sq / n - sm * sm)), bm, Math.sqrt(Math.max(0, bq / n - bm * bm)));
        }
    }
}

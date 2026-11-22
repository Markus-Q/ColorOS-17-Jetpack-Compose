import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Hypotheses for the progressive blur of the real top bar (gray values):
 *   out = (1 - a(u)) * blur_sigma(u)(BG) + a(u) * 0        (tint colour black)
 * with u = distance from the bar's bottom edge / barH (1 at the top, 0 at the bottom).
 *   sigma:  linear  sigma = sTop*u      |  fast   sigma = sTop*min(1, speed*u)
 *   alpha:  linear  a = k*u             |  ramp   a = k*piecewise(0:0, 0.4:0.7, 1:1)
 * args: S1 BG
 */
public class BarFit2 {
    static int refl(int i, int n) { if (i < 0) i = -i - 1; if (i >= n) i = 2 * n - i - 1; return Math.max(0, Math.min(n - 1, i)); }
    static double[][] gray(BufferedImage im, int h) {
        int w = im.getWidth(); double[][] o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { int p = im.getRGB(x, y); o[y][x] = (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / (3 * 255.0); }
        return o;
    }
    static double[][] blur(double[][] src, double sigma) {
        int h = src.length, w = src[0].length;
        if (sigma < 0.3) return src;
        int r = (int) Math.ceil(sigma * 3);
        double[] k = new double[2 * r + 1]; double s = 0;
        for (int i = -r; i <= r; i++) { k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma)); s += k[i + r]; }
        for (int i = 0; i < k.length; i++) k[i] /= s;
        double[][] t = new double[h][w], o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * src[y][refl(x + i, w)]; t[y][x] = a; }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * t[refl(y + i, h)][x]; o[y][x] = a; }
        return o;
    }
    static double ramp(double u) { return u <= 0.4 ? 0.7 * u / 0.4 : 0.7 + 0.3 * (u - 0.4) / 0.6; }

    public static void main(String[] a) throws Exception {
        int H = 700;
        double[][] real = gray(ImageIO.read(new File(a[0])), H), bg = gray(ImageIO.read(new File(a[1])), H);
        double[] levels = {0, 4, 8, 12, 16, 20, 25, 30, 36, 45, 60};
        double[][][] bl = new double[levels.length][][];
        for (int i = 0; i < levels.length; i++) bl[i] = blur(bg, levels[i]);
        int W = real[0].length;
        boolean[][] use = new boolean[H][W];
        for (int y = 100; y < 330; y++) for (int x = 180; x < 1040; x++) use[y][x] = !(x >= 170 && x < 660 && y >= 150 && y < 250);
        String[] names = {"sigma linear / alpha linear", "sigma linear / alpha ramp", "sigma fast(3) / alpha linear", "sigma fast(3) / alpha ramp"};
        for (int hyp = 0; hyp < 4; hyp++) {
            double best = 1e9; String bestS = "";
            for (double sTop : new double[]{25, 30, 36})
              for (double barH : new double[]{250, 260, 270, 280, 290, 300, 320})
                for (double k = 0.0; k <= 0.9; k += 0.05) {
                    double se = 0; long n = 0;
                    for (int y = 100; y < 330; y++) {
                        double u = Math.max(0, Math.min(1, (barH - y) / barH));
                        double sg = (hyp < 2) ? sTop * u : sTop * Math.min(1, 3 * u);
                        double al = (hyp % 2 == 0) ? k * u : k * ramp(u);
                        if (y >= barH) { sg = 0; al = 0; }
                        int li = 0; while (li < levels.length - 2 && sg > levels[li + 1]) li++;
                        double t = (sg - levels[li]) / (levels[li + 1] - levels[li]); t = Math.max(0, Math.min(1, t));
                        for (int x = 180; x < 1040; x++) if (use[y][x]) {
                            double d = (1 - t) * bl[li][y][x] + t * bl[li + 1][y][x];
                            double e = ((1 - al) * d - real[y][x]) * 255; se += e * e; n++;
                        }
                    }
                    double r = Math.sqrt(se / n);
                    if (r < best) { best = r; bestS = String.format("sigmaTop=%.0f barH=%.0f k=%.2f  rmse %.3f", sTop, barH, k, r); }
                }
            System.out.println(names[hyp] + ":  " + bestS);
        }
    }
}

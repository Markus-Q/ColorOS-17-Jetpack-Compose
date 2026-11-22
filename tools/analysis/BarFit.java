import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Fits a progressive blur top bar model: out(x,y) = (1 - a(y)) * blur_sigma(y)(BG)(x,y) + a(y) * tintGray
 * with sigma(y) falling linearly from sigmaTop (y=0) to 0 (y=barH), a(y) falling linearly from aTop to aBottom.
 * Gray values only. args: S1 BG
 */
public class BarFit {
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
    public static void main(String[] a) throws Exception {
        int H = 700;   // rows used for the blur (content below the bar is part of the backdrop)
        double[][] real = gray(ImageIO.read(new File(a[0])), H), bg = gray(ImageIO.read(new File(a[1])), H);
        double[] levels = {0, 4, 8, 12, 16, 20, 25, 30, 40, 50, 60};
        double[][][] bl = new double[levels.length][][];
        for (int i = 0; i < levels.length; i++) bl[i] = blur(bg, levels[i]);
        int W = real[0].length;
        boolean[][] use = new boolean[H][W];
        for (int y = 100; y < 280; y++) for (int x = 180; x < 1040; x++) {
            boolean title = x >= 170 && x < 660 && y >= 150 && y < 250;
            use[y][x] = !title;
        }
        double best = 1e9; String bestS = "";
        for (double sTop : new double[]{20, 30, 40, 50, 60})
          for (double barH : new double[]{262, 270, 280, 300})
            for (double aTop : new double[]{0, 0.1, 0.2, 0.25, 0.3, 0.4})
              for (double aBot : new double[]{0, 0.05, 0.1}) {
                double se = 0; long n = 0;
                for (int y = 100; y < 280; y++) {
                    double f = Math.max(0, Math.min(1, y / barH));          // 0 top .. 1 bottom
                    double sg = sTop * (1 - f);
                    double al = aTop + (aBot - aTop) * f;
                    if (y >= barH) { sg = 0; al = 0; }
                    // interpolate between blur levels
                    int li = 0; while (li < levels.length - 2 && sg > levels[li + 1]) li++;
                    double t = (sg - levels[li]) / (levels[li + 1] - levels[li]); t = Math.max(0, Math.min(1, t));
                    for (int x = 180; x < 1040; x++) if (use[y][x]) {
                        double d = (1 - t) * bl[li][y][x] + t * bl[li + 1][y][x];
                        double m = (1 - al) * d;
                        double e = (m - real[y][x]) * 255; se += e * e; n++;
                    }
                }
                double r = Math.sqrt(se / n);
                if (r < best) { best = r; bestS = String.format("sigmaTop=%.0f barH=%.0f aTop=%.2f aBottom=%.2f rmse %.3f", sTop, barH, aTop, aBot, r); }
              }
        System.out.println("best: " + bestS);
    }
}

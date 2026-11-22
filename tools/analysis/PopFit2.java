import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Offline fit for window level popups: blur(sigma, mirror at panel bounds) -> mix (src over) -> overlay.
 * Pixels brighter than a threshold in the real image (text, icons) are excluded (dilated).
 * args: A.png B.png left top right bottom [thr margin sat]
 */
public class PopFit2 {
    static double[][][] load(BufferedImage im, int x0, int y0, int w, int h) {
        double[][][] o = new double[3][h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = im.getRGB(x0 + x, y0 + y);
            o[0][y][x] = ((p >> 16) & 255) / 255.0; o[1][y][x] = ((p >> 8) & 255) / 255.0; o[2][y][x] = (p & 255) / 255.0;
        }
        return o;
    }
    static int refl(int i, int n) { if (i < 0) i = -i - 1; if (i >= n) i = 2 * n - i - 1; return Math.max(0, Math.min(n - 1, i)); }
    static double[][] blur(double[][] src, double sigma) {
        int h = src.length, w = src[0].length;
        if (sigma < 0.05) return src;
        int r = (int) Math.ceil(sigma * 3);
        double[] k = new double[2 * r + 1]; double s = 0;
        for (int i = -r; i <= r; i++) { k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma)); s += k[i + r]; }
        for (int i = 0; i < k.length; i++) k[i] /= s;
        double[][] t = new double[h][w], o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * src[y][refl(x + i, w)]; t[y][x] = a; }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * t[refl(y + i, h)][x]; o[y][x] = a; }
        return o;
    }
    static double overlay(double cb, double cs) { return cb <= 0.5 ? 2 * cb * cs : 1 - 2 * (1 - cb) * (1 - cs); }

    public static void main(String[] a) throws Exception {
        BufferedImage A = ImageIO.read(new File(a[0])), B = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), Bt = Integer.parseInt(a[5]);
        double thr = a.length > 6 ? Double.parseDouble(a[6]) / 255.0 : 70 / 255.0;
        int margin = a.length > 7 ? Integer.parseInt(a[7]) : 10;
        int w = R - L, h = Bt - T;
        double[][][] src = load(A, L, T, w, h), real = load(B, L, T, w, h);
        boolean[][] ex = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double m = Math.max(real[0][y][x], Math.max(real[1][y][x], real[2][y][x]));
            if (m > thr) for (int j = -margin; j <= margin; j++) for (int i = -margin; i <= margin; i++) {
                int yy = y + j, xx = x + i; if (yy >= 0 && yy < h && xx >= 0 && xx < w) ex[yy][xx] = true;
            }
        }
        // signal mask: where the blurred backdrop differs from its local mean (ignore flat areas)
        double[][] lum = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) lum[y][x] = (src[0][y][x] + src[1][y][x] + src[2][y][x]) / 3;
        double[][] ref = blur(lum, 30), ref2 = blur(lum, 90);
        boolean[][] sig = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) sig[y][x] = Math.abs(ref[y][x] - ref2[y][x]) > 1.0 / 255 * 3;
        double mixA = 0.600098, mixC = 0.149048;
        for (double sg : new double[]{10, 15, 20, 25, 30, 35, 40, 50, 60, 80}) {
            double se = 0, sb = 0, se2 = 0; int n = 0, n2 = 0;
            double[][][] bl = new double[3][][];
            for (int c = 0; c < 3; c++) bl[c] = blur(src[c], sg);
            for (int y = margin + 14; y < h - margin - 14; y++) for (int x = margin + 14; x < w - margin - 14; x++) {
                if (ex[y][x]) continue;
                for (int c = 0; c < 3; c++) {
                    double d = bl[c][y][x];
                    double d1 = d * (1 - mixA) + mixC * mixA;
                    double o = d1 * (1 - mixA) + overlay(d1, mixC) * mixA;
                    double e = (o - real[c][y][x]) * 255;
                    se += e * e; sb += e; n++;
                    if (sig[y][x]) { se2 += e * e; n2++; }
                }
            }
            System.out.printf("sigma %4.0f  rmse(all) %.3f  bias %+.3f   rmse(signal px) %.3f n=%d/%d%n", sg, Math.sqrt(se / n), sb / n, n2 > 0 ? Math.sqrt(se2 / n2) : -1, n, n2);
        }
    }
}

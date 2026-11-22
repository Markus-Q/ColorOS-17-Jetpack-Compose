import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Offline fit of a "window level" material: blur(sigma, mirror at the panel bounds) -> mix colour (src over)
 * -> blend colour (overlay), compared against the real popup screenshot.
 * args: A.png B.png left top right bottom [mixARGB blendARGB]
 */
public class PopFit {
    static double[][][] load(BufferedImage im, int x0, int y0, int w, int h) {
        double[][][] o = new double[3][h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = im.getRGB(x0 + x, y0 + y);
            o[0][y][x] = ((p >> 16) & 255) / 255.0;
            o[1][y][x] = ((p >> 8) & 255) / 255.0;
            o[2][y][x] = (p & 255) / 255.0;
        }
        return o;
    }

    static int refl(int i, int n) {
        if (i < 0) i = -i - 1;
        if (i >= n) i = 2 * n - i - 1;
        return Math.max(0, Math.min(n - 1, i));
    }

    static double[] kernel(double sigma) {
        int r = (int) Math.ceil(sigma * 3.0);
        double[] k = new double[2 * r + 1];
        double s = 0;
        for (int i = -r; i <= r; i++) { k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma)); s += k[i + r]; }
        for (int i = 0; i < k.length; i++) k[i] /= s;
        return k;
    }

    static double[][] blur(double[][] src, double sigma) {
        int h = src.length, w = src[0].length;
        if (sigma < 0.01) return src;
        double[] k = kernel(sigma);
        int r = k.length / 2;
        double[][] t = new double[h][w], o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double a = 0;
            for (int i = -r; i <= r; i++) a += k[i + r] * src[y][refl(x + i, w)];
            t[y][x] = a;
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double a = 0;
            for (int i = -r; i <= r; i++) a += k[i + r] * t[refl(y + i, h)][x];
            o[y][x] = a;
        }
        return o;
    }

    static double overlay(double cb, double cs) {
        return cb <= 0.5 ? 2 * cb * cs : 1 - 2 * (1 - cb) * (1 - cs);
    }

    static double[] col(int argb) {
        return new double[]{((argb >> 16) & 255) / 255.0, ((argb >> 8) & 255) / 255.0, (argb & 255) / 255.0, ((argb >>> 24) & 255) / 255.0};
    }

    public static void main(String[] a) throws Exception {
        BufferedImage A = ImageIO.read(new File(a[0])), B = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), Bt = Integer.parseInt(a[5]);
        int mixArgb = a.length > 6 ? (int) Long.parseLong(a[6], 16) : 0x99262626;
        int blendArgb = a.length > 7 ? (int) Long.parseLong(a[7], 16) : 0x99262626;
        int w = R - L, h = Bt - T;
        double[][][] src = load(A, L, T, w, h), real = load(B, L, T, w, h);
        double[] mix = col(mixArgb), bl = col(blendArgb);
        double[] sigmas = {10, 15, 20, 25, 30, 35, 40, 50, 60, 80};
        boolean[][] mask = new boolean[h][w];
        { double[][] b0 = blur(src[0], 30); double base = 0.0 * (1 - mix[3]) + mix[0] * mix[3]; base = base * (1 - bl[3]) + overlay(base, bl[0]) * bl[3];
          for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double d1 = b0[y][x] * (1 - mix[3]) + mix[0] * mix[3]; double o = d1 * (1 - bl[3]) + overlay(d1, bl[0]) * bl[3]; mask[y][x] = Math.abs(o - base) > 2.0 / 255.0; } }
        for (double sg : sigmas) {
            double[][][] out = new double[3][][];
            for (int c = 0; c < 3; c++) {
                double[][] b = blur(src[c], sg);
                out[c] = new double[h][w];
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    double d = b[y][x];
                    double d1 = d * (1 - mix[3]) + mix[c] * mix[3];
                    out[c][y][x] = d1 * (1 - bl[3]) + overlay(d1, bl[c]) * bl[3];
                }
            }
            double se = 0, sb = 0; int n = 0;
            for (int y = 40; y < h - 40; y++) for (int x = 110; x < w - 40; x++) {
                int gx = L + x, gy = T + y;
                boolean text = !mask[y][x] || (gx > 575 && gx < 790 && ((gy > 335 && gy < 405) || (gy > 450 && gy < 530)));
                if (text) continue;
                for (int c = 0; c < 3; c++) {
                    double e = (out[c][y][x] - real[c][y][x]) * 255.0;
                    se += e * e; sb += e; n++;
                }
            }
            System.out.printf("sigma %5.0f  rmse %.3f  bias %+.3f  n=%d%n", sg, Math.sqrt(se / n), sb / n, n);
        }
    }
}

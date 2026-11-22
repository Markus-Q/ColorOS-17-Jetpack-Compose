import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Fits the blur kernel of a window level popup as a mixture of two Gaussians (gray backdrops only):
 * out = material( w*G(s1) + (1-w)*G(s2) ), material = mix 0.149@0.6 (src over, gray) then overlay 0.149@0.6.
 * Compares with the real screenshot inside the popup, away from the edge, text excluded.
 * args: A.png B.png L T R B  (popup rect)   edgeSkip thr margin
 */
public class KernelFit {
    static double[][] load(BufferedImage im, int x0, int y0, int w, int h) {
        double[][] o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = im.getRGB(x0 + x, y0 + y);
            o[y][x] = (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / (3 * 255.0);
        }
        return o;
    }
    static int refl(int i, int n) { if (i < 0) i = -i - 1; if (i >= n) i = 2 * n - i - 1; return Math.max(0, Math.min(n - 1, i)); }
    static double[][] blur(double[][] src, double sigma) {
        int h = src.length, w = src[0].length;
        int r = (int) Math.ceil(sigma * 3);
        double[] k = new double[2 * r + 1]; double s = 0;
        for (int i = -r; i <= r; i++) { k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma)); s += k[i + r]; }
        for (int i = 0; i < k.length; i++) k[i] /= s;
        double[][] t = new double[h][w], o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * src[y][refl(x + i, w)]; t[y][x] = a; }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * t[refl(y + i, h)][x]; o[y][x] = a; }
        return o;
    }
    static double mat(double d) {
        double d1 = d * 0.4 + 0.149 * 0.6;
        double ov = d1 < 0.5 ? 2 * 0.149 * d1 : 1 - 2 * (1 - 0.149) * (1 - d1);
        return d1 * 0.4 + ov * 0.6;
    }
    public static void main(String[] a) throws Exception {
        BufferedImage A = ImageIO.read(new File(a[0])), B = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), Bt = Integer.parseInt(a[5]);
        int skip = Integer.parseInt(a[6]); double thr = Double.parseDouble(a[7]) / 255.0; int margin = Integer.parseInt(a[8]);
        int pad = 160;
        int X0 = Math.max(0, L - pad), Y0 = Math.max(0, T - pad), X1 = Math.min(A.getWidth(), R + pad), Y1 = Math.min(A.getHeight(), Bt + pad);
        double[][] src = load(A, X0, Y0, X1 - X0, Y1 - Y0);
        int w = R - L, h = Bt - T;
        double[][] real = load(B, L, T, w, h);
        boolean[][] ex = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (real[y][x] > thr)
            for (int j = -margin; j <= margin; j++) for (int i = -margin; i <= margin; i++) { int yy = y + j, xx = x + i; if (yy >= 0 && yy < h && xx >= 0 && xx < w) ex[yy][xx] = true; }
        double[] s1s = {6, 10, 14, 18, 22, 26}, s2s = {30, 36, 44, 54, 66, 80};
        double[] ws = {0, 0.2, 0.35, 0.5, 0.65, 0.8, 1.0};
        double best = 1e9; String bestS = "";
        double[][][] b1 = new double[s1s.length][][], b2 = new double[s2s.length][][];
        for (int i = 0; i < s1s.length; i++) b1[i] = blur(src, s1s[i]);
        for (int i = 0; i < s2s.length; i++) b2[i] = blur(src, s2s[i]);
        // single gaussians first
        System.out.println("single gaussian:");
        for (double sg : new double[]{20, 25, 30, 34, 38, 42, 50}) {
            double[][] bl = blur(src, sg); double se = 0; long n = 0;
            for (int y = skip; y < h - skip; y++) for (int x = skip; x < w - skip; x++) if (!ex[y][x]) { double m = mat(bl[y + (T - Y0)][x + (L - X0)]); double e = (m - real[y][x]) * 255; se += e * e; n++; }
            System.out.printf("  sigma %4.0f  rmse %.3f%n", sg, Math.sqrt(se / n));
        }
        System.out.println("mixtures (s1, s2, w for s1):");
        for (int i = 0; i < s1s.length; i++) for (int j = 0; j < s2s.length; j++) for (double wt : ws) {
            double se = 0; long n = 0;
            for (int y = skip; y < h - skip; y++) for (int x = skip; x < w - skip; x++) if (!ex[y][x]) {
                double d = wt * b1[i][y + (T - Y0)][x + (L - X0)] + (1 - wt) * b2[j][y + (T - Y0)][x + (L - X0)];
                double e = (mat(d) - real[y][x]) * 255; se += e * e; n++;
            }
            double r = Math.sqrt(se / n);
            if (r < best) { best = r; bestS = String.format("s1=%.0f s2=%.0f w1=%.2f  rmse %.3f", s1s[i], s2s[j], wt, r); }
        }
        System.out.println("best mixture: " + bestS);
    }
}

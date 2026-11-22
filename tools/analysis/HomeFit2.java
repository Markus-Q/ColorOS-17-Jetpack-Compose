import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Tests the exact OPlus shader chain on the launcher popup: glowMix(luminosity) -> overlay, against B. */
public class HomeFit2 {
    static final int F = 4;
    static double[][][] load(BufferedImage im) {
        int w = im.getWidth() / F, h = im.getHeight() / F;
        double[][][] o = new double[3][h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double r = 0, g = 0, b = 0;
            for (int j = 0; j < F; j++) for (int i = 0; i < F; i++) { int p = im.getRGB(x * F + i, y * F + j); r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; }
            o[0][y][x] = r / (F * F * 255.0); o[1][y][x] = g / (F * F * 255.0); o[2][y][x] = b / (F * F * 255.0);
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
    static double[] affine(double[] x, double[] y, int n) {
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = 0; i < n; i++) { sx += x[i]; sy += y[i]; sxx += x[i] * x[i]; sxy += x[i] * y[i]; }
        double g = (n * sxy - sx * sy) / (n * sxx - sx * sx), o = (sy - g * sx) / n;
        return new double[]{g, o};
    }
    static double overlay(double s, double d) { return d < 0.5 ? 2 * s * d : 1 - 2 * (1 - s) * (1 - d); }
    /** exact port of the SkSL luminosity() in librenderengineextimpl */
    static double[] luminosity(double[] s, double[] d) {
        double dl = 0.3 * d[0] + 0.59 * d[1] + 0.11 * d[2], sl = 0.3 * s[0] + 0.59 * s[1] + 0.11 * s[2];
        double lum = sl - dl;
        double[] c = {d[0] + lum, d[1] + lum, d[2] + lum};
        double mn = Math.min(Math.min(c[0], c[1]), c[2]), mx = Math.max(Math.max(c[0], c[1]), c[2]);
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) {
            if (mn < 0) r[i] = sl + ((c[i] - sl) * sl) / (sl - mn);
            else if (mx > 1) r[i] = sl + ((c[i] - sl) * (1 - sl)) / (mx - sl);
            else r[i] = c[i];
        }
        return r;
    }
    public static void main(String[] a) throws Exception {
        double[][][] A = load(ImageIO.read(new File(a[0]))), B = load(ImageIO.read(new File(a[1])));
        int L = Integer.parseInt(a[2]) / F, T = Integer.parseInt(a[3]) / F, R = Integer.parseInt(a[4]) / F, Bt = Integer.parseInt(a[5]) / F;
        int h = A[0].length, w = A[0][0].length;
        boolean[][] outMask = new boolean[h][w], inMask = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int gx = x * F, gy = y * F;
            boolean nearPopup = x >= L - 10 && x < R + 10 && y >= T - 10 && y < Bt + 10;
            boolean chrome = gx > 560 && gx < 780 && gy > 1400 && gy < 1620;
            outMask[y][x] = !nearPopup && !chrome && gy >= 140;
            inMask[y][x] = x >= L + 8 && x < R - 8 && y >= T + 8 && y < Bt - 8 && Math.max(B[0][y][x], Math.max(B[1][y][x], B[2][y][x])) < 0.30;
        }
        double[][][] S = new double[3][][];
        for (int c = 0; c < 3; c++) {
            double[][] b = blur(A[c], 10);   // workspace blur 40 px
            int n = 0; double[] xs = new double[w * h], ys = new double[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (outMask[y][x]) { xs[n] = b[y][x]; ys[n] = B[c][y][x]; n++; }
            double[] r = affine(xs, ys, n);
            S[c] = new double[h][w];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) S[c][y][x] = r[0] * b[y][x] + r[1];
        }
        double[] mix = {0.149048, 0.149048, 0.149048}; double ma = 0.600098;
        String[] names = {"plain mix + overlay", "glow(luminosity) mix + overlay", "luminosity only", "plain mix only"};
        for (double sp : new double[]{4, 6, 7, 8, 10}) {
            double[][][] Sb = new double[3][][];
            for (int c = 0; c < 3; c++) Sb[c] = blur(S[c], sp);
            double[] se = new double[4]; int n = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (inMask[y][x]) {
                double[] d = {Sb[0][y][x], Sb[1][y][x], Sb[2][y][x]};
                double[] lm = luminosity(mix, d);
                for (int v = 0; v < 4; v++) {
                    double[] o = new double[3];
                    for (int c = 0; c < 3; c++) {
                        double m1 = (v == 1 || v == 2) ? d[c] * (1 - ma) + lm[c] * ma : d[c] * (1 - ma) + mix[c] * ma;
                        double m2 = (v >= 2) ? m1 : m1 * (1 - ma) + overlay(mix[c], m1) * ma;
                        o[c] = m2;
                        double e = (m2 - B[c][y][x]) * 255; se[v] += e * e;
                    }
                }
                n += 3;
            }
            StringBuilder sb = new StringBuilder(String.format("sigmaP %3.0f px:", sp * F));
            for (int v = 0; v < 4; v++) sb.append(String.format("  [%s] %.2f", names[v], Math.sqrt(se[v] / n)));
            System.out.println(sb);
        }
    }
}

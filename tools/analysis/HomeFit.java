import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Two stage fit of the launcher long press scene (all at 1/4 resolution):
 *  1. workspace blur: B outside the popup ~ affine(blur(A, sigmaWs))
 *  2. popup material: B inside the popup ~ M(blur(S, sigmaP)),  S = affine(blur(A, sigmaWs))
 * args: A.png B.png left top right bottom
 */
public class HomeFit {
    static final int F = 4;

    static double[][][] load(BufferedImage im) {
        int w = im.getWidth() / F, h = im.getHeight() / F;
        double[][][] o = new double[3][h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double r = 0, g = 0, b = 0;
            for (int j = 0; j < F; j++) for (int i = 0; i < F; i++) {
                int p = im.getRGB(x * F + i, y * F + j);
                r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255;
            }
            o[0][y][x] = r / (F * F * 255.0); o[1][y][x] = g / (F * F * 255.0); o[2][y][x] = b / (F * F * 255.0);
        }
        return o;
    }

    static int refl(int i, int n) {
        if (i < 0) i = -i - 1;
        if (i >= n) i = 2 * n - i - 1;
        return Math.max(0, Math.min(n - 1, i));
    }

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

    /** least squares y = g*x + o, returns {g, o, rmse} */
    static double[] affine(double[] x, double[] y, int n) {
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = 0; i < n; i++) { sx += x[i]; sy += y[i]; sxx += x[i] * x[i]; sxy += x[i] * y[i]; }
        double g = (n * sxy - sx * sy) / (n * sxx - sx * sx), o = (sy - g * sx) / n, se = 0;
        for (int i = 0; i < n; i++) { double e = g * x[i] + o - y[i]; se += e * e; }
        return new double[]{g, o, Math.sqrt(se / n)};
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
            boolean topArea = gy < 140;                // status bar
            boolean inPopup = x >= L + 8 && x < R - 8 && y >= T + 8 && y < Bt - 8;
            outMask[y][x] = !nearPopup && !chrome && !topArea;
            inMask[y][x] = inPopup && Math.max(B[0][y][x], Math.max(B[1][y][x], B[2][y][x])) < 0.30;
        }
        System.out.println("--- stage 1: workspace blur (outside popup)");
        double bestS = 0, bestE = 1e9;
        for (double sg : new double[]{3, 5, 7, 8, 9, 10, 12, 14, 16, 20, 25}) {   // sigma in 1/4 px
            double tot = 0; double[][] fit = new double[3][];
            for (int c = 0; c < 3; c++) {
                double[][] b = blur(A[c], sg);
                int n = 0; double[] xs = new double[w * h], ys = new double[w * h];
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (outMask[y][x]) { xs[n] = b[y][x]; ys[n] = B[c][y][x]; n++; }
                double[] r = affine(xs, ys, n); fit[c] = r; tot += r[2] * r[2];
            }
            double e = Math.sqrt(tot / 3) * 255;
            System.out.printf("sigmaWs %5.0f px (full res)  rmse %.2f  gain/off R %.3f/%.3f G %.3f/%.3f B %.3f/%.3f%n", sg * F, e,
                fit[0][0], fit[0][1] * 255, fit[1][0], fit[1][1] * 255, fit[2][0], fit[2][1] * 255);
            if (e < bestE) { bestE = e; bestS = sg; }
        }
        System.out.println("best sigmaWs (full res px) = " + bestS * F);
        // stage 2 with the best workspace blur
        double[][][] S = new double[3][][];
        for (int c = 0; c < 3; c++) {
            double[][] b = blur(A[c], bestS);
            int n = 0; double[] xs = new double[w * h], ys = new double[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (outMask[y][x]) { xs[n] = b[y][x]; ys[n] = B[c][y][x]; n++; }
            double[] r = affine(xs, ys, n);
            S[c] = new double[h][w];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) S[c][y][x] = r[0] * b[y][x] + r[1];
        }
        System.out.println("--- stage 2: popup material, hypotheses (mix src-over c1, then overlay c2)");
        double[] mixA = {0.99, 0.0, 0};
        for (double sp : new double[]{0, 4, 6, 8, 10, 14}) {   // sigma in 1/4 px
            double[][][] Sb = new double[3][][];
            for (int c = 0; c < 3; c++) Sb[c] = blur(S[c], sp);
            // general 3x3 + offset linear regression on inside pixels
            int n = 0; for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (inMask[y][x]) n++;
            double[][] X = new double[n][4]; double[][] Y = new double[n][3]; int k = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (inMask[y][x]) {
                X[k][0] = Sb[0][y][x]; X[k][1] = Sb[1][y][x]; X[k][2] = Sb[2][y][x]; X[k][3] = 1;
                for (int c = 0; c < 3; c++) Y[k][c] = B[c][y][x];
                k++;
            }
            double[][] coef = new double[3][];
            double tot = 0;
            for (int c = 0; c < 3; c++) { coef[c] = solve(X, Y, c, n); }
            for (int i = 0; i < n; i++) for (int c = 0; c < 3; c++) {
                double p = coef[c][0] * X[i][0] + coef[c][1] * X[i][1] + coef[c][2] * X[i][2] + coef[c][3];
                tot += (p - Y[i][c]) * (p - Y[i][c]);
            }
            System.out.printf("sigmaP %4.0f px: linear3x3 rmse %.2f  | R=%.2f r%+.2f g%+.2f b%+.2f ... G=%.2f r%+.2f g%+.2f b%+.2f ... B=%.2f r%+.2f g%+.2f b%+.2f%n",
                sp * F, Math.sqrt(tot / (3.0 * n)) * 255,
                coef[0][3] * 255, coef[0][0], coef[0][1], coef[0][2],
                coef[1][3] * 255, coef[1][0], coef[1][1], coef[1][2],
                coef[2][3] * 255, coef[2][0], coef[2][1], coef[2][2]);
        }
        System.out.println("--- stage 3: explicit model  saturate(s) -> mix(src over) -> overlay, popup colours #99262626");
        for (double sp : new double[]{4, 6, 8, 10}) {
            double[][][] Sb = new double[3][][];
            for (int c = 0; c < 3; c++) Sb[c] = blur(S[c], sp);
            StringBuilder sbd = new StringBuilder(String.format("sigmaP %3.0f px:", sp * F));
            for (double sat : new double[]{1.0, 1.3, 1.6, 2.0, 2.5, 3.0, 4.0}) {
                double se = 0; int n = 0;
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (inMask[y][x]) {
                    double r = Sb[0][y][x], g = Sb[1][y][x], b = Sb[2][y][x];
                    double lum = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                    double[] d = {lum + sat * (r - lum), lum + sat * (g - lum), lum + sat * (b - lum)};
                    for (int c = 0; c < 3; c++) {
                        double dd = Math.max(0, Math.min(1, d[c]));
                        double d1 = dd * 0.4 + 0.149 * 0.6;
                        double ov = d1 <= 0.5 ? 2 * d1 * 0.149 : 1 - 2 * (1 - d1) * (1 - 0.149);
                        double o = d1 * 0.4 + ov * 0.6;
                        double e = (o - B[c][y][x]) * 255; se += e * e; n++;
                    }
                }
                sbd.append(String.format("  s=%.1f:%.2f", sat, Math.sqrt(se / n)));
            }
            System.out.println(sbd);
        }
    }

    /** normal equations for one output channel, 4 unknowns */
    static double[] solve(double[][] X, double[][] Y, int c, int n) {
        double[][] M = new double[4][5];
        for (int i = 0; i < n; i++) for (int p = 0; p < 4; p++) { for (int q = 0; q < 4; q++) M[p][q] += X[i][p] * X[i][q]; M[p][4] += X[i][p] * Y[i][c]; }
        for (int p = 0; p < 4; p++) {
            int piv = p; for (int r = p + 1; r < 4; r++) if (Math.abs(M[r][p]) > Math.abs(M[piv][p])) piv = r;
            double[] t = M[p]; M[p] = M[piv]; M[piv] = t;
            for (int r = 0; r < 4; r++) if (r != p) { double f = M[r][p] / M[p][p]; for (int q = p; q < 5; q++) M[r][q] -= f * M[p][q]; }
        }
        double[] s = new double[4]; for (int p = 0; p < 4; p++) s[p] = M[p][4] / M[p][p];
        return s;
    }
}

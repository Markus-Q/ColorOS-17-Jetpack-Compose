import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Compares edge treatments of a window level blur: (a) mirror at the panel rect, (b) real content outside the rect.
 * Material: glow(luminosity) mix -> overlay (popup) or plain mix -> overlay (sheet) selectable.
 * args: A.png B.png rectL rectT rectR rectB evalL evalT evalR evalB sigma mixGray mixA blendGray blendA glow(0/1) [thr margin]
 */
public class MatFit2 {
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
        int r = (int) Math.ceil(sigma * 3);
        double[] k = new double[2 * r + 1]; double s = 0;
        for (int i = -r; i <= r; i++) { k[i + r] = Math.exp(-(i * i) / (2 * sigma * sigma)); s += k[i + r]; }
        for (int i = 0; i < k.length; i++) k[i] /= s;
        double[][] t = new double[h][w], o = new double[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * src[y][refl(x + i, w)]; t[y][x] = a; }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { double a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * t[refl(y + i, h)][x]; o[y][x] = a; }
        return o;
    }
    static double overlay(double s, double d) { return d < 0.5 ? 2 * s * d : 1 - 2 * (1 - s) * (1 - d); }
    static double[] luminosity(double s, double[] d) {
        double dl = 0.3 * d[0] + 0.59 * d[1] + 0.11 * d[2];
        double lum = s - dl; double[] c = {d[0] + lum, d[1] + lum, d[2] + lum};
        double mn = Math.min(Math.min(c[0], c[1]), c[2]), mx = Math.max(Math.max(c[0], c[1]), c[2]);
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) r[i] = mn < 0 ? s + ((c[i] - s) * s) / (s - mn) : mx > 1 ? s + ((c[i] - s) * (1 - s)) / (mx - s) : c[i];
        return r;
    }
    public static void main(String[] a) throws Exception {
        BufferedImage A = ImageIO.read(new File(a[0])), B = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), Bt = Integer.parseInt(a[5]);
        int eL = Integer.parseInt(a[6]), eT = Integer.parseInt(a[7]), eR = Integer.parseInt(a[8]), eB = Integer.parseInt(a[9]);
        double sg = Double.parseDouble(a[10]), mixC = Double.parseDouble(a[11]), mixA = Double.parseDouble(a[12]), blC = Double.parseDouble(a[13]), blA = Double.parseDouble(a[14]);
        boolean glow = Integer.parseInt(a[15]) != 0;
        double thr = a.length > 16 ? Double.parseDouble(a[16]) / 255.0 : 90 / 255.0;
        int margin = a.length > 17 ? Integer.parseInt(a[17]) : 10;
        int iw = A.getWidth(), ih = A.getHeight();
        Bt = Math.min(Bt, ih);
        int pad = (int) Math.ceil(sg * 3) + 4;
        int X0 = Math.max(0, L - pad), Y0 = Math.max(0, T - pad), X1 = Math.min(iw, R + pad), Y1 = Math.min(ih, Bt + pad);
        double[][][] big = load(A, X0, Y0, X1 - X0, Y1 - Y0);
        double[][][] inner = load(A, L, T, R - L, Bt - T);
        double[][][] real = load(B, L, T, R - L, Bt - T);
        int w = R - L, h = Bt - T;
        double[][][] ext = new double[3][][], mir = new double[3][][];
        for (int c = 0; c < 3; c++) { ext[c] = blur(big[c], sg); mir[c] = blur(inner[c], sg); }
        boolean[][] ex = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double m = Math.max(real[0][y][x], Math.max(real[1][y][x], real[2][y][x]));
            if (m > thr) for (int j = -margin; j <= margin; j++) for (int i = -margin; i <= margin; i++) { int yy = y + j, xx = x + i; if (yy >= 0 && yy < h && xx >= 0 && xx < w) ex[yy][xx] = true; }
        }
        // rows/cols near the rect edge: compare in bands of distance from the edge (px)
        int[] bands = {0, 20, 40, 80, 160, 100000};
        double[][] se = new double[2][bands.length - 1]; int[][] cnt = new int[2][bands.length - 1];
        for (int y = Math.max(eT - T, 0); y < Math.min(eB - T, h); y++) for (int x = Math.max(eL - L, 0); x < Math.min(eR - L, w); x++) {
            if (ex[y][x]) continue;
            int dist = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y));
            // if rect side is outside the image (clipped) treat as far
            int bi = 0; while (dist >= bands[bi + 1]) bi++;
            for (int v = 0; v < 2; v++) {
                double[][][] bl = v == 0 ? mir : ext;
                double[] d = new double[3]; for (int c = 0; c < 3; c++) d[c] = v == 0 ? bl[c][y][x] : bl[c][y + (T - Y0)][x + (L - X0)];
                double[] lm = glow ? luminosity(mixC, d) : null;
                for (int c = 0; c < 3; c++) {
                    double m1 = glow ? d[c] * (1 - mixA) + lm[c] * mixA : d[c] * (1 - mixA) + mixC * mixA;
                    double o = m1 * (1 - blA) + overlay(blC, m1) * blA;
                    double e = (o - real[c][y][x]) * 255; se[v][bi] += e * e; cnt[v][bi]++;
                }
            }
        }
        System.out.println("distance-from-edge band (px):   mirror-at-rect rmse   |   real-content-outside rmse   (n)");
        for (int b = 0; b < bands.length - 1; b++)
            System.out.printf("  %5d - %-6d : %8.3f   |   %8.3f    (%d)%n", bands[b], bands[b + 1], cnt[0][b] > 0 ? Math.sqrt(se[0][b] / cnt[0][b]) : -1, cnt[1][b] > 0 ? Math.sqrt(se[1][b] / cnt[1][b]) : -1, cnt[0][b]);
    }
}

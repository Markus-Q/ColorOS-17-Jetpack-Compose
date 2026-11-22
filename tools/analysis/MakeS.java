import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Builds the model of the launcher workspace under the popup: S = affine(blur(A, sigma)) fitted on the outside of the popup in B. args: A B L T R B sigma out.png */
public class MakeS {
    static int refl(int i, int n) { if (i < 0) i = -i - 1; if (i >= n) i = 2 * n - i - 1; return Math.max(0, Math.min(n - 1, i)); }
    static float[][] blur(float[][] src, double sigma) {
        int h = src.length, w = src[0].length, r = (int) Math.ceil(sigma * 3);
        float[] k = new float[2 * r + 1]; double s = 0;
        for (int i = -r; i <= r; i++) { k[i + r] = (float) Math.exp(-(i * i) / (2 * sigma * sigma)); s += k[i + r]; }
        for (int i = 0; i < k.length; i++) k[i] /= s;
        float[][] t = new float[h][w], o = new float[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { float a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * src[y][refl(x + i, w)]; t[y][x] = a; }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { float a = 0; for (int i = -r; i <= r; i++) a += k[i + r] * t[refl(y + i, h)][x]; o[y][x] = a; }
        return o;
    }
    public static void main(String[] a) throws Exception {
        BufferedImage A = ImageIO.read(new File(a[0])), B = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), Bt = Integer.parseInt(a[5]);
        double sigma = Double.parseDouble(a[6]);
        int w = A.getWidth(), h = A.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        float[][][] bl = new float[3][][];
        for (int c = 0; c < 3; c++) {
            float[][] ch = new float[h][w];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) ch[y][x] = ((A.getRGB(x, y) >> (16 - 8 * c)) & 255) / 255f;
            bl[c] = blur(ch, sigma);
        }
        double[] gain = new double[3], off = new double[3];
        for (int c = 0; c < 3; c++) {
            double sx = 0, sy = 0, sxx = 0, sxy = 0; long n = 0;
            for (int y = 140; y < h; y += 2) for (int x = 0; x < w; x += 2) {
                boolean near = x >= L - 40 && x < R + 40 && y >= T - 40 && y < Bt + 40;
                boolean chrome = x > 560 && x < 780 && y > 1400 && y < 1620;
                if (near || chrome) continue;
                double bx = bl[c][y][x], by = ((B.getRGB(x, y) >> (16 - 8 * c)) & 255) / 255.0;
                sx += bx; sy += by; sxx += bx * bx; sxy += bx * by; n++;
            }
            gain[c] = (n * sxy - sx * sy) / (n * sxx - sx * sx); off[c] = (sy - gain[c] * sx) / n;
            System.out.printf("channel %d gain %.3f offset %.2f%n", c, gain[c], off[c] * 255);
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int rgb = 0;
            for (int c = 0; c < 3; c++) { int v = (int) Math.round(Math.max(0, Math.min(1, gain[c] * bl[c][y][x] + off[c])) * 255); rgb |= v << (16 - 8 * c); }
            out.setRGB(x, y, rgb);
        }
        ImageIO.write(out, "png", new File(a[7]));
    }
}

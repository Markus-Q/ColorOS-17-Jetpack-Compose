import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Compares a lab screenshot with a real screenshot inside a rectangle.
 * Pixels brighter than thr in the REAL image (text, icons) are excluded together with a dilation margin.
 * Prints rmse / signed bias (lab - real) for all pixels and by distance from the rectangle edge.
 * args: lab.png real.png L T R B [thr margin]
 */
public class RegionCmp {
    public static void main(String[] a) throws Exception {
        BufferedImage lab = ImageIO.read(new File(a[0])), real = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), B = Integer.parseInt(a[5]);
        double thr = a.length > 6 ? Double.parseDouble(a[6]) : 90;
        int margin = a.length > 7 ? Integer.parseInt(a[7]) : 8;
        B = Math.min(B, real.getHeight());
        int w = R - L, h = B - T;
        boolean[][] ex = new boolean[h][w];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int p = real.getRGB(L + x, T + y);
            int m = Math.max(Math.max((p >> 16) & 255, (p >> 8) & 255), p & 255);
            if (m > thr) for (int j = -margin; j <= margin; j++) for (int i = -margin; i <= margin; i++) {
                int yy = y + j, xx = x + i; if (yy >= 0 && yy < h && xx >= 0 && xx < w) ex[yy][xx] = true;
            }
        }
        int[] bands = {0, 6, 12, 24, 48, 96, 100000};
        double[] se = new double[bands.length - 1], sb = new double[bands.length - 1]; long[] n = new long[bands.length - 1];
        double[] chBias = new double[3]; long tot = 0; double seAll = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (ex[y][x]) continue;
            int dist = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y));
            int bi = 0; while (dist >= bands[bi + 1]) bi++;
            int p = lab.getRGB(L + x, T + y), q = real.getRGB(L + x, T + y);
            for (int c = 0; c < 3; c++) {
                int s = 16 - 8 * c;
                double e = ((p >> s) & 255) - ((q >> s) & 255);
                se[bi] += e * e; sb[bi] += e; n[bi]++;
                chBias[c] += e; seAll += e * e; tot++;
            }
        }
        System.out.printf("ALL  rmse %.3f   bias R %+.3f G %+.3f B %+.3f   (n=%d)%n", Math.sqrt(seAll / tot), chBias[0] / (tot / 3), chBias[1] / (tot / 3), chBias[2] / (tot / 3), tot / 3);
        for (int b = 0; b < bands.length - 1; b++)
            if (n[b] > 0) System.out.printf("  edge distance %3d-%-6d  rmse %.3f  bias %+.3f  (n=%d)%n", bands[b], bands[b + 1], Math.sqrt(se[b] / n[b]), sb[b] / n[b], n[b] / 3);
    }
}

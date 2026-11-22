import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Residual (real - lab) as a function of the distance from each side of a glass rectangle.
 * Averages over the straight part of the side (corner zone excluded); bright text pixels are skipped.
 * args: lab.png real.png L T R B maxd corner thr
 */
public class EdgeProfile {
    static double lum(int p) { return (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3.0; }
    public static void main(String[] a) throws Exception {
        BufferedImage lab = ImageIO.read(new File(a[0])), real = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), B = Integer.parseInt(a[5]);
        int maxd = Integer.parseInt(a[6]), corner = Integer.parseInt(a[7]);
        double thr = a.length > 8 ? Double.parseDouble(a[8]) : 90;
        int H = real.getHeight(); boolean bottomOnScreen = B <= H; B = Math.min(B, H);
        String[] names = {"top", "bottom", "left", "right"};
        double[][] sum = new double[4][maxd]; int[][] cnt = new int[4][maxd];
        for (int d = 0; d < maxd; d++) {
            for (int x = L + corner; x < R - corner; x++) {
                int[][] pts = {{x, T + d}, {x, B - 1 - d}};
                for (int s = 0; s < 2; s++) {
                    if (s == 1 && !bottomOnScreen) continue;
                    int px = pts[s][0], py = pts[s][1];
                    int p = real.getRGB(px, py), q = lab.getRGB(px, py);
                    if (Math.max(Math.max((p >> 16) & 255, (p >> 8) & 255), p & 255) > thr) continue;
                    sum[s][d] += lum(p) - lum(q); cnt[s][d]++;
                }
            }
            for (int y = T + corner; y < B - corner; y++) {
                int[][] pts = {{L + d, y}, {R - 1 - d, y}};
                for (int s = 0; s < 2; s++) {
                    int px = pts[s][0], py = pts[s][1];
                    int p = real.getRGB(px, py), q = lab.getRGB(px, py);
                    if (Math.max(Math.max((p >> 16) & 255, (p >> 8) & 255), p & 255) > thr) continue;
                    sum[2 + s][d] += lum(p) - lum(q); cnt[2 + s][d]++;
                }
            }
        }
        System.out.println("d     top   bottom    left   right   (real - lab, grey levels)");
        for (int d = 0; d < maxd; d++) {
            StringBuilder sb = new StringBuilder(String.format("%3d", d));
            for (int s = 0; s < 4; s++) sb.append(cnt[s][d] > 20 ? String.format("  %6.2f", sum[s][d] / cnt[s][d]) : "     n/a");
            System.out.println(sb);
        }
    }
}

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * How far did the content of horizontal bands move between two screenshots? For every band y0..y1 (columns x0..x1) it
 * tries vertical and horizontal shifts of -maxS..maxS px and prints the one with the smallest mean absolute difference
 * (positive dy = content of B is lower than in A).
 * args: A.png B.png maxS x0 x1 y0:y1 [y0:y1 ...]
 */
public class BandShift {
    static int gray(int p) { return ((p >> 16 & 255) * 3 + (p >> 8 & 255) * 6 + (p & 255)) / 10; }

    public static void main(String[] a) throws Exception {
        BufferedImage A = ImageIO.read(new File(a[0])), B = ImageIO.read(new File(a[1]));
        int maxS = Integer.parseInt(a[2]), x0 = Integer.parseInt(a[3]), x1 = Integer.parseInt(a[4]);
        for (int i = 5; i < a.length; i++) {
            String[] r = a[i].split(":");
            int y0 = Integer.parseInt(r[0]), y1 = Integer.parseInt(r[1]);
            double best = 1e18; int bdx = 0, bdy = 0;
            double zero = 0;
            for (int dy = -maxS; dy <= maxS; dy++) {
                for (int dx = -maxS; dx <= maxS; dx++) {
                    double sum = 0; long n = 0;
                    for (int y = y0 + maxS; y < y1 - maxS; y += 1) {
                        for (int x = x0 + maxS; x < x1 - maxS; x += 2) {
                            int pa = gray(A.getRGB(x, y)), pb = gray(B.getRGB(x + dx, y + dy));
                            sum += Math.abs(pa - pb); n++;
                        }
                    }
                    double m = sum / n;
                    if (dx == 0 && dy == 0) zero = m;
                    if (m < best) { best = m; bdx = dx; bdy = dy; }
                }
            }
            System.out.printf("band %d:%d  best dx=%d dy=%d (mad %.2f), no shift mad %.2f%n", y0, y1, bdx, bdy, best, zero);
        }
    }
}

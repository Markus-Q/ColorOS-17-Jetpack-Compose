import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Prints the RGB values along an axis-aligned line of a PNG.
 * args: file x0 y0 x1 y1 [step]   (either x0==x1 or y0==y1)
 */
public class Prof {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        int x0 = Integer.parseInt(a[1]), y0 = Integer.parseInt(a[2]);
        int x1 = Integer.parseInt(a[3]), y1 = Integer.parseInt(a[4]);
        int step = a.length > 5 ? Integer.parseInt(a[5]) : 1;
        int dx = Integer.signum(x1 - x0), dy = Integer.signum(y1 - y0);
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= n; i += step) {
            int x = x0 + dx * i, y = y0 + dy * i;
            int p = im.getRGB(x, y);
            System.out.printf("%d,%d  %3d %3d %3d%n", x, y, (p >> 16) & 255, (p >> 8) & 255, p & 255);
        }
    }
}

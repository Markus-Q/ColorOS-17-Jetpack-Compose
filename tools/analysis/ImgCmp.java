import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Compares two PNGs: prints RMSE over the whole image and over the lower 3/4 (to skip clock text). args: a b */
public class ImgCmp {
    public static void main(String[] a) throws Exception {
        BufferedImage p = ImageIO.read(new File(a[0])), q = ImageIO.read(new File(a[1]));
        int w = p.getWidth(), h = p.getHeight();
        double se = 0; long n = 0; int maxd = 0;
        for (int y = h / 4; y < h; y++) for (int x = 0; x < w; x++) {
            int u = p.getRGB(x, y), v = q.getRGB(x, y);
            for (int s = 0; s <= 16; s += 8) { int d = ((u >> s) & 255) - ((v >> s) & 255); se += d * d; n++; maxd = Math.max(maxd, Math.abs(d)); }
        }
        System.out.printf("rmse %.3f  max %d  (%dx%d)%n", Math.sqrt(se / n), maxd, w, h);
    }
}

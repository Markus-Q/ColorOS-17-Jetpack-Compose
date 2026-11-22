import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Boundary of a glass panel in the top-left corner zone: for each row the first x (sub-pixel) where the brightness passes a threshold. args: lab real L T zone thr */
public class CornerCmp {
    static double edgeX(BufferedImage im, int y, int x0, int x1, double thr) {
        double prev = lum(im.getRGB(x0, y));
        for (int x = x0 + 1; x < x1; x++) {
            double cur = lum(im.getRGB(x, y));
            if (prev < thr && cur >= thr) return x - 1 + (thr - prev) / (cur - prev);
            prev = cur;
        }
        return Double.NaN;
    }
    static double lum(int p) { return (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3.0; }
    public static void main(String[] a) throws Exception {
        BufferedImage lab = ImageIO.read(new File(a[0])), real = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), zone = Integer.parseInt(a[4]);
        double thr = Double.parseDouble(a[5]);
        System.out.println("row   real_x   lab_x   diff");
        for (int dy = 0; dy < zone; dy += 4) {
            int y = T + dy;
            double xr = edgeX(real, y, L - 6, L + zone + 40, thr), xl = edgeX(lab, y, L - 6, L + zone + 40, thr);
            System.out.printf("%3d  %7.2f %7.2f %6.2f%n", dy, xr - L, xl - L, xl - xr);
        }
    }
}

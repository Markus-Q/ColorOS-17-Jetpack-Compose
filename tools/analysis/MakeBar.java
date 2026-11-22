import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Finds the scroll shift between two screenshots and builds the backdrop of S1: bg(y) = S2(y + shift). args: S1 S2 out [lo hi] */
public class MakeBar {
    static double gray(int p) { return (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3.0; }
    public static void main(String[] a) throws Exception {
        BufferedImage s1 = ImageIO.read(new File(a[0])), s2 = ImageIO.read(new File(a[1]));
        int lo = a.length > 3 ? Integer.parseInt(a[3]) : 380, hi = a.length > 4 ? Integer.parseInt(a[4]) : 560;
        int W = s1.getWidth(), H = s1.getHeight();
        int best = lo; double bestE = 1e18;
        for (int d = lo; d <= hi; d++) {
            double e = 0; long n = 0;
            for (int y = 700; y < 1700; y += 3) for (int x = 20; x < W - 20; x += 6) { e += Math.abs(gray(s1.getRGB(x, y)) - gray(s2.getRGB(x, y + d))); n++; }
            e /= n;
            if (e < bestE) { bestE = e; best = d; }
        }
        System.out.println("shift " + best + " mean abs diff " + bestE);
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) out.setRGB(x, y, (y + best < H ? s2.getRGB(x, y + best) : s1.getRGB(x, y)) & 0xFFFFFF);
        ImageIO.write(out, "png", new File(a[2]));
    }
}

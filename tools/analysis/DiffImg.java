import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Signed difference picture of a region: red where real is brighter than lab, blue where darker, 8x amplified. args: lab real L T R B gain out */
public class DiffImg {
    public static void main(String[] a) throws Exception {
        BufferedImage lab = ImageIO.read(new File(a[0])), real = ImageIO.read(new File(a[1]));
        int L = Integer.parseInt(a[2]), T = Integer.parseInt(a[3]), R = Integer.parseInt(a[4]), B = Math.min(Integer.parseInt(a[5]), real.getHeight());
        double gain = Double.parseDouble(a[6]);
        BufferedImage out = new BufferedImage(R - L, B - T, BufferedImage.TYPE_INT_RGB);
        for (int y = T; y < B; y++) for (int x = L; x < R; x++) {
            int p = lab.getRGB(x, y), q = real.getRGB(x, y);
            double d = 0; for (int s = 0; s <= 16; s += 8) d += ((q >> s) & 255) - ((p >> s) & 255); d /= 3;
            int v = (int) Math.min(255, Math.abs(d) * gain);
            out.setRGB(x - L, y - T, d >= 0 ? (v << 16) | (v / 4 << 8) : (v) | (v / 4 << 8));
        }
        ImageIO.write(out, "png", new File(a[7]));
    }
}

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
/** Prints pixel R,G,B along a row/column. args: png axis(x|y) fixed from to [step] */
public class Row {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        boolean row = a[1].equals("y");
        int fixed = Integer.parseInt(a[2]), s = Integer.parseInt(a[3]), e = Integer.parseInt(a[4]);
        int step = a.length > 5 ? Integer.parseInt(a[5]) : 1;
        StringBuilder sb = new StringBuilder();
        for (int i = s; i <= e; i += step) {
            int rgb = row ? im.getRGB(i, fixed) : im.getRGB(fixed, i);
            sb.append(String.format("%d:%d,%d,%d  ", i, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255));
        }
        System.out.println(sb);
    }
}

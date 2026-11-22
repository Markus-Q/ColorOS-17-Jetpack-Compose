import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Left edge profile of a dark page that is shifted over a lighter one (back swipe preview): for every row y0..y1 scans from
 * xStart to the left and prints the first x whose luminance reaches thr (the page edge is just right of it).
 * args: png y0 y1 xStart thr [step]     output: y:edgeX ...
 */
public class LeftEdge {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        int y0 = Integer.parseInt(a[1]), y1 = Integer.parseInt(a[2]), xs = Integer.parseInt(a[3]), thr = Integer.parseInt(a[4]);
        int step = a.length > 5 ? Integer.parseInt(a[5]) : 6;
        StringBuilder sb = new StringBuilder();
        for (int y = y0; y <= y1; y += step) {
            int x = xs;
            for (; x >= 0; x--) {
                int rgb = im.getRGB(x, y);
                int lum = (((rgb >> 16) & 255) * 299 + ((rgb >> 8) & 255) * 587 + (rgb & 255) * 114) / 1000;
                if (lum >= thr) break;
            }
            sb.append(y).append(':').append(x + 1).append(' ');
        }
        System.out.println(sb);
    }
}

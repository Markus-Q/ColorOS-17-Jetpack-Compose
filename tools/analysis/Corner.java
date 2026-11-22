import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
/** Prints the left-edge profile of a card: first x (from the left) differing from bg for rows top..top+N. args: png topY N bgR bgG bgB thr [step] */
public class Corner {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        int top = Integer.parseInt(a[1]), n = Integer.parseInt(a[2]);
        int br = Integer.parseInt(a[3]), bg = Integer.parseInt(a[4]), bb = Integer.parseInt(a[5]), thr = Integer.parseInt(a[6]);
        int step = a.length > 7 ? Integer.parseInt(a[7]) : 2;
        StringBuilder sb = new StringBuilder();
        for (int y = top; y < top + n; y += step) {
            int x = 0;
            for (; x < 300; x++) {
                int rgb = im.getRGB(x, y);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                if (Math.max(Math.abs(r - br), Math.max(Math.abs(g - bg), Math.abs(b - bb))) >= thr) break;
            }
            sb.append(y - top).append(':').append(x).append(' ');
        }
        System.out.println(sb);
    }
}

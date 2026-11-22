import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Vertical structure of a settings-like page: card extents (rows where x=1020 differs from the page background) and
 * divider rows (rows where nearly all pixels between x0 and x1 have one uniform color that differs from the card).
 * usage: java Struct.java screen.png [y0=250] [y1=2354] [x0=100] [x1=980]
 */
public class Struct {
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        int y0 = a.length > 1 ? Integer.parseInt(a[1]) : 250, y1 = a.length > 2 ? Integer.parseInt(a[2]) : im.getHeight();
        int x0 = a.length > 3 ? Integer.parseInt(a[3]) : 100, x1 = a.length > 4 ? Integer.parseInt(a[4]) : 980;
        int bg = im.getRGB(10, y0 + 5) & 0xFFFFFF;
        StringBuilder cards = new StringBuilder("cards (x=1020 != page bg):");
        int start = -1;
        for (int y = y0; y < y1; y++) {
            boolean in = (im.getRGB(1020, y) & 0xFFFFFF) != bg;
            if (in && start < 0) start = y;
            if (!in && start >= 0) { cards.append(String.format(" [%d..%d]", start, y - 1)); start = -1; }
        }
        if (start >= 0) cards.append(String.format(" [%d..%d]", start, y1 - 1));
        System.out.println(cards);
        StringBuilder div = new StringBuilder("dividers (uniform rows):");
        for (int y = y0; y < y1; y++) {
            Map<Integer, Integer> cnt = new HashMap<>();
            for (int x = x0; x <= x1; x++) cnt.merge(im.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
            Map.Entry<Integer, Integer> best = null;
            for (Map.Entry<Integer, Integer> e : cnt.entrySet()) if (best == null || e.getValue() > best.getValue()) best = e;
            int rgb = best.getKey();
            int card = im.getRGB(70, y) & 0xFFFFFF;
            if (best.getValue() > 0.97 * (x1 - x0 + 1) && rgb != card && rgb != bg && (im.getRGB(1020, y) & 0xFFFFFF) != bg) {
                div.append(String.format(" y=%d(#%06X)", y, rgb));
            }
        }
        System.out.println(div);
    }
}

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Prints the ink box (x, y, width, height) of every text node of a screen.
 * usage: java InkAll.java screen.xml screen.png [thr=60]
 */
public class InkAll {
    static BufferedImage im;
    static int thr = 60;

    public static void main(String[] a) throws Exception {
        im = ImageIO.read(new File(a[1]));
        if (a.length > 2) thr = Integer.parseInt(a[2]);
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(a[0])).getDocumentElement();
        walk(root);
    }

    static void walk(Element e) {
        if (e.getTagName().equals("node")) {
            String txt = e.getAttribute("text");
            if (!txt.isEmpty()) {
                String[] p = e.getAttribute("bounds").replace("][", ",").replace("[", "").replace("]", "").split(",");
                int x0 = Math.max(0, Integer.parseInt(p[0])), y0 = Math.max(0, Integer.parseInt(p[1]));
                int x1 = Math.min(im.getWidth(), Integer.parseInt(p[2])), y1 = Math.min(im.getHeight(), Integer.parseInt(p[3]));
                if (x1 - x0 > 1 && y1 - y0 > 1) {
                    int bg = im.getRGB(x0, y0);
                    int br = (bg >> 16) & 255, bgg = (bg >> 8) & 255, bb = bg & 255;
                    int l = 1 << 30, t = 1 << 30, r = -1, b = -1;
                    for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                        int px = im.getRGB(x, y);
                        int d = Math.max(Math.abs(((px >> 16) & 255) - br), Math.max(Math.abs(((px >> 8) & 255) - bgg), Math.abs((px & 255) - bb)));
                        if (d > thr) {
                            if (x < l) l = x;
                            if (x > r) r = x;
                            if (y < t) t = y;
                            if (y > b) b = y;
                        }
                    }
                    String name = txt.length() > 36 ? txt.substring(0, 36) : txt;
                    if (r >= 0) {
                        System.out.printf("%-38s node [%d,%d][%d,%d]  ink x=%d y=%d w=%d h=%d%n", name, x0, y0, x1, y1, l, t, r - l + 1, b - t + 1);
                    }
                }
            }
        }
        NodeList ch = e.getChildNodes();
        for (int i = 0; i < ch.getLength(); i++) {
            Node n = ch.item(i);
            if (n instanceof Element) walk((Element) n);
        }
    }
}

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Compares the text elements of two screens (real app vs catalog): matches nodes with the same text string, measures
 * the ink box of every match inside its uiautomator bounds and prints them side by side with the width ratio
 * (width ratio = font size ratio when the weight is the same). Also prints the ink top relative to the enclosing row
 * (the smallest enclosing node that is at least 800 px wide and 100 px tall), so vertical text placement can be compared.
 * usage: java TextCmp.java real.xml real.png kit.xml kit.png [thr=60]
 */
public class TextCmp {
    record T(String text, int l, int t, int r, int b, int rowTop, int rowBottom) {}

    static List<T> texts(String xml) throws Exception {
        List<T> out = new ArrayList<>();
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xml)).getDocumentElement();
        walk(root, out, new ArrayDeque<>());
        return out;
    }

    static int[] bounds(Element e) {
        String[] p = e.getAttribute("bounds").replace("][", ",").replace("[", "").replace("]", "").split(",");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])};
    }

    static void walk(Element e, List<T> out, Deque<int[]> stack) {
        boolean node = e.getTagName().equals("node");
        if (node) {
            int[] b = bounds(e);
            String txt = e.getAttribute("text");
            if (!txt.isEmpty()) {
                int rt = b[1], rb = b[3];
                for (int[] s : stack) { // innermost first
                    if (s[2] - s[0] >= 800 && s[3] - s[1] >= 100 && s[1] <= b[1] && s[3] >= b[3]) { rt = s[1]; rb = s[3]; break; }
                }
                out.add(new T(txt, b[0], b[1], b[2], b[3], rt, rb));
            }
            stack.push(b);
        }
        NodeList ch = e.getChildNodes();
        for (int i = 0; i < ch.getLength(); i++) {
            Node n = ch.item(i);
            if (n instanceof Element) walk((Element) n, out, stack);
        }
        if (node) stack.pop();
    }

    static int[] ink(BufferedImage im, T t, int thr) {
        int x0 = Math.max(0, t.l), y0 = Math.max(0, t.t), x1 = Math.min(im.getWidth(), t.r), y1 = Math.min(im.getHeight(), t.b);
        if (x1 - x0 < 2 || y1 - y0 < 2) return null;
        int bg = im.getRGB(x0, y0);
        int br = (bg >> 16) & 255, bgg = (bg >> 8) & 255, bb = bg & 255;
        int l = 1 << 30, tp = 1 << 30, r = -1, b = -1;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int p = im.getRGB(x, y);
            int d = Math.max(Math.abs(((p >> 16) & 255) - br), Math.max(Math.abs(((p >> 8) & 255) - bgg), Math.abs((p & 255) - bb)));
            if (d > thr) {
                if (x < l) l = x;
                if (x > r) r = x;
                if (y < tp) tp = y;
                if (y > b) b = y;
            }
        }
        return r < 0 ? null : new int[]{l, tp, r, b};
    }

    public static void main(String[] a) throws Exception {
        int thr = a.length > 4 ? Integer.parseInt(a[4]) : 60;
        BufferedImage ri = ImageIO.read(new File(a[1])), ki = ImageIO.read(new File(a[3]));
        Map<String, T> real = new LinkedHashMap<>();
        for (T t : texts(a[0])) real.putIfAbsent(t.text, t);
        Set<String> seen = new HashSet<>();
        System.out.printf("%-30s | %-20s | %-20s | %-7s | %-22s | %s%n", "text", "real x,y,w,h", "kit x,y,w,h", "w ratio", "real dTop/dBottom(row)", "kit dTop/dBottom(row)");
        for (T k : texts(a[2])) {
            T r = real.get(k.text);
            if (r == null || !seen.add(k.text)) continue;
            int[] ri2 = ink(ri, r, thr), ki2 = ink(ki, k, thr);
            if (ri2 == null || ki2 == null) continue;
            int rw = ri2[2] - ri2[0] + 1, rh = ri2[3] - ri2[1] + 1, kw = ki2[2] - ki2[0] + 1, kh = ki2[3] - ki2[1] + 1;
            String name = k.text.length() > 28 ? k.text.substring(0, 28) : k.text;
            System.out.printf("%-30s | %4d,%4d,%4d,%3d  | %4d,%4d,%4d,%3d  | %.3f   | %4d / %4d (row %3d)    | %4d / %4d (row %3d)%n",
                name, ri2[0], ri2[1], rw, rh, ki2[0], ki2[1], kw, kh, rw / (double) kw,
                ri2[1] - r.rowTop, r.rowBottom - ri2[3], r.rowBottom - r.rowTop,
                ki2[1] - k.rowTop, k.rowBottom - ki2[3], k.rowBottom - k.rowTop);
        }
    }
}

import java.io.*;
import java.util.*;

/**
 * Peak and mean brightness of a region per frame (alpha / fade curves of text on a dark background).
 * args: W H [x0 y0 x1 y1]     output per frame: idx,maxChannel,meanLuma
 */
public class Peak {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int x0 = a.length > 5 ? Integer.parseInt(a[2]) : 0, y0 = a.length > 5 ? Integer.parseInt(a[3]) : 0;
        int x1 = a.length > 5 ? Integer.parseInt(a[4]) : W, y1 = a.length > 5 ? Integer.parseInt(a[5]) : H;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            int mx = 0;
            double sum = 0;
            for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                int i = (y * W + x) * 3;
                int r = b[i] & 255, g = b[i + 1] & 255, bl = b[i + 2] & 255;
                mx = Math.max(mx, Math.max(r, Math.max(g, bl)));
                sum += 0.299 * r + 0.587 * g + 0.114 * bl;
            }
            System.out.printf(Locale.ROOT, "%d,%d,%.2f%n", idx++, mx, sum / ((x1 - x0) * (y1 - y0)));
        }
    }
}

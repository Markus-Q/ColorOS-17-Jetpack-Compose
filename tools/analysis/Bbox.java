import java.io.*;
import java.util.*;

/**
 * Bounding box of bright pixels (text, icons) per frame, for tracking scale and position of a popup's content.
 * args: W H thr [x0 y0 x1 y1]   (max channel >= thr counts, optional sub-rectangle)
 * output per frame: idx,left,top,right,bottom,count,cx,cy
 */
public class Bbox {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]), thr = Integer.parseInt(a[2]);
        int x0 = a.length > 6 ? Integer.parseInt(a[3]) : 0, y0 = a.length > 6 ? Integer.parseInt(a[4]) : 0;
        int x1 = a.length > 6 ? Integer.parseInt(a[5]) : W, y1 = a.length > 6 ? Integer.parseInt(a[6]) : H;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            int l = 1 << 30, t = 1 << 30, r = -1, bt = -1, n = 0;
            double sx = 0, sy = 0;
            for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                int i = (y * W + x) * 3;
                int m = Math.max(b[i] & 255, Math.max(b[i + 1] & 255, b[i + 2] & 255));
                if (m >= thr) {
                    if (x < l) l = x;
                    if (x > r) r = x;
                    if (y < t) t = y;
                    if (y > bt) bt = y;
                    n++; sx += x; sy += y;
                }
            }
            if (n == 0) System.out.printf(Locale.ROOT, "%d,NaN,NaN,NaN,NaN,0,NaN,NaN%n", idx++);
            else System.out.printf(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%.1f,%.1f%n", idx++, l, t, r, bt, n, sx / n, sy / n);
        }
    }
}

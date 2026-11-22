import java.io.*;
import java.util.*;

/**
 * Brightness run tracker for highlights: in every frame (raw rgb24 crop on stdin) finds the vertical run of pixels with
 * luminance >= thr around (cx, cy) and prints its top, bottom and height, the luminance at (cx, cy) and at an optional
 * reference point (rx, ry), plus the left/right extents of the run along the row cy.
 * args: W H cx cy thr [rx ry]    output per frame: idx,top,bottom,height,lumC,lumR,left,right
 */
public class Col {
    static int W, H;
    static byte[] b;

    static int lum(int x, int y) {
        int i = (y * W + x) * 3;
        return ((b[i] & 255) * 299 + (b[i + 1] & 255) * 587 + (b[i + 2] & 255) * 114) / 1000;
    }

    public static void main(String[] a) throws Exception {
        W = Integer.parseInt(a[0]);
        H = Integer.parseInt(a[1]);
        int cx = Integer.parseInt(a[2]), cy = Integer.parseInt(a[3]), thr = Integer.parseInt(a[4]);
        int rx = a.length > 6 ? Integer.parseInt(a[5]) : 0, ry = a.length > 6 ? Integer.parseInt(a[6]) : 0;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            int top = cy, bot = cy;
            if (lum(cx, cy) >= thr) {
                while (top > 0 && lum(cx, top - 1) >= thr) top--;
                while (bot < H - 1 && lum(cx, bot + 1) >= thr) bot++;
            } else { top = -1; bot = -1; }
            int l = cx, r = cx;
            if (lum(cx, cy) >= thr) {
                while (l > 0 && lum(l - 1, cy) >= thr) l--;
                while (r < W - 1 && lum(r + 1, cy) >= thr) r++;
            } else { l = -1; r = -1; }
            System.out.printf(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%d%n", idx++, top, bot, top < 0 ? 0 : bot - top + 1, lum(cx, cy),
                a.length > 6 ? lum(rx, ry) : 0, l, r);
        }
    }
}

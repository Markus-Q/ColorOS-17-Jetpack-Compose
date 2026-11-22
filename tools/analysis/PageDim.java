import java.io.*;
import java.util.*;

/**
 * Brightness of a screen region per frame (dimming of a page under a transition): the 99.5th percentile and the mean of the
 * gray values in the columns [x0, x1) of every frame (raw gray8 W x H band on stdin).
 * args: W H x0 x1      output per frame: idx,p995,mean
 */
public class PageDim {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int x0 = Integer.parseInt(a[2]), x1 = Integer.parseInt(a[3]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            int[] hist = new int[256];
            long sum = 0, n = 0;
            for (int y = 0; y < H; y++) for (int x = x0; x < x1; x++) { int v = b[y * W + x] & 255; hist[v]++; sum += v; n++; }
            long acc = 0; int p = 0;
            for (int v = 255; v >= 0; v--) { acc += hist[v]; if (acc >= n * 0.005) { p = v; break; } }
            System.out.printf(Locale.ROOT, "%d,%d,%.2f%n", idx++, p, (double) sum / n);
        }
    }
}

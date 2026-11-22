import java.io.*;
import java.util.*;

/**
 * Runs of pixels with a gray value in [lo, hi] along one row of every frame (raw rgb24 on stdin), for tracking the edges
 * of pills and fields. Runs shorter than minRun are ignored.
 * args: W H row lo hi [minRun=12]      output per frame: idx,count,start-end,start-end,...
 */
public class RowRuns {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]), row = Integer.parseInt(a[2]);
        int lo = Integer.parseInt(a[3]), hi = Integer.parseInt(a[4]);
        int minRun = a.length > 5 ? Integer.parseInt(a[5]) : 12;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            StringBuilder sb = new StringBuilder();
            int n = 0, start = -1;
            for (int x = 0; x <= W; x++) {
                boolean ok = false;
                if (x < W) {
                    int i = (row * W + x) * 3;
                    int g = ((b[i] & 255) * 299 + (b[i + 1] & 255) * 587 + (b[i + 2] & 255) * 114) / 1000;
                    ok = g >= lo && g <= hi;
                }
                if (ok && start < 0) start = x;
                if (!ok && start >= 0) {
                    if (x - start >= minRun) { sb.append(',').append(start).append('-').append(x - 1); n++; }
                    start = -1;
                }
            }
            System.out.printf(Locale.ROOT, "%d,%d%s%n", idx++, n, sb);
        }
    }
}

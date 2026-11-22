import java.io.*;
import java.util.*;

/**
 * Tracks the dominant text band (largest bright-pixel mass) of a region, for large collapsing titles.
 * args: W H thr [x0 x1]   per frame output: idx,bandTop,bandBottom,bandHeight,mass,left,right,peakBrightness
 */
public class TitleTrack {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]), thr = Integer.parseInt(a[2]);
        int x0 = a.length > 4 ? Integer.parseInt(a[3]) : 0, x1 = a.length > 4 ? Integer.parseInt(a[4]) : W;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            int[] mass = new int[H];
            for (int y = 0; y < H; y++) {
                int c = 0;
                for (int x = x0; x < x1; x++) {
                    int i = (y * W + x) * 3;
                    if (Math.max(b[i] & 255, Math.max(b[i + 1] & 255, b[i + 2] & 255)) >= thr) c++;
                }
                mass[y] = c;
            }
            // bands separated by at least 6 empty rows
            int bestTop = -1, bestBottom = -1, bestMass = 0;
            int y = 0;
            while (y < H) {
                if (mass[y] == 0) { y++; continue; }
                int top = y, sum = 0, gap = 0, last = y;
                while (y < H && gap < 6) {
                    if (mass[y] > 0) { sum += mass[y]; gap = 0; last = y; } else gap++;
                    y++;
                }
                if (sum > bestMass) { bestMass = sum; bestTop = top; bestBottom = last; }
            }
            int l = 1 << 30, r = -1, peak = 0;
            if (bestTop >= 0) {
                for (int yy = bestTop; yy <= bestBottom; yy++) for (int x = x0; x < x1; x++) {
                    int i = (yy * W + x) * 3;
                    int m = Math.max(b[i] & 255, Math.max(b[i + 1] & 255, b[i + 2] & 255));
                    if (m >= thr) { if (x < l) l = x; if (x > r) r = x; }
                    if (m > peak) peak = m;
                }
            }
            if (bestTop < 0) System.out.printf(Locale.ROOT, "%d,-1,-1,0,0,-1,-1,0%n", idx++);
            else System.out.printf(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%d%n", idx++, bestTop, bestBottom, bestBottom - bestTop + 1, bestMass, l, r, peak);
        }
    }
}

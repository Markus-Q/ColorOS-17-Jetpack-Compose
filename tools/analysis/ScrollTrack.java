import java.io.*;
import java.util.*;

/**
 * Vertical position of scrolling content over a whole recording: the shift between every two consecutive frames (zero mean
 * normalized correlation of the gray rows), accumulated. Positive = content moved down (the list scrolled towards its top).
 * Looks for the shift near the previous one first, so the repeating rows of a list do not alias.
 * Raw gray8 W x H band on stdin.
 * args: W H maxDelta      output per frame: delta,cum,ncc
 */
public class ScrollTrack {
    static int W, H;

    static double ncc(byte[] cur, byte[] prev, int s) {
        int y0 = Math.max(0, s), y1 = Math.min(H, H + s);
        if (y1 - y0 < H / 3) return -2;
        double sa = 0, sb = 0, saa = 0, sbb = 0, sab = 0;
        int n = 0;
        for (int y = y0; y < y1; y += 2) {
            int ra = y * W, rb = (y - s) * W;
            for (int x = 0; x < W; x += 3) {
                int p = cur[ra + x] & 255, q = prev[rb + x] & 255;
                sa += p; sb += q; saa += (double) p * p; sbb += (double) q * q; sab += (double) p * q; n++;
            }
        }
        double va = n * saa - sa * sa, vb = n * sbb - sb * sb;
        if (va < 1e-6 || vb < 1e-6) return -2;
        return (n * sab - sa * sb) / Math.sqrt(va * vb);
    }

    public static void main(String[] a) throws Exception {
        W = Integer.parseInt(a[0]);
        H = Integer.parseInt(a[1]);
        int maxD = Integer.parseInt(a[2]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] prev = null;
        int lastD = 0;
        double cum = 0;
        while (true) {
            byte[] cur = new byte[W * H];
            try { in.readFully(cur); } catch (EOFException e) { break; }
            if (prev == null) { System.out.println("0,0.0,1.0"); prev = cur; continue; }
            // identical frame -> no motion
            double bestV = -9; int bestS = 0;
            for (int s = -maxD; s <= maxD; s += 2) {
                double v = ncc(cur, prev, s);
                if (v > bestV) { bestV = v; bestS = s; }
            }
            for (int s = bestS - 2; s <= bestS + 2; s++) {
                double v = ncc(cur, prev, s);
                if (v > bestV) { bestV = v; bestS = s; }
            }
            // prefer the shift that continues the motion when it is nearly as good as the global best (repeating rows)
            int lo = lastD - 24, hi = lastD + 24;
            double nearV = -9; int nearS = bestS;
            for (int s = Math.max(-maxD, lo); s <= Math.min(maxD, hi); s++) {
                double v = ncc(cur, prev, s);
                if (v > nearV) { nearV = v; nearS = s; }
            }
            if (nearV > bestV - 0.03) { bestV = nearV; bestS = nearS; }
            lastD = bestS;
            cum += bestS;
            System.out.printf(Locale.ROOT, "%d,%.1f,%.4f%n", bestS, cum, bestV);
            prev = cur;
        }
    }
}

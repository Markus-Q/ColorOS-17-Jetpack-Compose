import java.io.*;
import java.util.*;

/**
 * Tracks the horizontal offsets of two pages during a push or pop in a screen recording, robust against dimming (zero mean
 * normalized correlation instead of absolute differences).
 *
 * Reads raw gray8 frames (W x H band) from stdin. The first frame must show the page that was on screen before the
 * transition, the last frame the page that is on screen after it.
 *   push: the new page (last frame) slides in from the right, the old page (first frame) drifts left.
 *   pop:  the old page (first frame) slides out to the right, the page below (last frame) comes back from the left.
 * In both modes the page on the right is matched first on the columns it covers (x >= s), the page on the left on the
 * columns that are left of it.
 *
 * args: W H mode(push|pop) [maxLeft=560]
 * output per frame: idx,sRight,nccRight,sLeft,nccLeft     (sRight in 0..W, sLeft in -maxLeft..0; -1 ncc = not measurable)
 */
public class PageShift2 {
    static int W, H;
    static final int CX = 3, CY = 4;

    /** NCC between fr(x) and ref(x - s) on the columns [x0, x1) that stay inside the reference. */
    static double ncc(byte[] fr, byte[] ref, int s, int x0, int x1) {
        int a = Math.max(x0, Math.max(0, s));
        int b = Math.min(x1, Math.min(W, W + s));
        if (b - a < 48) return -2;
        double sa = 0, sb = 0, saa = 0, sbb = 0, sab = 0;
        int n = 0;
        for (int y = 0; y < H; y += CY) {
            int row = y * W;
            for (int x = a; x < b; x += CX) {
                int p = fr[row + x] & 255, q = ref[row + x - s] & 255;
                sa += p; sb += q; saa += (double) p * p; sbb += (double) q * q; sab += (double) p * q; n++;
            }
        }
        double va = n * saa - sa * sa, vb = n * sbb - sb * sb;
        if (va < 1e-6 || vb < 1e-6) return -1;
        return (n * sab - sa * sb) / Math.sqrt(va * vb);
    }

    /** Best shift in [lo, hi]; the region is [xFrom(s), xTo(s)). */
    interface Region { int from(int s); int to(int s); }

    static double[] best(byte[] fr, byte[] ref, int lo, int hi, Region r) {
        double bestV = -9; int bestS = 0;
        for (int s = lo; s <= hi; s += 6) {
            double v = ncc(fr, ref, s, r.from(s), r.to(s));
            if (v > bestV) { bestV = v; bestS = s; }
        }
        int c = bestS;
        for (int s = Math.max(lo, c - 6); s <= Math.min(hi, c + 6); s++) {
            double v = ncc(fr, ref, s, r.from(s), r.to(s));
            if (v > bestV) { bestV = v; bestS = s; }
        }
        return new double[] { bestS, bestV };
    }

    public static void main(String[] a) throws Exception {
        W = Integer.parseInt(a[0]);
        H = Integer.parseInt(a[1]);
        boolean push = a[2].equals("push");
        int maxLeft = a.length > 3 ? Integer.parseInt(a[3]) : 560;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        List<byte[]> frames = new ArrayList<>();
        while (true) {
            byte[] b = new byte[W * H];
            try { in.readFully(b); } catch (EOFException e) { break; }
            frames.add(b);
        }
        byte[] first = frames.get(0), last = frames.get(frames.size() - 1);
        byte[] rightRef = push ? last : first, leftRef = push ? first : last;
        for (int f = 0; f < frames.size(); f++) {
            byte[] fr = frames.get(f);
            double[] r = best(fr, rightRef, 0, W, new Region() {
                public int from(int s) { return s; }
                public int to(int s) { return W; }
            });
            final int sR = (int) r[0];
            double[] l = best(fr, leftRef, -maxLeft, 0, new Region() {
                public int from(int s) { return 0; }
                public int to(int s) { return Math.max(0, sR - 4); }
            });
            System.out.printf(Locale.ROOT, "%d,%d,%.3f,%d,%.3f%n", f, sR, r[1], (int) l[0], l[1]);
        }
    }
}

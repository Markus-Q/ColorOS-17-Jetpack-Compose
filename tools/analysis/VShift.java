import java.io.*;
import java.util.*;

/**
 * Vertical offset of scrolling content per frame: the shift s (px, positive = content moved down) that best maps the first
 * frame onto each frame (zero mean normalized correlation of the gray rows, columns sub-sampled). Raw gray8 W x H band on
 * stdin; the first frame is the reference.
 * args: W H minShift maxShift       output per frame: idx,shift,ncc
 */
public class VShift {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int lo = Integer.parseInt(a[2]), hi = Integer.parseInt(a[3]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        List<byte[]> frames = new ArrayList<>();
        while (true) {
            byte[] b = new byte[W * H];
            try { in.readFully(b); } catch (EOFException e) { break; }
            frames.add(b);
        }
        byte[] ref = frames.get(0);
        int cx = 5;
        for (int f = 0; f < frames.size(); f++) {
            byte[] fr = frames.get(f);
            double bestV = -9; int bestS = 0;
            for (int pass = 0; pass < 2; pass++) {
                int from = pass == 0 ? lo : bestS - 5, to = pass == 0 ? hi : bestS + 5, step = pass == 0 ? 4 : 1;
                for (int s = from; s <= to; s += step) {
                    int y0 = Math.max(0, s), y1 = Math.min(H, H + s);
                    if (y1 - y0 < H / 3) continue;
                    double sa = 0, sb = 0, saa = 0, sbb = 0, sab = 0; int n = 0;
                    for (int y = y0; y < y1; y += 2) {
                        int ra = y * W, rb = (y - s) * W;
                        for (int x = 0; x < W; x += cx) {
                            int p = fr[ra + x] & 255, q = ref[rb + x] & 255;
                            sa += p; sb += q; saa += (double) p * p; sbb += (double) q * q; sab += (double) p * q; n++;
                        }
                    }
                    double va = n * saa - sa * sa, vb = n * sbb - sb * sb;
                    if (va < 1e-6 || vb < 1e-6) continue;
                    double v = (n * sab - sa * sb) / Math.sqrt(va * vb);
                    if (v > bestV) { bestV = v; bestS = s; }
                }
            }
            System.out.printf(Locale.ROOT, "%d,%d,%.3f%n", f, bestS, bestV);
        }
    }
}

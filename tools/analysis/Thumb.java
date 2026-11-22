import java.io.*;
import java.util.*;

/**
 * Measures a bright blob (e.g. a white switch thumb) in every frame of a raw rgb24 stream.
 * args: W H lo hi   weight = clamp((min(r,g,b) - lo) / (hi - lo), 0, 1)
 * output per frame: idx,cx,cy,areaPx,widthAtCenterRow,heightAtCenterCol,left,right
 */
public class Thumb {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        double lo = Double.parseDouble(a[2]), hi = Double.parseDouble(a[3]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] buf = new byte[W * H * 3];
        double[][] w = new double[H][W];
        int idx = 0;
        while (true) {
            try { in.readFully(buf); } catch (EOFException e) { break; }
            double sum = 0, sx = 0, sy = 0;
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
                int i = (y * W + x) * 3;
                int m = Math.min(buf[i] & 255, Math.min(buf[i + 1] & 255, buf[i + 2] & 255));
                double v = Math.max(0, Math.min(1, (m - lo) / (hi - lo)));
                w[y][x] = v;
                sum += v; sx += v * (x + 0.5); sy += v * (y + 0.5);
            }
            if (sum < 1) { System.out.printf(Locale.ROOT, "%d,NaN,NaN,0,NaN,NaN,NaN,NaN%n", idx++); continue; }
            double cx = sx / sum, cy = sy / sum;
            int ry = (int) Math.min(H - 1, Math.max(0, Math.round(cy - 0.5)));
            int rx = (int) Math.min(W - 1, Math.max(0, Math.round(cx - 0.5)));
            double wr = 0, hc = 0, left = 1e9, right = -1e9;
            for (int x = 0; x < W; x++) { wr += w[ry][x]; if (w[ry][x] > 0.02) { left = Math.min(left, x + 0.5 - (w[ry][x] - 0.5)); right = Math.max(right, x + 0.5 + (w[ry][x] - 0.5)); } }
            for (int y = 0; y < H; y++) hc += w[y][rx];
            System.out.printf(Locale.ROOT, "%d,%.2f,%.2f,%.1f,%.2f,%.2f,%.2f,%.2f%n", idx++, cx, cy, sum, wr, hc, left, right);
        }
    }
}

import java.io.*;
import java.util.*;

/**
 * Average color (3x3 box) of fixed points in every frame of a raw rgb24 band on stdin.
 * args: W H x1 y1 [x2 y2 ...]     output per frame: idx,r1,g1,b1,r2,g2,b2,...
 */
public class PtColor {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int n = (a.length - 2) / 2;
        int[] xs = new int[n], ys = new int[n];
        for (int k = 0; k < n; k++) { xs[k] = Integer.parseInt(a[2 + 2 * k]); ys[k] = Integer.parseInt(a[3 + 2 * k]); }
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            StringBuilder sb = new StringBuilder().append(idx++);
            for (int k = 0; k < n; k++) {
                int r = 0, g = 0, bl = 0, c = 0;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int x = xs[k] + dx, y = ys[k] + dy;
                    if (x < 0 || y < 0 || x >= W || y >= H) continue;
                    int i = (y * W + x) * 3;
                    r += b[i] & 255; g += b[i + 1] & 255; bl += b[i + 2] & 255; c++;
                }
                sb.append(',').append(r / c).append(',').append(g / c).append(',').append(bl / c);
            }
            System.out.println(sb);
        }
    }
}

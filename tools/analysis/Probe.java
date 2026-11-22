import java.io.*;
import java.util.*;

/**
 * Measures the edges of one element in every frame of a raw rgb24 stream.
 * args: W H cx cy bgR bgG bgB T [sx sy]
 *   cx,cy  start point inside the element (crop coordinates), follows the element afterwards
 *   bg     background color around the element, T threshold on max channel distance
 *   sx,sy  optional sample pixel whose color is printed (relative to the element center, in px)
 * output per frame: idx,left,right,top,bottom,width,height,cx,cy[,r,g,b]
 */
public class Probe {
    static int W, H, br, bg, bb, T;

    static double dist(byte[] b, int x, int y) {
        int i = (y * W + x) * 3;
        int r = b[i] & 255, g = b[i + 1] & 255, bl = b[i + 2] & 255;
        return Math.max(Math.abs(r - br), Math.max(Math.abs(g - bg), Math.abs(bl - bb)));
    }

    /** returns the subpixel boundary coordinate along the scan axis, or NaN. */
    static double scan(byte[] b, int x0, int y0, int dx, int dy) {
        int x = x0, y = y0;
        double prev = dist(b, x, y);
        while (true) {
            int nx = x + dx, ny = y + dy;
            if (nx < 0 || ny < 0 || nx >= W || ny >= H) return Double.NaN;
            double d = dist(b, nx, ny);
            if (d < T && prev >= T) {
                boolean ok = true;
                for (int k = 1; k <= 3; k++) {
                    int mx = nx + dx * k, my = ny + dy * k;
                    if (mx < 0 || my < 0 || mx >= W || my >= H) break;
                    if (dist(b, mx, my) >= T) { ok = false; break; }
                }
                if (ok) {
                    double frac = (prev - T) / (prev - d);
                    return dx != 0 ? x + 0.5 + dx * frac : y + 0.5 + dy * frac;
                }
            }
            prev = d;
            x = nx;
            y = ny;
        }
    }

    public static void main(String[] a) throws Exception {
        W = Integer.parseInt(a[0]);
        H = Integer.parseInt(a[1]);
        double cx = Double.parseDouble(a[2]), cy = Double.parseDouble(a[3]);
        br = Integer.parseInt(a[4]);
        bg = Integer.parseInt(a[5]);
        bb = Integer.parseInt(a[6]);
        T = Integer.parseInt(a[7]);
        String lock = System.getenv("PROBE_LOCK") == null ? "" : System.getenv("PROBE_LOCK");
        double lockX = cx, lockY = cy;
        boolean sample = a.length >= 10;
        int sx = sample ? Integer.parseInt(a[8]) : 0, sy = sample ? Integer.parseInt(a[9]) : 0;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] buf = new byte[W * H * 3];
        int idx = 0;
        StringBuilder sb = new StringBuilder();
        while (true) {
            try {
                in.readFully(buf);
            } catch (EOFException e) {
                break;
            }
            int ix = (int) Math.max(1, Math.min(W - 2, Math.round(cx)));
            int iy = (int) Math.max(1, Math.min(H - 2, Math.round(cy)));
            double l = scan(buf, ix, iy, -1, 0), r = scan(buf, ix, iy, 1, 0);
            double t = scan(buf, ix, iy, 0, -1), bt = scan(buf, ix, iy, 0, 1);
            double w = r - l, h = bt - t;
            if (!Double.isNaN(l) && !Double.isNaN(r)) cx = lock.contains("x") ? lockX : (l + r) / 2;
            if (!Double.isNaN(t) && !Double.isNaN(bt)) cy = lock.contains("y") ? lockY : (t + bt) / 2;
            sb.setLength(0);
            sb.append(String.format(Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f", idx, l, r, t, bt, w, h, cx, cy));
            if (sample) {
                int px = (int) Math.max(0, Math.min(W - 1, Math.round(cx + sx)));
                int py = (int) Math.max(0, Math.min(H - 1, Math.round(cy + sy)));
                int i = (py * W + px) * 3;
                sb.append(String.format(Locale.ROOT, ",%d,%d,%d", buf[i] & 255, buf[i + 1] & 255, buf[i + 2] & 255));
            }
            System.out.println(sb);
            idx++;
        }
    }
}

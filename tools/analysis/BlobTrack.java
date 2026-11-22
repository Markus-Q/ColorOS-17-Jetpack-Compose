import java.io.*;
import java.util.*;

/**
 * Tracks the largest white blob (a slider thumb) in every frame of a raw rgb24 band on stdin, ignoring smaller white
 * things such as tick marks, and the end of the accent fill on the row through the blob.
 * args: W H thr minArea       white = min(r,g,b) >= thr
 * output per frame: idx,cx,cy,area,bboxW,bboxH,fillEnd     (cx = NaN when no blob)
 * fillEnd = last x of the blue fill (b >= 200, r <= 110) on the blob's center row, -1 if none.
 */
public class BlobTrack {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int thr = Integer.parseInt(a[2]), minArea = Integer.parseInt(a[3]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int[] label = new int[W * H];
        int[] stack = new int[W * H];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            Arrays.fill(label, 0);
            int best = 0, bestArea = 0;
            double bx = 0, by = 0;
            int bx0 = 0, bx1 = 0, by0 = 0, by1 = 0;
            int next = 1;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int p = y * W + x;
                    if (label[p] != 0) continue;
                    int i = p * 3;
                    int m = Math.min(b[i] & 255, Math.min(b[i + 1] & 255, b[i + 2] & 255));
                    if (m < thr) continue;
                    // flood fill
                    int sp = 0;
                    stack[sp++] = p;
                    label[p] = next;
                    int area = 0;
                    double sx = 0, sy = 0;
                    int x0 = x, x1 = x, y0 = y, y1 = y;
                    while (sp > 0) {
                        int q = stack[--sp];
                        int qx = q % W, qy = q / W;
                        area++;
                        sx += qx; sy += qy;
                        if (qx < x0) x0 = qx;
                        if (qx > x1) x1 = qx;
                        if (qy < y0) y0 = qy;
                        if (qy > y1) y1 = qy;
                        int[] nb = { qx > 0 ? q - 1 : -1, qx < W - 1 ? q + 1 : -1, qy > 0 ? q - W : -1, qy < H - 1 ? q + W : -1 };
                        for (int n : nb) {
                            if (n < 0 || label[n] != 0) continue;
                            int j = n * 3;
                            int mm = Math.min(b[j] & 255, Math.min(b[j + 1] & 255, b[j + 2] & 255));
                            if (mm >= thr) { label[n] = next; stack[sp++] = n; }
                        }
                    }
                    if (area > bestArea) {
                        bestArea = area; best = next;
                        bx = sx / area + 0.5; by = sy / area + 0.5;
                        bx0 = x0; bx1 = x1; by0 = y0; by1 = y1;
                    }
                    next++;
                }
            }
            if (bestArea < minArea) {
                System.out.printf(Locale.ROOT, "%d,NaN,NaN,0,0,0,-1%n", idx++);
                continue;
            }
            int row = Math.min(H - 1, Math.max(0, (int) Math.round(by - 0.5)));
            int fillEnd = -1;
            for (int x = 0; x < W; x++) {
                int i = (row * W + x) * 3;
                int r = b[i] & 255, bl = b[i + 2] & 255;
                if (bl >= 200 && r <= 110) fillEnd = x;
            }
            System.out.printf(Locale.ROOT, "%d,%.2f,%.2f,%d,%d,%d,%d%n", idx++, bx, by, bestArea, bx1 - bx0 + 1, by1 - by0 + 1, fillEnd);
        }
    }
}

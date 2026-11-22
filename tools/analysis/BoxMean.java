import java.io.*;
import java.util.*;

/**
 * Mean luminance of several rectangles in every frame of a raw rgb24 band on stdin (selection state of icons and labels).
 * args: W H x0 y0 x1 y1 [x0 y0 x1 y1 ...]     output per frame: idx,mean1,mean2,...
 */
public class BoxMean {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int n = (a.length - 2) / 4;
        int[][] box = new int[n][4];
        for (int k = 0; k < n; k++) for (int j = 0; j < 4; j++) box[k][j] = Integer.parseInt(a[2 + 4 * k + j]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] b = new byte[W * H * 3];
        int idx = 0;
        while (true) {
            try { in.readFully(b); } catch (EOFException e) { break; }
            StringBuilder sb = new StringBuilder().append(idx++);
            for (int k = 0; k < n; k++) {
                long sum = 0; int c = 0;
                for (int y = box[k][1]; y < box[k][3] && y < H; y++) for (int x = box[k][0]; x < box[k][2] && x < W; x++) {
                    int i = (y * W + x) * 3;
                    sum += ((b[i] & 255) * 299 + (b[i + 1] & 255) * 587 + (b[i + 2] & 255) * 114) / 1000;
                    c++;
                }
                sb.append(',').append(String.format(Locale.ROOT, "%.1f", c == 0 ? 0.0 : (double) sum / c));
            }
            System.out.println(sb);
        }
    }
}

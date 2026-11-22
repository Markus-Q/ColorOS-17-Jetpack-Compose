import java.io.*;
import java.util.*;

/**
 * Tracks horizontal page offsets in a screen recording. Reads raw gray8 frames (W x H band) from stdin.
 * For every frame finds the shift s (px) that best maps the LAST frame onto it (incoming page, s>=0)
 * and the shift that best maps the FIRST frame onto it (outgoing page, s<=0).
 * args: W H firstRowStep
 */
public class PageShift {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        List<byte[]> frames = new ArrayList<>();
        while (true) { byte[] b = new byte[W * H]; try { in.readFully(b); } catch (EOFException e) { break; } frames.add(b); }
        byte[] first = frames.get(0), last = frames.get(frames.size() - 1);
        int cs = 3; // column step
        for (int f = 0; f < frames.size(); f++) {
            byte[] fr = frames.get(f);
            double bestIn = 1e18; int sIn = 0, sOut = 0; double bestOut = 1e18;
            for (int s = 0; s <= W; s += 2) {          // incoming: fr(x) ~ last(x - s)
                double e = 0; int n = 0;
                for (int x = s; x < W; x += cs) for (int y = 0; y < H; y += 3) { e += Math.abs((fr[y * W + x] & 255) - (last[y * W + x - s] & 255)); n++; }
                if (n > 60) { e /= n; if (e < bestIn) { bestIn = e; sIn = s; } }
            }
            for (int s = 0; s >= -W / 2; s -= 2) {     // outgoing: fr(x) ~ first(x - s), s negative
                double e = 0; int n = 0;
                for (int x = 0; x < W + s; x += cs) for (int y = 0; y < H; y += 3) { e += Math.abs((fr[y * W + x] & 255) - (first[y * W + x - s] & 255)); n++; }
                if (n > 60) { e /= n; if (e < bestOut) { bestOut = e; sOut = s; } }
            }
            System.out.printf(Locale.ROOT, "%d,%d,%.1f,%d,%.1f%n", f, sIn, bestIn, sOut, bestOut);
        }
    }
}

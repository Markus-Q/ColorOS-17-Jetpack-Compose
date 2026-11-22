import java.io.*;

/**
 * Centroid of the bright pixels (an icon glyph on a dark button) of every frame: where the element is while it is dragged,
 * and how much glyph is lit (it shrinks while the element is pressed). Raw rgb24 W x H frames on stdin.
 * args: W H [minLuma=170]      output per frame: cx,cy,count
 */
public class GlyphTrack {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int minLuma = a.length > 2 ? Integer.parseInt(a[2]) : 170;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] f = new byte[W * H * 3];
        while (true) {
            try { in.readFully(f); } catch (EOFException e) { break; }
            long sx = 0, sy = 0; int n = 0;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int i = (y * W + x) * 3;
                    int luma = ((f[i] & 255) * 3 + (f[i + 1] & 255) * 6 + (f[i + 2] & 255)) / 10;
                    if (luma >= minLuma) { sx += x; sy += y; n++; }
                }
            }
            if (n == 0) System.out.println("-1,-1,0");
            else System.out.printf(java.util.Locale.ROOT, "%.2f,%.2f,%d%n", sx / (double) n, sy / (double) n, n);
        }
    }
}

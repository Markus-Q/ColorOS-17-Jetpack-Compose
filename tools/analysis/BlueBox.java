import java.io.*;

/**
 * Bounding box of the accent coloured pixels (a primary button, even while it is lightened by the press) of every frame:
 * the pixels with a strong blue part and a weak red part. Raw rgb24 W x H frames on stdin.
 * args: W H [minBlue=170] [maxRed=150]      output per frame: top,bottom,left,right,count
 */
public class BlueBox {
    public static void main(String[] a) throws Exception {
        int W = Integer.parseInt(a[0]), H = Integer.parseInt(a[1]);
        int minBlue = a.length > 2 ? Integer.parseInt(a[2]) : 170;
        int maxRed = a.length > 3 ? Integer.parseInt(a[3]) : 150;
        DataInputStream in = new DataInputStream(new BufferedInputStream(System.in, 1 << 22));
        byte[] f = new byte[W * H * 3];
        while (true) {
            try { in.readFully(f); } catch (EOFException e) { break; }
            int top = -1, bottom = -1, left = W, right = -1, n = 0;
            for (int y = 0; y < H; y++) {
                int rowCount = 0;
                for (int x = 0; x < W; x += 2) {
                    int i = (y * W + x) * 3;
                    int r = f[i] & 255, b = f[i + 2] & 255, g = f[i + 1] & 255;
                    if (b >= minBlue && r <= maxRed && b - r > 60) {
                        rowCount++;
                        if (x < left) left = x;
                        if (x > right) right = x;
                    }
                }
                if (rowCount > 20) { // a button row, not a stray pixel
                    if (top < 0) top = y;
                    bottom = y;
                    n += rowCount;
                }
            }
            System.out.println(top + "," + bottom + "," + (right < 0 ? -1 : left) + "," + right + "," + n);
        }
    }
}

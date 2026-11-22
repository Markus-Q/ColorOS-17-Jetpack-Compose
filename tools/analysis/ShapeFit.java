import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/** Fits (radius, smoothing) of the corner-smoothing construction to a measured card corner. args: png topY leftEdgeX N bgR cardR */
public class ShapeFit {
    static double[][] curve(double r, double s) { // top-left corner polyline in px, from (0,p) to (p,0)
        double budget = 1e9; double p = (1 + s) * r;
        double arcMeasure = 90 * (1 - s);
        double arcLen = Math.sin(Math.toRadians(arcMeasure / 2)) * r * Math.sqrt(2);
        double angleAlpha = (90 - arcMeasure) / 2;
        double p3p4 = r * Math.tan(Math.toRadians(angleAlpha / 2));
        double beta = Math.toRadians(45 * s);
        double c = p3p4 * Math.cos(beta), d = c * Math.tan(beta);
        double b = (p - arcLen - c - d) / 3, a = 2 * b;
        // local coords: x along incoming edge (up, i.e. -y on screen), y along outgoing edge (+x on screen). screen = x*(ux,uy)+y*(vx,vy) with u=(0,-1), v=(1,0)
        List<double[]> pts = new ArrayList<>();
        double[] cur = {0, p}; // screen
        pts.add(cur.clone());
        double[][] segs = new double[3][];
        // seg1 relative cubic in local: (a,0),(a+b,0),(a+b+c,d)
        double theta = Math.toRadians(arcMeasure);
        double k = 4.0 / 3.0 * Math.tan(theta / 4.0) * r;
        double t0x = Math.cos(beta), t0y = Math.sin(beta), t3x = Math.sin(beta), t3y = Math.cos(beta);
        double[][] loc = {
            {a, 0, a + b, 0, a + b + c, d},
            {k * t0x, k * t0y, arcLen - k * t3x, arcLen - k * t3y, arcLen, arcLen},
            {d, c, d, b + c, d, a + b + c}};
        for (double[] sg : loc) {
            double[] p0 = cur.clone();
            double[] c1 = {p0[0] + toX(sg[0], sg[1]), p0[1] + toY(sg[0], sg[1])};
            double[] c2 = {p0[0] + toX(sg[2], sg[3]), p0[1] + toY(sg[2], sg[3])};
            double[] p3 = {p0[0] + toX(sg[4], sg[5]), p0[1] + toY(sg[4], sg[5])};
            for (int i = 1; i <= 40; i++) {
                double t = i / 40.0, u = 1 - t;
                pts.add(new double[]{u*u*u*p0[0] + 3*u*u*t*c1[0] + 3*u*t*t*c2[0] + t*t*t*p3[0], u*u*u*p0[1] + 3*u*u*t*c1[1] + 3*u*t*t*c2[1] + t*t*t*p3[1]});
            }
            cur = p3;
        }
        return pts.toArray(new double[0][]);
    }
    static double toX(double lx, double ly) { return lx * 0 + ly * 1; }   // u=(0,-1), v=(1,0): x = lx*ux + ly*vx
    static double toY(double lx, double ly) { return lx * -1 + ly * 0; }  // y = lx*uy + ly*vy
    static double xAtY(double[][] c, double y) { // leftmost x of the shape at row y (screen y downwards from the top edge)
        double best = Double.NaN;
        for (int i = 1; i < c.length; i++) {
            double y0 = c[i-1][1], y1 = c[i][1];
            if ((y0 - y) * (y1 - y) <= 0 && y0 != y1) {
                double t = (y - y0) / (y1 - y0);
                double x = c[i-1][0] + t * (c[i][0] - c[i-1][0]);
                if (Double.isNaN(best) || x < best) best = x;
            }
        }
        return Double.isNaN(best) ? 0 : best;
    }
    public static void main(String[] a) throws Exception {
        BufferedImage im = ImageIO.read(new File(a[0]));
        int top = Integer.parseInt(a[1]), left = Integer.parseInt(a[2]), n = Integer.parseInt(a[3]);
        int cardR = Integer.parseInt(a[4]);
        double[] real = new double[n];
        for (int y = 0; y < n; y++) {
            // subpixel left edge: scan for coverage using the card color
            double edge = left + 250;
            for (int x = Math.max(1, left - 70); x < left + 250; x++) {
                int rgb = im.getRGB(x, top + y);
                double v = ((rgb >> 16) & 255) / (double) cardR; // black bg -> coverage
                if (v >= 0.5) { edge = x + 0.5 - Math.min(1, v) + 0.5; // approximate boundary
                    // refine: use previous pixel partial coverage
                    if (x > 0) { double pv = ((im.getRGB(x - 1, top + y) >> 16) & 255) / (double) cardR; edge = x - pv; }
                    break; }
            }
            real[y] = edge - left;
        }
        double best = 1e18; double br = 0, bs = 0;
        for (double rdp = 12; rdp <= 32; rdp += 0.5)
            for (double s = 0; s <= 1.0001; s += 0.05) {
                double r = rdp * 3;
                double[][] c = curve(r, s);
                double err = 0; int cnt = 0;
                for (int y = 0; y < n; y++) { if (real[y] > 40) continue; double m = xAtY(c, y + 0.5); err += (m - real[y]) * (m - real[y]); cnt++; }
                err = Math.sqrt(err / Math.max(1, cnt));
                if (err < best) { best = err; br = rdp; bs = s; }
            }
        System.out.printf(Locale.ROOT, "best radius=%.1fdp smoothing=%.2f rms=%.2fpx%n", br, bs, best);
        double[][] c = curve(br * 3, bs);
        StringBuilder sb = new StringBuilder("row: real/model  ");
        for (int y = 0; y < 60; y += 6) sb.append(String.format(Locale.ROOT, "%d:%.1f/%.1f ", y, real[y], xAtY(c, y + 0.5)));
        System.out.println(sb);
    }
}

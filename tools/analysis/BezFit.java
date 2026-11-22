import java.nio.file.*;
import java.util.*;

/** Fits cubic-bezier easing + duration to a measured incoming page shift curve (csv from pshift: t,in,err,out,err). */
public class BezFit {
    static double bez(double x1, double y1, double x2, double y2, double x) {
        double lo = 0, hi = 1;
        for (int i = 0; i < 30; i++) {
            double s = (lo + hi) / 2, xs = 3*(1-s)*(1-s)*s*x1 + 3*(1-s)*s*s*x2 + s*s*s;
            if (xs < x) lo = s; else hi = s;
        }
        double s = (lo + hi) / 2;
        return 3*(1-s)*(1-s)*s*y1 + 3*(1-s)*s*s*y2 + s*s*s;
    }
    public static void main(String[] a) throws Exception {
        double t0 = Double.parseDouble(a[1]), t1 = Double.parseDouble(a[2]), start = Double.parseDouble(a[3]);
        List<double[]> pts = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            double t = Double.parseDouble(p[0]), s = Double.parseDouble(p[1]), err = Double.parseDouble(p[2]);
            if (t < t0 || t > t1 || err > 3 || s > 1070) continue;
            pts.add(new double[]{t, 1 - s / 1078.0});
        }
        double best = 1e18; double[] bp = null;
        for (double x1 = 0; x1 <= 0.8; x1 += 0.05) for (double y1 = 0; y1 <= 1.0; y1 += 0.05)
            for (double x2 = 0; x2 <= 1.0; x2 += 0.05) for (double y2 = 0.6; y2 <= 1.2; y2 += 0.05)
                for (double D = 120; D <= 360; D += 10) for (double sh = -12; sh <= 12; sh += 4) {
                    double err = 0;
                    for (double[] p : pts) { double x = (p[0] - start - sh) / D; double y = x <= 0 ? 0 : x >= 1 ? 1 : bez(x1, y1, x2, y2, x); err += (y - p[1]) * (y - p[1]); }
                    err = Math.sqrt(err / pts.size());
                    if (err < best) { best = err; bp = new double[]{x1, y1, x2, y2, D, sh}; }
                }
        System.out.printf(Locale.ROOT, "n=%d best rms=%.4f cubic-bezier(%.2f, %.2f, %.2f, %.2f) duration=%.0fms shift=%.0f%n", pts.size(), best, bp[0], bp[1], bp[2], bp[3], bp[4], bp[5]);
    }
}

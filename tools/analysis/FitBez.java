import java.nio.file.*;
import java.util.*;

/**
 * Fits a cubic-bezier easing curve with a duration to a measured series (random search plus hill climbing).
 * usage: java FitBez.java file.csv timeCol valueCol tStart tEnd vStart vEnd
 * Prints cubic-bezier(x1, y1, x2, y2), the duration in ms, the start time and the rms residual (fraction of travel).
 */
public class FitBez {
    static double bez(double x1, double y1, double x2, double y2, double x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        double lo = 0, hi = 1;
        for (int i = 0; i < 28; i++) {
            double s = (lo + hi) / 2, xs = 3 * (1 - s) * (1 - s) * s * x1 + 3 * (1 - s) * s * s * x2 + s * s * s;
            if (xs < x) lo = s; else hi = s;
        }
        double s = (lo + hi) / 2;
        return 3 * (1 - s) * (1 - s) * s * y1 + 3 * (1 - s) * s * s * y2 + s * s * s;
    }

    static List<double[]> pts = new ArrayList<>();

    static double err(double[] p) {
        double e = 0;
        for (double[] q : pts) {
            double y = bez(p[0], p[1], p[2], p[3], (q[0] - p[5]) / p[4]);
            e += (y - q[1]) * (y - q[1]);
        }
        return Math.sqrt(e / pts.size());
    }

    public static void main(String[] a) throws Exception {
        int tc = Integer.parseInt(a[1]), vc = Integer.parseInt(a[2]);
        double t0 = Double.parseDouble(a[3]), t1 = Double.parseDouble(a[4]);
        double vs = Double.parseDouble(a[5]), ve = Double.parseDouble(a[6]);
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            if (p.length <= Math.max(tc, vc)) continue;
            double t = Double.parseDouble(p[tc]);
            double v;
            try { v = Double.parseDouble(p[vc]); } catch (Exception e) { continue; }
            if (Double.isNaN(v) || v == -1 || t < t0 || t > t1) continue;
            pts.add(new double[]{t, (v - vs) / (ve - vs)});
        }
        Random r = new Random(7);
        double[] best = null;
        double be = 1e18;
        for (int i = 0; i < 400000; i++) {
            double[] p = {r.nextDouble(), r.nextDouble() * 1.4 - 0.2, r.nextDouble(), r.nextDouble() * 1.4 - 0.2,
                100 + r.nextDouble() * 800, t0 - 300 + r.nextDouble() * (t1 - t0 + 300)};
            double e = err(p);
            if (e < be) { be = e; best = p; }
        }
        double[] scale = {0.05, 0.05, 0.05, 0.05, 20, 10};
        for (int round = 0; round < 40; round++) {
            for (int i = 0; i < 4000; i++) {
                double[] p = best.clone();
                for (int k = 0; k < 6; k++) p[k] += (r.nextDouble() - 0.5) * 2 * scale[k] * (1.0 - round / 45.0);
                p[0] = Math.max(0, Math.min(1, p[0]));
                p[2] = Math.max(0, Math.min(1, p[2]));
                if (p[4] < 50) continue;
                double e = err(p);
                if (e < be) { be = e; best = p; }
            }
        }
        System.out.printf(Locale.ROOT, "n=%d  cubic-bezier(%.2f, %.2f, %.2f, %.2f)  duration=%.0f ms  start=%.0f ms  rms=%.4f%n",
            pts.size(), best[0], best[1], best[2], best[3], best[4], best[5], be);
    }
}

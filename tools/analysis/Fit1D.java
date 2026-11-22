import java.nio.file.*;
import java.util.*;

/**
 * Fits a damped spring step to a measured one dimensional series (open / close / settle animations).
 * usage: java Fit1D.java file.csv timeCol valueCol tStart tEnd vStart vEnd [zMin zMax]
 * Model: v(t) = vEnd + (vStart - vEnd) * (1 - step(t - t0)) with step the response of a unit mass spring (damping
 * ratio z, natural frequency w). Prints the best z, w (rad/s), stiffness (= w*w for Compose), response time 2*pi/w and
 * the start time t0, plus the rms residual in units of the travel. Compose: spring(dampingRatio = z, stiffness = w*w).
 */
public class Fit1D {
    static double step(double t, double z, double w) {
        if (t <= 0) return 0;
        double e = Math.exp(-z * w * t);
        if (z < 0.9999) {
            double wd = w * Math.sqrt(1 - z * z);
            return 1 - e * (Math.cos(wd * t) + z * w / wd * Math.sin(wd * t));
        }
        return 1 - e * (1 + w * t);
    }

    public static void main(String[] a) throws Exception {
        int tc = Integer.parseInt(a[1]), vc = Integer.parseInt(a[2]);
        double t0 = Double.parseDouble(a[3]), t1 = Double.parseDouble(a[4]);
        double vs = Double.parseDouble(a[5]), ve = Double.parseDouble(a[6]);
        double zMin = a.length > 7 ? Double.parseDouble(a[7]) : 0.4, zMax = a.length > 8 ? Double.parseDouble(a[8]) : 1.0;
        List<double[]> pts = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            if (p.length <= Math.max(tc, vc)) continue;
            double t = Double.parseDouble(p[tc]);
            double v;
            try { v = Double.parseDouble(p[vc]); } catch (Exception e) { continue; }
            if (Double.isNaN(v) || v == -1 || t < t0 || t > t1) continue;
            pts.add(new double[]{t, (v - vs) / (ve - vs)});
        }
        double best = 1e18;
        double[] bp = null;
        for (double z = zMin; z <= zMax + 1e-9; z += 0.02) {
            for (double w = 5; w <= 90; w += 0.5) {
                for (double sh = t0 - 200; sh <= t1; sh += 2) {
                    double err = 0;
                    for (double[] p : pts) {
                        double y = step((p[0] - sh) / 1000.0, z, w);
                        err += (y - p[1]) * (y - p[1]);
                    }
                    err = Math.sqrt(err / pts.size());
                    if (err < best) { best = err; bp = new double[]{z, w, sh}; }
                }
            }
        }
        System.out.printf(Locale.ROOT, "n=%d  damping=%.2f  w=%.1f rad/s  stiffness=%.0f  response=%.3f s  start=%.0f ms  rms=%.4f%n",
            pts.size(), bp[0], bp[1], bp[1] * bp[1], 2 * Math.PI / bp[1], bp[2], best);
    }
}

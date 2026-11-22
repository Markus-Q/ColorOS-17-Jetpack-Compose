import java.nio.file.*;
import java.util.*;

/** Fits a two-edge spring model to a measured switch toggle. args: thumb.csv */
public class FitSwitch {
    static double[] T, CX, W;
    static double step(double t, double z, double w0, double v0) { // spring step response with initial velocity v0 (travel/s), t in s
        if (t <= 0) return 0;
        double x0 = -1, v = v0;
        double e = Math.exp(-z * w0 * t);
        if (z < 0.9999) {
            double wd = w0 * Math.sqrt(1 - z * z);
            double a = x0, b = (v + z * w0 * x0) / wd;
            return 1 + e * (a * Math.cos(wd * t) + b * Math.sin(wd * t));
        } else {
            double b = v + w0 * x0;
            return 1 + e * (x0 + b * t);
        }
    }
    public static void main(String[] a) throws Exception {
        List<double[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            if (p.length < 7) continue;
            double cx = Double.parseDouble(p[1]), w = Double.parseDouble(p[4]);
            if (Double.isNaN(cx)) continue;
            rows.add(new double[]{Double.parseDouble(p[0]), cx, w});
        }
        // windows: toggle 1 OFF->ON (use t>=1565 to avoid track contamination), toggle 2 ON->OFF
        double L0a = 53.0, R0a = 107.0, L1a = 113.4, R1a = 166.8;   // OFF rest -> ON rest
        double[][] win = {{1565, 2100, 1}, {3500, 4100, -1}};
        double best = 1e18; double[] bp = null;
        for (double z = 0.40; z <= 1.0001; z += 0.02)
        for (double w0 = 12; w0 <= 48; w0 += 1)
        for (double tau = 0; tau <= 50; tau += 2)
        for (double v0 = 0; v0 <= 6; v0 += 2) {
            double total = 0; double[] t0s = new double[2];
            for (int k = 0; k < 2; k++) {
                double dir = win[k][2];
                double bestK = 1e18;
                double base = k == 0 ? 1500 : 3530;
                for (double t0 = base - 40; t0 <= base + 40; t0 += 2) {
                    double err = 0; int n = 0;
                    for (double[] r : rows) {
                        if (r[0] < win[k][0] || r[0] > win[k][1]) continue;
                        double Lm = r[1] - r[2] / 2, Rm = r[1] + r[2] / 2;
                        double L0 = dir > 0 ? L0a : L1a, R0 = dir > 0 ? R0a : R1a, L1 = dir > 0 ? L1a : L0a, R1 = dir > 0 ? R1a : R0a;
                        double tt = (r[0] - t0) / 1000.0;
                        double lead = step(tt, z, w0, v0), trail = step(tt - tau / 1000.0, z, w0, v0);
                        double pR, pL;
                        if (dir > 0) { pR = lead; pL = trail; } else { pL = lead; pR = trail; }
                        double Lp = L0 + (L1 - L0) * pL, Rp = R0 + (R1 - R0) * pR;
                        err += (Lp - Lm) * (Lp - Lm) + (Rp - Rm) * (Rp - Rm); n++;
                    }
                    err /= Math.max(1, n);
                    if (err < bestK) { bestK = err; t0s[k] = t0; }
                }
                total += bestK;
            }
            if (total < best) { best = total; bp = new double[]{z, w0, tau, v0, t0s[0], t0s[1]}; }
        }
        System.out.printf(Locale.ROOT, "best rms=%.2f px  zeta=%.2f omega0=%.1f (response %.3fs) tau=%.0fms v0=%.0f t0=%.0f/%.0f%n", Math.sqrt(best / 2), bp[0], bp[1], 2 * Math.PI / bp[1], bp[2], bp[3], bp[4], bp[5]);
    }
}

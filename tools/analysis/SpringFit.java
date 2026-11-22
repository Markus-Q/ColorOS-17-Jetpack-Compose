import java.nio.file.*;
import java.util.*;

public class SpringFit {
    static double step(double t, double z, double w0) {
        if (t <= 0) return 0;
        double e = Math.exp(-z * w0 * t);
        if (z < 0.9999) { double wd = w0 * Math.sqrt(1 - z * z); return 1 - e * (Math.cos(wd * t) + z * w0 / wd * Math.sin(wd * t)); }
        return 1 - e * (1 + w0 * t);
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
        for (double z = 0.5; z <= 1.0001; z += 0.02) for (double w = 8; w <= 70; w += 1) for (double sh = -30; sh <= 20; sh += 1) {
            double err = 0;
            for (double[] p : pts) { double y = step((p[0] - start - sh) / 1000.0, z, w); err += (y - p[1]) * (y - p[1]); }
            err = Math.sqrt(err / pts.size());
            if (err < best) { best = err; bp = new double[]{z, w, sh}; }
        }
        System.out.printf(Locale.ROOT, "n=%d spring rms=%.4f zeta=%.2f omega=%.0f (response %.3fs) shift=%.0fms%n", pts.size(), best, bp[0], bp[1], 2 * Math.PI / bp[1], bp[2]);
        StringBuilder sb = new StringBuilder();
        for (double[] p : pts) sb.append(String.format(Locale.ROOT, "%.0f:%.2f/%.2f ", p[0] - start, p[1], step((p[0] - start - bp[2]) / 1000.0, bp[0], bp[1])));
        System.out.println(sb);
    }
}

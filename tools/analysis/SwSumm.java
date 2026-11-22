import java.nio.file.*;
import java.util.*;

/** Summarizes switch toggles from a rec2 thumb csv: t,cx,cy,area,w,h,... */
public class SwSumm {
    public static void main(String[] a) throws Exception {
        List<double[]> r = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            if (p.length < 6) continue;
            double cx = Double.parseDouble(p[1]);
            if (Double.isNaN(cx)) continue;
            r.add(new double[]{Double.parseDouble(p[0]), cx, Double.parseDouble(p[4])});
        }
        int i = 0;
        while (i < r.size() - 1) {
            // find start of a toggle: cx moves more than 1.5px from the rest value within 60 ms
            int s = -1;
            for (int k = i + 1; k < r.size(); k++) {
                if (Math.abs(r.get(k)[1] - r.get(k - 1)[1]) > 1.5 && Math.abs(r.get(k)[1] - r.get(i)[1]) > 1.5) { s = k - 1; break; }
            }
            if (s < 0) break;
            double x0 = r.get(s)[1], t0 = r.get(s)[0];
            // end: last sample within 700 ms where cx still differs from final
            int e = s;
            while (e < r.size() - 1 && r.get(e)[0] - t0 < 900) e++;
            double xf = r.get(e)[1];
            double dir = Math.signum(xf - x0), travel = Math.abs(xf - x0);
            if (travel < 20) { i = s + 1; continue; }
            double peakW = 0, tPeakW = 0, minW = 1e9, tMinW = 0, over = 0, tOver = 0, wRest = r.get(e)[2];
            double t10 = -1, t50 = -1, t90 = -1, tSettle = -1;
            for (int k = s; k <= e; k++) {
                double t = r.get(k)[0] - t0, p = (r.get(k)[1] - x0) / (xf - x0), w = r.get(k)[2];
                if (w > peakW) { peakW = w; tPeakW = t; }
                if (p > 1.0 && (p - 1) * travel > over) { over = (p - 1) * travel; tOver = t; }
                if (t10 < 0 && p >= 0.1) t10 = t;
                if (t50 < 0 && p >= 0.5) t50 = t;
                if (t90 < 0 && p >= 0.9) t90 = t;
            }
            for (int k = s; k <= e; k++) { double t = r.get(k)[0] - t0; if (t > tPeakW + 50 && r.get(k)[2] < minW) { minW = r.get(k)[2]; tMinW = t; } }
            for (int k = e; k > s; k--) if (Math.abs(r.get(k)[1] - xf) > 0.5) { tSettle = r.get(k)[0] - t0; break; }
            System.out.printf(Locale.ROOT, "toggle at %.0fms dir=%+.0f travel=%.1fpx | t10=%.0f t50=%.0f t90=%.0f overshoot=%.1fpx(%.1f%%) at %.0fms settle=%.0fms | width rest=%.1f peak=%.1f at %.0fms min=%.1f at %.0fms%n",
                t0, dir, travel, t10, t50, t90, over, 100 * over / travel, tOver, tSettle, wRest, peakW, tPeakW, minW, tMinW);
            i = e;
        }
    }
}

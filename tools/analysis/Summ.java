import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Summarizes a press / release measurement taken by rec.sh.
 * usage: java Summ.java file.csv [column=6 (width) | 7 (height)] [minHoldMs=300]
 * Prints base value, pressed plateau, press timing, release peak and return timing.
 */
public class Summ {
    public static void main(String[] a) throws Exception {
        int col = a.length > 1 ? Integer.parseInt(a[1]) : 6;
        List<double[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            if (p.length < 8) continue;
            double v = Double.parseDouble(p[col]);
            if (Double.isNaN(v)) continue;
            double r = p.length > 12 ? Double.parseDouble(p[10]) : 0;
            double g = p.length > 12 ? Double.parseDouble(p[11]) : 0;
            double b = p.length > 12 ? Double.parseDouble(p[12]) : 0;
            rows.add(new double[]{Double.parseDouble(p[0]), v, r, g, b});
        }
        int n = rows.size();
        double[] first = new double[Math.min(4, n)];
        for (int i = 0; i < first.length; i++) first[i] = rows.get(i)[1];
        Arrays.sort(first);
        double base = first[first.length / 2];
        // global minimum = pressed plateau
        double vmin = 1e9;
        int imin = 0;
        for (int i = 0; i < n; i++) if (rows.get(i)[1] < vmin) { vmin = rows.get(i)[1]; imin = i; }
        // plateau: median of samples within 0.4 px of the minimum
        List<Double> pl = new ArrayList<>();
        for (double[] r : rows) if (r[1] < vmin + 0.4) pl.add(r[1]);
        Collections.sort(pl);
        double plateau = pl.get(pl.size() / 2);
        double drop = base - plateau;
        // press start: first sample deviating from base by more than 8% of the drop
        int iStart = 0;
        for (int i = 1; i < n; i++) if (base - rows.get(i)[1] > 0.08 * drop) { iStart = i - 1; break; }
        double tStart = rows.get(iStart)[0];
        double t50 = Double.NaN, t90 = Double.NaN;
        for (int i = iStart; i < n; i++) {
            double f = (base - rows.get(i)[1]) / drop;
            if (Double.isNaN(t50) && f >= 0.5) t50 = rows.get(i)[0] - tStart;
            if (Double.isNaN(t90) && f >= 0.9) t90 = rows.get(i)[0] - tStart;
        }
        // plateau start/end: first and last sample within 0.4 px of plateau
        int pStart = -1, pEnd = -1;
        for (int i = 0; i < n; i++) if (Math.abs(rows.get(i)[1] - plateau) < 0.5) { if (pStart < 0) pStart = i; pEnd = i; }
        // release start: first sample after pEnd that leaves the plateau by > 8% of drop
        int iRel = Math.min(pEnd, n - 1);
        double tRel = rows.get(iRel)[0];
        double peak = -1e9;
        double tPeak = Double.NaN;
        for (int i = iRel; i < n; i++) if (rows.get(i)[1] > peak) { peak = rows.get(i)[1]; tPeak = rows.get(i)[0] - tRel; }
        double tBack = Double.NaN;
        boolean passedBase = false;
        for (int i = iRel; i < n; i++) {
            double v = rows.get(i)[1];
            if (v >= base - 0.3) passedBase = true;
            if (passedBase && rows.get(i)[0] - tRel > tPeak && v <= base + 0.4 && Double.isNaN(tBack)) tBack = rows.get(i)[0] - tRel;
        }
        double t50r = Double.NaN, t100r = Double.NaN;
        for (int i = iRel; i < n; i++) {
            double f = (rows.get(i)[1] - plateau) / drop;
            if (Double.isNaN(t50r) && f >= 0.5) t50r = rows.get(i)[0] - tRel;
            if (Double.isNaN(t100r) && f >= 0.97) t100r = rows.get(i)[0] - tRel;
        }
        double[] c0 = rows.get(0), cp = rows.get(pStart >= 0 ? (pStart + pEnd) / 2 : 0);
        System.out.printf(Locale.ROOT,
            "base=%.2f plateau=%.2f scale=%.4f | press: start=%.0fms t50=%.0fms t90=%.0fms | release(from plateau end): t50=%.0fms t97=%.0fms peak=%.2f (%.4f) at %.0fms back-to-base=%.0fms | color base=%d,%d,%d pressed=%d,%d,%d%n",
            base, plateau, plateau / base, tStart, t50, t90, t50r, t100r, peak, peak / base, tPeak, tBack,
            (int) c0[2], (int) c0[3], (int) c0[4], (int) cp[2], (int) cp[3], (int) cp[4]);
    }
}

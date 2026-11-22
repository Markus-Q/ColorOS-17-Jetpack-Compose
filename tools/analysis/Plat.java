import java.nio.file.*;
import java.util.*;

/**
 * Finds plateaus (stable segments) in a rec.sh measurement.
 * usage: java Plat.java file.csv [tolPx=0.3] [minMs=300] [mask=whxy]
 * mask selects which of width(w), height(h), cx(x), cy(y) must be stable. All four are printed.
 */
public class Plat {
    public static void main(String[] a) throws Exception {
        double tol = a.length > 1 ? Double.parseDouble(a[1]) : 0.3;
        double minMs = a.length > 2 ? Double.parseDouble(a[2]) : 300;
        String mask = a.length > 3 ? a[3] : "whxy";
        boolean[] use = {mask.contains("w"), mask.contains("h"), mask.contains("x"), mask.contains("y")};
        List<double[]> r = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(a[0]))) {
            String[] p = line.split(",");
            if (p.length < 10) continue;
            double w = Double.parseDouble(p[6]), h = Double.parseDouble(p[7]);
            double cx = Double.parseDouble(p[8]), cy = Double.parseDouble(p[9]);
            if (Double.isNaN(w) || Double.isNaN(h)) continue;
            r.add(new double[]{Double.parseDouble(p[0]), w, h, cx, cy});
        }
        int i = 0;
        while (i < r.size()) {
            int j = i;
            double[] mn = {1e9, 1e9, 1e9, 1e9}, mx = {-1e9, -1e9, -1e9, -1e9};
            while (j < r.size()) {
                boolean ok = true;
                double[] nmn = mn.clone(), nmx = mx.clone();
                for (int k = 0; k < 4; k++) {
                    double v = r.get(j)[k + 1];
                    nmn[k] = Math.min(nmn[k], v);
                    nmx[k] = Math.max(nmx[k], v);
                    if (use[k] && nmx[k] - nmn[k] > tol) ok = false;
                }
                if (!ok) break;
                mn = nmn;
                mx = nmx;
                j++;
            }
            double dur = r.get(Math.max(i, j - 1))[0] - r.get(i)[0];
            if (dur >= minMs) {
                double[] s = new double[4];
                int n = 0;
                for (int k = i; k < j; k++) { for (int c = 0; c < 4; c++) s[c] += r.get(k)[c + 1]; n++; }
                System.out.printf(Locale.ROOT, "plateau %7.0f-%7.0f ms  w=%7.2f h=%7.2f cx=%8.2f cy=%8.2f%n",
                    r.get(i)[0], r.get(j - 1)[0], s[0] / n, s[1] / n, s[2] / n, s[3] / n);
            }
            i = Math.max(j, i + 1);
        }
    }
}

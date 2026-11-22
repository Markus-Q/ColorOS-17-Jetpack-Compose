import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Checks every relative link, image and anchor in README.md and docs/**.md. Run from the repository root:
 * <pre>java tools/docs/LinkCheck.java</pre>
 * (or tools/linkcheck.sh). Prints the broken ones and exits with 1 if there are any. Anchors are computed the way GitHub
 * does it (lower case, punctuation removed, spaces to hyphens, repeated headings numbered), explicit anchors
 * (id="..." / name="...") count too. External links are not followed.
 */
public class LinkCheck {
    static final Path ROOT = Path.of("").toAbsolutePath();
    static final Pattern MD_LINK = Pattern.compile("!?\\[[^\\]]*\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    static final Pattern HTML_LINK = Pattern.compile("(?:href|src)=\"([^\"]+)\"");
    static final Map<Path, Set<String>> anchorCache = new HashMap<>();

    static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8).replace("\r\n", "\n"); }

    static String slug(String heading) {
        String s = heading.replaceAll("<[^>]+>", "").replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1").replace("`", "").replace("*", "");
        return s.strip().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} _-]", "").replace(' ', '-');
    }

    static Set<String> anchors(Path file) throws IOException {
        Set<String> cached = anchorCache.get(file);
        if (cached != null) return cached;
        Set<String> out = new HashSet<>();
        Map<String, Integer> seen = new HashMap<>();
        boolean fence = false;
        String text = read(file);
        for (String line : text.split("\n")) {
            if (line.startsWith("```")) { fence = !fence; continue; }
            if (fence) continue;
            Matcher h = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*$").matcher(line);
            if (h.find()) {
                String s = slug(h.group(2));
                int n = seen.merge(s, 1, Integer::sum) - 1;
                out.add(n == 0 ? s : s + "-" + n);
            }
        }
        Matcher a = Pattern.compile("(?:id|name)=\"([^\"]+)\"").matcher(text);
        while (a.find()) out.add(a.group(1));
        anchorCache.put(file, out);
        return out;
    }

    public static void main(String[] args) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String f : new String[]{"README.md"}) if (Files.exists(ROOT.resolve(f))) files.add(ROOT.resolve(f));
        try (Stream<Path> ps = Files.walk(ROOT.resolve("docs"))) {
            ps.filter(p -> p.toString().endsWith(".md")).sorted().forEach(files::add);
        }
        int checked = 0, broken = 0;
        for (Path file : files) {
            String text = read(file);
            // drop fenced code blocks, links inside code are not links
            StringBuilder sb = new StringBuilder();
            boolean fence = false;
            for (String line : text.split("\n", -1)) {
                if (line.startsWith("```")) { fence = !fence; continue; }
                if (!fence) sb.append(line.replaceAll("`[^`]*`", "")).append('\n');
            }
            Set<String> targets = new LinkedHashSet<>();
            for (Pattern p : new Pattern[]{MD_LINK, HTML_LINK}) {
                Matcher m = p.matcher(sb);
                while (m.find()) targets.add(m.group(1));
            }
            for (String t : targets) {
                if (t.startsWith("http://") || t.startsWith("https://") || t.startsWith("mailto:")) continue;
                checked++;
                String path = t, anchor = null;
                int hash = t.indexOf('#');
                if (hash >= 0) { path = t.substring(0, hash); anchor = t.substring(hash + 1); }
                Path target = path.isEmpty() ? file : file.getParent().resolve(path.replace("%20", " ")).normalize();
                String where = ROOT.relativize(file).toString().replace('\\', '/');
                if (!Files.exists(target)) { System.out.println(where + ": missing file " + t); broken++; continue; }
                if (anchor != null && !anchor.isEmpty() && target.toString().endsWith(".md")) {
                    if (anchor.startsWith("L") && anchor.substring(1).matches("\\d+")) continue;
                    if (!anchors(target).contains(anchor)) { System.out.println(where + ": missing anchor " + t); broken++; }
                } else if (anchor != null && anchor.matches("L\\d+")) {
                    // line anchor into a source file: only the file has to exist
                }
            }
        }
        System.out.println(checked + " relative links checked, " + broken + " broken");
        if (broken > 0) System.exit(1);
    }
}

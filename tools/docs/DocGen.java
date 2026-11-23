import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * Generates reference documentation from the source tree. Run it from the repository root:
 * <pre>java tools/docs/DocGen.java</pre>
 * (or tools/docs.sh). Output, all of it overwritten on every run:
 * <ul>
 *   <li>docs/api/README.md: the module graph and an index of every public declaration</li>
 *   <li>docs/api/MODULE.md: every public declaration of one module with its KDoc, its signature and a link to the source line</li>
 *   <li>docs/catalog.md: every catalog screen with its description, the screenshots in docs/media/catalog/{dark,light},
 *       the kit APIs the demo calls and, where one exists, a recording</li>
 * </ul>
 * The parser is deliberately small: it reads top-level declarations at column 0 and members at four spaces of indentation,
 * which is how this code base is formatted.
 */
public class DocGen {
    static final Path ROOT = Path.of("").toAbsolutePath();
    static final Set<String> MODS = Set.of("public", "protected", "private", "internal", "inline", "noinline", "crossinline",
        "operator", "infix", "tailrec", "suspend", "data", "enum", "sealed", "annotation", "value", "abstract", "open",
        "const", "lateinit", "expect", "actual", "external", "override");
    static final Set<String> KEYWORDS = Set.of("fun", "class", "object", "interface", "val", "var", "typealias");
    static final Pattern ANNOT = Pattern.compile("^@[\\w.]+(?:\\([^)]*\\))?\\s*");
    /** Kit names too common to be worth listing as "used by" a demo. */
    static final Set<String> NOT_LISTED = Set.of("IrisTheme", "IrisIcons", "IrisText");

    record Module(String name, String description, List<String> apiDeps, List<String> implDeps) {}
    record Member(String summary, String header) {}
    record Doc(String summary, String body, List<String[]> params, String returns) {
        static final Doc EMPTY = new Doc("", "", List.of(), null);
    }
    record Decl(String module, String file, int line, String kind, String name, String receiver, Doc doc,
                List<String> annotations, String header, List<Member> members) {
        String display() { return receiver == null ? name : receiver + "." + name; }
    }

    // ------------------------------------------------------------------ modules

    static List<Module> readModules() throws IOException {
        List<Module> out = new ArrayList<>();
        Matcher m = Pattern.compile("include\\(\":(iris-[\\w-]+)\"\\)\\s*//\\s*(.*)").matcher(read(ROOT.resolve("settings.gradle.kts")));
        while (m.find()) {
            String name = m.group(1);
            String gradle = read(ROOT.resolve(name).resolve("build.gradle.kts"));
            List<String> api = new ArrayList<>(), impl = new ArrayList<>();
            Matcher d = Pattern.compile("(api|implementation)\\(project\\(\":(iris-[\\w-]+)\"\\)\\)").matcher(gradle);
            while (d.find()) (d.group(1).equals("api") ? api : impl).add(d.group(2));
            out.add(new Module(name, m.group(2).trim(), api, impl));
        }
        return out;
    }

    // ------------------------------------------------------------------ source parsing

    static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8).replace("\r\n", "\n"); }

    static int skipString(String t, int i) {
        if (t.startsWith("\"\"\"", i)) {
            int e = t.indexOf("\"\"\"", i + 3);
            return e < 0 ? t.length() : e + 2;
        }
        for (int j = i + 1; j < t.length(); j++) {
            char c = t.charAt(j);
            if (c == '\\') j++;
            else if (c == '"') return j;
        }
        return t.length();
    }

    /** Index just behind the bracket that closes the one at [open], or -1. Skips strings and comments. */
    static int skipBalanced(String t, int open, char o, char c) {
        int depth = 0;
        for (int i = open; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '"') { i = skipString(t, i); continue; }
            if (ch == '/' && i + 1 < t.length()) {
                if (t.charAt(i + 1) == '/') { int e = t.indexOf('\n', i); i = e < 0 ? t.length() : e; continue; }
                if (t.charAt(i + 1) == '*') { int e = t.indexOf("*/", i + 2); i = e < 0 ? t.length() : e + 1; continue; }
            }
            if (ch == o) depth++;
            else if (ch == c && --depth == 0) return i + 1;
        }
        return -1;
    }

    static String joinLines(List<String> lines, int from, int max) {
        return String.join("\n", lines.subList(from, Math.min(lines.size(), from + max)));
    }

    /** The header of a function: everything up to the body ({ or =), the parameter list may span lines. */
    static String funHeader(String text) {
        int p = text.indexOf('(');
        if (p < 0) return firstLine(text);
        int e = skipBalanced(text, p, '(', ')');
        if (e < 0) return firstLine(text);
        int end = e;
        while (end < text.length() && text.charAt(end) != '{' && text.charAt(end) != '=' && text.charAt(end) != '\n') end++;
        return text.substring(0, end).stripTrailing();
    }

    static String firstLine(String s) { int n = s.indexOf('\n'); return (n < 0 ? s : s.substring(0, n)).stripTrailing(); }

    /** Result of scanning a class-like header: the header text and the index of its opening brace, or -1. */
    record ClassHeader(String header, int braceAt) {}

    static ClassHeader classHeader(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') { i = skipString(text, i); continue; }
            if (ch == '(') depth++;
            else if (ch == ')') depth--;
            else if (depth == 0 && ch == '{') return new ClassHeader(text.substring(0, i).stripTrailing(), i);
            else if (depth == 0 && ch == '\n') {
                String sofar = text.substring(0, i).stripTrailing();
                if (!(sofar.endsWith(":") || sofar.endsWith(",") || sofar.endsWith("="))) return new ClassHeader(sofar, -1);
            }
        }
        return new ClassHeader(text.stripTrailing(), -1);
    }

    static String stripComments(String s) {
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    /** Enum entries written one per line, without the members behind the first semicolon. */
    static String enumBlock(String header, String body) {
        String b = stripComments(body);
        int depth = 0, end = b.length();
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            if (c == '(' || c == '{') depth++;
            else if (c == ')' || c == '}') depth--;
            else if (c == ';' && depth == 0) { end = i; break; }
        }
        b = b.substring(0, end);
        List<String> entries = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        depth = 0;
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            if (c == '(' || c == '{') depth++;
            else if (c == ')' || c == '}') depth--;
            if (c == ',' && depth == 0) { entries.add(cur.toString().strip()); cur.setLength(0); }
            else cur.append(c);
        }
        entries.add(cur.toString().strip());
        StringBuilder sb = new StringBuilder(header).append(" {\n");
        for (String e : entries) if (!e.isEmpty()) sb.append("    ").append(e.replaceAll("\\s+", " ")).append(",\n");
        return sb.append("}").toString();
    }

    static Doc parseDoc(String raw) {
        if (raw == null) return Doc.EMPTY;
        String t = raw.strip();
        t = t.replaceFirst("^/\\*\\*", "").replaceFirst("\\*/$", "");
        StringBuilder body = new StringBuilder();
        List<String[]> params = new ArrayList<>();
        String returns = null;
        String tag = null, tagName = null;
        StringBuilder cur = new StringBuilder();
        List<Runnable> none = new ArrayList<>();
        for (String l : t.split("\n")) {
            l = l.strip();
            if (l.startsWith("*")) l = l.substring(1);
            if (l.startsWith(" ")) l = l.substring(1);
            l = l.stripTrailing();
            Matcher m = Pattern.compile("^@(param|property|return|see|throws|sample)\\b\\s*(.*)$").matcher(l.strip());
            if (m.find() && l.strip().startsWith("@")) {
                if (tag != null) { if (tag.equals("param") || tag.equals("property")) params.add(new String[]{tagName, cur.toString().strip()}); else if (tag.equals("return")) returns = cur.toString().strip(); }
                tag = m.group(1); cur.setLength(0);
                String rest = m.group(2);
                if (tag.equals("param") || tag.equals("property")) {
                    Matcher n = Pattern.compile("^(\\w+)\\s*(.*)$").matcher(rest);
                    if (n.find()) { tagName = n.group(1); cur.append(n.group(2)); }
                } else cur.append(rest);
            } else if (tag != null) {
                cur.append(' ').append(l.strip());
            } else {
                body.append(l).append('\n');
            }
        }
        if (tag != null) { if (tag.equals("param") || tag.equals("property")) params.add(new String[]{tagName, cur.toString().strip()}); else if (tag.equals("return")) returns = cur.toString().strip(); }
        String b = links(body.toString().strip());
        List<String[]> ps = new ArrayList<>();
        for (String[] p : params) ps.add(new String[]{p[0], links(p[1])});
        return new Doc(summary(b), b, ps, returns == null ? null : links(returns));
    }

    /** KDoc links ([Name], [text][Name]) become code spans or plain text. */
    static String links(String s) {
        s = s.replaceAll("\\[([^\\]]+)\\]\\[[^\\]]+\\]", "$1");
        s = s.replaceAll("\\[([\\w.]+)\\](?!\\()", "`$1`");
        return s;
    }

    static String summary(String body) {
        if (body.isBlank()) return "";
        String para = body.split("\n\\s*\n", 2)[0].replace('\n', ' ').replaceAll("\\s+", " ").strip();
        Matcher m = Pattern.compile("^(.*?[.!?])(\\s|$)").matcher(para);
        return m.find() ? m.group(1) : para;
    }

    static List<Decl> parseFile(Path file, String module) throws IOException {
        List<String> lines = Arrays.asList(read(file).split("\n", -1));
        String rel = ROOT.relativize(file).toString().replace('\\', '/');
        List<Decl> out = new ArrayList<>();
        String doc = null;
        List<String> anns = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("/**")) {
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < lines.size()) { sb.append(lines.get(j)).append('\n'); if (lines.get(j).contains("*/")) break; j++; }
                doc = sb.toString(); anns.clear(); i = j; continue;
            }
            if (line.isBlank()) { doc = null; anns.clear(); continue; }
            if (line.startsWith("//") || line.startsWith("/*")) continue;
            if (Character.isWhitespace(line.charAt(0))) continue;
            String rest = line;
            List<String> myAnns = new ArrayList<>(anns);
            Matcher am;
            while ((am = ANNOT.matcher(rest)).find()) { myAnns.add(am.group().strip()); rest = rest.substring(am.end()); }
            if (rest.isEmpty()) { anns = myAnns; continue; }
            Decl d = parseDecl(lines, i, rest, myAnns, doc, module, rel, true);
            if (d != null) out.add(d);
            doc = null; anns = new ArrayList<>();
        }
        return out;
    }

    static Decl parseDecl(List<String> lines, int i, String rest, List<String> anns, String rawDoc, String module, String rel, boolean top) {
        Matcher tok = Pattern.compile("\\S+").matcher(rest);
        boolean hidden = false, isEnum = false;
        String kw = null;
        int kwEnd = 0;
        while (tok.find()) {
            String t = tok.group();
            if (t.equals("private") || t.equals("internal") || t.equals("protected") || (!top && t.equals("override"))) hidden = true;
            if (t.equals("enum")) isEnum = true;
            if (KEYWORDS.contains(t)) {
                if (t.equals("fun") && rest.startsWith("interface", tok.end() + 1)) continue;
                kw = t; kwEnd = tok.end(); break;
            }
            if (!MODS.contains(t)) return null;
        }
        if (kw == null || hidden) return null;
        String after = rest.substring(kwEnd).stripLeading();
        String name, receiver = null, kind = kw;
        boolean classLike = kw.equals("class") || kw.equals("object") || kw.equals("interface");
        String text = rest + "\n" + joinLines(lines, i + 1, classLike ? lines.size() : 60);   // a class body can be long
        String header;
        List<Member> members = new ArrayList<>();
        if (kw.equals("fun")) {
            if (after.startsWith("<")) { int e = skipBalanced(after, 0, '<', '>'); if (e > 0) after = after.substring(e).stripLeading(); }
            int p = after.indexOf('(');
            if (p < 0) return null;
            String seg = after.substring(0, p).strip();
            int dot = -1, ang = 0;
            for (int k = 0; k < seg.length(); k++) {
                char c = seg.charAt(k);
                if (c == '<') ang++; else if (c == '>') ang--; else if (c == '.' && ang == 0) dot = k;
            }
            name = dot < 0 ? seg : seg.substring(dot + 1);
            receiver = dot < 0 ? null : seg.substring(0, dot);
            header = funHeader(text);
        } else {
            Matcher n = Pattern.compile("^(?:[\\w<>?,. ]+\\.)?(\\w+)").matcher(after);
            if (!n.find()) return null;
            name = n.group(1);
            if (kw.equals("class") || kw.equals("object") || kw.equals("interface")) {
                if (isEnum) kind = "enum";
                ClassHeader ch = classHeader(text);
                header = ch.header();
                if (ch.braceAt() >= 0) {
                    int close = skipBalanced(text, ch.braceAt(), '{', '}');
                    String body = close < 0 ? "" : text.substring(ch.braceAt() + 1, close - 1);
                    if (isEnum) header = enumBlock(header, body);
                    else members = members(body);
                }
            } else {
                header = valHeader(rest);
            }
        }
        Doc doc = parseDoc(rawDoc);
        return new Decl(module, rel, i + 1, kind, name, receiver, doc, anns, header, members);
    }

    static String valHeader(String line) {
        String h = line;
        for (String cut : new String[]{" =", " by ", " get()"}) { int k = h.indexOf(cut); if (k > 0) h = h.substring(0, k); }
        return h.strip();
    }

    /** Public members at one level of indentation (four spaces) of a class-like body. */
    static List<Member> members(String body) {
        List<String> ls = Arrays.asList(body.split("\n", -1));
        List<Member> out = new ArrayList<>();
        String doc = null;
        for (int i = 0; i < ls.size(); i++) {
            String line = ls.get(i);
            if (line.startsWith("    /**") && !line.startsWith("     ")) {
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < ls.size()) { sb.append(ls.get(j).strip()).append('\n'); if (ls.get(j).contains("*/")) break; j++; }
                doc = sb.toString(); i = j; continue;
            }
            if (!line.startsWith("    ") || line.startsWith("     ") || line.isBlank()) { if (line.isBlank()) doc = null; continue; }
            String rest = line.strip();
            if (rest.startsWith("//") || rest.startsWith("}")) continue;
            Matcher am;
            while ((am = ANNOT.matcher(rest)).find()) rest = rest.substring(am.end());
            if (rest.isEmpty()) continue;
            Decl d = parseDecl(ls, i, rest, List.of(), null, "", "", false);
            if (d != null && !d.name().equals("Companion")) {
                String h = d.kind().equals("fun") ? d.header() : (d.kind().equals("val") || d.kind().equals("var")) ? valHeader(rest) : firstLine(d.header());
                out.add(new Member(parseDoc(doc).summary(), h.strip()));   // continuation lines keep their own indentation
            }
            doc = null;
        }
        return out;
    }

    // ------------------------------------------------------------------ markdown helpers

    static String slug(String heading, Map<String, Integer> seen) {
        String s = heading.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} _-]", "").replace(' ', '-');
        int n = seen.merge(s, 1, Integer::sum) - 1;
        return n == 0 ? s : s + "-" + n;
    }

    /** Escapes < and > outside of code spans. */
    static String esc(String s) {
        String[] parts = s.split("`", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('`');
            sb.append(i % 2 == 0 ? parts[i].replace("<", "&lt;").replace(">", "&gt;") : parts[i]);
        }
        return sb.toString();
    }

    static String cell(String s) { return esc(s).replace("|", "\\|").replace("\n", " "); }

    static void write(String relative, String text) throws IOException {
        Path p = ROOT.resolve(relative);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text.replace("\r\n", "\n"), StandardCharsets.UTF_8);
        System.out.println("wrote " + relative + " (" + text.length() / 1024 + " KB)");
    }

    // ------------------------------------------------------------------ API pages

    static Map<String, String> apiAnchors = new HashMap<>();   // declaration name -> "api/module.md#slug"

    static void writeApi(List<Module> modules, Map<String, List<Decl>> byModule) throws IOException {
        StringBuilder index = new StringBuilder();
        index.append("# API reference\n\n");
        index.append("Generated from the source and its KDoc by `tools/docs/DocGen.java`: do not edit by hand, change the KDoc in the source and run `tools/docs.sh`.\n");
        index.append("Every public declaration of every library module is listed, with its signature and a link to the line it is declared on. ");
        index.append("The [catalog tour](../catalog.md) shows what each of them looks like, the guides in [docs](../README.md) explain how they fit together.\n\n");
        index.append("## Modules\n\n```mermaid\ngraph LR\n");
        for (Module m : modules) {
            for (String d : m.apiDeps()) index.append("    ").append(id(m.name())).append(" --> ").append(id(d)).append('\n');
            for (String d : m.implDeps()) index.append("    ").append(id(m.name())).append(" -.-> ").append(id(d)).append('\n');
        }
        index.append("```\n\nSolid arrows are `api` dependencies (their types appear in the public API), dotted ones are `implementation` dependencies.\n");
        index.append("`iris-ui` is the umbrella: depend on it for the whole kit, or on single modules.\n\n");
        for (Module m : modules) {
            List<Decl> decls = byModule.getOrDefault(m.name(), List.of());
            if (decls.isEmpty()) continue;
            Map<String, Integer> seen = new HashMap<>();
            StringBuilder page = new StringBuilder();
            slug(m.name(), seen);
            page.append("# ").append(m.name()).append("\n\n");
            page.append("Package `").append(packageOf(decls.get(0))).append("`. ").append(esc(cap(m.description()))).append(".\n\n");
            page.append("[All modules](README.md)\n\n");
            Map<String, List<Decl>> byFile = new TreeMap<>();
            for (Decl d : decls) byFile.computeIfAbsent(d.file(), k -> new ArrayList<>()).add(d);
            for (Map.Entry<String, List<Decl>> f : byFile.entrySet()) {
                String fileName = f.getKey().substring(f.getKey().lastIndexOf('/') + 1);
                slug(fileName, seen);
                page.append("## ").append(fileName).append("\n\n");
                for (Decl d : f.getValue()) {
                    String s = slug(d.display(), seen);
                    apiAnchors.putIfAbsent(d.name(), "api/" + m.name() + ".md#" + s);
                    d.members();
                    page.append("### ").append(d.display()).append("\n\n");
                    if (!d.doc().body().isBlank()) page.append(esc(d.doc().body())).append("\n\n");
                    if (!d.doc().params().isEmpty()) {
                        page.append("**Parameters**\n\n");
                        for (String[] p : d.doc().params()) page.append("- `").append(p[0]).append("`: ").append(esc(p[1])).append('\n');
                        page.append('\n');
                    }
                    if (d.doc().returns() != null) page.append("**Returns** ").append(esc(d.doc().returns())).append("\n\n");
                    page.append("```kotlin\n");
                    for (String a : d.annotations()) page.append(a).append('\n');
                    page.append(d.header());
                    if (!d.members().isEmpty()) {
                        page.append(" {\n");
                        for (Member mem : d.members()) {
                            if (!mem.summary().isEmpty()) page.append("    // ").append(mem.summary()).append('\n');
                            page.append("    ").append(mem.header()).append('\n');
                        }
                        page.append("}");
                    }
                    page.append("\n```\n\n");
                    page.append("<sub>Source: [").append(fileName).append(":").append(d.line()).append("](../../").append(d.file()).append("#L").append(d.line()).append(")</sub>\n\n");
                }
            }
            write("docs/api/" + m.name() + ".md", page.toString());
        }
        for (Module m : modules) {
            List<Decl> decls = byModule.getOrDefault(m.name(), List.of());
            if (decls.isEmpty()) continue;
            index.append("## [").append(m.name()).append("](").append(m.name()).append(".md)\n\n");
            index.append(esc(cap(m.description()))).append(". Package `").append(packageOf(decls.get(0))).append("`.\n\n");
            index.append("| Declaration | What it is |\n| --- | --- |\n");
            Map<String, Integer> seen = new HashMap<>();
            slug(m.name(), seen);
            Set<String> files = new TreeSet<>();
            for (Decl d : decls) files.add(d.file());
            Map<String, List<Decl>> byFile = new TreeMap<>();
            for (Decl d : decls) byFile.computeIfAbsent(d.file(), k -> new ArrayList<>()).add(d);
            for (Map.Entry<String, List<Decl>> f : byFile.entrySet()) {
                slug(f.getKey().substring(f.getKey().lastIndexOf('/') + 1), seen);
                for (Decl d : f.getValue()) {
                    String s = slug(d.display(), seen);
                    index.append("| [`").append(d.display()).append("`](").append(m.name()).append(".md#").append(s).append(") | ").append(cell(d.doc().summary())).append(" |\n");
                }
            }
            index.append('\n');
        }
        write("docs/api/README.md", index.toString());
    }

    static String id(String module) { return module.replace("-", "_"); }
    static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    static String packageOf(Decl d) {
        try {
            Matcher m = Pattern.compile("(?m)^package\\s+(\\S+)").matcher(read(ROOT.resolve(d.file())));
            return m.find() ? m.group(1) : "";
        } catch (IOException e) { return ""; }
    }

    // ------------------------------------------------------------------ catalog tour

    static final Map<String, String[]> RECORDINGS = new LinkedHashMap<>();
    static {
        RECORDINGS.put("BottomNavPill", new String[]{"bottom-nav-pill-closeup.gif", "bottom-nav-pill.gif"});
        RECORDINGS.put("TopBar", new String[]{"top-bar.gif"});
        RECORDINGS.put("PrimaryButton", new String[]{"primary-button.gif"});
        RECORDINGS.put("SecondaryButton", new String[]{"secondary-button.gif"});
        RECORDINGS.put("RoundPlusButton", new String[]{"round-plus-button.gif"});
        RECORDINGS.put("TextButton", new String[]{"text-button.gif"});
        RECORDINGS.put("TextField", new String[]{"text-field.gif"});
        RECORDINGS.put("SearchBar", new String[]{"search-bar.gif"});
        RECORDINGS.put("Card", new String[]{"card.gif"});
        RECORDINGS.put("ListItem", new String[]{"list-item.gif"});
        RECORDINGS.put("GroupedList", new String[]{"grouped-list.gif"});
        RECORDINGS.put("SectionHeader", new String[]{"section-header.gif"});
        RECORDINGS.put("Divider", new String[]{"divider.gif"});
        RECORDINGS.put("Badge", new String[]{"badge.gif"});
        RECORDINGS.put("BlurLab", new String[]{"blur-lab.gif"});
        RECORDINGS.put("Haptics", new String[]{"haptics.gif"});
        RECORDINGS.put("PersistentGlow", new String[]{"persistent-glow.gif"});
        RECORDINGS.put("LargeTitle", new String[]{"large-title.gif"});
        RECORDINGS.put("SegmentedTabs", new String[]{"segmented-tabs.gif"});
        RECORDINGS.put("PageTransitions", new String[]{"page-back-swipe.gif"});
        RECORDINGS.put("Switch", new String[]{"switch.gif"});
        RECORDINGS.put("Slider", new String[]{"slider.gif"});
        RECORDINGS.put("Dialog", new String[]{"dialog.gif"});
        RECORDINGS.put("BottomSheet", new String[]{"bottom-sheet.gif"});
        RECORDINGS.put("PopupMenu", new String[]{"popup-menu.gif"});
        RECORDINGS.put("PressAnimation", new String[]{"press-animation.gif"});
        RECORDINGS.put("Checkbox", new String[]{"checkbox.gif"});
        RECORDINGS.put("TouchGlow", new String[]{"touch-glow.gif"});
        RECORDINGS.put("Progress", new String[]{"progress.gif"});
        RECORDINGS.put("DragPhysics", new String[]{"drag-physics.gif", "drag-free-reorder.gif"});
    }

    /** Descriptions for the screens that do not give their page a subtitle. */
    static final Map<String, String> FALLBACK = Map.of(
        "BottomNavPill", "Tap, or slide a finger across the pill",
        "TopBar", "Glass top bar with a back button, a centred title and actions",
        "LargeTitle", "A large title that collapses into the top bar while the list scrolls under it",
        "BlurLab", "Tune a glass material over any background and read its numbers",
        "HabitsExample", "A tracker page: card with a progress ring, tabs, a checklist, a bottom pill and a sheet to add an item",
        "TypeScale", "Every text style of the theme with its name, size and weight");

    static List<String[]> catalogEntries = new ArrayList<>();
    static Map<String, String> catalogSections = new LinkedHashMap<>();

    static void writeCatalog(Map<String, List<Decl>> byModule) throws IOException {
        Path cat = ROOT.resolve("catalog/src/main/kotlin/dev/irisui/catalog");
        String dest = read(cat.resolve("CatalogDestination.kt"));
        String mainContent = read(cat.resolve("MainContent.kt"));
        // sections
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher sm = Pattern.compile("(?m)^\\s+(\\w+)\\(\"([^\"]+)\"\\),?$").matcher(dest.substring(0, dest.indexOf("enum class CatalogDestination")));
        while (sm.find()) sections.put(sm.group(1), sm.group(2));
        // demo sources
        Map<String, String> funBodies = new HashMap<>();
        Map<String, String> funFile = new HashMap<>();
        try (Stream<Path> ps = Files.walk(cat.resolve("demos"))) {
            for (Path p : ps.filter(x -> x.toString().endsWith(".kt")).sorted().toList()) {
                String src = read(p);
                Matcher fm = Pattern.compile("(?m)^(?:private |internal )?fun (?:<[^>]*> )?(\\w+)\\(").matcher(src);
                while (fm.find()) {
                    int open = src.indexOf('(', fm.end() - 1);
                    int e = skipBalanced(src, open, '(', ')');
                    int brace = e < 0 ? -1 : src.indexOf('{', e);
                    int close = brace < 0 ? -1 : skipBalanced(src, brace, '{', '}');
                    if (close > 0) { funBodies.put(fm.group(1), src.substring(brace, close)); funFile.put(fm.group(1), ROOT.relativize(p).toString().replace('\\', '/')); }
                }
            }
        }
        Map<String, String> demoFun = new HashMap<>();
        Matcher dm = Pattern.compile("CatalogDestination\\.(\\w+)\\s*->\\s*(\\w+)\\(").matcher(mainContent);
        while (dm.find()) demoFun.put(dm.group(1), dm.group(2));
        // kit names
        Map<String, Decl> kit = new LinkedHashMap<>();
        for (List<Decl> ds : byModule.values()) for (Decl d : ds) kit.putIfAbsent(d.name(), d);

        StringBuilder sb = new StringBuilder();
        sb.append("# Catalog tour\n\n");
        sb.append("Every screen of the catalog app next to the kit APIs it uses, in the dark and in the light theme. ");
        sb.append("The screenshots were taken on a OnePlus 15 (ColorOS 17.0.1) and show the first screen of each demo with the status bar cropped; click one for the full size. ");
        sb.append("Where a recording exists it is shown below the description. All recordings are also on [one page](recordings.md). The last section has example screens that put the components together into whole pages.\n\n");
        sb.append("Every screen can be opened directly:\n\n```\nadb shell am start -n dev.irisui.catalog/.MainActivity --es destination Slider\n```\n\n");
        sb.append("Add `--ez light true` for the light theme and `--ez touches true` for a circle under the finger (for screen recordings). ");
        sb.append("The name after `--es destination` is the entry of `CatalogDestination`. ");
        sb.append("This page is generated by `tools/docs/DocGen.java`; the screenshots by `tools/tour.sh`.\n\n");
        Matcher em = Pattern.compile("(?m)^\\s+(\\w+)\\(CatalogSection\\.(\\w+), \"([^\"]+)\"\\)").matcher(dest);
        List<String[]> entries = new ArrayList<>();
        while (em.find()) entries.add(new String[]{em.group(1), em.group(2), em.group(3)});
        catalogEntries = entries;
        catalogSections = sections;
        for (Map.Entry<String, String> sec : sections.entrySet()) {
            sb.append("## ").append(sec.getValue()).append("\n\n<table>\n");
            for (String[] e : entries) {
                if (!e[1].equals(sec.getKey())) continue;
                String name = e[0], title = e[2];
                String fn = demoFun.get(name);
                String body = fn == null ? "" : funBodies.getOrDefault(fn, "");
                String sub = null;
                Matcher subm = Pattern.compile("subtitle\\s*=\\s*\"([^\"]*)\"").matcher(body);
                if (subm.find()) sub = subm.group(1);
                if (FALLBACK.containsKey(name)) sub = FALLBACK.get(name);
                if (sub == null) sub = "";
                Set<String> chrome = new HashSet<>();
                for (Map.Entry<String, String> ff : funFile.entrySet()) if (ff.getValue().endsWith("DemoKit.kt")) chrome.add(ff.getKey());
                List<String> used = usedApis(body, funBodies, kit, chrome, 0);
                String dark = "docs/media/catalog/dark/" + name + ".png", light = "docs/media/catalog/light/" + name + ".png";
                boolean haveDark = Files.exists(ROOT.resolve(dark)), haveLight = Files.exists(ROOT.resolve(light));
                sb.append("<tr>\n<td valign=\"top\" width=\"34%\"><a id=\"").append(name.toLowerCase(Locale.ROOT)).append("\"></a><b>").append(esc(title)).append("</b>");
                if (!sub.isEmpty()) sb.append("<br>").append(esc(sub));
                if (!used.isEmpty()) {
                    sb.append("<br><sub>Uses ");
                    int shown = 0;
                    for (String u : used) {
                        if (shown++ == 10) { sb.append(" and ").append(used.size() - 10).append(" more"); break; }
                        Decl d = kit.get(u);
                        String anchor = apiAnchors.get(u);
                        if (shown > 1) sb.append(", ");
                        sb.append(anchor == null ? "<code>" + u + "</code>" : "<a href=\"" + anchor + "\"><code>" + (d != null && d.receiver() != null ? d.display() : u) + "</code></a>");
                    }
                    sb.append("</sub>");
                }
                sb.append("<br><sub><code>--es destination ").append(name).append("</code>");
                if (fn != null && funFile.containsKey(fn)) sb.append(" · <a href=\"../").append(funFile.get(fn)).append("\">source</a>");
                sb.append("</sub>");
                for (String gif : RECORDINGS.getOrDefault(name, new String[0])) {
                    if (Files.exists(ROOT.resolve("docs/media/" + gif))) sb.append("<br><img src=\"media/").append(gif).append("\" width=\"200\" alt=\"").append(esc(title)).append(" in motion\">");
                }
                sb.append("</td>\n");
                for (String[] img : new String[][]{{dark, "dark"}, {light, "light"}}) {
                    boolean have = img[1].equals("dark") ? haveDark : haveLight;
                    sb.append("<td valign=\"top\" align=\"center\">");
                    if (have) {
                        String rel = img[0].substring("docs/".length());
                        sb.append("<a href=\"").append(rel).append("\"><img src=\"").append(rel).append("\" width=\"150\" alt=\"").append(esc(title)).append(", ").append(img[1]).append("\"></a><br><sub>").append(img[1]).append("</sub>");
                    }
                    sb.append("</td>\n");
                }
                sb.append("</tr>\n");
            }
            sb.append("</table>\n\n");
        }
        write("docs/catalog.md", sb.toString());
    }

    /** Kit declarations a demo calls, in order of first use; helper functions of the demos are followed. */
    static List<String> usedApis(String body, Map<String, String> funBodies, Map<String, Decl> kit, Set<String> visited, int depth) {
        Map<String, Integer> first = new TreeMap<>();
        collect(body, funBodies, kit, visited, depth, first, 0);
        return first.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    static void collect(String body, Map<String, String> funBodies, Map<String, Decl> kit, Set<String> visited, int depth, Map<String, Integer> first, int offset) {
        String code = stripComments(body);
        for (String name : kit.keySet()) {
            if (NOT_LISTED.contains(name)) continue;
            Decl d = kit.get(name);
            Pattern p = d.receiver() != null && d.kind().equals("fun")
                ? Pattern.compile("\\." + Pattern.quote(name) + "\\b")
                : Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
            Matcher m = p.matcher(code);
            if (m.find()) first.merge(name, offset + m.start(), Math::min);
        }
        if (depth >= 2) return;
        for (Map.Entry<String, String> f : funBodies.entrySet()) {
            if (visited.contains(f.getKey()) || f.getKey().endsWith("Demo")) continue;
            Matcher m = Pattern.compile("\\b" + Pattern.quote(f.getKey()) + "\\(").matcher(code);
            if (m.find()) { visited.add(f.getKey()); collect(f.getValue(), funBodies, kit, visited, depth + 1, first, offset + m.start()); }
        }
    }

    // ------------------------------------------------------------------ recordings overview

    static final Map<String, String> GIF_LABEL = Map.of(
        "bottom-nav-pill-closeup.gif", "Bottom nav pill: close-up",
        "bottom-nav-pill.gif", "Bottom nav pill: whole screen",
        "drag-free-reorder.gif", "Drag physics: free drag and re-order",
        "home-scroll.gif", "Home screen: scroll and bounce",
        "divider.gif", "Divider: dark and light",
        "badge.gif", "Badge: dark and light");

    /** docs/recordings.md: every recording of docs/media on one page, in the order of the catalog. */
    static void writeRecordings() throws IOException {
        Path media = ROOT.resolve("docs/media");
        Set<String> used = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        sb.append("# Recordings\n\n");
        sb.append("Every screen of the catalog in motion, recorded on a OnePlus 15 in the dark theme. ");
        sb.append("The grey circle under the finger is drawn by the catalog app (`--ez touches true`), the system \"show touches\" setting was left alone; ");
        sb.append("the edge swipe of the back gesture belongs to the system, so it shows no circle. ");
        sb.append("Click a title for the screen with its screenshots and the kit APIs it uses.\n\n");
        sb.append("This page is generated by `tools/docs/DocGen.java`; the recordings are made with `tools/gif.sh`.\n\n");
        for (Map.Entry<String, String> sec : catalogSections.entrySet()) {
            List<String[]> cells = new ArrayList<>();
            for (String[] e : catalogEntries) {
                if (!e[1].equals(sec.getKey())) continue;
                for (String gif : RECORDINGS.getOrDefault(e[0], new String[0])) {
                    if (!Files.exists(media.resolve(gif))) continue;
                    used.add(gif);
                    String title = GIF_LABEL.getOrDefault(gif, e[2]);
                    cells.add(new String[]{gif, title, e[0].toLowerCase(Locale.ROOT)});
                }
            }
            if (cells.isEmpty()) continue;
            sb.append("## ").append(sec.getValue()).append("\n\n");
            appendGrid(sb, cells);
        }
        List<String[]> other = new ArrayList<>();
        try (Stream<Path> ps = Files.list(media)) {
            for (Path p : ps.filter(x -> x.toString().endsWith(".gif")).sorted().toList()) {
                String name = p.getFileName().toString();
                if (used.contains(name)) continue;
                String title = GIF_LABEL.getOrDefault(name, cap(name.substring(0, name.length() - 4).replace('-', ' ')));
                other.add(new String[]{name, title, null});
            }
        }
        if (!other.isEmpty()) {
            sb.append("## More\n\n");
            appendGrid(sb, other);
        }
        write("docs/recordings.md", sb.toString());
    }

    static void appendGrid(StringBuilder sb, List<String[]> cells) {
        sb.append("<table>\n");
        for (int i = 0; i < cells.size(); i++) {
            if (i % 4 == 0) sb.append("<tr>\n");
            String[] c = cells.get(i);
            sb.append("<td align=\"center\" valign=\"top\"><img src=\"media/").append(c[0]).append("\" width=\"200\" alt=\"").append(esc(c[1])).append("\"><br><sub>");
            if (c[2] != null) sb.append("<a href=\"catalog.md#").append(c[2]).append("\"><b>").append(esc(c[1])).append("</b></a>");
            else sb.append("<b>").append(esc(c[1])).append("</b>");
            sb.append("</sub></td>\n");
            if (i % 4 == 3 || i == cells.size() - 1) sb.append("</tr>\n");
        }
        sb.append("</table>\n\n");
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws Exception {
        List<Module> modules = readModules();
        Map<String, List<Decl>> byModule = new LinkedHashMap<>();
        for (Module m : modules) {
            Path src = ROOT.resolve(m.name()).resolve("src/main/kotlin");
            List<Decl> decls = new ArrayList<>();
            if (Files.isDirectory(src)) {
                try (Stream<Path> ps = Files.walk(src)) {
                    for (Path p : ps.filter(x -> x.toString().endsWith(".kt")).sorted().toList()) decls.addAll(parseFile(p, m.name()));
                }
            }
            byModule.put(m.name(), decls);
        }
        writeApi(modules, byModule);
        if (args.length == 0 || args[0].equals("catalog")) { writeCatalog(byModule); writeRecordings(); }
    }
}

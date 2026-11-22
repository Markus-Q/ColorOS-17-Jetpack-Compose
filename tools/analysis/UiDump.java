import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Prints a uiautomator dump as a compact tree: depth, short class, resource id, text / description, bounds, flags.
 * usage: java UiDump.java ui.xml [minDepth]
 * flags: c clickable, l long clickable, s scrollable, k checked, d disabled, f focused
 */
public class UiDump {
    static String[] ARGS;

    public static void main(String[] a) throws Exception {
        ARGS = a;
        Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(a[0])).getDocumentElement();
        walk(root, 0);
    }

    static void walk(Element e, int depth) {
        if (e.getTagName().equals("node")) {
            String cls = e.getAttribute("class");
            cls = cls.substring(cls.lastIndexOf('.') + 1);
            String id = e.getAttribute("resource-id");
            id = id.substring(id.lastIndexOf('/') + 1);
            boolean noText = ARGS.length > 1 && ARGS[1].equals("notext");
            String txt = noText ? "" : e.getAttribute("text");
            String desc = noText ? "" : e.getAttribute("content-desc");
            StringBuilder fl = new StringBuilder();
            if (e.getAttribute("clickable").equals("true")) fl.append('c');
            if (e.getAttribute("long-clickable").equals("true")) fl.append('l');
            if (e.getAttribute("scrollable").equals("true")) fl.append('s');
            if (e.getAttribute("checked").equals("true")) fl.append('k');
            if (e.getAttribute("enabled").equals("false")) fl.append('d');
            if (e.getAttribute("focused").equals("true")) fl.append('f');
            System.out.println("  ".repeat(depth) + cls + (id.isEmpty() ? "" : " #" + id) + (txt.isEmpty() ? "" : " \"" + txt + "\"")
                + (desc.isEmpty() ? "" : " (" + desc + ")") + " " + e.getAttribute("bounds") + (fl.length() > 0 ? " " + fl : ""));
            depth++;
        }
        NodeList ch = e.getChildNodes();
        for (int i = 0; i < ch.getLength(); i++) {
            Node n = ch.item(i);
            if (n instanceof Element) walk((Element) n, depth);
        }
    }
}

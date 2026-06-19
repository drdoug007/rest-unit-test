package one.dastec.restunittest.js;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.stream.Collectors;

public class DomJS {

    public static class DOMParser {
        public NodeWrapper parseFromString(String string, String type) {
            Document doc;
            if ("application/xml".equalsIgnoreCase(type) || "text/xml".equalsIgnoreCase(type)) {
                doc = Jsoup.parse(string, "", org.jsoup.parser.Parser.xmlParser());
            } else {
                doc = Jsoup.parse(string);
            }
            return new NodeWrapper(doc);
        }
    }

    public static class NodeWrapper {
        public final Node node;

        public NodeWrapper(Node node) {
            this.node = node;
        }

        public String getNodeName() { return node.nodeName(); }
        public String getNodeValue() { return node instanceof org.jsoup.nodes.TextNode ? ((org.jsoup.nodes.TextNode)node).text() : null; }
        public int getNodeType() {
            if (node instanceof Document) return 9;
            if (node instanceof Element) return 1;
            if (node instanceof org.jsoup.nodes.TextNode) return 3;
            if (node instanceof org.jsoup.nodes.Comment) return 8;
            return 0;
        }
        public NodeWrapper getParentNode() { return wrap(node.parentNode()); }
        public List<NodeWrapper> getChildNodes() {
            return node.childNodes().stream().map(NodeWrapper::new).collect(Collectors.toList());
        }
        public NodeWrapper getNextSibling() { return wrap(node.nextSibling()); }
        public NodeWrapper getPreviousSibling() { return wrap(node.previousSibling()); }
        public String getTextContent() {
            if (node instanceof Element) return ((Element) node).text();
            if (node instanceof org.jsoup.nodes.TextNode) return ((org.jsoup.nodes.TextNode) node).text();
            return "";
        }
        public String getXml() {
            if (node instanceof Element) {
                Element el = (Element) node;
                Document doc = el.ownerDocument();
                if (doc != null) {
                    Document.OutputSettings settings = doc.outputSettings();
                    settings.prettyPrint(true);
                    settings.indentAmount(2);
                    settings.syntax(Document.OutputSettings.Syntax.xml);
                }
                String xml = el.toString();
                if (xml.contains("\n")) {
                    // Try to fix short text nodes: <tag>\n  text\n</tag> -> <tag>text</tag>
                    xml = xml.replaceAll("<([^>]+)>\\s*\n\\s*([^<\\s][^<]*)\\s*\n\\s*</\\1>", "<$1>$2</$1>");
                }
                return xml;
            }
            if (node instanceof Document) {
                Document doc = (Document) node;
                Document.OutputSettings settings = doc.outputSettings();
                settings.prettyPrint(true);
                settings.indentAmount(2);
                settings.syntax(Document.OutputSettings.Syntax.xml);
                String xml = doc.toString();
                if (xml.contains("\n")) {
                    xml = xml.replaceAll("<([^>]+)>\\s*\n\\s*([^<\\s][^<]*)\\s*\n\\s*</\\1>", "<$1>$2</$1>");
                }
                return xml;
            }
            if (node instanceof org.jsoup.nodes.TextNode) return ((org.jsoup.nodes.TextNode) node).toString();
            return "";
        }
        public String getTagName() {
            return node instanceof Element ? ((Element) node).tagName() : (node instanceof Document ? "html" : null);
        }
        public String getId() {
            return node instanceof Element ? ((Element) node).id() : null;
        }
        public String getClassName() {
            return node instanceof Element ? ((Element) node).className() : null;
        }
        public List<NodeWrapper> getElementsByTag(String tag) {
            if (!(node instanceof Element)) return List.of();
            return ((Element) node).getElementsByTag(tag).stream().map(NodeWrapper::new).collect(Collectors.toList());
        }
        public List<NodeWrapper> getElementsByClass(String cls) {
            if (!(node instanceof Element)) return List.of();
            return ((Element) node).getElementsByClass(cls).stream().map(NodeWrapper::new).collect(Collectors.toList());
        }
        public List<NodeWrapper> getElementsByAttribute(String attr, String value) {
            if (!(node instanceof Element)) return List.of();
            return ((Element) node).getElementsByAttributeValue(attr, value).stream().map(NodeWrapper::new).collect(Collectors.toList());
        }
        public NodeWrapper getElementById(String id) {
            if (node instanceof Document) return wrap(((Document) node).getElementById(id));
            if (node instanceof Element) return wrap(((Element) node).getElementById(id));
            return null;
        }
        public NodeWrapper createElement(String tag) {
            if (!(node instanceof Document)) return null;
            return wrap(((Document) node).createElement(tag));
        }

        private NodeWrapper wrap(Node n) { return n != null ? new NodeWrapper(n) : null; }
    }
}

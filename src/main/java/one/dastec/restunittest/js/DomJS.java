package one.dastec.restunittest.js;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DomJS {

    public static class DOMParser {
        public NodeWrapper parseFromString(String string, String type) {
            if ("application/xml".equalsIgnoreCase(type) || "text/xml".equalsIgnoreCase(type)) {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
                    return new NodeWrapper(doc);
                } catch (Exception e) {
                    // Fallback to jsoup if W3C parsing fails
                    return new NodeWrapper(Jsoup.parse(string, "", org.jsoup.parser.Parser.xmlParser()));
                }
            } else {
                return new NodeWrapper(Jsoup.parse(string));
            }
        }
    }

    public static class NodeWrapper {
        public final Object node;

        public NodeWrapper(Object node) {
            this.node = node;
        }

        public Object getNode() { return node; }

        public String getNodeName() {
            if (node instanceof Node) return ((Node) node).nodeName();
            if (node instanceof org.w3c.dom.Node) return ((org.w3c.dom.Node) node).getNodeName();
            return null;
        }

        public String getNodeValue() {
            if (node instanceof org.jsoup.nodes.TextNode) return ((org.jsoup.nodes.TextNode) node).text();
            if (node instanceof org.w3c.dom.Node) return ((org.w3c.dom.Node) node).getNodeValue();
            return null;
        }

        public int getNodeType() {
            if (node instanceof Document || node instanceof org.w3c.dom.Document) return 9;
            if (node instanceof Element || node instanceof org.w3c.dom.Element) return 1;
            if (node instanceof org.jsoup.nodes.TextNode || node instanceof org.w3c.dom.Text) return 3;
            if (node instanceof org.jsoup.nodes.Comment || node instanceof org.w3c.dom.Comment) return 8;
            return 0;
        }

        public NodeWrapper getParentNode() {
            if (node instanceof Node) return wrap(((Node) node).parentNode());
            if (node instanceof org.w3c.dom.Node) return wrap(((org.w3c.dom.Node) node).getParentNode());
            return null;
        }

        public List<NodeWrapper> getChildNodes() {
            if (node instanceof Node) {
                return ((Node) node).childNodes().stream().map(NodeWrapper::new).collect(Collectors.toList());
            }
            if (node instanceof org.w3c.dom.Node) {
                org.w3c.dom.NodeList nl = ((org.w3c.dom.Node) node).getChildNodes();
                List<NodeWrapper> list = new ArrayList<>();
                for (int i = 0; i < nl.getLength(); i++) {
                    list.add(new NodeWrapper(nl.item(i)));
                }
                return list;
            }
            return List.of();
        }

        public NodeWrapper getNextSibling() {
            if (node instanceof Node) return wrap(((Node) node).nextSibling());
            if (node instanceof org.w3c.dom.Node) return wrap(((org.w3c.dom.Node) node).getNextSibling());
            return null;
        }

        public NodeWrapper getPreviousSibling() {
            if (node instanceof Node) return wrap(((Node) node).previousSibling());
            if (node instanceof org.w3c.dom.Node) return wrap(((org.w3c.dom.Node) node).getPreviousSibling());
            return null;
        }

        public String getTextContent() {
            if (node instanceof Element) return ((Element) node).text();
            if (node instanceof org.jsoup.nodes.TextNode) return ((org.jsoup.nodes.TextNode) node).text();
            if (node instanceof org.w3c.dom.Node) return ((org.w3c.dom.Node) node).getTextContent();
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

            if (node instanceof org.w3c.dom.Node) {
                org.w3c.dom.Node n = (org.w3c.dom.Node) node;
                try {
                    javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
                    javax.xml.transform.Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                    java.io.StringWriter writer = new java.io.StringWriter();
                    transformer.transform(new javax.xml.transform.dom.DOMSource(n), new javax.xml.transform.stream.StreamResult(writer));
                    String xml = writer.getBuffer().toString();
                    if (xml.contains("\n")) {
                        xml = xml.replaceAll("<([^>]+)>\\s*\n\\s*([^<\\s][^<]*)\\s*\n\\s*</\\1>", "<$1>$2</$1>");
                    }
                    return xml;
                } catch (Exception e) {
                    return "";
                }
            }
            return "";
        }

        public String getTagName() {
            if (node instanceof Element) return ((Element) node).tagName();
            if (node instanceof Document) return "html";
            if (node instanceof org.w3c.dom.Element) return ((org.w3c.dom.Element) node).getTagName();
            if (node instanceof org.w3c.dom.Document) return "xml";
            return null;
        }

        public String getId() {
            if (node instanceof Element) return ((Element) node).id();
            if (node instanceof org.w3c.dom.Element) return ((org.w3c.dom.Element) node).getAttribute("id");
            return null;
        }

        public String getClassName() {
            if (node instanceof Element) return ((Element) node).className();
            if (node instanceof org.w3c.dom.Element) return ((org.w3c.dom.Element) node).getAttribute("class");
            return null;
        }

        public List<NodeWrapper> getElementsByTag(String tag) {
            if (node instanceof Element) {
                return ((Element) node).getElementsByTag(tag).stream().map(NodeWrapper::new).collect(Collectors.toList());
            }
            if (node instanceof org.w3c.dom.Element || node instanceof org.w3c.dom.Document) {
                org.w3c.dom.NodeList nl;
                if (node instanceof org.w3c.dom.Element) nl = ((org.w3c.dom.Element) node).getElementsByTagName(tag);
                else nl = ((org.w3c.dom.Document) node).getElementsByTagName(tag);
                List<NodeWrapper> list = new ArrayList<>();
                for (int i = 0; i < nl.getLength(); i++) {
                    list.add(new NodeWrapper(nl.item(i)));
                }
                return list;
            }
            return List.of();
        }

        public List<NodeWrapper> getElementsByClass(String cls) {
            if (node instanceof Element) {
                return ((Element) node).getElementsByClass(cls).stream().map(NodeWrapper::new).collect(Collectors.toList());
            }
            if (node instanceof org.w3c.dom.Element || node instanceof org.w3c.dom.Document) {
                org.w3c.dom.NodeList nl;
                if (node instanceof org.w3c.dom.Element) nl = ((org.w3c.dom.Element) node).getElementsByTagName("*");
                else nl = ((org.w3c.dom.Document) node).getElementsByTagName("*");
                List<NodeWrapper> list = new ArrayList<>();
                for (int i = 0; i < nl.getLength(); i++) {
                    org.w3c.dom.Node n = nl.item(i);
                    if (n instanceof org.w3c.dom.Element) {
                        String classAttr = ((org.w3c.dom.Element) n).getAttribute("class");
                        if (classAttr != null && Arrays.asList(classAttr.split("\\s+")).contains(cls)) {
                            list.add(new NodeWrapper(n));
                        }
                    }
                }
                return list;
            }
            return List.of();
        }

        public List<NodeWrapper> getElementsByAttribute(String attr, String value) {
            if (node instanceof Element) {
                return ((Element) node).getElementsByAttributeValue(attr, value).stream().map(NodeWrapper::new).collect(Collectors.toList());
            }
            if (node instanceof org.w3c.dom.Element || node instanceof org.w3c.dom.Document) {
                org.w3c.dom.NodeList nl;
                if (node instanceof org.w3c.dom.Element) nl = ((org.w3c.dom.Element) node).getElementsByTagName("*");
                else nl = ((org.w3c.dom.Document) node).getElementsByTagName("*");

                List<NodeWrapper> list = new ArrayList<>();
                for (int i = 0; i < nl.getLength(); i++) {
                    org.w3c.dom.Node n = nl.item(i);
                    if (n instanceof org.w3c.dom.Element && value.equals(((org.w3c.dom.Element) n).getAttribute(attr))) {
                        list.add(new NodeWrapper(n));
                    }
                }
                return list;
            }
            return List.of();
        }

        public NodeWrapper getElementById(String id) {
            if (node instanceof Document) return wrap(((Document) node).getElementById(id));
            if (node instanceof Element) return wrap(((Element) node).getElementById(id));
            if (node instanceof org.w3c.dom.Document) {
                org.w3c.dom.Element el = ((org.w3c.dom.Document) node).getElementById(id);
                if (el != null) return wrap(el);
                // Fallback: search for id attribute
                List<NodeWrapper> found = getElementsByAttribute("id", id);
                return found.isEmpty() ? null : found.get(0);
            }
            if (node instanceof org.w3c.dom.Element) {
                List<NodeWrapper> found = getElementsByAttribute("id", id);
                return found.isEmpty() ? null : found.get(0);
            }
            return null;
        }

        public NodeWrapper createElement(String tag) {
            if (node instanceof Document) return wrap(((Document) node).createElement(tag));
            if (node instanceof org.w3c.dom.Document) return wrap(((org.w3c.dom.Document) node).createElement(tag));
            return null;
        }

        public List<NodeWrapper> xpath(String expression) {
            if (node instanceof org.w3c.dom.Node) {
                try {
                    XPath xpath = XPathFactory.newInstance().newXPath();
                    org.w3c.dom.NodeList result = (org.w3c.dom.NodeList) xpath.evaluate(expression, node, XPathConstants.NODESET);
                    List<NodeWrapper> list = new ArrayList<>();
                    for (int i = 0; i < result.getLength(); i++) {
                        list.add(new NodeWrapper(result.item(i)));
                    }
                    return list;
                } catch (Exception e) {
                    return List.of();
                }
            }
            return List.of();
        }

        private NodeWrapper wrap(Object n) {
            return n != null ? new NodeWrapper(n) : null;
        }
    }
}

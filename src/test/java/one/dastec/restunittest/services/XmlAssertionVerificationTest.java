package one.dastec.restunittest.services;

import one.dastec.restunittest.js.DomJS;
import one.dastec.restunittest.js.HttpClientJS;
import one.dastec.restunittest.js.ResponseJS;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class XmlAssertionVerificationTest {

    @Autowired
    private GraalJsService graalJsService;

    @Test
    void testXmlAssertionCreation() {
        String xml = "<List>\n" +
                "  <item>\n" +
                "    <id>1</id>\n" +
                "    <name>Luxury Motors</name>\n" +
                "  </item>\n" +
                "  <item>\n" +
                "    <id>2</id>\n" +
                "    <name>Reliable Rides</name>\n" +
                "  </item>\n" +
                "</List>";

        try (Context context = graalJsService.createContext()) {
            HttpClientJS httpClientJS = new HttpClientJS();
            DomJS.NodeWrapper doc = new DomJS.DOMParser().parseFromString(xml, "application/xml");
            
            // Set up the context exactly as RestTestService does
            setupGraalJsContext(context, httpClientJS, doc, xml);

            // 1. Test clicking on the first "name"
            // The generated assertion would be:
            String assertion1 = "client.test(\"Check name\", () => {\n" +
                    "    client.assert(response.body.getElementsByTagName(\"name\")[0].textContent === \"Luxury Motors\", \"Expected name to be Luxury Motors\");\n" +
                    "});";
            
            context.eval("js", assertion1);
            List<String> results1 = httpClientJS.getTestResults();
            assertEquals(1, results1.size());
            assertTrue(results1.get(0).contains("✅ Check name"), "First assertion failed: " + results1.get(0));
            httpClientJS.getTestResults().clear();

            // 2. Test clicking on the second "name"
            // The generated assertion would be:
            String assertion2 = "client.test(\"Check name\", () => {\n" +
                    "    client.assert(response.body.getElementsByTagName(\"name\")[1].textContent === \"Reliable Rides\", \"Expected name to be Reliable Rides\");\n" +
                    "});";
            
            context.eval("js", assertion2);
            List<String> results2 = httpClientJS.getTestResults();
            assertEquals(1, results2.size());
            assertTrue(results2.get(0).contains("✅ Check name"), "Second assertion failed: " + results2.get(0));
            httpClientJS.getTestResults().clear();

            // 3. Negative test: incorrect value should fail
            String assertion3 = "client.test(\"Check name fail\", () => {\n" +
                    "    client.assert(response.body.getElementsByTagName(\"name\")[0].textContent === \"Wrong Name\", \"Expected name to be Wrong Name\");\n" +
                    "});";
            context.eval("js", assertion3);
            List<String> results3 = httpClientJS.getTestResults();
            assertEquals(1, results3.size());
            assertTrue(results3.get(0).contains("❌ Check name fail"), "Assertion should have failed: " + results3.get(0));
            httpClientJS.getTestResults().clear();
        }
    }

    private void setupGraalJsContext(Context context, HttpClientJS httpClientJS, DomJS.NodeWrapper doc, String rawXml) {
        var bindings = context.getBindings("js");
        bindings.putMember("__client", httpClientJS);
        bindings.putMember("__domParser", new DomJS.DOMParser());

        // Mock response object
        ResponseJS responseJS = new ResponseJS(200, Map.of("Content-Type", "application/xml"), null, rawXml);
        bindings.putMember("response", responseJS);

        // Define client object
        context.eval("js", "var client = { " +
                "test: function(name, callback) { __client.test(name, callback); }," +
                "assert: function(condition, message) { __client.assertCondition(condition, message); }" +
                "};");

        // Wrap the Java doc into the JS-friendly DOM node (mimicking RestTestService.java lines 726-763)
        context.getBindings("js").putMember("javaNode", doc);
        context.eval("js", "var wrap = function(jn) { " +
                "  if (!jn) return null; " +
                "  var node = { " +
                "    get nodeName() { return jn.getNodeName(); }, " +
                "    get nodeValue() { return jn.getNodeValue(); }, " +
                "    get nodeType() { return jn.getNodeType(); }, " +
                "    get parentNode() { return wrap(jn.getParentNode()); }, " +
                "    get childNodes() { return jn.getChildNodes().toArray().map(wrap); }, " +
                "    get textContent() { return jn.getTextContent(); }, " +
                "    get xml() { return jn.getXml(); }, " +
                "    get tagName() { return jn.getTagName(); }, " +
                "    getElementsByTagName: function(tag) { return jn.getElementsByTag(tag).toArray().map(wrap); }, " +
                "    toJSON: function() { return this.xml; }, " +
                "    toString: function() { return this.xml; }, " +
                "    valueOf: function() { return this.xml; }, " +
                "    _jn: jn " +
                "  }; " +
                "  return node; " +
                "}; " +
                "response.body = wrap(javaNode);");
    }
}

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLParser {

    /**
     * Function that parses XML and gets all of the attributes within it
     * @param XML the XML string to be parsed
     * @return HashMap in the form "<attribute, value>"
     */
    public HashMap<String, String> getAttributes(String XML) {
        // Create a hashmap to hold data in the form of <attribute, value>
        HashMap<String, String> finalAttributes = new HashMap<String,String>();
        try {
            // Built-in library to parse HTML-like documents
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(XML.getBytes()));

            doc.getDocumentElement().normalize();

            // Get the root node of the document, in the case of our XML it would be <stats>
            Element root = doc.getDocumentElement();

            // Iterate through all of the children of the root node
            NodeList rootAttributes = root.getChildNodes();
            for(int i = 0; i < rootAttributes.getLength(); i++) {
                Node node = rootAttributes.item(i);
                // The child must be of type Element, otherwise it doesn't work
                if(node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Trim the text content and add it to the hashmap
                    String content = element.getTextContent().trim();
                    if(!content.isEmpty()) {
                        finalAttributes.put(element.getTagName(), content);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return finalAttributes;
        }

}

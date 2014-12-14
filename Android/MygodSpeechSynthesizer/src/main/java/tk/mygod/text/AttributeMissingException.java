package tk.mygod.text;

import org.xml.sax.SAXException;

/**
 * @author Mygod
 */
public final class AttributeMissingException extends SAXException {
    public AttributeMissingException(String attributeNames) {
        super(attributeNames + " required.");
    }
}

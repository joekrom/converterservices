package de.axxepta.converterservices.utils;

import org.json.JSONObject;
import org.json.XML;

public class JSONUtils {

    /**
     * Converts JSON String to XML String, wraps it in an JSON tag.
     * The order of the elements might not be as in the original JSON!
     * @param str JSON String
     * @param rootElementName Name of XML root element, "JSON" if not provided.
     * @return XML String
     */
    public static String JsonToXmlString(String str, String... rootElementName) {
        String tagName = rootElementName.length > 0 ? rootElementName[0] : "JSON";
        JSONObject json = new JSONObject(str);
        return XML.toString(json, tagName);
    }

    public static String JsonArrayToXmlString(String str, String rootArrayName, String... rootElementName) {
        String tagName = rootElementName.length > 0 ? rootElementName[0] : "JSON";
        JSONObject json = new JSONObject(
                str.startsWith("[") ?
                        "{ \"" + (rootArrayName.equals("") ? "array" : rootArrayName) + "\" : " + str + "}" :
                        str
        );
        return XML.toString(json, tagName);
    }

    public static String XmlToJsonString(String str) {
        JSONObject json = XML.toJSONObject(str);
        return json.toString();
    }
}

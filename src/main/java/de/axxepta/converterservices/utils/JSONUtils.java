package de.axxepta.converterservices.utils;

import org.json.JSONObject;
import org.json.XML;

public class JSONUtils {

    /**
     * Converts JSON String to XML String, wraps it in an JSON tag.
     * The order of the elements might not be as in the original JSON!
     * @param str JSON String
     * @return  XML String
     */
    public static String JsonToXmlString(String str) {
        JSONObject json = new JSONObject(str);
        return XML.toString(json, "JSON");

    }

    public static String XmlToJsonString(String str) {
        JSONObject json = XML.toJSONObject(str);
        return json.toString();
    }
}

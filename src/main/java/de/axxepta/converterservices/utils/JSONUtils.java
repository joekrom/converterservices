package de.axxepta.converterservices.utils;

import org.json.JSONObject;
import org.json.XML;

public class JSONUtils {

    public static String JsonToXmlString(String str) {
        JSONObject json = new JSONObject(str);
        return XML.toString(json);

    }

    public static String XmlToJsonString(String str) {
        JSONObject json = XML.toJSONObject(str);
        return json.toString();
    }
}

package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.Saxon;
import org.w3c.dom.Document;

public class PipeExec {

    public static void main(String[] args) {
        if (args.length != 0) {
            execProcess(args[0]);
        }
        test1();
    }

    public static void execProcess(String xmlFile) {
    }

    private static void test1() {
        String inPath = "E:/Dateien/code/Java/voith";
        String outPath = "E:/Dateien/code/Java/voith/03_Epub/out";
        String workPath = "E:/Dateien/code/Java/voith/03_Epub/work";
        String basePath =  inPath + "/03_Epub/IS_de-ePub/";

        String query =
                "declare variable $base external; \n" +
                "declare variable $base-title external; \n" +
                "let $html:= \n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "<title>{$base-title}</title>\n" +
                "</head>\n" +
                "    <body>{\n" +
                "    for $item in .//*:item[@media-type='application/xhtml+xml']\n" +
                "    let $path := $base || \"OEBPS/\" || $item/@href\n" +
                "    let $chapter := doc($path)\n" +
                "    return \n" +
                "        <section data-path=\"{$path}\">\n" +
                "            {$chapter/*:html/*:body/node()}\n" +
                "        </section>\n" +
                "    }</body>\n" +
                "</html>\n" +
                "return $html";

        String params =
                "base = " + basePath + " as xs:string\n" +
                "base-title = IntegraScreen IS as xs:string\n" +
                ":OUTPUT: as node()";

        Saxon saxon = new Saxon();
        try {
            Object dom = saxon.xquery(query, basePath + "OEBPS/content.opf", params);
            if (dom instanceof Document) {
                Saxon.saveDOM((Document) dom, outPath + "/test1.xml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void test2() {
        String inPath = "E:/Dateien/code/Java/voith";
        String outPath = "E:/Dateien/code/Java/voith/03_Epub/out";
        String workPath = "E:/Dateien/code/Java/voith/03_Epub/work";
        String basePath =  inPath + "/03_Epub/IS_de-ePub/";

        String query =
                "declare variable $base external; \n" +
                        "let $base-title := 'IntegraScreen IS' \n" +
                        "let $html:= \n" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                        "<head>\n" +
                        "<title>{$base-title}</title>\n" +
                        "</head>\n" +
                        "    <body>{\n" +
                        "       $base\n" +
                        "    }</body>\n" +
                        "</html>\n" +
                        "return $html";

        String params =
                "base = " + basePath + " as xs:string\n" +
                        ":OUTPUT: as node()";

        Saxon saxon = new Saxon();
        try {
            Object dom = saxon.xquery(query, Saxon.XQUERY_NO_CONTEXT, params);
            if (dom instanceof Document) {
                Saxon.saveDOM((Document) dom, outPath + "/test2.xml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package de.axxepta.converterservices;

import spark.servlet.SparkApplication;

/**
 * This class can be used to start the converterservices in a context other than with embedded jetty, e.g. combining it
 * with a different servlet. For starting it as servlet with jetty you would add something like the following lines to
 * the web.xml:
 * <blockquote><p>
 * &lt;filter&gt;<br>
 * &lt;filter-name&gt;SparkFilter&lt;/filter-name&gt;<br>
 * &lt;filter-class&gt;spark.servlet.SparkFilter&lt;/filter-class&gt;<br>
 * &lt;init-param&gt;<br>
 * &lt;param-name&gt;applicationClass&lt;/param-name&gt;<br>
 * &lt;param-value&gt;de.axxepta.converterservices.ConverterSparkApplication&lt;/param-value&gt;<br>
 * &lt;/init-param&gt;<br>
 * &lt;/filter&gt;<br>
 * &lt;filter-mapping&gt;<br>
 * &lt;filter-name&gt;SparkFilter&lt;/filter-name&gt;<br>
 * &lt;url-pattern&gt;/spark/*&lt;/url-pattern&gt;<br>
 * &lt;/filter-mapping&gt;
 * </p></blockquote>
 */
public class ConverterSparkApplication implements SparkApplication {

    private static final String RELATIVE_PATH              = "/spark";

    @Override
    public void init() {
        App.init(RELATIVE_PATH);
    }
}

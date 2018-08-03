package de.axxepta.converterservices.proc;

/**
 * This interface has to be implemented by classes which are referenced in a pipeline XML description by the <b>class</b>
 *     attribute in the <b>pipeline</b> element. In the <i>builder</i> function the defining builder can be prepared and
 *     returned.
 */
public interface IPipelineBuilderProvider {

    Pipeline.PipelineBuilder builder();
}

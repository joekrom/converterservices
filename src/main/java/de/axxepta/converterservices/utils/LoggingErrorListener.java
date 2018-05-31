package de.axxepta.converterservices.utils;

import javax.xml.transform.TransformerException;

/**
 * @author Max on 16.10.2015.
 */
public class LoggingErrorListener implements javax.xml.transform.ErrorListener {

    private FileArray _array;

    public LoggingErrorListener(){
        _array = new FileArray();
    }

    public FileArray getFileArray(){
        return _array;
    }

    public void setFileArray(FileArray array){
        _array = array;
    }

    @Override
    public void warning(TransformerException e) throws TransformerException {
        _array.add(e.toString());
    }

    @Override
    public void error(TransformerException e) throws TransformerException {
        _array.add(e.toString());
    }

    @Override
    public void fatalError(TransformerException e) throws TransformerException {
        _array.add(e.toString());
    }
}

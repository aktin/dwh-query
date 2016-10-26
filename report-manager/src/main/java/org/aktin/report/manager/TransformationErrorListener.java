package org.aktin.report.manager;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public class TransformationErrorListener implements ErrorListener{
	private Logger log;
	
	public TransformationErrorListener(Logger logger) {
		this.log = logger;
	}
	@Override
	public void error(TransformerException e) throws TransformerException {
		log.log(Level.WARNING, "Transformation error", e);
	}

	@Override
	public void fatalError(TransformerException e) throws TransformerException {
		log.log(Level.WARNING, "Transformation failed", e);
	}

	@Override
	public void warning(TransformerException e) throws TransformerException {
		log.log(Level.WARNING, "Transformation warning", e);
	}

}

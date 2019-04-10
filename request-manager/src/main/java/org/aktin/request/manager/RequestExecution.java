package org.aktin.request.manager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.QueryHandlerFactory;
import org.aktin.broker.query.io.MultipartDirectory;
import org.aktin.broker.query.io.MultipartDirectoryWriter;
import org.aktin.broker.query.io.MultipartEntry;
import org.aktin.broker.query.io.MultipartOutputStream;
import org.aktin.broker.query.io.ZipArchiveWriter;
import org.aktin.broker.request.RequestStatus;
import org.aktin.broker.request.RetrievedRequest;
import org.w3c.dom.Element;

public class RequestExecution implements Runnable{
	private final Logger log;
	private RetrievedRequest request;
	private RequestProcessor processor;
	private QueryHandlerFactory[] handlers;
	private Element[] sources;
	
	RequestExecution(RequestProcessor processor, RetrievedRequest request){
		// create logger containing the request id
		this.log = Logger.getLogger(RequestProcessor.class.getName()+"."+request.getRequestId());
		this.request = request;
		this.processor = processor;
	}

	private void changeStatus(RequestStatus status, String description){
		try {
			request.changeStatus(null, status, description);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Unable to change status for request "+request.getRequestId()+" to "+status, e);
		}
	}
	private void executeRequestSource(int partIndex, MultipartDirectory input, MultipartOutputStream target) throws IOException {
		QueryHandlerFactory handler = handlers[partIndex];
		Element source = sources[partIndex];
		
		Map<String,String> props = processor.compileProperties(request, handler);
		
		
		request.setProcessing(props, source.getLocalName(), partIndex+1, sources.length);
		// write properties and change status to processing
		QueryHandler h = handler.parse(source, props::get);
		Objects.requireNonNull(h, "Query handler parsing failed for " + handler.getClass().getName());
		
		h.execute(input, target);
		
		
	}
	@Override
	public void run() {

		if( !assignHandlerFactories() ) {
			// unable to execute query
			// fail immediately
			changeStatus(RequestStatus.Failed, "No handler found for query extension");
			// done
			return;
		}

		MultipartDirectoryWriter dw = null;
		try {
			if( sources.length > 1 ){
				dw = new MultipartDirectoryWriter(request.createIntermediateDirectory(1), StandardCharsets.UTF_8);
				// first step will not receive input - it will produce the data for the next steps
				executeRequestSource(0, null, dw);
				// run intermediate parts, except for the final part

				for( int i=1; i<sources.length-1; i++ ) {
					// create target directory
					MultipartDirectoryWriter nw = new MultipartDirectoryWriter(request.createIntermediateDirectory(1+i), StandardCharsets.UTF_8);
					// execute next stage
					executeRequestSource(i, dw, nw);
					// verify previous step didn't clutter the directory with undocumented files
					removeIntermediateDirectory(dw);
					dw = nw;
				}
			}
			// final step should write to ZIP archive
			request.createResultData("application/zip");
			try( OutputStream out = request.getResultData().getOutputStream();
					ZipArchiveWriter zip = new ZipArchiveWriter(out, StandardCharsets.UTF_8) )
			{
				executeRequestSource(sources.length-1, dw, zip);
			}
			changeStatus(RequestStatus.Completed, null);
		}catch( Throwable e ){
			// execution failed
			// add stacktrace to error description
			log.log(Level.SEVERE, "Query execution failed", e);
			changeStatus(RequestStatus.Failed, Util.stringStackTrace(e));
		}
		// clean up and delete intermediate directory, if used
		if( dw != null ) {
			removeIntermediateDirectory(dw);
		}
	}

	/**
	 * Make sure the intermediate directory contains only files, which
	 * were written on purpose. Additional files will be deleted with
	 * a warning log entry.
	 * @param dw intermediate directory
	 */
	private void removeIntermediateDirectory(MultipartDirectoryWriter dw) {
		for( MultipartEntry me : dw.getEntries() ) {
			// delete files
			try {
				Files.delete(dw.getBasePath().resolve(me.getName()));
			} catch (IOException e) {
				log.log(Level.WARNING, "Unable to delete intermediate file "+me.getName(), e);
			}
		}
		// TODO delete files still remaining with a warning entry
		try( Stream<Path> files = Files.list(dw.getBasePath()) ){
			files.forEach(file -> {
				log.warning("Removing undocumented file: "+file.toString());
				try {
					Files.delete(file);
				}catch( IOException e ) {
					log.log(Level.WARNING, "Unable to delete the file", e);
				}
			});
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to list files in intermediate directory", e);
		}
		// delete directory (should be empty now)
		try {
			Files.delete(dw.getBasePath());
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to delete intermediate directory "+dw.getBasePath(), e);
		}		
	}

	private boolean assignHandlerFactories(){
		List<Element> ext = request.getRequest().getQuery().extensions;
		sources = new Element[ext.size()];
		handlers = new QueryHandlerFactory[ext.size()];
		int num = 0;
		for( int i=0; i<ext.size(); i++ ) {
			sources[i] = ext.get(i);
			// try to find a factory to handle the source
			handlers[i] = processor.findHandlerFactory(sources[i]);
			if( handlers[i] != null ) {
				num ++;
			}else {
				// no handler found
				String msg = "No handler found for query extension "+sources[i].getTagName()+", xmlns="+sources[i].getNamespaceURI();
				log.severe(msg);
			}
		}
		// return whether we found handlers for all sources
		return num == sources.length;
	}
}
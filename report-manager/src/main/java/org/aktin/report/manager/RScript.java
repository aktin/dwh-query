package org.aktin.report.manager;

import java.nio.file.Path;

/**
 * Takes data files from DataExtractor (patient, encounter, etc.)
 * and runs an R script via the command {@code Rscript}.
 * <p>
 * The script should write all generated output (e.g. plots, diagrams,
 * tables) to files in the working directory. The script should 
 * not output anything on stdout or stderr. Any output on stdout is
 * treated as warnings, any output on stderr is treated as error
 * messages.
 * 
 * @author R.W.Majeed
 *
 */
class RScript {

	/** executable path of the Rscript command */
	private Path rScriptExecutable;
	
	/**
	 * Constructs the class. Locates the Rscript executable and
	 * verifies that all dependencies are available.
	 */
	public RScript(){
		// TODO implement
	}
	
	public void runRscript(Path workingDir, Path mainScript){
		ProcessBuilder pb = new ProcessBuilder(rScriptExecutable.toString(), workingDir.relativize(mainScript).toString());
		pb.directory(workingDir.toFile());
		// TODO run, check output, clean up, etc.
		throw new UnsupportedOperationException("TODO implement");
	}
}

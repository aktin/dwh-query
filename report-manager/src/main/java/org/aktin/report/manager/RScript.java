package org.aktin.report.manager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.FileSystem; 
import java.util.logging.Logger;



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
	private static final Logger log = Logger.getLogger(RScript.class.getName());	

	/** executable path of the Rscript command */
	private Path rScriptExecutable;
	
	/**
	 * Constructs the class. Locates the Rscript executable and
	 * verifies that all dependencies are available.
	 */
	public RScript(){
		// TODO this works only for testing on windows not for deployment
		if (System.getProperty("os.name").substring(0,7).equals("Windows")) { //hoping this will work on every Windows Version
			FileSystem fs = FileSystems.getDefault();
			this.rScriptExecutable = fs.getPath("C:/Program Files/R/R-3.2.4revised/bin/Rscript.exe");
			//System.out.println(rScriptExecutable.toString());
		}
	}
	
	public void runRscript(Path workingDir, Path mainScript){
		log.info("RScript Start");
		log.info(workingDir.relativize(mainScript).toString());
		log.info(rScriptExecutable.toString());
		ProcessBuilder pb = new ProcessBuilder(rScriptExecutable.toString(), workingDir.relativize(mainScript).toString());		
		pb.directory(workingDir.toFile());
		//System.out.println(pb.command());
		try{
            Process process = pb.start();

            // get the error stream of the process and print it
            InputStream error = process.getErrorStream();
            for (int i = 0; i < error.available(); i++) {
            	log.info("" + error.read());
            }
            // get the output stream of the process and print it
            InputStream output = process.getInputStream();
            for (int i = 0; i < output.available(); i++) {
            	log.info("" + output.read());
            }
            try {
            	log.info(Integer.toString((process.waitFor())));  //Should return 0
            } catch(InterruptedException ex) {
            	ex.printStackTrace();
            }
            
            }
            catch(IOException ex)
            {
                ex.printStackTrace();
            }
		// TODO run, check output, clean up, etc.
		//throw new UnsupportedOperationException("TODO implement");
	}
	
}

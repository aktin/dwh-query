package org.aktin.report.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.FileSystem;
import java.util.logging.Logger;

/**
 * Takes data files from DataExtractor (patient, encounter, etc.) and runs an R
 * script via the command {@code Rscript}.
 * <p>
 * The script should write all generated output (e.g. plots, diagrams, tables)
 * to files in the working directory. The script should not output anything on
 * stdout or stderr. Any output on stdout is treated as warnings, any output on
 * stderr is treated as error messages.
 * 
 * @author R.W.Majeed
 *
 */
class RScript {
	private static final Logger log = Logger.getLogger(RScript.class.getName());

	/** executable path of the Rscript command */
	private Path rScriptExecutable;

	/**
	 * Constructs the class. Locates the Rscript executable and verifies that
	 * all dependencies are available.
	 */
	public RScript() {
		// TODO this works only for testing on windows not for deployment
		if (System.getProperty("os.name").substring(0, 7).equals("Windows")) { // hoping
																				// this
																				// will
																				// work
																				// on
																				// every
																				// Windows
																				// Version
			FileSystem fs = FileSystems.getDefault();
			this.rScriptExecutable = fs.getPath("C:/Program Files/R/R-3.2.4revised/bin/Rscript.exe");
			// System.out.println(rScriptExecutable.toString());
		}
	}

	public RScript(Path executable) {
		this.rScriptExecutable = executable;
	}

	public void runRscript(Path workingDir, String mainScript) throws IOException {
		// log.info("RScript Start");
		// log.info(mainScript.toString());
		// log.info(workingDir.toString());
		// log.info(workingDir.resolve(mainScript).toString());
		// log.info(rScriptExecutable.toString());
		ProcessBuilder pb = new ProcessBuilder(rScriptExecutable.toString(), mainScript);
		pb.directory(workingDir.toFile());

		Process process = pb.start();
		// get the error stream of the process and print it
		InputStream error = process.getErrorStream();

		// get the output stream of the process and print it
		InputStream output = process.getInputStream();

		try {
			log.info("R Script Return Code:" + Integer.toString((process.waitFor()))); // Should
																						// return
																						// 0
		} catch (InterruptedException e) {
			throw new IOException("Interrupted during R execution", e);
		}

		/*
		 * for (int i = 0; i < error.available(); i++) { log.info("" +
		 * error.read()); }
		 * 
		 * for (int i = 0; i < output.available(); i++) { log.info("" +
		 * output.read()); }
		 */

		if (error.available() > 0) {
			log.info(convertStreamToString(error));
		}
		if (output.available() > 0) {
			log.info(convertStreamToString(output));
		}

		// TODO run, check output, clean up, etc.
		// throw new UnsupportedOperationException("TODO implement");
	}

	// debugging only
	public String convertStreamToString(InputStream is) throws IOException {
		// To convert the InputStream to String we use the
		// Reader.read(char[] buffer) method. We iterate until the
		// Reader return -1 which means there's no more data to
		// read. We use the StringWriter class to produce the string.
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		}
		return "";
	}

}

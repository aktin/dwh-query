package org.aktin.report;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface for reports. 
 * A report specifies (1) which data should be extracted, (2) one or more R scripts
 * to execute with the extracted data and (3) transformation scripts to generate 
 * PDF or HTML reports.
 * 
 * @author R.W.Majeed
 *
 */
public interface Report {

	/**
	 * Get the report name
	 * @return report name
	 */
	String getName();
	
	/**
	 * Get concept names which are guaranteed to occur only once per encounter.
	 * These concepts will be included in the generated encounter data table.
	 * @return concept IDs
	 */
	String[] getEncounterConcepts();
	/**
	 * Get concepts names which may repeat in a given encounter. For each
	 * of these concepts, a separate data table will be generated.
	 * @return concept IDs
	 */
	String[] getRepeatingConcepts();
	
	/**
	 * Copies all scripts and resource needed for the Rscript invocation
	 * to the specified working directory. The file names of all copied
	 * resources are returned on exit.
	 * 
	 * @param workingDirectory working directory for the R invocation.
	 * @return file names of the copied resources. The first element of the
	 * returned array is the main script that should be run to generate
	 * the report resource.
	 *
	 * @throws IOException IO error. No files were copied.
	 */
	String[] copyResourcesForR(Path workingDirectory)throws IOException;
	
	/**
	 * Copies all transformation scripts needed for the XML-FO transformation
	 * to generate a PDF report to the specified working directory.
	 * 
	 * 
	 * @param workingDirectory for the Apache FOP invocation
	 * @return files names of copied resources. At least two files must
	 * be returned: First element the XML input file and second element 
	 * is the XSL file.
	 * 
	 * @throws IOException IO error. No files were copied.
	 */
	String[] copyResourcesForFOP(Path workingDirectory)throws IOException;
	
	/**
	 * Read static resource for web reports. Static resources are independent
	 * of any generated report data. E.g. css, images, javascript files
	 * @param path to the static resource
	 * @return input stream or {@code null} if the resource is not available
	 */
	InputStream readStaticWebResource(String path);
}

package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.Path;

import org.aktin.report.Report;

/**
 * Generates a Report from the Rscript output files
 * at the desired location.
 * <p>
 * Implementations will generate a PDF document or
 * HTML with image files in specified directory.
 * <p>
 * See also  https://xmlgraphics.apache.org/fop/1.1/embedding.html
 * 
 * @author R.W.Majeed
 *
 */
class PDFGenerator {

	
	/**
	 * Generate a PDF report. After return of the function, no additional files
	 * should remain in {@code workingPath}.
	 * 
	 * @param workingPath path containing the data files generated from R
	 * @param report report to use
	 * @param pdf PDF output location
	 * @throws IOException IO error. {@code workingPath} looks as if 
	 * this function was never called.
	 */
	public void generatePDF(Path workingPath, Report report, Path pdf) throws IOException{
		String[] files = report.copyResourcesForFOP(workingPath);
		// invoke Apache FOP
		// clean up afterwards
		throw new UnsupportedOperationException("TODO implement");
	}
}

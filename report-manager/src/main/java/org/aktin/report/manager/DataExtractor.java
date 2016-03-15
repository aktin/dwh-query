package org.aktin.report.manager;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;

import org.aktin.report.Report;

/**
 * Takes a time interval and concept list and extracts all
 * relevant data to a tabular representation which is readable
 * by R. 
 * <p> 
 * As file format, tab-separated-values are used. The first
 * line contains column headers. In the data values, all
 * newline characters and tab characters are replaced with spaces.
 * <p>
 * The patient and encounter tables are written always. For
 * each repeating concept, additional tables are written (e.g. diagnoses) 
 * 
 * @author R.W.Majeed
 *
 */
class DataExtractor implements Closeable{

	/**
	 * Opens database connections to i2b2 and prepares
	 * SQL statements for execution.
	 * 
	 * @throws IOException io error
	 * @throws SQLException sql error
	 */
	public DataExtractor() throws IOException, SQLException{
		// use JNDI for database connection
	}

	/**
	 * Extracts data to a directory. After return, the directory
	 * will contain the following files:
	 * <ul>
	 * 	<li>patients.txt</li>
	 *  <li>encounters.txt</li>
	 *  <li>[concept].txt for each concept in {@link Report#getRepeatingConcepts()}
	 * </ul>
	 * <p>
	 * In case of a checked exception, it is guaranteed that no files and 
	 * directories are created.
	 * <p>
	 * The 
	 * @param fromTimestamp start timestamp for the data to extract
	 * @param endTimestamp end timestamp for the data to extract
	 * @param report report for which the data will be extracted
	 * @return File names of the created files
	 * 
	 * @throws IOException error writing files
	 * @throws SQLException error while extracting data
	 */
	public String[] extractData(Instant fromTimestamp, Instant endTimestamp, Report report, Path destinationDir) throws IOException, SQLException{
		
		throw new UnsupportedOperationException("TODO implement");
	}
	/**
	 * Closes data base connection to the i2b2 database
	 */
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
}

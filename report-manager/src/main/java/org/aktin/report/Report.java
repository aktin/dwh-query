package org.aktin.report;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Period;
import java.util.Arrays;

import javax.xml.transform.Source;

/**
 * Interface for reports. 
 * A report specifies (1) which data should be extracted, (2) one or more R scripts
 * to execute with the extracted data and (3) transformation scripts to generate 
 * PDF or HTML reports.
 * 
 * <p>
 * XXX TODO We need a way to pass report configuration/settings to the
 * appropriate methods (copyResourcesForFOP, copyResourcesForR). Some
 * parameters are common (e.g. organisation, organisational unit) while
 * others are only valid for reports (e.g. data extraction start timestamp, end timestamp, current time)
 * and some may be report specific (e.g. disable certain parts/plots).
 * Maybe use a Map<String,String> for the prototype.
 * </p>
 * 
 * TODO move this file to the report-manager project
 * 
 * @author R.W.Majeed
 *
 */
public interface Report {

	/**
	 * ID string to differentiate between reports.
	 * Should contain only filename/URL safe characters.
	 * <p>
	 * The default implementation returns the {@link Class#getCanonicalName()}.
	 * </p>
	 * 
	 * @return report ID
	 */
	default String getId(){
		return getClass().getCanonicalName();
	}

	/**
	 * Get the report name
	 * @return report name
	 */
	String getName();
	
	/**
	 * Description 
	 * @return description
	 */
	String getDescription();
	
	/**
	 * Default period for the report, relative to the reference date.
	 * Use only positive values, e.g. 1 Month, 1 Week.
	 * @return
	 */
	Period getDefaultPeriod();

	/**
	 * Get a descriptor for the export configuration.
	 * Can be either an {@code ExportDescriptor} object (see HIStream-export)
	 * or an {@link InputStream} to an XML representation of the export
	 * descriptor.
	 * <p>
	 * If an {@link InputStream} is returned, the stream must be closed by the
	 * caller.
	 * 
	 * </p>
	 * @return export descriptor as {@link InputStream} or {@code ExportDescriptor}
	 */
	Source getExportDescriptor();


	/**
	 * Get available configuration options for the report.
	 * <p>
	 * The returned options and order should remain the same 
	 * (in terms of {@link Arrays#deepEquals(Object[], Object[])}).
	 * </p>
	 * <p>
	 * XXX this method may be moved to a ReportFactory, since it is basically
	 * static for the report.
	 * </p>
	 * @return report options
	 */
	ReportOption<?>[] getConfigurationOptions();
	
	/**
	 * Set value for the specified option
	 * @param option option to set
	 * @param value value
	 * @throws IllegalArgumentException if the option is not supported, e.g. does not belong to this report
	 */
	<U> void setOption(ReportOption<U> option, U value) throws IllegalArgumentException;
	<U> U getOption(ReportOption<U> option)throws IllegalArgumentException;
	
	/**
	 * Read static resource for web reports. Static resources are independent
	 * of any generated report data. E.g. css, images, javascript files
	 * @param path to the static resource
	 * @return input stream or {@code null} if the resource is not available
	 */
	InputStream readStaticWebResource(String path);
	

	/**
	 * Get required preference keys, which will be used to add
	 * preference values during report creation.
	 * <p>The default implementation returns an empty list</p>
	 * 
	 * @return preference keys
	 */
	default String[] getRequiredPreferenceKeys(){
		return new String[]{};
	}
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
	
}

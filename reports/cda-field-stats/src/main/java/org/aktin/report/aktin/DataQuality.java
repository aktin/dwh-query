package org.aktin.report.aktin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.aktin.report.AnnotatedReport;
import java.util.logging.Logger;

@AnnotatedReport.Report(
		displayName="AKTIN Daten-Qualitätsbericht", 
		description="Standardisierter Qualitätsbericht des AKTIN Projekts",
		defaultPeriod="P1M",
		preferences={"local.o", "local.ou"} // see dwh-api/org.aktin.dwh.PreferenceKey
		)
public class DataQuality extends AnnotatedReport {
	private static final Logger log = Logger.getLogger(DataQuality.class.getName());	


	@Override
	public Source getExportDescriptor() {
		return new StreamSource(getClass().getResourceAsStream("/export-descriptor.xml"));
	}

	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String[] files = new String[]{"data-quality.R","xhtml-table.R"};
		for( String file : files ){
			try( InputStream in = getClass().getResourceAsStream("/"+file) ){
				Files.copy(in, workingDirectory.resolve(file));			
			}			
		}
		return files;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		String[] files = new String[]{"report-content.xml","fo-report-quality.xsl","report-data.xml","AKTIN_Logo_final.svg","BMBF.svg"};
		for( String file : files ){
			try( InputStream in = getClass().getResourceAsStream("/"+file) ){
				Files.copy(in, workingDirectory.resolve(file));			
			}			
		}
		return files;
	}

}

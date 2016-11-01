package org.aktin.report.aktin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
//import java.util.logging.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.aktin.report.AnnotatedReport;

@AnnotatedReport.Report(
		displayName="AKTIN Monatsbericht", 
		description="Standardisierter Monatsbericht des AKTIN Projekts",
		defaultPeriod="P1M",
		preferences={"local.o", "local.ou"} // see dwh-api/org.aktin.dwh.PreferenceKey
		)
public class AktinMonthly extends AnnotatedReport {
	//private static final Logger log = Logger.getLogger(AktinMonthly.class.getName());	


	@Option
	private Boolean dummyOption;
	
	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String[] resNames = {"generate-report-resources.R","xhtml-table.R","CEDIS.csv","ICD-3Steller.csv","factors.csv","parse_derive.R","chapter1.R","chapter2.R","chapter3.R","chapter4.R","chapter5.R","chapter6.R","chapter7.R","chapter8.R","chapter9.R"};
		copyClasspathResources(workingDirectory, resNames);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		//log.info("Using configuration option="+dummyOption);
		String[] resNames = {"report-content.xml","fo-report-fertig.xsl","report-data.xml","AKTIN_Logo_final.svg","BMBF.svg"};
		copyClasspathResources(workingDirectory, resNames);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}

	@Override
	public InputStream readStaticWebResource(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Source getExportDescriptor() {
		return new StreamSource(getClass().getResourceAsStream("/export-descriptor.xml"));
	}

}

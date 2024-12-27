package org.aktin.report.aktin;

import java.io.IOException;
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
	
	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String[] resNames = {
		"src/generate-report-resources.R",
		"src/xhtml-table.R",
		"data/CEDIS.csv",
		"data/ICD-3Steller.csv",
		"data/factors.csv",
		"src/parse_derive.R",
		"src/chapters/chapter1.R",
		"src/chapters/chapter2.R",
		"src/chapters/chapter3.R",
		"src/chapters/chapter4.R",
		"src/chapters/chapter5.R",
		"src/chapters/chapter6.R",
		"src/chapters/chapter7.R",
		"src/chapters/chapter8.R",
		"src/chapters/chapter9.R"};
		copyClasspathResources(workingDirectory, resNames);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		//log.info("Using configuration option="+dummyOption);
		String[] resNames = {
			"data/report-content.xml",
			"data/fo-report.xsl",
			"data/report-data.xml",
			"data/Notaufnahmeregister_Logo_2021.svg",
			"data/BMBF.svg"};
		copyClasspathResources(workingDirectory, resNames);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}

	@Override
	public Source getExportDescriptor() {
		return new StreamSource(getClass().getResourceAsStream("data/export-descriptor.xml"));
	}

}

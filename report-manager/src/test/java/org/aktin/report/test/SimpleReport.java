package org.aktin.report.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.aktin.report.AnnotatedReport;

@AnnotatedReport.Report(
		displayName="TestReport", 
		description="Report for testing",
		defaultPeriod="P1M",
		preferences={"local.o", "local.ou", "local.cn"} // see dwh-api/org.aktin.dwh.PreferenceKey
		)
public class SimpleReport extends AnnotatedReport{

	@Override
	public Source getExportDescriptor() {
		return new StreamSource(getClass().getResourceAsStream("/export-descriptor.xml"));
	}

	@Override
	public InputStream readStaticWebResource(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String file = "demo.R";
		try( InputStream in = getClass().getResourceAsStream("/"+file) ){
			Files.copy(in, workingDirectory.resolve(file));			
		}
		return new String[]{file};
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		String[] files = new String[]{"empty.xml","fo-report-test.xsl"};
		for( String file : files ){
			try( InputStream in = getClass().getResourceAsStream("/"+file) ){
				Files.copy(in, workingDirectory.resolve(file));			
			}			
		}
		return files;
	}

}

package org.aktin.report.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

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
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String[] files = new String[]{"demo.R", "include.R"};
		copyClasspathResources(workingDirectory, files);
		return files;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		String[] files = new String[]{"empty.xml","fo-report-test.xsl"};
		copyClasspathResources(workingDirectory, files);
		return files;
	}

}

package org.aktin.report.wolfsburg;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import javax.xml.transform.Source;

import org.aktin.report.AnnotatedReport;

@AnnotatedReport.Report(
		displayName="AKTIN Monatsbericht", 
		description="Standardisierter Monatsbericht des AKTIN Projekts",
		defaultPeriod="P1M",
		preferences={"local.o", "local.ou", "local.cn"} // see dwh-api/org.aktin.dwh.PreferenceKey
		)
public class WolfsburgMonthly extends AnnotatedReport {
	private static final Logger log = Logger.getLogger(WolfsburgMonthly.class.getName());	


	@Option
	private Boolean dummyOption;
	
	private void copyResources(String[] names, String resourcePrefix, Path workingDirectory) throws IOException{
		//System.out.println(names.toString());
		//log.info(resourcePrefix);
		for( String name : names ){
			//log.info(name);
			try( InputStream in = getClass().getResourceAsStream(resourcePrefix+name) ){
				//System.out.println(in.toString());
				//System.out.println(workingDirectory.resolve(name));
				Files.copy(in, workingDirectory.resolve(name)); 				
			}
		}		
	}
	
	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		//String[] resNames = {"empty-test.R","generate-report-resources-mod.R","xhtml-table.R"};
		String[] resNames = {"generate-report-resources-mod.R","xhtml-table.R"};
		String resPrefix = "/";
		//log.info(workingDirectory.toString());
		copyResources(resNames, resPrefix, workingDirectory);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		log.info("Using configuration option="+dummyOption);
		String[] resNames = {"report-content.xml","fo-report-fertig.xsl","report-data.xml"};
		String resPrefix = "/";
		copyResources(resNames, resPrefix, workingDirectory);
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
		URL url = getClass().getResource("/export-descriptor.xml");
		return this.createStreamSource(url);
	}

}

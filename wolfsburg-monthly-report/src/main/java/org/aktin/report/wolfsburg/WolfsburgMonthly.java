package org.aktin.report.wolfsburg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.aktin.report.Report;

public class WolfsburgMonthly implements Report {
	private static final Logger log = Logger.getLogger(WolfsburgMonthly.class.getName());	

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getEncounterConcepts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getRepeatingConcepts() {
		// TODO Auto-generated method stub
		return null;
	}


	private void copyResources(String[] names, String resourcePrefix, Path workingDirectory) throws IOException{
		System.out.println(names.toString());
		for( String name : names ){
			try( InputStream in = getClass().getResourceAsStream(resourcePrefix+name) ){
				System.out.println(in.toString());
				System.out.println(workingDirectory.resolve(name));
				Files.copy(in, workingDirectory.resolve(name));				
			}
		}		
	}
	
	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String[] resNames = {"generate-report-resources-mod.R"};
		String resPrefix = "/";
		log.info(workingDirectory.toString());
		copyResources(resNames, resPrefix, workingDirectory);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
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

}

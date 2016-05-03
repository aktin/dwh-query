package org.aktin.report.wolfsburg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.aktin.report.Report;

public class WolfsburgMonthly implements Report {

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
		for( String name : names ){
			try( InputStream in = getClass().getResourceAsStream(resourcePrefix+name) ){
				Files.copy(in, workingDirectory.resolve(name));				
			}
		}		
	}
	
	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		String[] resNames = {"generate-report-resources-mod.R"};
		String resPrefix = "/";
		copyResources(resNames, resPrefix, workingDirectory);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
		
		//ToDo put RScript in workingDirectory
		//getClass().getResource(name);
		//String [] Rfiles = {"C:\\temp\\RScript-Test\\generate-report-resources-mod.R"};
		//return Rfiles;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		//ToDo put FOP-files in workingDirectory
		//getClass().getResource(name);
		String [] FOPfiles = {"C:\\temp\\target\\report-content.xml","C:\\temp\\target\\fo-report-fertig.xsl","C:\\temp\\target\\report-data.xml"}; //it is not necessary to put the data file in the array but it has to be loaded and should be cleaned up afterwards
		return FOPfiles;
	}

	@Override
	public InputStream readStaticWebResource(String path) {
		// TODO Auto-generated method stub
		return null;
	}

}

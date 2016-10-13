package org.aktin.report.manager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.aktin.report.Report;

public class Snueffelstueck {
	private static final Logger log = Logger.getLogger(Snueffelstueck.class.getName());	
	
	//This is a manager/test class to orchestrate the report-component implementations
	//Example execution of WolfsburgMonthly Report for testing
	
	public static void main(String[] args) {
		//Step -1: What is needed additionally?
		FileSystem fs = FileSystems.getDefault();
		Path work = fs.getPath("C:/temp/RScript-Tempdir"); //WorkDir is a parameter for following steps
		Path pdf = fs.getPath("C:/temp/RScript-Tempdir/AKTIN-Monatsbericht.pdf"); //pdf is a parameter for the final PDF output location
		
		//Step 0: Instantiate WolfsburgMonthly
		//not really a seperate step since nothing happens before Data Extraction
		Report ReportAktin= new org.aktin.report.aktin.AktinMonthly();
		
		
		//Step 1: Data Extraction
		//toDo - not implemented yet
		//at this point Data Sources (TSV) are just present in the WorkDir
		try {
			DataExtractorImpl DatEx = new DataExtractorImpl(null);
			DatEx.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//Step 2: Execute R-Script
		RScript RExecutor = new RScript();
		Path script;
		try {
			String [] files = ReportAktin.copyResourcesForR(work);
			
			//forTesting - place patient.txt and encounter.txt
			Snueffelstueck sn = new Snueffelstueck();
			sn.copyTestResourcesForR(work);
			
			// first element is file to execute
			RExecutor.runRscript(work,files[0]);
			//ToDo - runRScript does not get the whole list of files, so we'll probably want to delete the R files here since we wont need them anymore
			deleteResources(files,work);
			//log.info("R Script executed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//Step 3: Execute PDFGenerator
		PDFGenerator PDFExecutor = new PDFGenerator();
		try {
			PDFExecutor.generatePDF(work, ReportAktin, pdf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//Step 4: Cleanup
		//toDo - Each Step should clean up, can be done as soon as the process is complete and all resources are available from WolfsburgMonthly.java
		//Cleanup is included in PDFGenerator
		
		//Only result.pdf stays in WorkDir (can be overwritten by next run)
	}
	
	private static void deleteResources(String[] names, Path workingDirectory) {
		//System.out.println(names.toString());
		//log.info(resourcePrefix);
		for( String name : names ){
			try {
			    Files.delete(workingDirectory.resolve(name));
			} catch (NoSuchFileException x) {
				log.severe("no such file or directory");
			} catch (DirectoryNotEmptyException x) {
				log.severe("directory not empty");
			} catch (IOException x) {
				log.severe("IOException");
			    // File permission problems are caught here.
			}
		}		
	}
	
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
	
	private String[] copyTestResourcesForR(Path workingDirectory) throws IOException {
		//String[] resNames = {"empty-test.R","generate-report-resources-mod.R","xhtml-table.R"};
		String[] resNames = {"patients.txt","encounters.txt","diagnoses.txt","aktin.png","bmbf.png"};
		String resPrefix = "/";
		//log.info(workingDirectory.toString());
		copyResources(resNames, resPrefix, workingDirectory);
		// return resource names within workingDirectory. 
		// do not include path separators or sub directories.
		return resNames;
	}
	
}

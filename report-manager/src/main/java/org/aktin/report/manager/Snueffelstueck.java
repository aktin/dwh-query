package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
		Path pdf = fs.getPath("C:/temp/result.pdf"); //pdf is a parameter for the final PDF output location
		
		//Step 0: Instantiate WolfsburgMonthly
		//not really a seperate step since nothing happens before Data Extraction
		Report ReportWolfsburg = new org.aktin.report.wolfsburg.WolfsburgMonthly();
		
		
		//Step 1: Data Extraction
		//toDo - not implemented yet
		//toDo - change DataSources to TSV
		//at this point Data Sources are just present in the WorkDir (old version csv)
		
		
		//Step 2: Execute R-Script
		RScript RExecutor = new RScript();
		Path script;
		try {
			script = fs.getPath(ReportWolfsburg.copyResourcesForR(work)[0]);
			RExecutor.runRscript(work,script);
			//ToDo - runRScript does not get the whole list of files, so we'll probably want to delete the R files here since we wont need them anymore
			log.info("R Script executed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		//Step 3: Execute PDFGenerator
		PDFGenerator PDFExecutor = new PDFGenerator();
		try {
			PDFExecutor.generatePDF(work, ReportWolfsburg, pdf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//Step 4: Cleanup
		//toDo - Each Step should clean up, can be done as soon as the process is complete and all resources are available from WolfsburgMonthly.java
	}
}

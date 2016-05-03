package org.aktin.report.manager;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.aktin.report.Report;

public class Snueffelstueck {
	
	//This is a manager/test class to orchestrate the report-component implementations
	//Example execution of WolfsburgMonthly Report for testing
	
	public static void main(String[] args) {
		//Step -1: What is needed additionally?
		FileSystem fs = FileSystems.getDefault();
		Path work = fs.getPath("C:\\temp\\RScript-Test"); //WorkDir is a parameter for following steps
		Path pdf = fs.getPath("C:\\temp\\result.pdf"); //pdf is a parameter for the final PDF output location
		
		//Step 0: Instantiate WolfsburgMonthly
		Report ReportWolfsburg = new org.aktin.report.wolfsburg.WolfsburgMonthly();
		//contains only methods to copy static report information/files to the workDir, not necessary for testing at this point
		
		
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
			System.out.println("R Script executed");
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

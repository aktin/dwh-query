package org.aktin.report.manager;

import org.aktin.dwh.ExtractedData;

import de.sekmi.histream.export.ExportSummary;

class ExtractedDataImpl implements ExtractedData {
	private int patientCount;
	private int visitCount;
	private String[] fileNames;

	public ExtractedDataImpl(ExportSummary summary){
		this.patientCount = summary.getPatientCount();
		this.visitCount = summary.getVisitCount();
	}
	@Override
	public int getPatientCount() {
		return this.patientCount;
	}

	@Override
	public int getVisitCount() {
		return this.visitCount;
	}

	@Override
	public String[] getDataFileNames() {
		return fileNames;
	}
	void setDataFileNames(String[] fileNames){
		this.fileNames = fileNames;
	}

}

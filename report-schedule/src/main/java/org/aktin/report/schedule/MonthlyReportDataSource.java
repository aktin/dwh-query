package org.aktin.report.schedule;


import org.aktin.report.ArchivedReport;

/**
 * Report data source giving pretty file names for emailed reports.
 * 
 * @author R.W.Majeed
 *
 */
public class MonthlyReportDataSource extends ArchivedReportDataSource {
	private String name;
	public MonthlyReportDataSource(ArchivedReport report, String name) {
		super(report);
		this.name = name;
	}

	@Override
	public String getName() {
		if( name != null ){
			return name;
		}else{
			return super.getName();
		}
	}

}

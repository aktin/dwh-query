package org.aktin.report.schedule;


import java.text.Normalizer;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

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

	public static String normalizedMonthName(Month month, Locale locale){
		String name = month.getDisplayName(TextStyle.FULL, locale);
		// make sure we don't have non-ASCII characters for file names
		// TODO this may fail for other languages, testing required
		if( locale.toLanguageTag().equals("de-DE") && month == Month.MARCH ){
			name = "Maerz";
		}else{
			name = Normalizer.normalize(name, Normalizer.Form.NFKC);
		}
		return name;
	}
	public static String createFriendlyFileName(Month month, int reportId, Locale locale){
		String monthName = normalizedMonthName(month, locale);
		return "AKTIN Monatsbericht "+monthName+" ("+reportId+")";
	}

}

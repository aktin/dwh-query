package org.aktin.report.schedule;

import java.time.Month;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

public class TestEmailAttachmentName {

	@Test
	public void verifyGermanMonthName(){
		Locale locale = Locale.forLanguageTag("de-DE");
		String name = MonthlyReportDataSource.normalizedMonthName(Month.MARCH, locale);
		Assert.assertEquals("Maerz", name);
		name = MonthlyReportDataSource.normalizedMonthName(Month.AUGUST, locale);
		Assert.assertEquals("August", name);
	}
}

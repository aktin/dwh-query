package org.aktin.report;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Period;

import javax.xml.transform.Source;

import org.junit.Test;
import static org.junit.Assert.*;

@AnnotatedReport.Report(displayName="rname", description="rdesc", defaultPeriod="P1M")
public class TestAnnotatedReport extends AnnotatedReport {

	@Option(displayName="oname1", description="odesc1", defaultValue="odef1")
	protected String optionString;
	
	@Option
	private Boolean optionBoolean;
	
	@Test
	public void verifyReportName(){
		assertEquals("rname", getName());
	}
	@Test
	public void verifyReportDescription(){
		assertEquals("rdesc", getDescription());
	}
	@Test
	public void verifyReportDefaultPeriod(){
		assertEquals(Period.ofMonths(1), getDefaultPeriod());
	}
	@Test
	@SuppressWarnings("unchecked")
	public void verifyOptions() throws Exception{
		ReportOption<?>[] options = getConfigurationOptions();
		assertEquals(2, options.length);
		ReportOption<String> str = null;
		ReportOption<Boolean> bool = null;
		for( ReportOption<?> opt : options ){
			if( opt.getType() == String.class ){
				str = (ReportOption<String>)opt;
			}else if( opt.getType() == Boolean.class ){
				bool = (ReportOption<Boolean>)opt;
			}else{
				throw new Exception("Unexpected option type "+opt.getType());
			}
		}
		assertNotNull(str);
		assertEquals("oname1", str.getDisplayName());

		assertNotNull(bool);
		assertEquals("optionBoolean", bool.getDisplayName());
		// check values
		setOption(str, "asdf");
		assertEquals("asdf", optionString);

		// boolean
		setOption(bool, true);
		assertEquals(true, optionBoolean);
	}
	@Override
	public Source getExportDescriptor() {
		return createStreamSource(getClass().getResource("/export-descriptor.xml"));
	}

	@Override
	public InputStream readStaticWebResource(String path) {
		return null;
	}

	@Override
	public String[] copyResourcesForR(Path workingDirectory) throws IOException {
		return null;
	}

	@Override
	public String[] copyResourcesForFOP(Path workingDirectory) throws IOException {
		return null;
	}

}

package org.aktin.report.manager;

import java.util.Arrays;
import java.util.Collections;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.Module;
import org.aktin.Preference;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.Report;

/**
 * Manage all registered reports. Generate
 * reports.
 * <p>
 * Future feature: load report specifications
 * without java code via dynamic CDI producers
 * http://jdevelopment.nl/dynamic-cdi-producers/
 * CDI Bean interface and Extension which observes
 * {@link AfterBeanDiscovery}.
 * </p>
 * @author Raphael
 *
 */
@Singleton
//@Preferences(group="reports")
public class ReportManager extends Module{
	@Inject @Any
	Instance<Report> cdiReports;
	private Report[] staticReports;
	
	@Inject @Preference(key=PreferenceKey.i2b2Project)
	String rScript;
	

	/**
	 * Empty constructor for CDI
	 */
	protected ReportManager(){
	}
	public ReportManager(String rScript, Report...reports){
		this.rScript = rScript;
		this.staticReports = reports;
		
	}
	public Iterable<Report> reports(){
		if( cdiReports != null ) {
			return cdiReports;
		}else if( staticReports != null ){
			return Arrays.asList(staticReports);
		}else{
			return Collections.emptyList();
		}
	}
	
}

package org.aktin.report.manager;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.Module;
import org.aktin.Preference;
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
	public static final String PREF_RSCRIPT = "reports.rScript";
	
	@Inject @Any
	Instance<Report> reports;
	
	@Preference(id=PREF_RSCRIPT)
	public void setRScript(String path){
	}
	
	public Iterable<Report> reports(){
		return reports;
	}
	
}

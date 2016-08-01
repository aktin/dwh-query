package org.aktin.report.schedule;

import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.NamingException;

import org.aktin.Module;
import org.aktin.dwh.db.Manager;
import org.aktin.report.manager.ReportManager;

/**
 * Reads schedule from database to generate reports and sends
 * them via Email to the specified recipients.
 * 
 * <p>
 * Requires a JNDI mail session configured at {@code java:comp/env/mail/aktin}.
 * See http://www.mastertheboss.com/jboss-server/jboss-configuration/jboss-mail-service-configuration
 * </p>
 * 
 * @author R.W.Majeed
 *
 */
@Singleton
@Startup
public class ReportSchedule extends Module {

	@Resource
	private TimerService timer;
	
	@Inject
	private ReportManager reports;
	
	@Inject
	public ReportSchedule(ReportManager reports) throws SQLException, NamingException{
		this.reports = reports;
		loadSchedule();
		this.reports.getVersion(); // prevent unused warning for now
	}
	
	private void loadSchedule() throws SQLException, NamingException{
		try( Connection c = Manager.openConnection() ){
			// TODO load config from database
		}
	}
	
	@Timeout
	private void timerCallback(Timer timer){
		// TODO check schedule, create report, submit result
	}

	// TODO methods to add/remove schedule entries
	
	// TODO flush config to database after change
	
}

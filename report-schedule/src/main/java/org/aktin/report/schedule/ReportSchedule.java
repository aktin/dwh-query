package org.aktin.report.schedule;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.aktin.Module;
import org.aktin.report.Report;
import org.aktin.report.ReportManager;

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
	
	// TODO inject
	private DataSource ds;
	
	@Inject
	private ReportManager reports;
	
	private Map<Timer, Report> schedule;
	
	@Inject
	public ReportSchedule(ReportManager reports) throws SQLException, NamingException{
		this.reports = reports;
		loadSchedule();
		this.reports.getClass(); // prevent unused warning for now
	}
	
	private void loadSchedule() throws SQLException, NamingException{
		try( Connection c = ds.getConnection() ){
			// TODO load config from database
			// create timer callbacks
			if( true )return;
			Report report = null;
			// TODO find report
			ScheduleExpression expr = new ScheduleExpression();
			// TODO use schedule from database
			Timer t = timer.createCalendarTimer(expr);
			schedule.put(t, report);
			
		}
	}
	
	@Timeout
	private void timerCallback(Timer timer){
		Report report = schedule.get(timer);
		
		// TODO check schedule, create report, submit result
	}

	// TODO methods to add/remove schedule entries
	
	// TODO flush config to database after change
	
}

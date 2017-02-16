package org.aktin.report.schedule;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.aktin.Module;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.ArchivedReport;
import org.aktin.report.Report;
import org.aktin.report.ReportArchive;
import org.aktin.report.ReportInfo;
import org.aktin.report.ReportManager;

/**
 * Reads schedule from database to generate reports and sends them via Email to
 * the specified recipients.
 * <p>
 * The first implementation supports only the monthly AKTIN report. The report
 * will be sent via email on the 3rd day of the month - with data for the
 * previous month.
 * </p>
 * <p>
 * Requires a JNDI mail session configured at {@code java:comp/env/mail/aktin}.
 * See
 * http://www.mastertheboss.com/jboss-server/jboss-configuration/jboss-mail-service-configuration
 * </p>
 * 
 * @author R.W.Majeed
 *
 */
@javax.ejb.Singleton
@javax.ejb.Startup
public class ReportSchedule extends Module {
	private static final Logger log = Logger.getLogger(ReportSchedule.class.getName());
	private static final String SCHEDULE_USER = "REPORT_SCHEDULE";
	private static final String MONTHLY_REPORT_ID = "org.aktin.report.aktin.AktinMonthly";

	@Inject
	private ReportManager reports;
	@Inject
	private ReportArchive archive;
	@Inject
	private Preferences prefs;
	@Resource
    private TimerService timer;

	private Map<Timer, Report> schedule;
	private ZoneId timeZone;
	private Address[] emailRecipients;

	private Session mailSession;

	public ReportSchedule() {
		// load preferences
	}

	private void lookupJndiMailSession() throws NamingException{
		String jndiName = prefs.get(PreferenceKey.emailSession);
		log.info("Using mail session "+jndiName);
		InitialContext ctx = new InitialContext();
		mailSession = (Session)ctx.lookup(jndiName);
	}

	@PostConstruct
	public void initialize(){
		try {
			loadConfiguration();
		} catch (AddressException | NamingException e) {
			throw new IllegalStateException(e);
		}
		loadSchedule();
	}
	private void loadConfiguration() throws AddressException, NamingException{
		timeZone = ZoneId.of(prefs.get(PreferenceKey.timeZoneId));
		emailRecipients = InternetAddress.parse(prefs.get(PreferenceKey.email));
		// TODO load email session
		lookupJndiMailSession();
	}
	private void loadSchedule() {
		schedule = new HashMap<Timer, Report>();
		// find report
		Report report = reports.getReport(MONTHLY_REPORT_ID);
		Objects.requireNonNull(report, "Scheduled report not available: " + MONTHLY_REPORT_ID);

		// create schedule
		ScheduleExpression expr = new ScheduleExpression();
		// run third of month
		expr.dayOfMonth(3);
		Timer t = timer.createCalendarTimer(expr);
		schedule.put(t, report);
		log.info("Monthly report scheduled for " + expr.toString());
		log.info("Time until report generation: " + Duration.ofMillis(t.getTimeRemaining()).toString());

		// verify email session
	}

	public void createAndSendMonthlyReport(Report report){
		// calculate timestamps for previous month
		// get current local date
		LocalDate today = Instant.now().atZone(timeZone).toLocalDate();
		// start on first day of month
		LocalDateTime start = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
		// end with start of first day in current month
		LocalDateTime end = today.withDayOfMonth(1).atStartOfDay();

		ReportInfo info = report.createReportInfo(start.atZone(timeZone).toInstant(), end.atZone(timeZone).toInstant());
		// create in archive
		final ArchivedReport ar;
		try {
			ar = archive.addReport(info, SCHEDULE_USER);
			ar.createAsync(reports).whenComplete( (v,t) -> reportFinished(ar,t) );
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to create report archive entry", e);
			return;
		}
	}

	private void reportFinished(ArchivedReport report, Throwable exception){
		// TODO send email
		if( exception != null ){
			// failed
			log.log(Level.WARNING, "TODO send email with failure for report "+report.getId(), exception);
		}else{
			log.info("TODO send email with generated report "+report.getId());
		}
	}

	@Timeout
//	@Schedule(dayOfMonth="3")
	private void timerCallback(Timer timer){
		Report report = schedule.get(timer);
		log.info("Scheduled report timer triggered: "+report.getId());
		createAndSendMonthlyReport(report);
		log.info("Next scheduled report at "+timer.getNextTimeout());
	}

	private void emailReport(ArchivedReport report) throws AddressException, MessagingException{
		MimeMessage msg = new MimeMessage(mailSession);
		
		// sender address
		Address[] replyTo = InternetAddress.parse(prefs.get(PreferenceKey.emailReplyTo));
		msg.setReplyTo(replyTo);

		msg.setRecipients(RecipientType.TO, emailRecipients);
		msg.setSubject("AKTIN Monatsbericht");
		msg.setSentDate(new Date());
		msg.setText("Sehr geehrte Damen und Herren,\nanbei finden Sie den aktuellen AKTIN Monatsbericht.\nDiese Nachricht wurde automatisch erzeugt von Ihrem AKTIN Server");
		MimeMultipart mp = new MimeMultipart(new FileDataSource(report.getLocation().toFile()));
		msg.setContent(mp);
		Transport.send(msg);
	}

	// TODO methods to add/remove schedule entries

	// TODO flush config to database after change

}

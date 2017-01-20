package org.aktin.report.schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage.RecipientType;

import org.aktin.Module;
import org.aktin.Preferences;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.ArchivedReport;
import org.aktin.report.GeneratedReport;
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
@Singleton
@Startup
public class ReportSchedule extends Module {
	private static final Logger log = Logger.getLogger(ReportSchedule.class.getName());
	private static final String SCHEDULE_USER = "REPORT_SCHEDULE";
	private static final String MONTHLY_REPORT_ID = "org.aktin.report.aktin.AktinMonthly";

	@Resource
	private TimerService timer;

	private ReportManager reports;
	private ReportArchive archive;

	private Map<Timer, Report> schedule;
	private ZoneId timeZone;
	private Path tempPath;
	private String emailRecipients;
	private Session emailSession;

	@Inject
	public ReportSchedule(ReportManager reports, ReportArchive archive, Preferences prefs) {
		this.reports = reports;
		this.archive = archive;
		this.reports.getClass(); // prevent unused warning for now
		// load preferences
		timeZone = ZoneId.of(prefs.get(PreferenceKey.timeZoneId));
		tempPath = Paths.get(prefs.get(PreferenceKey.reportTempPath));
		emailRecipients = "rmajeed@gmx.de"; // TODO load from preferences
		// TODO load email session
		String jndiMail = prefs.get(PreferenceKey.emailSession);
		log.info("Using mail session "+jndiMail);
		emailSession = null; // TODO use jndi
	}

	@PostConstruct
	public void loadSchedule() {
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
		final int archiveId;
		try {
			archiveId = archive.addReport(info, SCHEDULE_USER).getId();
		} catch (IOException e) {
			log.log(Level.SEVERE, "Failed to create report archive entry", e);
			return;
		}
		// generate report
		try {
			Path temp = Files.createTempFile(tempPath, "scheduled", ".pdf");
			CompletableFuture<? extends GeneratedReport> f = reports.generateReport(info, temp);
			f.thenAccept(r -> reportCompleted(archiveId, r));
		} catch (IOException e) {
			reportFailure(archiveId, null, e);
		}		
	}
	@Timeout
	private void timerCallback(Timer timer){
		Report report = schedule.get(timer);
		log.info("Scheduled report timer triggered: "+report.getId());
		createAndSendMonthlyReport(report);
		log.info("Next scheduled report at "+timer.getNextTimeout());
	}

	private void reportFailure(int archiveId, String description, Exception e) {
		try {
			archive.setReportFailure(archiveId, description, e);
			// log warning. the error was also stored in the report archive
			log.log(Level.WARNING, "Scheduled report failed: " + archiveId, e);
		} catch (IOException e1) {
			e.addSuppressed(e1);
			log.log(Level.SEVERE, "Unable to store report failure", e);
		}
	}

	private void reportCompleted(int archiveId, GeneratedReport report) {
		ArchivedReport archived;
		try {
			archived = archive.setReportResult(archiveId, report);
			log.info("Scheduled report completed: " + archiveId);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Unable to store report result", e);
			return;
		}
		emailReport(archived);
	}

	private void emailReport(ArchivedReport report){
		javax.mail.Session session = null;
		// TODO get from JNDI java:comp/env/mail/aktin
		MimeMessage msg = new MimeMessage(session);
		try{
			msg.setRecipients(RecipientType.TO, emailRecipients);
			msg.setSubject("AKTIN Monatsbericht");
			msg.setSentDate(new Date());
			msg.setText("Sehr geehrte Damen und Herren,\nanbei finden Sie den aktuellen AKTIN Monatsbericht.\nDiese Nachricht wurde automatisch erzeugt von Ihrem AKTIN Server");
			MimeMultipart mp = new MimeMultipart(new FileDataSource(report.getLocation().toFile()));
			msg.setContent(mp);
			Transport.send(msg);
		}catch( MessagingException e ){
			log.log(Level.SEVERE, "Failed to send report via email", e);
		}
	}

	// TODO methods to add/remove schedule entries

	// TODO flush config to database after change

}

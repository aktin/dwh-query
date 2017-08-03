package org.aktin.report.schedule;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.aktin.Module;
import org.aktin.Preferences;
import org.aktin.dwh.EMail;
import org.aktin.dwh.PreferenceKey;
import org.aktin.report.ArchivedReport;
import org.aktin.report.ArchivedReport.Status;
import org.aktin.report.InsufficientDataException;
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
	private Address[] replyTo;

	private Session mailSession;
	private Locale locale;

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
		replyTo = InternetAddress.parse(prefs.get(PreferenceKey.emailReplyTo));
		// determine language
		String langTag = prefs.get(PreferenceKey.languageTag);
		if( langTag == null ){
			langTag = "de-DE"; // default to German
		}
		locale = Locale.forLanguageTag(langTag);
		log.info("Using locale "+locale);
		
		// load email session
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
		expr.dayOfMonth(3).hour(6).minute(13);
		// create non-persistent timer. we need the timer callback in the same instance
		Timer t = timer.createCalendarTimer(expr, new TimerConfig(MONTHLY_REPORT_ID, false));
		schedule.put(t, report);
		log.info("Monthly report scheduled for " + expr.toString());
		log.info("Time until report generation: " + Duration.ofMillis(t.getTimeRemaining()).toString());

		// verify email session
	}

	private ReportInfo reportForPreviousMonth(Report report){
		// calculate timestamps for previous month
		// get current local date
		LocalDate today = Instant.now().atZone(timeZone).toLocalDate();
		// start on first day of month
		LocalDateTime start = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
		// end with start of first day in current month
		LocalDateTime end = today.withDayOfMonth(1).atStartOfDay();

		return report.createReportInfo(start.atZone(timeZone).toInstant(), end.atZone(timeZone).toInstant());
	}

	/**
	 * J2EE event observer method which submits reports via email.
	 * The report is generated (and archived) if necessary.
	 *
	 * @param info report to send.
	 */
	public void sendReportViaEmail(@Observes @EMail ReportInfo info){
		if( info instanceof ArchivedReport ){
			// already generated, just send the email
			reportFinished((ArchivedReport)info, null);
		}else{
			// need to generate the report first
			final ArchivedReport ar;
			try {
				ar = archive.addReport(info, SCHEDULE_USER);
				ar.createAsync(reports).whenComplete( (v,t) -> reportFinished(ar,t) );
			} catch (IOException e) {
				log.log(Level.SEVERE, "Failed to create report archive entry", e);
			}
		}
	}
	private void reportFinished(ArchivedReport report, Throwable exception){
		try {
			sendReport(report, exception);
		} catch (MessagingException e) {
			log.log(Level.SEVERE, "Unable to send monthly report email", e);
		}
	}

	private static void appendStacktrace(StringBuilder body, Throwable exception){
		StringWriter w = new StringWriter(2048);
		try( PrintWriter p = new PrintWriter(w) ){
			exception.printStackTrace(p);
		}
		body.append(w.getBuffer());
	}

	// TODO use string templating engine and move email code to separate bean
	// TODO use locale for different languages
	private void sendReport(ArchivedReport report, Throwable exception) throws MessagingException{
		// use default locale

		// log error first
		StringBuilder body = new StringBuilder();
		String friendlyFileName;
		body.append("Sehr geehrte Damen und Herren,\n\n");
		if( exception != null ){
			// unwrap
			if( exception instanceof CompletionException ){
				exception = exception.getCause();
			}
			// log error
			log.log(Level.WARNING, "Scheduled report generation failed: "+report.getId(), exception);
			// build email message
			body.append("der aktuelle Monatsbericht konnte leider nicht erzeugt werden.\n");
			if( exception instanceof InsufficientDataException ){
				body.append("Grund dafür ist eine unzureichende Anzahl an Patienten im Berichtszeitraum.\n");				
			}else{
				body.append("Nachfolgend finden Sie die Fehlerbeschreibung.\n");
				body.append("Bitte leiten Sie diesen Fehler an it-support@aktin.org weiter.\n\n");
				// append stack trace
				appendStacktrace(body, exception);
				body.append('\n');
			}
			friendlyFileName = null;
		}else{
			// build email message
			body.append("anbei erhalten Sie den aktuellen Monatsbericht.\n");
			body.append("Der Berichtszeitraum erstreckt sich von ");
			LocalDateTime localStart = report.getStartTimestamp().atZone(timeZone).toLocalDateTime();
			body.append(localStart.toString());
			body.append(" bis ");
			body.append(report.getEndTimestamp().atZone(timeZone).toLocalDateTime().toString());
			body.append(".\n");
			Instant ts = report.getDataTimestamp();
			if( ts != null ){
				body.append("Datenstand des Berichts ist "+ts.atZone(timeZone).toLocalDateTime().toString());
				body.append(".\n");
			}
			// use start timestamp to generate human readable name
			friendlyFileName = MonthlyReportDataSource.createFriendlyFileName(localStart.getMonth(), report.getId(), locale);
		}
		body.append("\nMit freundlichen Grüßen,\n");
		body.append("Ihr lokaler AKTIN-Server\n");
		MimeMessage msg = new MimeMessage(mailSession);
		msg.setRecipients(RecipientType.TO, emailRecipients);
		msg.setReplyTo(replyTo);
		msg.setSubject("AKTIN Monatsbericht");
		msg.setSentDate(new Date());
		MimeMultipart mp = new MimeMultipart();
		// add text body part
		MimeBodyPart bp = new MimeBodyPart();
		bp.setText(body.toString(), "UTF-8");
		mp.addBodyPart(bp);
		if( report != null && report.getStatus() == Status.Completed ){
			// set attachment
			bp = new MimeBodyPart();
			DataSource ds = new MonthlyReportDataSource(report, friendlyFileName);
			bp.setFileName(ds.getName());
			bp.setDataHandler(new DataHandler(ds));
			mp.addBodyPart(bp);
		}
		msg.setContent(mp);
		Transport.send(msg);
	}

	@Timeout
	private void timerCallback(Timer timer){
		String templateId = (String)timer.getInfo();
		
		Report report = schedule.get(timer);
		if( report == null ){
			// timer not found in schedule, something is wrong (persistent timer?)
			log.warning("Timer callback unable to find report in schedule. Aborting report generation: "+templateId);
			return;
		}
		log.info("Scheduled report timer triggered: "+report.getId());
		ReportInfo info = reportForPreviousMonth(report);
		sendReportViaEmail(info);

		log.info("Next scheduled report at "+timer.getNextTimeout());
	}
//
//	private void emailReport(ArchivedReport report) throws AddressException, MessagingException{
//		MimeMessage msg = new MimeMessage(mailSession);
//		
//		// sender address
//		Address[] replyTo = InternetAddress.parse(prefs.get(PreferenceKey.emailReplyTo));
//		msg.setReplyTo(replyTo);
//
//		msg.setRecipients(RecipientType.TO, emailRecipients);
//		msg.setSubject("AKTIN Monatsbericht");
//		msg.setSentDate(new Date());
//		msg.setText("Sehr geehrte Damen und Herren,\nanbei finden Sie den aktuellen AKTIN Monatsbericht.\nDiese Nachricht wurde automatisch erzeugt von Ihrem AKTIN Server");
//		MimeMultipart mp = new MimeMultipart(new FileDataSource(report.getLocation().toFile()));
//		msg.setContent(mp);
//		Transport.send(msg);
//	}

	// TODO methods to add/remove schedule entries

	// TODO flush config to database after change

}

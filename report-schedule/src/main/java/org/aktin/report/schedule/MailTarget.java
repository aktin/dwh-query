package org.aktin.report.schedule;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Date;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class MailTarget extends AbstractTarget{

	private Address[] recipients;
	
	public MailTarget(URI uri) throws AddressException{
		if( !uri.getScheme().equals("mailto") ){
			throw new IllegalArgumentException("mailto URI scheme required");
		}
		recipients = InternetAddress.parse(uri.getPath());
		// TODO parse target email addresses, subject line, etc.
	}
	@Override
	public void submit(Result result) throws IOException{
		try {
			sendEmail(result);
		} catch( NamingException e){
			throw new IOException("Email server not configured", e);
		} catch( MessagingException e){
			throw new IOException("Unable to send email message", e);
		}
	}
	private void sendEmail(Result result) throws NamingException, MessagingException{		
		InitialContext ic = new InitialContext();
		String snName = "java:comp/env/mail/aktin";
		Session session = (Session)ic.lookup(snName);
		// create email
		Message msg = new MimeMessage(session);
		msg.setSubject("AKTIN: Test report");
		msg.setSentDate(new Date());
		msg.setFrom();
		msg.setRecipients(Message.RecipientType.TO, recipients);
		// email body
		MimeBodyPart mbp = new MimeBodyPart();
		mbp.setText("See attachment.");
		Multipart mp = new MimeMultipart();
		mp.addBodyPart(mbp);
		// add attachment
		mbp = new MimeBodyPart();
		// XXX better use generic file name derived from report name
		mbp.setFileName(result.report.getFileName().toString());
		try {
			mbp.setDataHandler(new DataHandler(result.report.toUri().toURL()));
		} catch (MalformedURLException e) {
			throw new MessagingException("Unable to create URL for report path: "+result.report, e);
		}
		mp.addBodyPart(mbp);;
		// use multipart content
		msg.setContent(mp);		
		// send email
		Transport.send(msg);
	}

}

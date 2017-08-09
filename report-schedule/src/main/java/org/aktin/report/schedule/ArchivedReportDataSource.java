package org.aktin.report.schedule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.activation.DataSource;

import org.aktin.report.ArchivedReport;

class ArchivedReportDataSource implements DataSource{

	private ArchivedReport report;

	public ArchivedReportDataSource(ArchivedReport report){
		this.report = report;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Files.newInputStream(report.getLocation());
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getContentType() {
		return report.getMediaType();
	}

	@Override
	public String getName() {
		return getReportAttachmentFileName(report);
	}

	public static String getReportAttachmentFileName(ArchivedReport report){
		return report.getLocation().getFileName().toString();
	}

}

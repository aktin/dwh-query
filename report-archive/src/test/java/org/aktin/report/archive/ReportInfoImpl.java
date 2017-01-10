package org.aktin.report.archive;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.aktin.report.GeneratedReport;
import org.aktin.report.ReportInfo;

public class ReportInfoImpl implements ReportInfo, GeneratedReport{
	public Instant start;
	public Instant end;
	public String templateId;
	public String templateVersion;
	public Map<String,String> prefs;

	public String mediaType;
	public Instant dataTimestamp;

	public ReportInfoImpl(String templateId, String version){
		this.templateId = templateId;
		this.templateVersion = version;
		this.prefs = new HashMap<>();
	}
	@Override
	public Instant getStartTimestamp() {
		return start;
	}

	@Override
	public Instant getEndTimestamp() {
		return end;
	}

	@Override
	public String getTemplateId() {
		return templateId;
	}

	@Override
	public String getTemplateVersion() {
		return templateVersion;
	}

	@Override
	public Map<String, String> getPreferences() {
		return prefs;
	}
	@Override
	public String getMediaType() {
		return mediaType;
	}
	@Override
	public Path getLocation() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Instant getDataTimestamp() {
		return dataTimestamp;
	}

}

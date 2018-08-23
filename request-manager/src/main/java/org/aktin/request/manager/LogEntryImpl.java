package org.aktin.request.manager;

import java.time.Instant;

import org.aktin.broker.request.ActionLogEntry;
import org.aktin.broker.request.RequestStatus;

public class LogEntryImpl implements ActionLogEntry {
	private String userId;
	private Instant timestamp;
	private RequestStatus oldStatus;
	private RequestStatus newStatus;
	private String description;

	public LogEntryImpl(String userId, long timestamp, RequestStatus old, RequestStatus newStatus, String description) {
		this.userId = userId;
		this.timestamp = Instant.ofEpochMilli(timestamp);
		this.oldStatus = old;
		this.newStatus = newStatus;
		this.description = description;
	}
	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public Instant getTimestamp() {
		return timestamp;
	}

	@Override
	public RequestStatus getOldStatus() {
		return oldStatus;
	}

	@Override
	public RequestStatus getNewStatus() {
		return newStatus;
	}

	@Override
	public String getDescription() {
		return description;
	}

}

package org.aktin.dwh.optinout.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PatientEntryImpl implements PatientEntry {
	private PatientReference reference;
	private Participation participation;
	private String root;
	private String extension;
	private String sIC;
	private String user;
	private Instant timestamp;
	private String comment;
	private Integer i2b2PatientNum;
	private String ide;
}

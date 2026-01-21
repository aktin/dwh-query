package org.aktin.dwh.optinout.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aktin.dwh.optinout.Participation;
import org.aktin.dwh.optinout.PatientEntry;
import org.aktin.dwh.optinout.PatientReference;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PatientEntryImpl implements PatientEntry {
	private PatientReference reference;
	private Participation participation;
	private String idRoot;
	private String idExt;
	private String sIC;
	private String user;
	private Instant timestamp;
	private String comment;
	private Integer i2b2PatientNum;
}

package org.aktin.dwh.optinout;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class PatientEntryImpl implements PatientEntry {
	private StudyImpl study;
	private PatientReference reference;
	private Participation participation;
	private String idRoot;
	private String idExt;
	private String sIC;
	private String user;
	private Instant timestamp;
	private String comment;
	private Integer i2b2PatientNum;

	public boolean equalsId(PatientEntry other) {
		return this.study.getId().equals(other.getStudy().getId())
				&& reference.equals(other.getReference())
				&& idRoot.equals(other.getIdRoot())
				&& idExt.equals(other.getIdExt());
		
	}
}

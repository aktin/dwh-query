package org.aktin.dwh.optinout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PatientEntryImpl implements PatientEntry {
	private StudyImpl study;
	private PatientReference reference;
	private Participation participation;
	private String idRoot;
	private String idExt;
	@Setter
	private String sIC;
	@Setter
	private String user;
	@Setter
	private Instant timestamp;
	@Setter
	private String comment;
	@Setter
	private Integer i2b2PatientNum;


	PatientEntryImpl(StudyImpl study, PatientReference reference, String idRoot, String idExt, Participation participation) {
		this.study = study;
		this.reference = reference;
		this.idRoot = idRoot;
		this.idExt = idExt;
		this.participation = participation;
	}

	@Override
	public void delete(String user) throws FileNotFoundException, IOException {
		try( Connection dbc = study.manager.getConnection() ){
			// create audit trail
			PreparedStatement ps = dbc.prepareStatement("INSERT INTO optinout_audittrail(study_id,pat_ref,pat_root,pat_ext,action_user,action_timestamp,action,study_subject_id) SELECT study_id,pat_ref,pat_root,pat_ext,?,NOW(),CONCAT('D',optinout),study_subject_id FROM optinout_patients WHERE study_id=? AND pat_ref=? AND pat_root=? AND pat_ext=?");
			ps.setString(1, user);
			ps.setString(2, study.getId());
			ps.setString(3, StudyImpl.serializeReferenceType(reference));
			ps.setString(4, idRoot);
			ps.setString(5, idExt);
			int n = ps.executeUpdate();
			ps.close();
			if( n == 0 ) {
				// no row found for patient
				throw new FileNotFoundException("No rows for patient "+this);
			}
			// delete entry
			ps = dbc.prepareStatement("DELETE FROM optinout_patients WHERE study_id=? AND pat_ref=? AND pat_root=? AND pat_ext=?");
			ps.setString(1, study.getId());
			ps.setString(2, StudyImpl.serializeReferenceType(reference));
			ps.setString(3, idRoot);
			ps.setString(4, idExt);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	public boolean equalsId(PatientEntry other) {
		return this.study.getId().equals(other.getStudy().getId())
				&& reference.equals(other.getReference())
				&& idRoot.equals(other.getIdRoot())
				&& idExt.equals(other.getIdExt());
		
	}
}

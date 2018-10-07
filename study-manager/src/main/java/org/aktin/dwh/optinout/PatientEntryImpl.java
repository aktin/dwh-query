package org.aktin.dwh.optinout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PatientEntryImpl implements PatientEntry{
	private StudyImpl study;
	private PatientReference id_ref;
	private Participation opt;
	private String id_root;
	private String id_ext;
	String sic;
	String user;
	long timestamp;
	String comment;
	Integer i2b2_patient_num;

	public PatientEntryImpl(StudyImpl study, PatientReference id_ref, String id_root, String id_ext, Participation opt) {
		this.study = study;
		this.id_ref = id_ref;
		this.id_root = id_root;
		this.id_ext = id_ext;
		this.opt = opt;
	}
	@Override
	public Study getStudy() {return study;}

	@Override
	public Participation getParticipation() {return opt;}

	@Override
	public PatientReference getReference() {return id_ref;}

	@Override
	public String getIdRoot() {return id_root;}

	@Override
	public String getIdExt() {return id_ext;}

	@Override
	public String getSIC() {return sic;}

	@Override
	public String getUser() {return user;}

	@Override
	public String getComment() {return comment;}

	@Override
	public Integer getI2b2PatientNum() {return i2b2_patient_num;}

	@Override
	public void delete(String user) throws FileNotFoundException, IOException {
		try( Connection dbc = study.manager.getConnection() ){
			// create audit trail
			PreparedStatement ps = dbc.prepareStatement("INSERT INTO optinout_audittrail(study_id,pat_ref,pat_root,pat_ext,action_user,action_timestamp,action,study_subject_id) SELECT study_id,pat_ref,pat_root,pat_ext,?,NOW(),CONCAT('D',optinout),study_subject_id FROM optinout_patients WHERE study_id=? AND pat_ref=? AND pat_root=? AND pat_ext=?");
			ps.setString(1, user);
			ps.setString(2, study.getId());
			ps.setString(3, StudyImpl.serializeReferenceType(id_ref));
			ps.setString(4, id_root);
			ps.setString(5, id_ext);
			int n = ps.executeUpdate();
			ps.close();
			if( n == 0 ) {
				// no row found for patient
				throw new FileNotFoundException("No rows for patient "+this);
			}
			// delete entry
			ps = dbc.prepareStatement("DELETE FROM optinout_patients WHERE study_id=? AND pat_ref=? AND pat_root=? AND pat_ext=?");
			ps.setString(1, study.getId());
			ps.setString(2, StudyImpl.serializeReferenceType(id_ref));
			ps.setString(3, id_root);
			ps.setString(4, id_ext);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	@Override
	public long getTimestamp() {return timestamp;}

	public boolean equalsId(PatientEntry other) {
		return this.study.getId().equals(other.getStudy().getId())
				&& id_ref.equals(other.getReference())
				&& id_root.equals(other.getIdRoot())
				&& id_ext.equals(other.getIdExt());
		
	}
}

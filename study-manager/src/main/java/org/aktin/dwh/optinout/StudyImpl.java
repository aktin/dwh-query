package org.aktin.dwh.optinout;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.aktin.dwh.optinout.sic.CodeGenerator;


public class StudyImpl implements Study{
	private static final Logger log = Logger.getLogger(StudyImpl.class.getName());
	StudyManagerImpl manager;
	

	private String id;
	private String title;
	private String description;
	Instant createdTime;
	/** when the study was closed. {@code null} if still open */
	Instant closedTime;

	private boolean hasOptIn;
	private boolean hasOptOut;

	private boolean manualCodes;
	private CodeGenerator codeGen;

	private static final String SELECT_PATIENT_SQL = "SELECT pat_ref,pat_root,pat_ext,optinout,create_user,create_timestamp,study_subject_id,comment,i2b2_patient_num FROM optinout_patients WHERE study_id=?";

	StudyImpl(StudyManagerImpl manager, String id, String title, String description){
		this.manager = manager;
		this.id = id;
		this.title = title;
		this.description = description;
	}

	@Override
	public String getId() {return id;}
	@Override
	public String getTitle() {return title;}
	@Override
	public String getDescription() {return description;}
	@Override
	public String validateSIC(String sic) {
		// TODO Auto-generated method stub
		return null;
	}

	void setManualCodes(boolean manual) {
		this.manualCodes = manual;
	}
	void setCodeGenerator(CodeGenerator codeGen) {
		this.codeGen = codeGen;
	}
	
	void loadOptions(String db_options) {
		// options default to false
		hasOptIn = false;
		hasOptOut = false;

		String[] opts = db_options.split(",");
		for( int i=0; i<opts.length; i++ ) {
			String option = opts[i];
			int pos = option.indexOf('=');
			if( pos == -1 ) {
				throw new IllegalArgumentException("Ignoring option without '=': "+option);
			}
			String val = option.substring(pos+1);
			switch( option.substring(0, pos) ){
			case "OPT":
				// allow opt in or out
				if( val.contains("I") ) {
					hasOptIn = true;
				}
				if( val.contains("O") ) {
					hasOptOut = true;
				}
				break;
			}
		}
		
	}
	@Override
	public String generateSIC() throws UnsupportedOperationException, IllegalStateException, IOException {
		if( codeGen == null ) {
			throw new UnsupportedOperationException("Generation of SICs not supported by this study: "+getTitle());
		}
		String code = codeGen.generateCode();
		// write state
		try( Connection dbc = manager.getConnection();
				PreparedStatement ps = dbc.prepareStatement("UPDATE optinout_studies SET sic_generator_state=? WHERE id=?") )
		{
			ps.setString(1, codeGen.getState());
			ps.setString(2, getId());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new IOException("Unable to store SIC generator state for study "+getTitle(), e);
		}
		return code;
	}
	@Override
	public PatientEntryImpl getPatientBySIC(String sic) throws IOException {
		PatientEntryImpl pat;
		try( Connection dbc = manager.getConnection();
				PreparedStatement ps = dbc.prepareStatement(SELECT_PATIENT_SQL+" AND study_subject_id=?"))
		{
			ps.setString(1, id);
			ps.setString(2, sic);
			ResultSet rs = ps.executeQuery();
			if( rs.next() ) {
				pat = loadPatient(rs);
			}else {
				pat = null;
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return pat;
	}
	@Override
	public PatientEntryImpl getPatientByID(PatientReference ref, String id_root, String id_ext) throws IOException {
		PatientEntryImpl pat;
		id_root = trimIdPart(id_root);
		id_ext = trimIdPart(id_ext);
		try( Connection dbc = manager.getConnection();
				PreparedStatement ps = dbc.prepareStatement(SELECT_PATIENT_SQL+" AND pat_ref=? AND pat_root=? AND pat_ext=?"))
		{
			ps.setString(1, id);
			ps.setString(2, StudyImpl.serializeReferenceType(ref));
			ps.setString(3, id_root);
			ps.setString(4, id_ext);
			ResultSet rs = ps.executeQuery();
			if( rs.next() ) {
				pat = loadPatient(rs);
			}else {
				pat = null;
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return pat;
	}

	private PatientEntryImpl loadPatient(ResultSet rs) throws SQLException {
		PatientEntryImpl pat = new PatientEntryImpl(this, 
				unserializeReferenceType(rs.getString(1)), 
				rs.getString(2), rs.getString(3),
				unserializeParticipationType(rs.getString(4)));
		pat.user = rs.getString(5);
		pat.timestamp = rs.getTimestamp(6).getTime();
		pat.sic = rs.getString(7);
		pat.comment = rs.getString(8);
		int num = rs.getInt(9);
		if( rs.wasNull() ) {
			pat.i2b2_patient_num = null;
		}else {
			pat.i2b2_patient_num = num;
		}
		return pat;
	}
	@Override
	public List<PatientEntryImpl> allPatients() throws IOException {
		List<PatientEntryImpl> list;
		try( Connection dbc = manager.getConnection();
				PreparedStatement ps = dbc.prepareStatement(SELECT_PATIENT_SQL))
		{
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			list = new ArrayList<>();
			while( rs.next() ) {
				list.add(loadPatient(rs));
			}
			rs.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return list;
	}

	static String serializeReferenceType(PatientReference ref) {
		switch( ref ) {
		case Patient:
			return "PAT";
		case Visit:
			return "VIS";
		case Encounter:
			return "ENC";
		case Billing:
			return "BIL";
		}
		throw new IllegalStateException("Enum value not handled: "+ref);
	}
	static PatientReference unserializeReferenceType(String ref) {
		switch( ref ) {
		case "PAT":
			return PatientReference.Patient;
		case "VIS":
			return PatientReference.Visit;
		case "ENC":
			return PatientReference.Encounter;
		case "BIL":
			return PatientReference.Billing;
		}
		throw new IllegalStateException("Enum value not handled: "+ref);
	}
	static String serializeParticipationType(Participation par) {
		switch( par ) {
		case OptIn:
			return "I";
		case OptOut:
			return "O";
		}
		throw new IllegalStateException("Enum value not handled: "+par);
	}
	static Participation unserializeParticipationType(String par) {
		switch( par ) {
		case "I":
			return Participation.OptIn;
		case "O":
			return Participation.OptOut;
		}
		throw new IllegalStateException("Enum value not handled: "+par);
	}

	public static final String trimIdPart(String id) {
		if( id == null ) {
			return null;
		}
		String trimmed = id.trim();
		if( !trimmed.contentEquals(id) ) {
			log.warning("Whitespace removed from ID '"+id+"'");
			return trimmed;
		}else {
			return id;
		}
	}

	@Override
	public PatientEntry addPatient(PatientReference ref, String id_root, String id_ext, Participation opt, String sic,
			String comment, String user) throws IOException {
		Objects.requireNonNull(manager.getAnonymizer());
		id_root = trimIdPart(id_root);
		id_ext = trimIdPart(id_ext);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		String psn = manager.getAnonymizer().calculateAbstractPseudonym(id_root,id_ext);
		try( Connection dbc = manager.getConnection() )
		{
			// write user
			PreparedStatement ps = dbc.prepareStatement("INSERT INTO optinout_patients(study_id,pat_ref,pat_root,pat_ext,pat_psn,create_user,create_timestamp,optinout,study_subject_id,comment)VALUES(?,?,?,?,?,?,?,?,?,?)");
			
			ps.setString(1, getId());
			ps.setString(2, serializeReferenceType(ref));
			ps.setString(3, id_root);
			ps.setString(4, id_ext);
			ps.setString(5, psn);
			ps.setString(6, user);
			ps.setTimestamp(7, now);
			ps.setString(8, serializeParticipationType(opt));
			ps.setString(9, sic);
			ps.setString(10, comment);
			ps.executeUpdate();
			ps.close();

			// write audit trail
			ps = dbc.prepareStatement("INSERT INTO optinout_audittrail(study_id,pat_ref,pat_root,pat_ext,action_user,action_timestamp,action,study_subject_id,comment)VALUES(?,?,?,?,?,?,?,?,?)");
			ps.setString(1, getId());
			ps.setString(2, serializeReferenceType(ref));
			ps.setString(3, id_root);
			ps.setString(4, id_ext);
			ps.setString(5, user);
			ps.setTimestamp(6, now);
			ps.setString(7, "C"+serializeParticipationType(opt));
			ps.setString(8, sic);
			ps.setString(9, comment);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			throw new IOException("Unable to add patient to database", e);
		}
		PatientEntryImpl pat = new PatientEntryImpl(this, ref, id_root, id_ext, opt);
		pat.sic = sic;
		pat.timestamp = now.getTime();
		pat.comment = comment;
		pat.user = user;
		return pat;
	}

	@Override
	public boolean isParticipationSupported(Participation participation) {
		switch( participation ) {
		case OptIn:
			return hasOptIn;
		case OptOut:
			return hasOptOut;
		default:
			return false;
		}
	}

	@Override
	public boolean supportsManualSICs() {
		return manualCodes;
	}

	@Override
	public Instant getCreatedTimestamp() {
		return createdTime;
	}

	@Override
	public Instant getClosedTimestamp() {
		return closedTime;
	}
	
}

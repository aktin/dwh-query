package org.aktin.dwh.optinout;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.aktin.Preferences;
import org.aktin.dwh.Anonymizer;
import org.aktin.dwh.PreferenceKey;
import org.aktin.dwh.optinout.sic.CodeGeneratorFactory;


@Singleton
public class StudyManagerImpl implements StudyManager {
	private static final Logger log = Logger.getLogger(StudyManagerImpl.class.getName());
	private List<StudyImpl> studies;
	private DataSource ds;
	private CodeGeneratorFactory codeFactory;
	private Anonymizer anon;
	private Preferences prefs;
	private Executor executor; // will be filled via resource injection or setter method

	// TODO memory cache of pat_ref,pat_psn to use for lookup during CDAImport
	
	public StudyManagerImpl() {
		codeFactory = new CodeGeneratorFactory();
	}

	/**
	 * Set the preference interface.
	 * This class reads the following properties: {@link PreferenceKey#i2b2DatasourceCRC}, {@link PreferenceKey#i2b2Project} for patient/visit linking via i2b2.patient_ide/encounter_ide.
	 * @param prefs preferences
	 */
	@Inject
	public void setPreferences(Preferences prefs) {
		this.prefs = prefs;
	}

	/**
	 * Set the datasource to be used for persistence in the study manager.
	 * If this method is not called or {@code ds} is set to {@code null},
	 * then the preferences (from {@link #setPreferences(Preferences)}) will
	 * be used to obtain a connection to the i2b2 CRC database.
	 * @param ds data source
	 */
	public void setDataSource(DataSource ds){
		this.ds = ds;
	}

	/**
	 * Set the executor service for running asynchronous operations.
	 * E.g. syncBatch
	 * @param executor executor service
	 */
	@Resource
	public void setExecutor(ManagedExecutorService executor){
		this.executor = executor;
	}

	private void initializeDatabase() throws NamingException, SQLException, IOException {
		// make sure we have a database to work on
		if( this.ds == null ) {
			// not set via setDataSource, use preferences
			String dsName = prefs.get(PreferenceKey.i2b2DatasourceCRC);//"java:/QueryToolDemoDS";
			log.info("Using i2b2 database via "+dsName);
			// following lines can throw NamingException
			InitialContext ctx = new InitialContext();
			this.ds = (DataSource)ctx.lookup(dsName);
		}

		try( Connection dbc = this.ds.getConnection() ){
			DatabaseTableManager dbm = new DatabaseTableManager(dbc);
			dbm.checkAndCreateTables();
		}
	}

	/**
	 * Method for testing. Resets the database to its initial state.
	 * @throws SQLException  SQL error
	 * @throws IOException  IO error
	 */
	void resetDatabaseEmpty() throws IOException, SQLException {
		try( Connection dbc = this.ds.getConnection() ){
			DatabaseTableManager dbm = new DatabaseTableManager(dbc);
			dbm.dropAllTables();
		}
	}
	@PostConstruct
	public void prepareDatabase() {
		log.info("Initializing study manager");
		try {
			initializeDatabase();
		} catch ( IOException | SQLException | NamingException e) {
			throw new IllegalStateException("Unable to initialize study manager", e);
		}
	}
	public void addStudy(String id, String title, String description, String options, String sic_generate) throws SQLException {
		try( Connection dbc = this.ds.getConnection() ){
			new DatabaseTableManager(dbc).addStudy(id, title, description, options, sic_generate);
		}
	}

	/**
	 * Setter method inject an anonymizer implementation.
	 * The anonymiyer is needed to generate one-way-hashes 
	 * for the database lookup operations.
	 * @param anonymizer anonymiyer interface
	 */
	@Inject
	public void setAnonymizer(Anonymizer anonymizer) {
		this.anon = anonymizer;
	}
	Anonymizer getAnonymizer() {
		return anon;
	}
	Connection getConnection() throws SQLException {
		return ds.getConnection();
	}
	@Override
	public List<StudyImpl> getStudies() throws IOException {
		if( studies == null ) {
			// load studies
			List<StudyImpl> list = new ArrayList<>();
			try( Connection dbc = getConnection();
					Statement st = dbc.createStatement() ){
				
				ResultSet rs = st.executeQuery("SELECT id, title, description, created_ts, closed_ts, options, sic_generate, sic_generator_state, sic_validate FROM optinout_studies ORDER BY id");
				while( rs.next() ) {
					StudyImpl s = new StudyImpl(this, rs.getString(1), rs.getString(2), rs.getString(3));
					// find and initialize code generator
					String gen = rs.getString(7);
					if( gen == null ) {
						// no SIC codes
						s.setCodeGenerator(null);
						s.setManualCodes(false);
					}else if( gen.equals("MANUAL") ) {
						// only manual SICs
						s.setCodeGenerator(null);
						s.setManualCodes(true);
					}else {
						// use generated codes
						s.setManualCodes(false);
						s.setCodeGenerator(codeFactory.createInstance(gen, rs.getString(8)));
					}
					// load options
					s.loadOptions(rs.getString(6));
					// load timestamps
					s.createdTime = rs.getTimestamp(4).toInstant();
					Timestamp closed = rs.getTimestamp(5);
					if( closed != null ) {
						s.closedTime = closed.toInstant();
					}
					// TODO add validation rules etc.
					list.add(s);
				}
				rs.close();
			}catch( SQLException e ) {
				throw new IOException(e);
			}
			this.studies = list;
		}
		return studies;
	}

	@Override
	public void linkPatientEntriesToData() throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not yet implemented");
	}


	/**
	 * Synchronize a single patient entry with existing data
	 * @param ref reference type
	 * @param root id root
	 * @param ext id extension
	 * @return completable future
	 */
	public CompletableFuture<SyncResult> syncSingle(PatientReference ref, String root, String ext){
		CompletableFuture<SyncResult> cf = new CompletableFuture<>();
		
		// TODO implement
		switch( ref ) {
		case Billing:
			break;
		case Encounter:
			break;
		case Patient:
			break;
		case Visit:
			break;
		default:
			break;
		
		}
		
		// TODO insert/replace fact in dwh to say patient member of study
		/*
		 * UPDATE optinout_patients o SET i2b2_patient_num = pm.patient_num 
		 *  FROM i2b2demodata.patient_mapping 
		 *  WHERE o.i2b2_patient_num IS NULL 
		 *  	AND o.pat_ref='PAT'
		 * 		AND pm.patient_ide=o.pat_psn
		 * 		AND pm.patient_ide_source='XXX AKTIN';
		 */
		/*
		 * UPDATE optinout_patients o SET i2b2_patient_num = v.patient_num 
		 *  FROM i2b2demodata.encounter_mapping 
		 *  INNER JOIN visit_dimension v USING(encounter_num)
		 *  WHERE o.i2b2_patient_num IS NULL 
		 *  	AND o.pat_ref='ENC'
		 * 		AND pm.encounter_ide=o.pat_psn
		 * 		AND pm.encounter_ide_source='XXX AKTIN';
		 */
		/*
		 * CREATE TEMPORARY TABLE x AS SELECT DISTINCT f.patient_num, f.xxvalue psn
		 * FROM i2b2demodata.observation_fact f, optionout_patients o
		 * WHERE o.i2b2_patient_num IS NULL AND o.pat_ref=BIL AND f.concept_cd=XXX AND f.tval_char=o.pat_psn;
		 *
		 * UPDATE optinout_patients o SET i2b2_patient_num = x.patient_num 
		 * FROM x WHERE o.pat_ref='BIL' AND o.i2b2_patient_num IS NULL AND o.pat_psn=x.psn;
		 * 
		 * DELETE TEMPORARY TABLE x;
		 * 
		 * XXX TODO assign last visit timestamp, update last sync timestamp 
		 */

		// TODO insert/replace fact in dwh to say patient member of study
		
		cf.completeExceptionally(new UnsupportedOperationException("Not yet implemented"));
		return cf;
	}
	
	private PreparedStatement prepareLastVisitUpdate(Connection dbc, Study study) throws SQLException {
		return null;
	}
	
	private PreparedStatement prepareSyncStatement(Connection dbc, PatientReference ref, Study study, boolean skipLinked, String idRoot, String idExt) throws SQLException {
		Objects.requireNonNull(dbc);
		Objects.requireNonNull(ref);
		Objects.requireNonNull(prefs);
		ArrayList<String> args = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		switch( ref ) {
		case Patient:
			sql.append("UPDATE optinout_patients o SET o.i2b2_patient_num = pm.patient_num, o.i2b2_sync_time=NOW() ");
			sql.append("  FROM patient_mapping pm");
			sql.append(" WHERE o.pat_ref='PAT'");
			sql.append("  AND o.pat_psn=pm.patient_ide");
			sql.append("  AND pm.patient_ide_source=?"); // i2b2 project
			args.add(prefs.get(PreferenceKey.i2b2Project));
			break;
		case Encounter:
			sql.append("UPDATE optinout_patients o SET o.i2b2_patient_num = em.patient_num, o.i2b2_sync_time=NOW() ");
			sql.append("  FROM encounter_mapping em");
			sql.append(" WHERE o.pat_ref='ENC'");
			sql.append("  AND o.pat_psn=em.encounter_ide");
			sql.append("  AND pm.encounter_ide_source=?"); // i2b2 project
			args.add(prefs.get(PreferenceKey.i2b2Project));
			// TODO test what happens if more encounters are there for single patient
		case Billing:
			sql.append("UPDATE optinout_patients o SET o.i2b2_patient_num = f.patient_num, o.i2b2_sync_time=NOW() ");
			sql.append("  FROM observation_fact f");
			sql.append(" WHERE o.pat_ref='BIL'");
			sql.append("  AND f.concept_cd='AKTIN:Fallkennzeichen'");// TODO use LOINC or other standard code for AKTIN:Fallkennzeichen
			sql.append("  AND o.pat_psn=f.tval_char");
			// no reference to i2b2 project
			// TODO this will fail in cases where different patients use the same billing number (e.g. mother + child)
		case Visit:
			throw new UnsupportedOperationException("Not yet implemented"); // TODO use other number for visit identification
		}
		if( idRoot != null ) {
			// limit to single patient
			sql.append(" AND o.pat_psn=?");
			String psn = anon.calculatePatientPseudonym(idRoot, idExt);
			args.add( psn );
		}
		if( skipLinked == true ) {
			// limit to unlinked patients
			sql.append("  AND o.i2b2_patient_num IS NULL");
		}
		if( study != null ) {
			// limit to single study
			sql.append("  AND o.study_id=?");
			args.add( study.getId() );
		}
		
		PreparedStatement ps = dbc.prepareStatement(sql.toString());
		// fill arguments
		for( int i=0; i<args.size(); i++ ) {
			ps.setString(i+1, args.get(i));
		}
		
		return ps;
	}
	/**
	 * Synchronize all entries with the specified reference type and study.
	 * The {@code study} argument can be {@code null} to refer to all studies
	 * @param ref reference type, can be {@code null} to sync all types
	 * @param study study to which the synchronization will be limited. Can be {@code null} to sync all studies.
	 * @param skipLinked {@code false} to relink patients who were already linked previously. {@code true} to limit
	 * linking to patients without previous link
	 * @return completable future
	 */
	public CompletableFuture<SyncResult> syncBatch(PatientReference ref, Study study, boolean skipLinked) {
		// TODO implement
		return CompletableFuture.supplyAsync(() -> {
			SyncResult result = new SyncResult();
			try( Connection dbc = getConnection() ){

				if( ref != null ) {
					// run sync for single reference type
					PreparedStatement ps = prepareSyncStatement(dbc, ref, study, skipLinked, null, null);
					result.numLinked = ps.executeUpdate();
					ps.close();
				}else {
					// run sync for all reference types
					PreparedStatement ps;
					ps = prepareSyncStatement(dbc, PatientReference.Patient, study, skipLinked, null, null);
					result.numLinked += ps.executeUpdate();
					ps.close();
					ps = prepareSyncStatement(dbc, PatientReference.Encounter, study, skipLinked, null, null);
					result.numLinked += ps.executeUpdate();
					ps.close();
					ps = prepareSyncStatement(dbc, PatientReference.Billing, study, skipLinked, null, null);
					result.numLinked += ps.executeUpdate();
					ps.close();
				}
				
			} catch (SQLException e) {
				throw new CompletionException(e);
			}
			return result;
			
		}, this.executor);
	}

}

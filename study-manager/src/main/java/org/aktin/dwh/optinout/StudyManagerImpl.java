package org.aktin.dwh.optinout;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

	// TODO memory cache of pat_ref,pat_psn to use for lookup during CDAImport
	
	public StudyManagerImpl() {
		codeFactory = new CodeGeneratorFactory();
	}

	/**
	 * Set the preference interface.
	 * This class reads the following properties: {@link PreferenceKey#i2b2DatasourceCRC}
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
	@Resource(lookup="java:jboss/datasources/AktinDS")
	public void setDataSource(DataSource ds){
		this.ds = ds;
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
	
	/**
	 * Synchronize all entries with the specified reference type and study.
	 * The {@code study} argument can be {@code null} to refer to all studies
	 * @param ref reference type
	 * @param study study, can be {@code null}
	 * @return completable future
	 */
	public CompletableFuture<SyncResult> syncBatch(PatientReference ref, Study study){
		CompletableFuture<SyncResult> cf = new CompletableFuture<>();
		
		// TODO implement

		cf.completeExceptionally(new UnsupportedOperationException("Not yet implemented"));
		return cf;
	}

}

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

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.aktin.dwh.optinout.sic.CodeGeneratorFactory;


@Singleton
public class StudyManagerImpl implements StudyManager {
	private List<StudyImpl> studies;
	private DataSource ds;
	private CodeGeneratorFactory codeFactory;

	public StudyManagerImpl() {
		codeFactory = new CodeGeneratorFactory();
	}
//	@Inject
//	private Preferences prefs;

	@Resource(lookup="java:jboss/datasources/AktinDS")
	public void setDataSource(DataSource ds){
		this.ds = ds;
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
		// TODO insert/replace fact in dwh to say patient member of study

		cf.completeExceptionally(new UnsupportedOperationException("Not yet implemented"));
		return cf;
	}

}

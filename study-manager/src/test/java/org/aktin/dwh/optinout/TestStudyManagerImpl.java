package org.aktin.dwh.optinout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.aktin.dwh.db.TestDataSourcePlain;
import org.aktin.dwh.db.TestDatabasePlain;
import org.junit.Before;
import org.junit.Test;

import liquibase.exception.LiquibaseException;

import static org.junit.Assert.*;

public class TestStudyManagerImpl {
	TestDataSourcePlain ds;
	StudyManagerImpl sm;
	public TestStudyManagerImpl() throws SQLException {
	}
	@Before
	public void initializeDatabase() throws SQLException, LiquibaseException, IOException {
		ds = new TestDataSourcePlain(new TestDatabasePlain("study_mgr"));
		
		sm = new StudyManagerImpl();
		sm.setAnonymizer( s -> String.join("/", s) ) ;
		sm.setDataSource(ds);
		sm.resetDatabaseEmpty();
		sm.prepareDatabase();
	}
	@Test
	public void verifyLoadStudies() throws IOException {
		List<StudyImpl> list = sm.getStudies();
		assertEquals(2, list.size());
		StudyImpl s = list.get(0);
		assertEquals("AKTIN", s.getTitle());
		s = list.get(1);
		assertEquals("ENQuIRE", s.getTitle());
	}

	@Test
	public void verifyGenerateSIC() throws IOException {
		StudyImpl s = sm.getStudies().get(1);
		String code = s.generateSIC();
		assertEquals("1000", code);
		code = s.generateSIC();
		assertEquals("1001", code);
		// verify persistence
		// use new database connection and check if the sequence continues
		sm = null;
		sm = new StudyManagerImpl();
		sm.setDataSource(ds);
		s = sm.getStudies().get(1);
		code = s.generateSIC();
		assertEquals("1002", code);		
	}

	@Test
	public void addListDeletePatients() throws IOException {
		StudyImpl s = sm.getStudies().get(1);
		assertTrue(s.isParticipationSupported(Participation.OptIn));
		assertFalse(s.isParticipationSupported(Participation.OptOut));
		
		s.addPatient(PatientReference.Patient, "0", "0", Participation.OptIn, s.generateSIC(), "First patient", "TestUser1");

		// same patient should throw exception
		try {
			// even if non-id values are different
			s.addPatient(PatientReference.Patient, "0", "0", Participation.OptOut, s.generateSIC(), "test", "test");
			fail();
		}catch( IOException e ) {
			// user already present, duplicate key exception
		}
		// add second (different) patient
		s.addPatient(PatientReference.Patient, "0", "1", Participation.OptIn, s.generateSIC(), "Second patient", "TestUser1");

		// list patients
		List<PatientEntryImpl> list = s.allPatients();
		assertEquals(2, list.size());

		// delete first patient
		list.get(0).delete("TestUser2");
		// delete again should throw exception
		try {
			list.get(0).delete("TestUser3");
			fail();
		}catch( FileNotFoundException e ) {
			// entry was deleted previously
		}

		// list should have only one entry left
		assertEquals(1, s.allPatients().size());
	}

	@Test
	public void findPatientBySIC() throws IOException {
		StudyImpl s = sm.getStudies().get(1);
		s.addPatient(PatientReference.Patient, "0", "0", Participation.OptIn, "4321", null, "TestUser1");
		// retrieve patient
		PatientEntryImpl pat = s.getPatientBySIC("4321");
		assertNotNull(pat);
		assertEquals("0", pat.getIdExt());
		// try to find nonexisting patient
		assertNull( s.getPatientBySIC("4320"));
		
	}
}

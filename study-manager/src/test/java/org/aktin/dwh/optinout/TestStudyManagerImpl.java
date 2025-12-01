package org.aktin.dwh.optinout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.val;
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
		sm.addStudy("TEST", "Test", "Test study", "OPT=I", "SEQUENCE(1000,1)");
		
	}
	@Test
	public void verifyLoadStudies() throws IOException {
		List<StudyImpl> list = sm.getStudies();
		assertEquals(3, list.size());
		StudyImpl s = list.get(0);
		assertEquals("AKTIN", s.getTitle());
		s = list.get(1);
		assertEquals("Zertifizierung", s.getTitle());
		s = list.get(2);
		assertEquals("Test", s.getTitle());

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

		val patientEntryData1 = new PatientEntryData("0", "0", "0", "First patient", false, Participation.OptIn, PatientReference.Patient);
		val patientEntryData2 = new PatientEntryData("0", "1", null, "Second patient", true, Participation.OptIn, PatientReference.Patient);
		val patientEntryData3 = new PatientEntryData("0", "2", null, "Third patient", true, Participation.OptIn, PatientReference.Patient);

		s.addPatients(Collections.singletonList(patientEntryData1), "testuser");

		// same patient should throw exception
		try {
			// even if non-id values are different
			s.addPatients(Collections.singletonList(patientEntryData1), "testuser");
			fail();
		}catch( IOException e ) {
			// user already present, duplicate key exception
		}
		// add two (different) patients
		s.addPatients(Arrays.asList(patientEntryData2, patientEntryData3), "testuser1");

		// list patients
		List<PatientEntryImpl> list = s.allPatients();
		assertEquals(3, list.size());

		// delete first patient
		// delete again should throw exception
		try {
			val pat = list.get(0);
			s.deletePatient(pat.getReference(), pat.getIdRoot(), pat.getIdExt(), "testuser2");
			s.deletePatient(pat.getReference(), pat.getIdRoot(), pat.getIdExt(), "testuser2");
			fail();
		}catch( IOException e ) {
			// entry was deleted previously
			list.remove(0);
		}

		// list should have two entries left
		assertEquals(2, s.allPatients().size());
	}

	@Test
	public void findPatientBySIC() throws IOException {
		StudyImpl s = sm.getStudies().get(0);
		val patientEntryData1 = new PatientEntryData("0", "0", "4321", "First patient", false, Participation.OptIn, PatientReference.Patient);

		s.addPatients(Collections.singletonList(patientEntryData1), "testuser");
		// retrieve patient
		PatientEntryImpl pat = s.getPatientBySIC("4321");
		assertNotNull(pat);
		assertEquals("0", pat.getIdExt());
		// try to find nonexisting patient
		assertNull( s.getPatientBySIC("4320"));
		
	}
}

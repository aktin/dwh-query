package org.aktin.dwh.optinout;

import lombok.val;
import org.aktin.dwh.db.TestDataSourcePlain;
import org.aktin.dwh.db.TestDatabasePlain;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TestStudyManagerImpl {
	StudyRepository studyRepository;
	PatientRepository patientRepository;
	TestDataSourceProvider dsp;
	TestPatientReferenceService prs;

	@Before
	public void initializeDatabase() throws SQLException, IOException, NamingException {
//		// Nutze weiterhin die echte Test-DB für Integrationstests
		dsp = new TestDataSourceProvider(new TestDataSourcePlain(new TestDatabasePlain("study_mgr")));

		dsp.resetDatabaseEmpty();
		dsp.initDatabase();

		prs = new TestPatientReferenceService();

		studyRepository = new StudyRepository(dsp);
		patientRepository = new PatientRepository(dsp,
				s -> String.join("/", s),
				studyRepository,
				prs);

		studyRepository.addStudy("TEST", "Test", "Test study", "OPT=I", "SEQUENCE(1000,1)");
	}

	@Test
	public void verifyLoadStudies() throws SQLException {
		val list = studyRepository.getStudies();
		// On init database, there are two studies plus the test study added before running the test
		assertEquals(3, list.size());
		StudyImpl s = list.get(0);
		assertEquals("AKTIN", s.getTitle());
		s = list.get(1);
		assertEquals("Zertifizierung", s.getTitle());
		s = list.get(2);
		assertEquals("Test", s.getTitle());
	}

	@Test
	public void verifyGenerateSIC() throws SQLException, IOException {
		String studyId = "TEST";
		String code = studyRepository.generateSIC(studyId);
		assertEquals("1000", code);
		code = studyRepository.generateSIC(studyId);
		assertEquals("1001", code);
		// verify persistence
		// use new database connection and check if the sequence continues
		studyRepository = new StudyRepository(dsp);
		code = studyRepository.generateSIC(studyId);
		assertEquals("1002", code);
	}

	@Test
	public void addListDeletePatients() throws IOException, SQLException {
		String studyId = "TEST";
		StudyImpl s = studyRepository.getStudy(studyId);
		assertEquals(Participation.OptIn, s.getParticipation());
		assertNotEquals(Participation.OptOut, s.getParticipation());

		val patientEntryData1 = new PatientEntryData("0", prs.getRoot(PatientReference.Patient), "0", "First patient", false, Participation.OptIn, PatientReference.Patient);
		val patientEntryData2 = new PatientEntryData("0", prs.getRoot(PatientReference.Billing), null, "Second patient", true, Participation.OptIn, PatientReference.Billing);
		val patientEntryData3 = new PatientEntryData("0", prs.getRoot(PatientReference.Encounter), null, "Third patient", true, Participation.OptIn, PatientReference.Encounter);

		patientRepository.addPatients(studyId, Collections.singletonList(patientEntryData1), "testuser");

		// same patient should throw exception
		try {
			// even if non-id values are different
			patientRepository.addPatients(studyId, Collections.singletonList(patientEntryData1), "testuser");
			fail();
		}catch( SQLException e ) {
			// user already present, duplicate key exception
		}
		// add two (different) patients
		patientRepository.addPatients(studyId, Arrays.asList(patientEntryData2, patientEntryData3), "testuser1");

		// list patients
		List<PatientEntryImpl> list = patientRepository.getAllPatientsOfStudy(studyId);
		assertEquals(3, list.size());

		val pat = list.get(0);
		// delete first patient
		// delete again should throw exception
		try {
			patientRepository.deletePatient(studyId, pat.getReference(), pat.getIdExt(), "testuser2");
			patientRepository.deletePatient(studyId, pat.getReference(), pat.getIdExt(), "testuser2");
			fail();
		}catch( IllegalArgumentException e ) {

		}
	}
//
//	@Test
//	public void findPatientBySIC() throws IOException {
//		StudyImpl s = studyService.getStudies().get(0);
//		val patientEntryData1 = new PatientEntryData("0", "0", "4321", "First patient", false, Participation.OptIn, PatientReference.Patient);
//
//		s.addPatients(Collections.singletonList(patientEntryData1), "testuser");
//		// retrieve patient
//		PatientEntryImpl pat = s.getPatientBySIC("4321");
//		assertNotNull(pat);
//		assertEquals("0", pat.getIdExt());
//		// try to find nonexisting patient
//		assertNull( s.getPatientBySIC("4320"));
//
//	}
}

package org.aktin.dwh.optinout;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class PatientServiceImpl implements PatientService {
    private PatientRepository repository;

    @Inject
    PatientServiceImpl(PatientRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PatientEntry> getAllPatientsOfStudy(String studyId) throws IOException {
            try {
                return (List<PatientEntry>)(List<?>) repository.getAllPatientsOfStudy(studyId);
            } catch( SQLException e ) {
                throw new IOException(e);
            }
    }

    @Override
    public PatientEntryImpl getPatientByID(String studyId, PatientReference ref, String extension) throws IOException {
        try {
            return repository.getPatientByID(studyId, ref, extension);
        } catch( SQLException e ) {
            throw new IOException(e);
        }
    }

    @Override
    public void addPatients(String studyId, List<PatientEntryData> patientEntryData, String user) throws IOException {
        try {
            repository.addPatients(studyId, patientEntryData, user);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deletePatient(String studyId, PatientReference ref, String extension, String user) throws IOException {
        try {
            repository.deletePatient(studyId, ref, extension, user);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void updatePatient(String studyId, PatientReference ref, String extension, PatientEntryData newData, String user) throws IOException {
        try {
            repository.updatePatient(studyId, ref, extension, newData, user);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<PatientEncounter> getEncounters(PatientReference ref, String extension) throws IOException {
        try {
            return repository.getEncounters(ref, extension);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public PatientMasterData getMasterData(PatientReference ref, String extension) throws IOException {
        try {
            return repository.getMasterData(ref, extension);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}

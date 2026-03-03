package org.aktin.dwh.optinout.service;

import org.aktin.dwh.optinout.model.PatientEncounterPeriod;
import org.aktin.dwh.optinout.model.PatientMasterData;
import org.aktin.dwh.optinout.model.PatientEntry;
import org.aktin.dwh.optinout.model.PatientEntryData;
import org.aktin.dwh.optinout.model.PatientReference;
import org.aktin.dwh.optinout.repository.PatientRepository;

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
                return repository.getAllPatientsOfStudy(studyId);
            } catch( SQLException e ) {
                throw new IOException(e);
            }
    }

    @Override
    public PatientEntry getPatientByID(String studyId, PatientReference ref, String extension) throws IOException {
        try {
            return repository.getPatientByID(studyId, ref, extension);
        } catch( SQLException e ) {
            throw new IOException(e);
        }
    }

    @Override
    public void addPatientsToStudy(String studyId, List<PatientEntryData> patientEntryData, String user) throws IOException {
        try {
            repository.addPatientsToStudy(studyId, patientEntryData, user);
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
    public List<PatientEncounterPeriod> getEncounterPeriods(PatientReference ref, String extension) throws IOException {
        try {
            return repository.getEncounterPeriods(ref, extension);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public List<PatientEncounterPeriod> getEncounterPeriods(PatientReference ref, List<String> extensions) throws IOException {
        try {
            return repository.getEncounterPeriods(ref, extensions);
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

    @Override
    public List<PatientMasterData> getMasterData(PatientReference ref, List<String> extensions) throws IOException {
        try {
            return repository.getMasterData(ref, extensions);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}

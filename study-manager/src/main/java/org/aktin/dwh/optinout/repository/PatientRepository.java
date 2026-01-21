package org.aktin.dwh.optinout.repository;

import lombok.val;
import org.aktin.dwh.Anonymizer;
import org.aktin.dwh.optinout.model.*;
import org.aktin.dwh.optinout.util.DataSourceProvider;
import org.aktin.dwh.optinout.util.PatientReferenceService;
import org.aktin.dwh.optinout.util.QueryResolver;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class PatientRepository {
    private DataSourceProvider dsp;
    private Anonymizer anonymizer;
    private StudyRepository studyRepository;
    private PatientReferenceService referenceService;

    @Inject
    public PatientRepository(DataSourceProvider dsp,
                             Anonymizer anonymizer,
                             StudyRepository studyRepository,
                             PatientReferenceService referenceService) {
        this.dsp = dsp;
        this.anonymizer = anonymizer;
        this.studyRepository = studyRepository;
        this.referenceService = referenceService;
    }

    /**
     * Retrieves a patient entry from the database based on the given study ID,
     * patient reference, and extension identifier.
     *
     * @param studyId   the ID of the study to which the patient belongs; must not be null.
     * @param ref       the reference containing patient identification information; must not be null.
     * @param extension the extension identifier used to locate the specific patient entry; must not be null.
     * @return a {@code PatientEntryImpl} object representing the patient entry, or {@code null} if no matching entry is found.
     * @throws SQLException if an SQL error occurs while executing the query or interacting with the database.
     */
    public PatientEntryImpl getPatientByID(String studyId, PatientReference ref, String extension) throws SQLException {
        try (val dbc = dsp.getDataSource().getConnection();
             val ps = dbc.prepareStatement(QueryResolver.SQL_PATIENT_BY_ID)) {
            ps.setString(1, studyId);
            ps.setString(2, serializeReferenceType(ref));
            ps.setString(3, referenceService.getRoot(ref));
            ps.setString(4, extension);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return toPatientEntry(rs);
            } else {
                return null;
            }
        }
    }

    /**
     * Retrieves all patient entries associated with the specified study ID.
     *
     * @param studyId the identifier of the study for which patients are to be retrieved; must not be null.
     * @return a {@code List} of {@code PatientEntryImpl} objects representing all patients associated with the study.
     * @throws SQLException if an SQL error occurs while executing the query or interacting with the database.
     */
    public List<PatientEntry> getAllPatientsOfStudy(String studyId) throws SQLException {
        val list = new ArrayList<PatientEntry>();
        try (val dbc = dsp.getDataSource().getConnection();
             val ps = dbc.prepareStatement(QueryResolver.SQL_ALL_PATIENTS_BY_STUDY_ID)) {
            ps.setString(1, studyId);
            val rs = ps.executeQuery();
            while (rs.next()) {
                list.add(toPatientEntry(rs));
            }
        }
        return list;
    }

    /**
     * Adds a list of patient entries to the database for a specified study. The entries
     * are optionally processed to generate SIC codes if necessary. An audit trail
     * is created for each operation. The operation is performed within a transaction.
     *
     * @param studyId the ID of the study to which the patients belong; must not be null.
     * @param entries a list of {@code PatientEntryData} objects representing the patients
     *                to be added; must not be null or empty.
     * @param user    the username of the person performing the operation; must not be null.
     * @throws IOException  if an I/O error occurs during SIC generation or database operations.
     * @throws SQLException if an SQL error occurs while interacting with the database.
     */
    public void addPatientsToStudy(String studyId, List<PatientEntryData> entries, String user) throws IOException, SQLException {
        Objects.requireNonNull(anonymizer);
        val now = new Timestamp(System.currentTimeMillis());

        try (val dbc = dsp.getDataSource().getConnection()) {
            //turn auto commit off for transaction
            dbc.setAutoCommit(false);

            for (val entry : entries) {
                if (entry.isGenerateSic()) {
                    entry.setSic(studyRepository.generateSIC(studyId));
                }

                insertEntry(studyId, entry, user, now);
                writeAuditTrail(studyId, entry, user, now, DatabaseAction.CREATE);
            }
            dbc.commit();
        }
    }

    /**
     * Updates the patient entry in the database with the provided new data
     * and writes an audit trail entry for the update action. The operation
     * is performed within a transactional context.
     *
     * @param studyId   the ID of the study to which the patient belongs; must not be null
     * @param ref       the reference containing patient identification information; must not be null
     * @param extension the extension identifier used to locate the specific patient entry; must not be null
     * @param newData   the updated patient data to be applied; must not be null
     * @param user      the username of the person performing the update; must not be null
     * @throws SQLException if an SQL error occurs while updating the database or writing to the audit trail
     */
    public void updatePatient(String studyId, PatientReference ref, String extension, PatientEntryData newData, String user) throws SQLException {
        Objects.requireNonNull(anonymizer);
        val now = new Timestamp(System.currentTimeMillis());
        try (val dbc = dsp.getDataSource().getConnection()) {
            //turn auto commit off for transaction
            dbc.setAutoCommit(false);

            updateEntry(studyId, ref, extension, newData);
            writeAuditTrail(studyId, newData, user, now, DatabaseAction.UPDATE);

            dbc.commit();
        }
    }

    /**
     * Deletes a patient entry from the database, writes an audit trail for the deletion,
     * and commits the transaction. Before deleting, ensures the patient exists in the
     * database and retrieves necessary data for auditing purposes.
     *
     * @param studyId   the ID of the study to which the patient belongs; must not be null
     * @param ref       the reference containing patient identification information; must not be null
     * @param extension the extension identifier used to locate the specific patient entry; must not be null
     * @param user      the username of the person performing the delete operation; must not be null
     * @throws SQLException if an SQL error occurs during deletion or while interacting with the database
     * @throws IllegalArgumentException if the patient is not found based on the given parameters
     */
    public void deletePatient(String studyId, PatientReference ref, String extension, String user) throws SQLException {
        Objects.requireNonNull(anonymizer);
        val now = new Timestamp(System.currentTimeMillis());
        val root = referenceService.getRoot(ref);

        val pat = getPatientByID(studyId, ref, extension);
        if (pat == null) {
            throw new IllegalArgumentException("Patient not found");
        }
        // need patient data object for audit trail
        val data = new PatientEntryData(root, extension, pat.getSIC(), pat.getComment(), false, pat.getParticipation(), pat.getReference());

        try (val dbc = dsp.getDataSource().getConnection()) {
            dbc.setAutoCommit(false);
            deleteEntry(studyId, ref, extension);
            writeAuditTrail(studyId, data, user, now, DatabaseAction.DELETE);

            dbc.commit();
        }
    }

    /**
     * Retrieves a list of patient encounters based on the provided patient reference
     * and a single extension identifier.
     *
     * @param ref the reference object identifying the patient whose encounters are to be retrieved
     * @param extension the extension identifier used to filter the encounters
     * @return a list of {@code PatientEncounter} objects matching the given patient reference and extension
     * @throws SQLException if a database access error occurs
     */
    public List<PatientEncounter> getEncounters(PatientReference ref, String extension) throws SQLException {
        Objects.requireNonNull(anonymizer);

        return getEncounters(ref, Collections.singletonList(extension));
    }

    /**
     * Retrieves a list of patient encounters based on the provided patient reference and extensions.
     * The method utilizes anonymized patient pseudonyms derived from the reference and extensions
     * for querying the database.
     *
     * @param ref The patient reference object used to identify the root patient record.
     * @param extensions A list of string extensions used to calculate patient pseudonyms.
     * @return A list of {@code PatientEncounter} objects representing the encounters for the patient.
     * @throws SQLException If a database access error occurs during the retrieval process.
     */
    public List<PatientEncounter> getEncounters(PatientReference ref, List<String> extensions) throws SQLException {
        Objects.requireNonNull(anonymizer);
        val root = referenceService.getRoot(ref);
        val pseudonyms = extensions.stream().map(e -> anonymizer.calculatePatientPseudonym(root, e)).collect(Collectors.toList());
        try (val dbc = dsp.getDataSource().getConnection();
             val ps = dbc.prepareStatement(QueryResolver.resolveEncounterQueryByReference(ref, extensions.size()))) {

            for (int i = 0; i < pseudonyms.size(); i++) {
                ps.setString(i, pseudonyms.get(i));
            }

            val rs = ps.executeQuery();
            val encounters = new ArrayList<PatientEncounter>();
            while (rs.next()) {
                val encounter = new PatientEncounterImpl(
                        rs.getString(1),
                        rs.getTimestamp(3).toInstant(),
                        rs.getTimestamp(4).toInstant());
                encounters.add(encounter);
            }
            return encounters;
        }
    }

    /**
     * Retrieves the master data of a patient using the given reference and extensions.
     *
     * This method resolves patient pseudonyms based on the reference and extensions,
     * then executes a query to fetch the corresponding master data from the database.
     *
     * @param ref        The patient reference used to identify the patient.
     * @param extensions A list of extensions used to calculate the patient pseudonyms.
     * @return A list of {@code PatientMasterData} objects containing the retrieved master data.
     * @throws SQLException If a database access error occurs.
     */
    public List<PatientMasterData> getMasterData(PatientReference ref, List<String> extensions) throws SQLException {
        Objects.requireNonNull(anonymizer);
        val root = referenceService.getRoot(ref);
        val pseudonyms = extensions.stream().map(e ->anonymizer.calculatePatientPseudonym(root, e)).collect(Collectors.toList());
        try (val dbc = dsp.getDataSource().getConnection();
             val ps = dbc.prepareStatement(QueryResolver.resolveMasterDataQueryByReference(ref))) {
            for (int i = 0; i < pseudonyms.size(); i++) {
                ps.setString(i+1, pseudonyms.get(i));
            }
            val rs = ps.executeQuery();
            val masterData = new ArrayList<PatientMasterData>();
            while (rs.next()) {
                val data = new PatientMasterDataImpl(
                        rs.getString(1),
                        rs.getTimestamp(2).toInstant(),
                        rs.getString(4),
                        rs.getString(3));
                masterData.add(data);
            }

            return masterData;
        }
    }

    /**
     * Retrieves the master data for a specific patient reference and extension.
     *
     * @param ref       the patient reference used to identify the patient.
     * @param extension the specific extension used to filter the master data.
     * @return the patient master data if available, or null if no data is found.
     * @throws SQLException if a database access error occurs.
     */
    public PatientMasterData getMasterData(PatientReference ref, String extension) throws SQLException {
        val masterData = getMasterData(ref, Collections.singletonList(extension));
        if (masterData.isEmpty()) {
            return null;
        }
        return masterData.get(0);
    }

    // CRUD operation helpers

    /**
     * Loads a patient entry from the given result set by extracting and transforming data
     * into a {@code PatientEntryImpl} object.
     *
     * @param rs the result set from which to load the patient entry; must not be null
     * @return a {@code PatientEntryImpl} object initialized with data from the result set
     * @throws SQLException if an SQL error occurs while accessing the result set data
     */
    private PatientEntryImpl toPatientEntry(ResultSet rs) throws SQLException {
        return new PatientEntryImpl(unserializeReferenceType(rs.getString(1)),
                unserializeParticipationType(rs.getString(4)),
                rs.getString(2),
                rs.getString(3),
                rs.getString(7),
                rs.getString(5),
                rs.getTimestamp(6).toInstant(),
                rs.getString(8),
                rs.getInt(9));
    }

    /**
     * Inserts a new patient entry into the database.
     *
     * @param studyId          ID of the study to which the patient belongs
     * @param entry            patient entry data
     * @param user             user who performed the action
     * @param createdTimestamp timestamp of the action
     * @throws SQLException
     */
    private void insertEntry(String studyId, PatientEntryData entry, String user, Timestamp createdTimestamp) throws SQLException {
        val psn = anonymizer.calculateAbstractPseudonym(entry.getRoot(), entry.getExtension());
        try (val insertEntry = dsp.getDataSource().getConnection().prepareStatement(QueryResolver.SQL_INSERT_PATIENT)) {
            insertEntry.setString(1, studyId);
            insertEntry.setString(2, serializeReferenceType(entry.getReference()));
            insertEntry.setString(3, entry.getRoot());
            insertEntry.setString(4, entry.getExtension());
            insertEntry.setString(5, psn);
            insertEntry.setString(6, user);
            insertEntry.setTimestamp(7, createdTimestamp);
            insertEntry.setString(8, serializeParticipationType(entry.getParticipation()));
            insertEntry.setString(9, entry.getSic());
            insertEntry.setString(10, entry.getComment());
            insertEntry.executeUpdate();
        }
    }

    /**
     * Writes an audit trail entry for the given patient entry data to the database.
     *
     * @param studyId          ID of the study to which the patient belongs
     * @param entry            patient entry data
     * @param user             user who performed the action
     * @param createdTimestamp timestamp of the action
     * @param action           C(R)UD action performed
     * @throws SQLException
     */
    private void writeAuditTrail(String studyId, PatientEntryData entry, String user, Timestamp createdTimestamp, DatabaseAction action) throws SQLException {
        try (val insertAudit = dsp.getDataSource().getConnection().prepareStatement(QueryResolver.SQL_INSERT_AUDIT_TRAIL)) {
            insertAudit.setString(1, studyId);
            insertAudit.setString(2, serializeReferenceType(entry.getReference()));
            insertAudit.setString(3, entry.getRoot());
            insertAudit.setString(4, entry.getExtension());
            insertAudit.setString(5, user);
            insertAudit.setTimestamp(6, createdTimestamp);
            insertAudit.setString(7, action.toString() + serializeParticipationType(entry.getParticipation()));
            insertAudit.setString(8, entry.getSic());
            insertAudit.setString(9, entry.getComment());
            insertAudit.executeUpdate();
        }
    }

    /**
     * Updates an entry in the optinout_patients database table with new patient data.
     *
     * @param studyId   ID of the study to which the patient belongs
     * @param ref       the patient reference containing information about the patient
     * @param extension the extension identifier for the patient
     * @param newData   the new patient data to be updated in the database
     * @throws SQLException if there is an error executing the update query or interacting with the database
     */
    private void updateEntry(String studyId, PatientReference ref, String extension, PatientEntryData newData) throws SQLException {
        val root = referenceService.getRoot(ref);
        try (val dbc = dsp.getDataSource().getConnection();
             val ps = dbc.prepareStatement(QueryResolver.SQL_UPDATE_PATIENT)) {
            ps.setString(1, newData.getComment());
            ps.setString(2, studyId);
            ps.setString(3, serializeReferenceType(ref));
            ps.setString(4, root);
            ps.setString(5, extension);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes an entry from the 'optinout_patients' table based on the specified parameters.
     *
     * @param studyId   ID of the study to which the patient belongs
     * @param ref       The reference to the patient whose entry is to be deleted.
     * @param extension The extension identifier of the patient entry.
     * @throws SQLException If an SQL error occurs while executing the delete operation.
     */
    private void deleteEntry(String studyId, PatientReference ref, String extension) throws SQLException {
        val root = referenceService.getRoot(ref);
        try (val dbc = dsp.getDataSource().getConnection();
             val ps = dbc.prepareStatement(QueryResolver.SQL_DELETE_PATIENT)) {
            ps.setString(1, studyId);
            ps.setString(2, serializeReferenceType(ref));
            ps.setString(3, root);
            ps.setString(4, extension);
            ps.executeUpdate();
        }
    }


    // --- Enum Handling Logic (Optimized) ---

    private String serializeReferenceType(PatientReference ref) {
        if (ref == null) return null; // Handle nulls gracefully if needed
        return ref.getCode();
    }

    private PatientReference unserializeReferenceType(String ref) {
        if (ref == null) return null;
        return PatientReference.fromCode(ref);
    }

    private String serializeParticipationType(Participation par) {
        if (par == null) return null;
        return par.getCode();
    }

    private Participation unserializeParticipationType(String par) {
        if (par == null) return null;
        return Participation.fromCode(par);
    }

    /**
     * Simple enum for C(R)UD operations
     * Used to log what kind of database action regarding a patient entry was performed
     */
    private enum DatabaseAction {
        CREATE("C"),
        READ("R"), // if not used, here for completeness sake
        UPDATE("U"),
        DELETE("D");

        private final String action;

        DatabaseAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }
    }
}

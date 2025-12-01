package org.aktin.dwh.optinout;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.aktin.dwh.optinout.sic.CodeGenerator;


public class StudyImpl implements Study {
    private static final Logger log = Logger.getLogger(StudyImpl.class.getName());

    @Getter
    private StudyManagerImpl manager;

    @Getter
    private String id;
    @Getter
    private String title;
    @Getter
    private String description;
    @Getter
    private Instant createdTimestamp;
    @Getter
    private Instant closedTimestamp;

    @Getter
    Participation participation;
    @Getter
    private boolean isOptIn;
    @Getter
    private boolean isOptOut;

    @Getter
    @Setter
    private String sicGenerator;

    @Getter
    @Setter
    private String sicGeneratorState;

    @Getter
    @Setter
    private SICGeneration sicGeneration;

    @Setter
    private CodeGenerator codeGenerator;

    private static final String SELECT_PATIENT_SQL = "SELECT pat_ref,pat_root,pat_ext,optinout,create_user,create_timestamp,study_subject_id,comment,i2b2_patient_num FROM optinout_patients WHERE study_id=?";

    StudyImpl(StudyManagerImpl manager, String id, String title, String description, Instant createdTimestamp, Instant closedTimestamp) {
        this.manager = manager;
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdTimestamp = createdTimestamp;
        this.closedTimestamp = closedTimestamp;
    }

    void loadOptions(String dbOptions) {
        // options default to false
        isOptIn = false;
        isOptOut = false;

        String[] opts = dbOptions.split(",");
        for (int i = 0; i < opts.length; i++) {
            String option = opts[i];
            int pos = option.indexOf('=');
            if (pos == -1) {
                throw new IllegalArgumentException("Ignoring option without '=': " + option);
            }
            String val = option.substring(pos + 1);
            switch (option.substring(0, pos)) {
                case "OPT":
                    // allow opt in or out
                    if (val.contains("I")) {
                        isOptIn = true;
                    }
                    if (val.contains("O")) {
                        isOptOut = true;
                    }
                    break;
            }
        }

    }

    @Override
    public String generateSIC() throws UnsupportedOperationException, IllegalStateException, IOException {
        if (codeGenerator == null) {
            throw new UnsupportedOperationException("Generation of SICs not supported by this study: " + getTitle());
        }
        String code = codeGenerator.generateCode();
        // write state
        try (Connection dbc = manager.getConnection();
             PreparedStatement ps = dbc.prepareStatement("UPDATE optinout_studies SET sic_generator_state=? WHERE id=?")) {
            ps.setString(1, codeGenerator.getState());
            ps.setString(2, getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Unable to store SIC generator state for study " + getTitle(), e);
        }
        return code;
    }

    @Override
    public PatientEntryImpl getPatientBySIC(String sic) throws IOException {
        PatientEntryImpl pat;
        try (Connection dbc = manager.getConnection();
             PreparedStatement ps = dbc.prepareStatement(SELECT_PATIENT_SQL + " AND study_subject_id=?")) {
            ps.setString(1, id);
            ps.setString(2, sic);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                pat = loadPatient(rs);
            } else {
                pat = null;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
        return pat;
    }

    @Override
    public PatientEntryImpl getPatientByID(PatientReference ref, String idRoot, String idExt) throws IOException {
        PatientEntryImpl pat;
        try (Connection dbc = manager.getConnection();
             PreparedStatement ps = dbc.prepareStatement(SELECT_PATIENT_SQL + " AND pat_ref=? AND pat_root=? AND pat_ext=?")) {
            ps.setString(1, id);
            ps.setString(2, serializeReferenceType(ref));
            ps.setString(3, idRoot);
            ps.setString(4, idExt);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                pat = loadPatient(rs);
            } else {
                pat = null;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }

        return pat;
    }

    @Override
    public List<PatientEntryImpl> allPatients() throws IOException {
        List<PatientEntryImpl> list;
        try (Connection dbc = manager.getConnection();
             PreparedStatement ps = dbc.prepareStatement(SELECT_PATIENT_SQL)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            list = new ArrayList<>();
            while (rs.next()) {
                list.add(loadPatient(rs));
            }
            rs.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
        return list;
    }

    @Override
    public List<PatientEntry> addPatients(List<PatientEntryData> entries, String user) throws IOException {
        Objects.requireNonNull(manager.getAnonymizer());
        val now = new Timestamp(System.currentTimeMillis());

        try (val dbc = manager.getConnection()) {
            //turn auto commit off for transaction
            dbc.setAutoCommit(false);

            for(val entry : entries) {
                if (entry.isGenerateSic()) {
                    entry.setSic(generateSIC());
                }

                insertEntry(dbc, entry, user, now);
                writeAuditTrail(dbc, entry, user, now, DatabaseAction.INSERT);
            }
            dbc.commit();
        } catch (SQLException e) {
            throw new IOException("Unable to add patient to database", e);
        }

        List<PatientEntry> patients = new ArrayList<>();

        for (val entry : entries) {
            patients.add(getPatientByID(entry.getReference(), entry.getRoot(), entry.getExtension()));
        }

        return patients;
    }

    @Override
    public PatientEntry updatePatient(PatientReference ref, String root, String extension, PatientEntryData newData, String user) throws IOException {
        Objects.requireNonNull(manager.getAnonymizer());
        val now = new Timestamp(System.currentTimeMillis());
        try (val dbc = manager.getConnection()) {
            //turn auto commit off for transaction
            dbc.setAutoCommit(false);

            updateEntry(dbc, ref, root, extension, newData, user);
            writeAuditTrail(dbc, newData, user, now, DatabaseAction.UPDATE);

            dbc.commit();
        } catch (SQLException e) {
            throw new IOException("Unable to add patient to database", e);
        }

        val pat = getPatientByID(ref, root, extension);
        return pat;
    }

    @Override
    public void deletePatient(PatientReference ref, String root, String extension, String user) throws IOException {
        Objects.requireNonNull(manager.getAnonymizer());
        val now = new Timestamp(System.currentTimeMillis());

        val pat = getPatientByID(ref, root, extension);
        if(pat == null) {
            throw new IOException("Patient not found: "+ref+" "+root+" "+extension);
        }
        // need patient data for audit trail
        val data = new PatientEntryData(root, extension, pat.getSIC(), pat.getComment(), false, pat.getParticipation(), pat.getReference());

        try( Connection dbc = manager.getConnection() ){
            deleteEntry(dbc, ref, root, extension);
            writeAuditTrail(dbc, data, user, now, DatabaseAction.DELETE);
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isParticipationSupported(Participation participation) {
        switch (participation) {
            case OptIn:
                return isOptIn;
            case OptOut:
                return isOptOut;
            default:
                return false;
        }
    }


    @Override
    public List<PatientEncounter> loadEncounters(PatientReference ref, String root, String extension) throws IOException {
        String ide = manager.getAnonymizer().calculatePatientPseudonym(root, extension);
        try (Connection dbc = manager.getConnection();
             PreparedStatement ps = dbc.prepareStatement(QueryResolver.resolveEncounterQueryByReference(ref))) {
            ps.setString(1, ide);
            ResultSet rs = ps.executeQuery();
            List<PatientEncounter> encounters = new ArrayList<>();
            while (rs.next()) {
                PatientEncounter encounter = new PatientEncounterImpl(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getTimestamp(3).toInstant(),
                        rs.getTimestamp(4).toInstant()
                );
                encounters.add(encounter);
            }
            rs.close();

            return encounters;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public PatientMasterData loadMasterData(PatientReference ref, String root, String extension) throws IOException {
        String ide = manager.getAnonymizer().calculatePatientPseudonym(root, extension);
        try (Connection dbc = manager.getConnection();
             PreparedStatement ps = dbc.prepareStatement(QueryResolver.resolveMasterDataQueryByReference(ref))) {
            ps.setString(1, ide);
            ResultSet rs = ps.executeQuery();
            PatientMasterData masterData;
            if (rs.isBeforeFirst()) {
                rs.next();
                masterData = new PatientMasterDataImpl(
                        rs.getTimestamp(2).toInstant(),
                        rs.getString(4),
                        rs.getString(3),
                        rs.getInt(1)
                );
            } else {
                masterData = null;
            }
            rs.close();
            return masterData;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Map<PatientEntryData, List<ValidationResult>> validatePatients(List<PatientEntryData> entries) throws IOException {
        val validatedEntries = new HashMap<PatientEntryData, List<ValidationResult>>();

        for (val entry : entries) {
            val results = new ArrayList<ValidationResult>();
            if (!entry.isGenerateSic()) {
                results.add(validateSic(entry.getSic(), entries));
            }

            results.add(validateExtension(entry.getExtension(), entries));
            results.add(validatePatientId(entry.getReference(), entry.getRoot(), entry.getExtension()));
            results.add(validateEncounters(entry.getReference(), entry.getRoot(), entry.getExtension()));
            results.add(validateMasterData(entry.getReference(), entry.getRoot(), entry.getExtension()));

            // if results are valid, validation result is not added to the list
            validatedEntries.put(entry, results.stream().filter(r -> r != ValidationResult.VALID).collect(Collectors.toList()));
        }

        return validatedEntries;
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
    private PatientEntryImpl loadPatient(ResultSet rs) throws SQLException {
        PatientEntryImpl pat = new PatientEntryImpl(this,
                unserializeReferenceType(rs.getString(1)),
                unserializeParticipationType(rs.getString(4)),
                rs.getString(2),
                rs.getString(3),
                rs.getString(7),
                rs.getString(5),
                rs.getTimestamp(6).toInstant(),
                rs.getString(8),
                rs.getInt(9));
        return pat;
    }

    /**
     * Inserts a new patient entry into the database.
     *
     * @param dbc connection to the database
     * @param entry patient entry data
     * @param user user who performed the action
     * @param createdTimestamp timestamp of the action
     * @throws SQLException
     */
    private void insertEntry(Connection dbc, PatientEntryData entry, String user, Timestamp createdTimestamp) throws SQLException {
        val psn = manager.getAnonymizer().calculateAbstractPseudonym(entry.getRoot(), entry.getExtension());
        try (val insertEntry = dbc.prepareStatement("INSERT INTO optinout_patients(study_id,pat_ref,pat_root,pat_ext,pat_psn,create_user,create_timestamp,optinout,study_subject_id,comment)VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            insertEntry.setString(1, getId());
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
     * @param dbc database connection
     * @param entry patient entry data
     * @param user user who performed the action
     * @param createdTimestamp timestamp of the action
     * @param action C(R)UD action performed
     * @throws SQLException
     */
    private void writeAuditTrail(Connection dbc, PatientEntryData entry, String user, Timestamp createdTimestamp, DatabaseAction action) throws SQLException {
        try (val insertAudit = dbc.prepareStatement("INSERT INTO optinout_audittrail(study_id,pat_ref,pat_root,pat_ext,action_user,action_timestamp,action,study_subject_id,comment)VALUES(?,?,?,?,?,?,?,?,?)")) {
            insertAudit.setString(1, getId());
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
     * @param dbc        the database connection used for executing the update query
     * @param ref        the patient reference containing information about the patient
     * @param root       the root identifier for the patient
     * @param extension  the extension identifier for the patient
     * @param newData    the new patient data to be updated in the database
     * @param user       the user performing the update operation
     * @throws SQLException if there is an error executing the update query or interacting with the database
     */
    private void updateEntry(Connection dbc, PatientReference ref, String root, String extension, PatientEntryData newData, String user) throws SQLException {
        try (val updateStatement = dbc.prepareStatement("UPDATE optinout_patients\n" +
                "SET comment = ?\n" +
                "WHERE study_id = ? and pat_ref = ? and pat_root = ? and pat_ext = ?;")) {
            updateStatement.setString(1, newData.getComment());
            updateStatement.setString(2, getId());
            updateStatement.setString(3, serializeReferenceType(ref));
            updateStatement.setString(4, root);
            updateStatement.setString(5, extension);
            updateStatement.executeUpdate();
        }
    }

    /**
     * Deletes an entry from the 'optinout_patients' table based on the specified parameters.
     *
     * @param dbc       The database connection to be used for the operation.
     * @param ref       The reference to the patient whose entry is to be deleted.
     * @param root      The root identifier of the patient entry.
     * @param extension The extension identifier of the patient entry.
     * @throws SQLException If an SQL error occurs while executing the delete operation.
     */
    private void deleteEntry(Connection dbc, PatientReference ref, String root, String extension) throws SQLException {
        try (val ps = dbc.prepareStatement("DELETE FROM optinout_patients WHERE study_id=? AND pat_ref=? AND pat_root=? AND pat_ext=?")) {
            ps.setString(1, getId());
            ps.setString(2, serializeReferenceType(ref));
            ps.setString(3, root);
            ps.setString(4, extension);
            ps.executeUpdate();
        }
    }

    // Validation helper methods

    /**
     * Validates the given SIC (Study Identification Code) for uniqueness and existence.
     * Checks if the SIC is unique within the provided list of patient entry data
     * and whether it already exists in the database.
     *
     * @param sic the Study Identification Code to be validated; may be null
     * @param entries the list of patient entry data to check for SIC uniqueness
     * @return {@code ValidationResult.DUPLICATE_SIC} if the SIC is found more than once in the entries,
     *         {@code ValidationResult.SIC_FOUND} if the SIC already exists in the database,
     *         or {@code ValidationResult.VALID} if the SIC is valid and unique
     * @throws IOException if an error occurs while accessing the database
     */
    private ValidationResult validateSic(String sic, List<PatientEntryData> entries) throws IOException {
        if (sic != null) {
            if (entries.stream().filter(s -> Objects.equals(s.getSic(), sic)).count() > 1) {
                return ValidationResult.DUPLICATE_SIC;
            }

            if (getPatientBySIC(sic) != null) {
                return ValidationResult.SIC_FOUND;
            }
        }
        return ValidationResult.VALID;
    }

    /**
     * Validates the given extension for uniqueness within the list of patient entries.
     * If the extension appears more than once among the entries, it is considered a duplicate.
     *
     * @param extension the extension identifier to validate
     * @param entries the list of patient entry data to be checked for duplicates
     * @return {@code ValidationResult.DUPLICATE_PAT_REF} if the extension is found more than once,
     *         otherwise {@code ValidationResult.VALID}
     */
    private ValidationResult validateExtension(String extension, List<PatientEntryData> entries) {
        if (entries.stream().filter(e -> Objects.equals(e.getExtension(), extension)).count() > 1) {
            return ValidationResult.DUPLICATE_PAT_REF;
        }
        return ValidationResult.VALID;
    }

    /**
     * Validates the patient identifier by checking if a patient with the given details exists.
     *
     * @param ref the reference type of the patient
     * @param root the root identifier for the patient
     * @param extension the extension identifier for the patient
     * @return {@code ValidationResult.ENTRY_FOUND} if a patient matching the given identifiers exists,
     *         otherwise {@code ValidationResult.VALID}
     * @throws IOException if an error occurs while accessing the patient information
     */
    private ValidationResult validatePatientId(PatientReference ref, String root, String extension) throws IOException {
        if (getPatientByID(ref, root, extension) != null) {
            return ValidationResult.ENTRY_FOUND;
        }
        return ValidationResult.VALID;
    }

    /**
     * Validates whether encounters exist for the given patient reference and identifiers.
     * If no encounters are found, a validation result indicating their absence is returned.
     *
     * @param ref the reference type of the patient
     * @param root the root identifier for the patient
     * @param extension the extension identifier for the patient
     * @return {@code ValidationResult.ENCOUNTERS_NOT_FOUND} if no encounters are found,
     *         {@code ValidationResult.VALID} if encounters are found
     * @throws IOException if an error occurs while loading patient encounters
     */
    private ValidationResult validateEncounters(PatientReference ref, String root, String extension) throws IOException {
        val encounters = loadEncounters(ref, root, extension);

        if (encounters.isEmpty()) {
            return ValidationResult.ENCOUNTERS_NOT_FOUND;
        }

        return ValidationResult.VALID;
    }

    /**
     * Validates the existence of master data for the given patient reference and identifiers.
     * If no master data is found, a specific validation result is returned.
     *
     * @param ref the reference type of the patient
     * @param root the root identifier for the patient
     * @param extension the extension identifier for the patient
     * @return {@code ValidationResult.MASTER_DATA_NOT_FOUND} if no master data is found,
     *         or {@code ValidationResult.VALID} if the master data exists
     * @throws IOException if an error occurs while accessing the master data
     */
    private ValidationResult validateMasterData(PatientReference ref, String root, String extension) throws IOException {
        val masterdata = loadMasterData(ref, root, extension);

        if (masterdata == null) {
            return ValidationResult.MASTER_DATA_NOT_FOUND;
        }

        return ValidationResult.VALID;
    }


    // --- Enum Handling Logic (Optimized) ---

    private static final Map<PatientReference, String> REF_TO_STRING;
    private static final Map<String, PatientReference> STRING_TO_REF;
    private static final Map<Participation, String> PAR_TO_STRING;
    private static final Map<String, Participation> STRING_TO_PAR;

    static {
        // Initialize Reference Maps
        Map<PatientReference, String> rMap = new EnumMap<>(PatientReference.class);
        rMap.put(PatientReference.Patient, "PAT");
        rMap.put(PatientReference.Visit, "VIS");
        rMap.put(PatientReference.Encounter, "ENC");
        rMap.put(PatientReference.Billing, "BIL");
        REF_TO_STRING = Collections.unmodifiableMap(rMap);

        Map<String, PatientReference> sRefMap = new HashMap<>();
        for (Map.Entry<PatientReference, String> entry : REF_TO_STRING.entrySet()) {
            sRefMap.put(entry.getValue(), entry.getKey());
        }
        STRING_TO_REF = Collections.unmodifiableMap(sRefMap);

        // Initialize Participation Maps
        Map<Participation, String> pMap = new EnumMap<>(Participation.class);
        pMap.put(Participation.OptIn, "I");
        pMap.put(Participation.OptOut, "O");
        PAR_TO_STRING = Collections.unmodifiableMap(pMap);

        Map<String, Participation> sParMap = new HashMap<>();
        for (Map.Entry<Participation, String> entry : PAR_TO_STRING.entrySet()) {
            sParMap.put(entry.getValue(), entry.getKey());
        }
        STRING_TO_PAR = Collections.unmodifiableMap(sParMap);
    }

    // Instance methods for serialization to allow overriding or easier testing if needed later
    // but implementation uses static maps for performance.

    private String serializeReferenceType(PatientReference ref) {
        if (ref == null) return null; // Handle nulls gracefully if needed
        String val = REF_TO_STRING.get(ref);
        if (val == null) {
            throw new IllegalStateException("Enum value not handled: " + ref);
        }
        return val;
    }

    private PatientReference unserializeReferenceType(String ref) {
        if (ref == null) return null;
        PatientReference val = STRING_TO_REF.get(ref);
        if (val == null) {
            throw new IllegalStateException("Enum value not handled: " + ref);
        }
        return val;
    }

    private String serializeParticipationType(Participation par) {
        if (par == null) return null;
        String val = PAR_TO_STRING.get(par);
        if (val == null) {
            throw new IllegalStateException("Enum value not handled: " + par);
        }
        return val;
    }

    private Participation unserializeParticipationType(String par) {
        if (par == null) return null;
        Participation val = STRING_TO_PAR.get(par);
        if (val == null) {
            throw new IllegalStateException("Enum value not handled: " + par);
        }
        return val;
    }

    /**
     * Simple enum for C(R)UD operations
     * Used to log what kind of database action regarding a patient entry was performed
     */
    private enum DatabaseAction {
        INSERT("C"),
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

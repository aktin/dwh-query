package org.aktin.dwh.optinout;

import lombok.val;
import org.aktin.dwh.Anonymizer;

import javax.faces.bean.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class PatientValidatorImpl implements PatientValidator {
    private PatientRepository patientEntryRepository;
    private Anonymizer anonymizer;
    private PatientReferenceService referenceService;

    @Inject
    PatientValidatorImpl(PatientRepository patientEntryRepository,
                         Anonymizer anonymizer,
                         PatientReferenceService referenceService) {
        this.patientEntryRepository = patientEntryRepository;
        this.anonymizer = anonymizer;
        this.referenceService = referenceService;
    }

    @Override
    public Map<PatientEntryData, List<ValidationResult>> validatePatients(String studyId, List<PatientEntryData> patients) throws IOException {
        if (studyId == null) {
            throw new IllegalArgumentException("Study ID must not be null");
        }

        if (patients.isEmpty()) {
            throw new IllegalArgumentException("No patient entries provided");
        }

        try {
            // preload everything we need for validation
            val existingPatients = patientEntryRepository.getAllPatientsOfStudy(studyId);
            val existingSics = existingPatients.stream().map(PatientEntry::getSIC).collect(Collectors.toList());

            val ref = patients.get(0).getReference();
            val extensions = patients.stream().map(PatientEntryData::getExtension).collect(Collectors.toList());
            val masterData = patientEntryRepository.getMasterData(ref, extensions);
            val encounters = patientEntryRepository.getEncounters(ref, extensions);


            val validatedEntries = new HashMap<PatientEntryData, List<ValidationResult>>();
            for (val entry : patients) {
                val results = new ArrayList<ValidationResult>();
                if (!entry.isGenerateSic()) {
                    results.add(validateSic(entry.getSic(), patients, existingSics));
                }

                results.add(validateExtension(entry.getExtension(), patients));
                results.add(validatePatientId(entry.getReference(), entry.getExtension(), existingPatients));
                results.add(validateEncounters(entry.getReference(), entry.getExtension(), encounters));
                results.add(validateMasterData(entry.getReference(), entry.getExtension(), masterData));

                // if results are valid, validation result is not added to the list
                validatedEntries.put(entry, results.stream().filter(r -> r != ValidationResult.VALID).collect(Collectors.toList()));
            }

            return validatedEntries;
        } catch (SQLException ex) {
            throw new IOException(ex);
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
    private ValidationResult validateSic(String sic, List<PatientEntryData> entries, List<String> existingSics) {
        if (sic != null) {
            if (entries.stream().filter(s -> Objects.equals(s.getSic(), sic)).count() > 1) {
                return ValidationResult.DUPLICATE_SIC;
            }

            if (existingSics.stream().anyMatch(sic::equals)) {
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
     * @param ref              the reference type of the patient
     * @param extension        the extension identifier for the patient
     * @param existingPatients the list of existing patients to be checked
     * @return {@code ValidationResult.ENTRY_FOUND} if a patient matching the given identifiers exists,
     * otherwise {@code ValidationResult.VALID}
     * @throws IOException if an error occurs while accessing the patient information
     */
    private ValidationResult validatePatientId(PatientReference ref, String extension, List<PatientEntryImpl> existingPatients) {
        if (existingPatients.stream().anyMatch(p -> p.getReference().equals(ref) && p.getIdExt().equals(extension))) {
            return ValidationResult.ENTRY_FOUND;
        }
        return ValidationResult.VALID;
    }

    /**
     * Validates whether encounters exist for the given patient reference and identifiers.
     * If no encounters are found, a validation result indicating their absence is returned.
     *
     * @param ref the reference type of the patient
     * @param extension the extension identifier for the patient
     * @param encounters the list of patient encounters to be checked
     * @return {@code ValidationResult.ENCOUNTERS_NOT_FOUND} if no encounters are found,
     *         {@code ValidationResult.VALID} if encounters are found
     * @throws IOException if an error occurs while loading patient encounters
     */
    private ValidationResult validateEncounters(PatientReference ref, String extension, List<PatientEncounter> encounters) {
        val root = referenceService.getRoot(ref);
        val pseudonym = anonymizer.calculatePatientPseudonym(root, extension);
        if (encounters.stream().noneMatch(e -> Objects.equals(e.getPseudonym(), pseudonym))) {
            return ValidationResult.ENCOUNTERS_NOT_FOUND;
        }

        return ValidationResult.VALID;
    }

    /**
     * Validates the existence of master data for the given patient reference and identifiers.
     * If no master data is found, a specific validation result is returned.
     *
     * @param ref the reference type of the patient
     * @param extension the extension identifier for the patient
     * @param masterData the list of patient master data to be checked
     * @return {@code ValidationResult.MASTER_DATA_NOT_FOUND} if no master data is found,
     *         or {@code ValidationResult.VALID} if the master data exists
     * @throws IOException if an error occurs while accessing the master data
     */
    private ValidationResult validateMasterData(PatientReference ref, String extension, List<PatientMasterData> masterData) {
        val root = referenceService.getRoot(ref);
        val pseudonym = anonymizer.calculatePatientPseudonym(root, extension);
        if (masterData.stream().noneMatch(m -> Objects.equals(m.getPseudonym(), pseudonym))) {
            return ValidationResult.MASTER_DATA_NOT_FOUND;
        }

        return ValidationResult.VALID;
    }
}

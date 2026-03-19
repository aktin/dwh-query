package org.aktin.dwh.optinout.util;

import lombok.val;
import org.aktin.dwh.optinout.model.PatientReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QueryResolver {
    public static final String SQL_ALL_STUDIES = "SELECT id, title, description, created_ts, closed_ts, options, sic_generate, sic_generator_state, sic_validate FROM optinout_studies ORDER BY id";
    public static final String SQL_STUDY_BY_ID = "SELECT id, title, description, created_ts, closed_ts, options, sic_generate, sic_generator_state, sic_validate FROM optinout_studies WHERE id = ? LIMIT 1";
    public static final String SQL_UPDATE_SIC_STATE = "UPDATE optinout_studies SET sic_generator_state=? WHERE id=?";
    public static final String SQL_INSERT_STUDY = "INSERT INTO optinout_studies (id,title,description,created_ts,options,sic_generate) VALUES (?,?,?,NOW(),?,?)";
    public static final String SQL_ALL_PATIENTS_BY_STUDY_ID = "SELECT pat_ref,pat_root,pat_ext,optinout,create_user,create_timestamp,study_subject_id,comment,i2b2_patient_num FROM optinout_patients WHERE study_id=?";
    public static final String SQL_PATIENT_BY_ID = SQL_ALL_PATIENTS_BY_STUDY_ID + " AND pat_ref=? AND pat_root=? AND pat_ext=?";
    public static final String SQL_INSERT_PATIENT = "INSERT INTO optinout_patients(study_id,pat_ref,pat_root,pat_ext,pat_psn,create_user,create_timestamp,optinout,study_subject_id,comment)VALUES(?,?,?,?,?,?,?,?,?,?)";
    public static final String SQL_INSERT_AUDIT_TRAIL = "INSERT INTO optinout_audittrail(study_id,pat_ref,pat_root,pat_ext,action_user,action_timestamp,action,study_subject_id,comment)VALUES(?,?,?,?,?,?,?,?,?)";
    public static final String SQL_UPDATE_MUTABLE_PATIENT_COLUMNS = "UPDATE optinout_patients SET comment = ? WHERE study_id = ? and pat_ref = ? and pat_root = ? and pat_ext = ?";
    public static final String SQL_DELETE_PATIENT = "DELETE FROM optinout_patients WHERE study_id=? AND pat_ref=? AND pat_root=? AND pat_ext=?";
    public static final String SQL_COUNT_STUDIES = "SELECT COUNT(*) FROM optinout_studies";

    private static final String SQL_ENCOUNTER_PERIOD_BY_PATIENT_REF = "SELECT pm.patient_ide, vd.encounter_num, vd.start_date, vd.end_date " +
            "FROM i2b2crcdata.visit_dimension vd " +
            "JOIN i2b2crcdata.patient_mapping pm on vd.patient_num = pm.patient_num " +
            "WHERE pm.patient_ide IN ({0}) " +
            "ORDER BY vd.patient_num asc, vd.start_date desc";
    private static final String SQL_ENCOUNTER_PERIOD_BY_ENCOUNTER_REF = "SELECT em.encounter_ide, vd.encounter_num, vd.start_date, vd.end_date " +
            "FROM i2b2crcdata.visit_dimension vd " +
            "JOIN i2b2crcdata.encounter_mapping em on vd.encounter_num = em.encounter_num " +
            "WHERE em.encounter_ide IN ({0}) " +
            "ORDER BY vd.patient_num asc, vd.start_date desc";
    private static final String SQL_ENCOUNTER_PERIOD_BY_BILLING_REF = "SELECT o.tval_char, vd.encounter_num, vd.start_date, vd.end_date " +
            "FROM i2b2crcdata.visit_dimension vd " +
            "JOIN i2b2crcdata.observation_fact o on vd.patient_num = o.patient_num " +
            "WHERE o.concept_cd LIKE 'AKTIN:Fall%' " +
            "AND o.tval_char IN ({0}) " +
            "ORDER BY vd.patient_num asc, vd.start_date desc";
    private static final Map<PatientReference, String> ENCOUNTER_PERIOD_QUERIES;


    private static final String SQL_MASTER_DATA_BY_PATIENT_REF = "SELECT pm.patient_ide, pd.birth_date, o.tval_char, pd.sex_cd " +
            "FROM i2b2crcdata.patient_dimension pd " +
            "JOIN i2b2crcdata.patient_mapping pm ON pm.patient_num = pd.patient_num " +
            "JOIN i2b2crcdata.observation_fact o on pd.patient_num = o.patient_num " +
            "WHERE pm.patient_ide IN ({0}) " +
            "AND o.concept_cd = 'AKTIN:ZIPCODE'";
    private static final String SQL_MASTER_DATA_BY_ENCOUNTER_REF = "SELECT em.encounter_ide, pd.birth_date, o.tval_char, pd.sex_cd " +
            "FROM i2b2crcdata.patient_dimension pd " +
            "JOIN i2b2crcdata.visit_dimension vm ON vm.patient_num = pd.patient_num " +
            "JOIN i2b2crcdata.encounter_mapping em on vm.encounter_num = em.encounter_num " +
            "JOIN i2b2crcdata.observation_fact o on pd.patient_num = o.patient_num " +
            "WHERE em.encounter_ide IN ({0}) " +
            "AND o.concept_cd = 'AKTIN:ZIPCODE'";
    private static final String SQL_MASTER_DATA_BY_BILLING_REF = "SELECT o.tval_char, pd.birth_date, o2.tval_char, pd.sex_cd " +
            "FROM i2b2crcdata.patient_dimension pd " +
            "JOIN i2b2crcdata.observation_fact o on pd.patient_num = o.patient_num " +
            "JOIN i2b2crcdata.observation_fact o2 on pd.patient_num = o2.patient_num " +
            "WHERE o.concept_cd LIKE 'AKTIN:Fall%' " +
            "AND o2.concept_cd = 'AKTIN:ZIPCODE' " +
            "AND o.tval_char IN ({0})";
    private static final Map<PatientReference, String> MASTER_DATA_QUERIES;

    static {
        val encounterMap = new HashMap<PatientReference, String>();
        encounterMap.put(PatientReference.Encounter, SQL_ENCOUNTER_PERIOD_BY_ENCOUNTER_REF);
        encounterMap.put(PatientReference.Patient, SQL_ENCOUNTER_PERIOD_BY_PATIENT_REF);
        encounterMap.put(PatientReference.Billing, SQL_ENCOUNTER_PERIOD_BY_BILLING_REF);
        ENCOUNTER_PERIOD_QUERIES = Collections.unmodifiableMap(encounterMap);

        val masterDataMap = new HashMap<PatientReference, String>();
        masterDataMap.put(PatientReference.Encounter, SQL_MASTER_DATA_BY_ENCOUNTER_REF);
        masterDataMap.put(PatientReference.Patient, SQL_MASTER_DATA_BY_PATIENT_REF);
        masterDataMap.put(PatientReference.Billing, SQL_MASTER_DATA_BY_BILLING_REF);
        MASTER_DATA_QUERIES = Collections.unmodifiableMap(masterDataMap);
    }

    public static String resolveEncounterQueryByReference(PatientReference ref) {
        return resolveEncounterQueryByReference(ref, 1);
    }

    public static String resolveEncounterQueryByReference(PatientReference ref, int elementCount) {
        val sql = ENCOUNTER_PERIOD_QUERIES.get(ref);

        if (sql == null) {
            throw new IllegalArgumentException("Unknown ref: " + ref);
        }

        return insertPlaceholders(sql, elementCount);
    }

    public static String resolveMasterDataQueryByReference(PatientReference ref) {
        return resolveMasterDataQueryByReference(ref, 1);
    }

    public static String resolveMasterDataQueryByReference(PatientReference ref, int elementCount) {
        val sql = MASTER_DATA_QUERIES.get(ref);

        if (sql == null) {
            throw new IllegalArgumentException("Unknown ref: " + ref);
        }

        return insertPlaceholders(sql, elementCount);
    }

    private static String insertPlaceholders(String sql, int elementCount){
        if(elementCount <= 0){
            throw new IllegalArgumentException("elementCount must be greater than 0");
        }

        val placeholders = String.join(",", Collections.nCopies(elementCount, "?"));
        return sql.replace("{0}", placeholders);
    }
}

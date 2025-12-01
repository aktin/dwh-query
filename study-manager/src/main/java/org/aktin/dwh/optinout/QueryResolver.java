package org.aktin.dwh.optinout;

import lombok.val;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolve queries based on conditions (e.g. patient reference)
 */
class QueryResolver {
    private static final String SQL_ENCOUNTER_BY_PATIENT_REF = "SELECT vd.patient_num, vd.encounter_num, vd.start_date, vd.end_date " +
            "FROM i2b2.i2b2crcdata.visit_dimension vd " +
            "JOIN i2b2.i2b2crcdata.patient_mapping pm on vd.patient_num = pm.patient_num " +
            "WHERE pm.patient_ide = ? " +
            "ORDER BY vd.patient_num asc, vd.start_date desc";
    private static final String SQL_ENCOUNTER_BY_ENCOUNTER_REF = "SELECT pm.patient_num, vd.encounter_num, vd.start_date, vd.end_date " +
            "FROM i2b2crcdata.visit_dimension vd " +
            "JOIN i2b2crcdata.patient_mapping pm on vd.patient_num = pm.patient_num " +
            "JOIN i2b2crcdata.encounter_mapping em on vd.encounter_num = em.encounter_num " +
            "WHERE em.encounter_ide = ? " +
            "ORDER BY vd.patient_num asc, vd.start_date desc";
    private static final String SQL_ENCOUNTER_BY_BILLING_REF = "SELECT pm.patient_num, vd.encounter_num, vd.start_date, vd.end_date " +
            "FROM i2b2crcdata.visit_dimension vd " +
            "JOIN i2b2crcdata.observation_fact o on vd.patient_num = o.patient_num " +
            "JOIN i2b2crcdata.patient_mapping pm on vd.patient_num = pm.patient_num " +
            "WHERE o.concept_cd LIKE 'AKTIN:Fall%' " +
            "AND o.tval_char = ? " +
            "ORDER BY vd.patient_num asc, vd.start_date desc";
    private static final Map<PatientReference, String> ENCOUNTER_QUERIES;


    private static final String SQL_MASTER_DATA_BY_PATIENT_REF = "SELECT pd.patient_num, pd.birth_date, pd.zip_cd, pd.sex_cd " +
            "FROM i2b2.i2b2crcdata.patient_dimension pd " +
            "JOIN i2b2.i2b2crcdata.patient_mapping pm ON pm.patient_num = pd.patient_num " +
            "WHERE pm.patient_ide = ? " +
            "LIMIT 1";
    private static final String SQL_MASTER_DATA_BY_ENCOUNTER_REF = "SELECT pd.patient_num, pd.birth_date, pd.zip_cd, pd.sex_cd\n" +
            "FROM i2b2.i2b2crcdata.patient_dimension pd\n" +
            "         JOIN i2b2.i2b2crcdata.visit_dimension vm ON vm.patient_num = pd.patient_num\n" +
            "         JOIN i2b2crcdata.encounter_mapping em on vm.encounter_num = em.encounter_num\n" +
            "WHERE em.encounter_ide = ?\n" +
            "LIMIT 1;";
    private static final String SQL_MASTER_DATA_BY_BILLING_REF = "SELECT pd.patient_num, pd.birth_date, pd.zip_cd, pd.sex_cd\n" +
            "FROM i2b2.i2b2crcdata.patient_dimension pd\n" +
            "    JOIN i2b2crcdata.observation_fact o on pd.patient_num = o.patient_num\n" +
            "WHERE o.concept_cd LIKE 'AKTIN:Fall%'\n" +
            "  AND o.tval_char = ?\n" +
            "LIMIT 1;";
    private static final Map<PatientReference, String> MASTER_DATA_QUERIES;

    static {
        val encounterMap = new HashMap<PatientReference, String>();
        encounterMap.put(PatientReference.Encounter, SQL_ENCOUNTER_BY_ENCOUNTER_REF);
        encounterMap.put(PatientReference.Patient, SQL_ENCOUNTER_BY_PATIENT_REF);
        encounterMap.put(PatientReference.Billing, SQL_ENCOUNTER_BY_BILLING_REF);
        ENCOUNTER_QUERIES = Collections.unmodifiableMap(encounterMap);

        val masterDataMap = new HashMap<PatientReference, String>();
        masterDataMap.put(PatientReference.Encounter, SQL_MASTER_DATA_BY_ENCOUNTER_REF);
        masterDataMap.put(PatientReference.Patient, SQL_MASTER_DATA_BY_PATIENT_REF);
        masterDataMap.put(PatientReference.Billing, SQL_MASTER_DATA_BY_BILLING_REF);
        MASTER_DATA_QUERIES = Collections.unmodifiableMap(masterDataMap);
    }

    /**
     * Determine sql query to get encounters by reference
     *
     * @param ref Patient reference
     * @return sql query
     */
    public static String resolveEncounterQueryByReference(PatientReference ref) {
        String sql = ENCOUNTER_QUERIES.get(ref);
        if (sql == null) {
            throw new IllegalArgumentException("Unknown ref: " + ref);
        }
        return sql;
    }

    /**
     * Determine sql query to get master data by reference
     *
     * @param ref Patient reference
     * @return sql query
     */
    public static String resolveMasterDataQueryByReference(PatientReference ref) {
        String sql = MASTER_DATA_QUERIES.get(ref);
        if (sql == null) {
            throw new IllegalArgumentException("Unknown ref: " + ref);
        }
        return sql;
    }
}
